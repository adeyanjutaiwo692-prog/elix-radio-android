-- ELIX creator tools: comments, follower visibility, and owner-only upload deletion.
-- Run once in the Supabase SQL Editor.

create table if not exists public.media_comments (
    id uuid primary key default gen_random_uuid(),
    media_id uuid not null references public.media_items(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    author_name text not null default 'ELIX member',
    comment_text text not null check (char_length(trim(comment_text)) between 1 and 2000),
    created_at timestamptz not null default now()
);

create index if not exists media_comments_media_created_idx
    on public.media_comments(media_id, created_at desc);

alter table public.media_comments enable row level security;

drop policy if exists "comments public read" on public.media_comments;
create policy "comments public read"
    on public.media_comments for select
    using (true);

drop policy if exists "comments signed-in create" on public.media_comments;
create policy "comments signed-in create"
    on public.media_comments for insert
    with check (user_id = auth.uid());

drop policy if exists "comments owner delete" on public.media_comments;
create policy "comments owner delete"
    on public.media_comments for delete
    using (user_id = auth.uid() or public.is_super_admin());

grant select on public.media_comments to anon, authenticated;
grant insert, delete on public.media_comments to authenticated;

-- Followers are public counts; each member can only follow/unfollow themselves.
drop policy if exists "follows own" on public.ministry_follows;
drop policy if exists "follows public read" on public.ministry_follows;
create policy "follows public read"
    on public.ministry_follows for select
    using (true);

drop policy if exists "follows own create" on public.ministry_follows;
create policy "follows own create"
    on public.ministry_follows for insert
    with check (user_id = auth.uid());

drop policy if exists "follows own delete" on public.ministry_follows;
create policy "follows own delete"
    on public.ministry_follows for delete
    using (user_id = auth.uid());

grant select on public.ministry_follows to anon, authenticated;
grant insert, delete on public.ministry_follows to authenticated;

-- Let a creator delete only their own media catalogue rows.
drop policy if exists "media owner delete" on public.media_items;
create policy "media owner delete"
    on public.media_items for delete
    using (uploaded_by = auth.uid() or public.is_super_admin());

grant delete on public.media_items to authenticated;

-- Uploaded object paths are stored as <auth-user-uuid>/<filename>.
drop policy if exists "elix media owner delete" on storage.objects;
create policy "elix media owner delete"
    on storage.objects for delete
    using (
        bucket_id = 'elix-media'
        and (
            (storage.foldername(name))[1] = auth.uid()::text
            or public.is_super_admin()
        )
    );
