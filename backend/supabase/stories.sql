-- ELIX 24-hour stories
create table if not exists public.media_stories (
    id uuid primary key default gen_random_uuid(),
    media_id uuid references public.media_items(id) on delete cascade,
    file_path text not null,
    media_type text not null check (media_type in ('image','video')),
    title text not null default 'ELIX story',
    created_by uuid not null references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '24 hours')
);

create index if not exists media_stories_active_idx
    on public.media_stories (expires_at desc, created_at desc);

alter table public.media_stories enable row level security;

drop policy if exists "Anyone signed in can view active stories" on public.media_stories;
create policy "Anyone signed in can view active stories"
    on public.media_stories for select
    to authenticated
    using (expires_at > now());

drop policy if exists "Members can publish their own stories" on public.media_stories;
create policy "Members can publish their own stories"
    on public.media_stories for insert
    to authenticated
    with check (created_by = auth.uid());

drop policy if exists "Members can delete their own stories" on public.media_stories;
create policy "Members can delete their own stories"
    on public.media_stories for delete
    to authenticated
    using (created_by = auth.uid());

grant select, insert, delete on public.media_stories to authenticated;
