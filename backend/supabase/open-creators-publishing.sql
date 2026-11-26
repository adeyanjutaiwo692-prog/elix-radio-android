-- ELIX open creator publishing: every registered member can publish and build a public creator profile.
-- Run once in Supabase SQL Editor.

-- Creator profiles are the existing public profiles rows; no ministry approval is required.
drop policy if exists "profile read" on public.profiles;
create policy "profile read" on public.profiles for select using (true);

-- Members may update public profile fields; column-level grants prevent role changes.
drop policy if exists "profile update" on public.profiles;
create policy "profile update" on public.profiles for update
  using (id = auth.uid()) with check (id = auth.uid());

grant select on public.profiles to anon, authenticated;
grant update (display_name, avatar_url) on public.profiles to authenticated;

-- A creator follow is tied to an account, not a ministry page.
create table if not exists public.creator_follows (
  follower_id uuid not null references auth.users(id) on delete cascade,
  creator_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, creator_id),
  check (follower_id <> creator_id)
);
create index if not exists creator_follows_creator_idx on public.creator_follows(creator_id);
alter table public.creator_follows enable row level security;
drop policy if exists "creator follows public read" on public.creator_follows;
create policy "creator follows public read" on public.creator_follows for select using (true);
drop policy if exists "creator follows own insert" on public.creator_follows;
create policy "creator follows own insert" on public.creator_follows for insert with check (follower_id = auth.uid());
drop policy if exists "creator follows own delete" on public.creator_follows;
create policy "creator follows own delete" on public.creator_follows for delete using (follower_id = auth.uid());
grant select on public.creator_follows to anon, authenticated;
grant insert, delete on public.creator_follows to authenticated;

create or replace view public.creator_follower_counts as
select creator_id, count(*)::bigint as follower_count
from public.creator_follows group by creator_id;
grant select on public.creator_follower_counts to anon, authenticated;

-- Open publishing: signed-in creators' uploads are immediately published.
alter table public.media_items alter column status set default 'approved';
drop policy if exists "media insert" on public.media_items;
create policy "media insert" on public.media_items for insert
  with check (uploaded_by = auth.uid() and status = 'approved'
    and (ministry_id is null or public.is_ministry_member(ministry_id) or public.is_super_admin()));
grant insert on public.media_items to authenticated;

-- Every signed-in creator can upload into their own storage folder.
drop policy if exists "elix media owner upload" on storage.objects;
create policy "elix media owner upload" on storage.objects for insert to authenticated
  with check (bucket_id = 'elix-media' and (storage.foldername(name))[1] = auth.uid()::text);

-- Creators can delete only their own stored files.
drop policy if exists "elix media owner delete" on storage.objects;
create policy "elix media owner delete" on storage.objects for delete to authenticated
  using (bucket_id = 'elix-media' and
    ((storage.foldername(name))[1] = auth.uid()::text or public.is_super_admin()));
