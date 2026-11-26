-- ELIX personalised For You Page signals
create table if not exists public.media_engagement_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  media_id uuid not null references public.media_items(id) on delete cascade,
  event_type text not null check (event_type in ('watch','like','save','share','skip','not_interested')),
  watch_seconds integer not null default 0 check (watch_seconds >= 0 and watch_seconds <= 86400),
  created_at timestamptz not null default now()
);
create index if not exists media_engagement_user_created_idx on public.media_engagement_events(user_id, created_at desc);
create index if not exists media_engagement_media_idx on public.media_engagement_events(media_id);
alter table public.media_engagement_events enable row level security;
drop policy if exists "engagement own select" on public.media_engagement_events;
create policy "engagement own select" on public.media_engagement_events for select to authenticated using (user_id = auth.uid());
drop policy if exists "engagement own insert" on public.media_engagement_events;
create policy "engagement own insert" on public.media_engagement_events for insert to authenticated with check (user_id = auth.uid());
grant select, insert on public.media_engagement_events to authenticated;
