-- ELIX Radio: distinguish live audio from live video sources.
alter table public.live_streams
  add column if not exists stream_type text not null default 'video'
  check (stream_type in ('audio','video'));

grant select on table public.live_streams to anon, authenticated;
