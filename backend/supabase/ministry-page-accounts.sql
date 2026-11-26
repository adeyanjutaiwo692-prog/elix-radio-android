-- ELIX ministry page accounts and publishing studio.
-- Run once in Supabase SQL Editor.

alter table public.ministries
  add column if not exists page_type text not null default 'ministry'
  check (page_type in ('ministry','church','organisation','public_figure','author','artist'));

grant select, insert on public.ministries to authenticated;
drop policy if exists "ELIX page owners read own pages" on public.ministries;
create policy "ELIX page owners read own pages" on public.ministries
  for select to authenticated using (owner_id = auth.uid());

-- Automatically attach the creator as the page owner/member.
create or replace function public.elix_add_page_owner()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  if new.owner_id is not null then
    insert into public.ministry_members(ministry_id,user_id,member_role)
    values(new.id,new.owner_id,'owner')
    on conflict (ministry_id,user_id) do update set member_role='owner';
  end if;
  return new;
end;
$$;
drop trigger if exists elix_page_owner_membership on public.ministries;
create trigger elix_page_owner_membership
after insert on public.ministries
for each row execute function public.elix_add_page_owner();

-- The owner may view the page team; page members may not add themselves to other pages.
grant select on public.ministry_members to authenticated;
drop policy if exists "ELIX page owner reads team" on public.ministry_members;
create policy "ELIX page owner reads team" on public.ministry_members
  for select to authenticated using (
    exists(select 1 from public.ministries m
      where m.id = ministry_id and m.owner_id = auth.uid())
  );

-- Text posts and stories belong to the page account, not the member's personal profile.
create table if not exists public.ministry_publications (
  id uuid primary key default gen_random_uuid(),
  ministry_id uuid not null references public.ministries(id) on delete cascade,
  created_by uuid not null references auth.users(id) on delete cascade,
  content_type text not null check(content_type in ('post','story')),
  title text not null,
  body text not null,
  created_at timestamptz not null default now(),
  expires_at timestamptz
);
create index if not exists ministry_publications_page_created_idx
  on public.ministry_publications(ministry_id,created_at desc);
alter table public.ministry_publications enable row level security;
drop policy if exists "ELIX page publications public read" on public.ministry_publications;
create policy "ELIX page publications public read" on public.ministry_publications
  for select using (
    exists(select 1 from public.ministries m
      where m.id=ministry_id and m.approved=true)
    or public.is_ministry_member(ministry_id)
    or public.is_super_admin()
  );
drop policy if exists "ELIX page members publish" on public.ministry_publications;
create policy "ELIX page members publish" on public.ministry_publications
  for insert to authenticated with check (
    created_by=auth.uid() and public.is_ministry_member(ministry_id)
  );
drop policy if exists "ELIX page authors edit publication" on public.ministry_publications;
create policy "ELIX page authors edit publication" on public.ministry_publications
  for update to authenticated using (
    created_by=auth.uid() or public.is_super_admin()
  ) with check (
    created_by=auth.uid() or public.is_super_admin()
  );
drop policy if exists "ELIX page authors delete publication" on public.ministry_publications;
create policy "ELIX page authors delete publication" on public.ministry_publications
  for delete to authenticated using (
    created_by=auth.uid() or public.is_super_admin()
  );
grant select,insert,update,delete on public.ministry_publications to authenticated;
grant select on public.ministry_publications to anon;

-- Page managers may create broadcast entries, but cannot self-authorise a source.
drop policy if exists "ELIX page members create stream" on public.live_streams;
create policy "ELIX page members create stream" on public.live_streams
  for insert to authenticated with check (
    public.is_ministry_member(ministry_id) and is_authorised=false
  );
drop policy if exists "ELIX page members update own stream" on public.live_streams;
create policy "ELIX page members update own stream" on public.live_streams
  for update to authenticated using (public.is_ministry_member(ministry_id))
  with check (public.is_ministry_member(ministry_id) and is_authorised=false);
grant select,insert,update on public.live_streams to authenticated;

-- Page members can manage their own programme listings.
grant select,insert,update,delete on public.programmes to authenticated;
