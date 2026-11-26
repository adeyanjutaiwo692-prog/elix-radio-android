-- ELIX social interactions: comments, likes/watch signals and creator subscriptions.
-- Run this entire script once in the Supabase SQL Editor.

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
drop policy if exists "ELIX comments public read" on public.media_comments;
create policy "ELIX comments public read" on public.media_comments for select using (true);
drop policy if exists "ELIX comments signed in insert" on public.media_comments;
create policy "ELIX comments signed in insert" on public.media_comments for insert to authenticated
  with check (user_id = auth.uid());
drop policy if exists "ELIX comments owner delete" on public.media_comments;
create policy "ELIX comments owner delete" on public.media_comments for delete to authenticated
  using (user_id = auth.uid() or public.is_super_admin());
grant select on public.media_comments to anon, authenticated;
grant insert, delete on public.media_comments to authenticated;

-- Engagement events power likes, saves, shares and personalised discovery ranking.
create table if not exists public.media_engagement_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  media_id uuid not null references public.media_items(id) on delete cascade,
  event_type text not null check (event_type in ('like','save','share','skip','not_interested','watch')),
  watch_seconds integer not null default 0 check (watch_seconds >= 0),
  created_at timestamptz not null default now()
);
create index if not exists media_engagement_user_created_idx
  on public.media_engagement_events(user_id, created_at desc);
create index if not exists media_engagement_media_type_idx
  on public.media_engagement_events(media_id, event_type);
alter table public.media_engagement_events enable row level security;
drop policy if exists "ELIX engagement own read" on public.media_engagement_events;
create policy "ELIX engagement own read" on public.media_engagement_events for select to authenticated
  using (user_id = auth.uid());
drop policy if exists "ELIX engagement own insert" on public.media_engagement_events;
create policy "ELIX engagement own insert" on public.media_engagement_events for insert to authenticated
  with check (user_id = auth.uid());
grant select, insert on public.media_engagement_events to authenticated;

-- Free creator subscriptions are distinct from paid billing subscriptions.
create table if not exists public.creator_subscriptions (
  user_id uuid not null references auth.users(id) on delete cascade,
  creator_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, creator_id),
  check (user_id <> creator_id)
);
create index if not exists creator_subscriptions_creator_idx
  on public.creator_subscriptions(creator_id);
alter table public.creator_subscriptions enable row level security;
drop policy if exists "ELIX creator subscriptions read own" on public.creator_subscriptions;
create policy "ELIX creator subscriptions read own" on public.creator_subscriptions for select to authenticated
  using (user_id = auth.uid());
drop policy if exists "ELIX creator subscriptions insert own" on public.creator_subscriptions;
create policy "ELIX creator subscriptions insert own" on public.creator_subscriptions for insert to authenticated
  with check (user_id = auth.uid());
drop policy if exists "ELIX creator subscriptions delete own" on public.creator_subscriptions;
create policy "ELIX creator subscriptions delete own" on public.creator_subscriptions for delete to authenticated
  using (user_id = auth.uid());
grant select, insert, delete on public.creator_subscriptions to authenticated;
