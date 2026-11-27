-- ELIX Radio database. Run in Supabase SQL Editor.
create extension if not exists pgcrypto;
create type public.account_role as enum ('listener','ministry_admin','super_admin');
create type public.review_status as enum ('draft','pending','approved','rejected','archived');
create type public.media_kind as enum ('sermon','worship','conference','prayer','interview','bible_study','children_family','podcast','other');
create type public.stream_protocol as enum ('hls','rtmp','srt','youtube_embed');
create type public.stream_state as enum ('offline','live','checking','error');
create table public.profiles(id uuid primary key references auth.users(id) on delete cascade,display_name text,avatar_url text,role public.account_role not null default 'listener',created_at timestamptz not null default now());
create table public.ministries(id uuid primary key default gen_random_uuid(),name text not null,slug text not null unique,description text,logo_url text,country text not null default 'Nigeria',city text,website_url text,contact_email text,owner_id uuid references auth.users(id),approved boolean not null default false,created_at timestamptz not null default now());
create table public.ministry_members(ministry_id uuid not null references public.ministries(id) on delete cascade,user_id uuid not null references auth.users(id) on delete cascade,member_role text not null default 'editor' check(member_role in('owner','editor')),created_at timestamptz not null default now(),primary key(ministry_id,user_id));
create table public.media_items(id uuid primary key default gen_random_uuid(),ministry_id uuid references public.ministries(id) on delete set null,uploaded_by uuid not null references auth.users(id),title text not null,description text,kind public.media_kind not null default 'sermon',media_type text not null check(media_type in('audio','video','pdf','image')),file_path text,external_url text,thumbnail_url text,preacher text,topic text,recorded_at date,duration_seconds integer,status public.review_status not null default 'pending',rights_confirmed boolean not null default false,created_at timestamptz not null default now(),published_at timestamptz);
create table public.live_streams(id uuid primary key default gen_random_uuid(),ministry_id uuid not null references public.ministries(id) on delete cascade,title text not null,protocol public.stream_protocol not null,playback_url text,ingest_reference text,state public.stream_state not null default 'offline',is_authorised boolean not null default false,last_checked_at timestamptz,created_at timestamptz not null default now());
create table public.programmes(id uuid primary key default gen_random_uuid(),ministry_id uuid references public.ministries(id) on delete set null,title text not null,description text,starts_at timestamptz not null,ends_at timestamptz,stream_id uuid references public.live_streams(id) on delete set null,status text not null default 'scheduled' check(status in('scheduled','cancelled','completed')),created_at timestamptz not null default now());
create table public.ministry_follows(user_id uuid not null references auth.users(id) on delete cascade,ministry_id uuid not null references public.ministries(id) on delete cascade,created_at timestamptz not null default now(),primary key(user_id,ministry_id));
create table public.radio_schedule(id uuid primary key default gen_random_uuid(),title text not null,source_type text not null check(source_type in('live_stream','media_item','external_audio')),stream_id uuid references public.live_streams(id) on delete set null,media_id uuid references public.media_items(id) on delete set null,external_audio_url text,starts_at timestamptz not null,ends_at timestamptz,enabled boolean not null default true,created_at timestamptz not null default now());
create table public.subscriptions(id uuid primary key default gen_random_uuid(),user_id uuid not null references auth.users(id) on delete cascade,plan text not null,provider text,provider_reference text unique,status text not null default 'pending' check(status in('pending','active','cancelled','expired','failed')),starts_at timestamptz,ends_at timestamptz,created_at timestamptz not null default now());
create table public.notifications(id uuid primary key default gen_random_uuid(),user_id uuid not null references auth.users(id) on delete cascade,title text not null,body text not null,data jsonb not null default '{}'::jsonb,read_at timestamptz,created_at timestamptz not null default now());
create function public.is_super_admin() returns boolean language sql stable security definer set search_path=public as $$ select exists(select 1 from public.profiles where id=auth.uid() and role='super_admin') $$;
create function public.is_ministry_member(target uuid) returns boolean language sql stable security definer set search_path=public as $$ select exists(select 1 from public.ministry_members where ministry_id=target and user_id=auth.uid()) $$;
create function public.handle_new_user() returns trigger language plpgsql security definer set search_path=public as $$ begin insert into public.profiles(id,display_name,avatar_url) values(new.id,coalesce(new.raw_user_meta_data->>'full_name',new.raw_user_meta_data->>'name'),new.raw_user_meta_data->>'avatar_url') on conflict(id) do nothing; return new; end $$;
create trigger on_auth_user_created after insert on auth.users for each row execute procedure public.handle_new_user();
alter table public.profiles enable row level security; alter table public.ministries enable row level security; alter table public.ministry_members enable row level security; alter table public.media_items enable row level security; alter table public.live_streams enable row level security; alter table public.programmes enable row level security; alter table public.ministry_follows enable row level security; alter table public.radio_schedule enable row level security; alter table public.subscriptions enable row level security; alter table public.notifications enable row level security;
create policy "profile read" on public.profiles for select using(id=auth.uid() or public.is_super_admin()); create policy "profile update" on public.profiles for update using(id=auth.uid()) with check(id=auth.uid() and role<>'super_admin');
create policy "ministries read" on public.ministries for select using(approved or owner_id=auth.uid() or public.is_super_admin() or public.is_ministry_member(id)); create policy "ministry apply" on public.ministries for insert with check(owner_id=auth.uid()); create policy "ministry update" on public.ministries for update using(public.is_super_admin() or owner_id=auth.uid()) with check(public.is_super_admin() or owner_id=auth.uid());
create policy "members read" on public.ministry_members for select using(user_id=auth.uid() or public.is_super_admin() or public.is_ministry_member(ministry_id)); create policy "members admin" on public.ministry_members for all using(public.is_super_admin()) with check(public.is_super_admin());
create policy "media read" on public.media_items for select using(status='approved' or uploaded_by=auth.uid() or public.is_super_admin() or (ministry_id is not null and public.is_ministry_member(ministry_id))); create policy "media insert" on public.media_items for insert with check(uploaded_by=auth.uid() and (ministry_id is null or public.is_ministry_member(ministry_id) or public.is_super_admin())); create policy "media update" on public.media_items for update using(uploaded_by=auth.uid() or public.is_super_admin()) with check(uploaded_by=auth.uid() or public.is_super_admin());
create policy "streams read" on public.live_streams for select using((is_authorised and exists(select 1 from public.ministries where id=ministry_id and approved)) or public.is_super_admin() or public.is_ministry_member(ministry_id)); create policy "streams manage" on public.live_streams for all using(public.is_super_admin() or public.is_ministry_member(ministry_id)) with check(public.is_super_admin() or public.is_ministry_member(ministry_id));
create policy "programmes read" on public.programmes for select using(status<>'cancelled' or public.is_super_admin()); create policy "programmes manage" on public.programmes for all using(public.is_super_admin() or (ministry_id is not null and public.is_ministry_member(ministry_id))) with check(public.is_super_admin() or (ministry_id is not null and public.is_ministry_member(ministry_id)));
create policy "follows own" on public.ministry_follows for all using(user_id=auth.uid()) with check(user_id=auth.uid()); create policy "radio read" on public.radio_schedule for select using(enabled or public.is_super_admin()); create policy "radio admin" on public.radio_schedule for all using(public.is_super_admin()) with check(public.is_super_admin()); create policy "subscriptions read" on public.subscriptions for select using(user_id=auth.uid() or public.is_super_admin()); create policy "subscriptions admin" on public.subscriptions for all using(public.is_super_admin()) with check(public.is_super_admin()); create policy "notifications own" on public.notifications for all using(user_id=auth.uid()) with check(user_id=auth.uid() or public.is_super_admin());
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types) values('elix-media','elix-media',false,1073741824,array['audio/mpeg','audio/mp4','audio/aac','video/mp4','video/quicktime','application/pdf','image/jpeg','image/png','image/webp']),('elix-public','elix-public',true,10485760,array['image/jpeg','image/png','image/webp']) on conflict(id) do nothing;
-- Storage access policies will be added separately after the core database is installed.

-- Security hardening: only ELIX Super Admin may approve ministries/content or authorise broadcast sources.
drop policy if exists "ministry update" on public.ministries;
create policy "ministry admin update only" on public.ministries for update using(public.is_super_admin()) with check(public.is_super_admin());
drop policy if exists "media update" on public.media_items;
create policy "media approval admin only" on public.media_items for update using(public.is_super_admin()) with check(public.is_super_admin());
drop policy if exists "streams manage" on public.live_streams;
create policy "stream source admin only" on public.live_streams for all using(public.is_super_admin()) with check(public.is_super_admin());


-- ELIX profile and ministry-page image upload repair.
-- Run once in Supabase SQL Editor. Safe to re-run.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('elix-public', 'elix-public', true, 10485760, array['image/jpeg','image/png','image/webp'])
on conflict (id) do update
set public = true,
    file_size_limit = 10485760,
    allowed_mime_types = array['image/jpeg','image/png','image/webp'];

drop policy if exists "ELIX public profile images are readable" on storage.objects;
create policy "ELIX public profile images are readable"
on storage.objects for select to public
using (bucket_id = 'elix-public');

drop policy if exists "ELIX users upload their own profile images" on storage.objects;
create policy "ELIX users upload their own profile images"
on storage.objects for insert to authenticated
with check (
  bucket_id = 'elix-public'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "ELIX users update their own profile images" on storage.objects;
create policy "ELIX users update their own profile images"
on storage.objects for update to authenticated
using (
  bucket_id = 'elix-public'
  and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
  bucket_id = 'elix-public'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "ELIX users delete their own profile images" on storage.objects;
create policy "ELIX users delete their own profile images"
on storage.objects for delete to authenticated
using (
  bucket_id = 'elix-public'
  and (storage.foldername(name))[1] = auth.uid()::text
);

-- Public creator profiles are needed to display avatars on public creator pages.
drop policy if exists "ELIX public creator profiles are readable" on public.profiles;
create policy "ELIX public creator profiles are readable"
on public.profiles for select to public
using (true);

-- Keep ministry approval fields protected while allowing each page owner to change its logo.
create or replace function public.set_ministry_logo(target_ministry uuid, new_logo_url text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'You must sign in to update a page logo.';
  end if;
  update public.ministries
     set logo_url = new_logo_url
   where id = target_ministry
     and owner_id = auth.uid();
  if not found then
    raise exception 'Page not found or you are not its owner.';
  end if;
end;
$$;

revoke all on function public.set_ministry_logo(uuid, text) from public;
grant execute on function public.set_ministry_logo(uuid, text) to authenticated;


-- ELIX Word, Community and Academy feature tables.
-- Run this migration once in the Supabase SQL Editor.

create table if not exists public.devotionals (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  scripture_reference text not null,
  reflection text not null,
  prayer text,
  is_published boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.reading_plans (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  description text,
  duration_days integer not null default 7 check (duration_days > 0),
  is_published boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.reading_plan_days (
  id uuid primary key default gen_random_uuid(),
  plan_id uuid not null references public.reading_plans(id) on delete cascade,
  day_number integer not null check (day_number > 0),
  scripture_reference text not null,
  reflection text,
  unique(plan_id, day_number)
);

create table if not exists public.reading_plan_progress (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  plan_id uuid not null references public.reading_plans(id) on delete cascade,
  day_number integer not null check (day_number > 0),
  completed_at timestamptz not null default now(),
  unique(user_id, plan_id, day_number)
);

create table if not exists public.prayer_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  title text not null,
  body text not null,
  visibility text not null default 'public' check (visibility in ('public','group','private')),
  status text not null default 'published' check (status in ('published','hidden')),
  created_at timestamptz not null default now()
);

create table if not exists public.testimonies (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  title text not null,
  body text not null,
  status text not null default 'published' check (status in ('published','hidden')),
  created_at timestamptz not null default now()
);

create table if not exists public.community_groups (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  description text,
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.community_group_members (
  group_id uuid not null references public.community_groups(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  joined_at timestamptz not null default now(),
  primary key(group_id, user_id)
);

create table if not exists public.academy_courses (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  description text,
  instructor text,
  difficulty text not null default 'All levels',
  duration_label text,
  is_published boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.academy_lessons (
  id uuid primary key default gen_random_uuid(),
  course_id uuid not null references public.academy_courses(id) on delete cascade,
  lesson_number integer not null check (lesson_number > 0),
  title text not null,
  content text not null,
  is_published boolean not null default false,
  unique(course_id, lesson_number)
);

create table if not exists public.course_enrollments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  course_id uuid not null references public.academy_courses(id) on delete cascade,
  enrolled_at timestamptz not null default now(),
  unique(user_id, course_id)
);

create table if not exists public.lesson_progress (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  lesson_id uuid not null references public.academy_lessons(id) on delete cascade,
  completed_at timestamptz not null default now(),
  unique(user_id, lesson_id)
);

alter table public.devotionals enable row level security;
alter table public.reading_plans enable row level security;
alter table public.reading_plan_days enable row level security;
alter table public.reading_plan_progress enable row level security;
alter table public.prayer_requests enable row level security;
alter table public.testimonies enable row level security;
alter table public.community_groups enable row level security;
alter table public.community_group_members enable row level security;
alter table public.academy_courses enable row level security;
alter table public.academy_lessons enable row level security;
alter table public.course_enrollments enable row level security;
alter table public.lesson_progress enable row level security;

drop policy if exists "published devotionals readable" on public.devotionals;
create policy "published devotionals readable" on public.devotionals for select to anon, authenticated using(is_published or public.is_super_admin());
drop policy if exists "published reading plans readable" on public.reading_plans;
create policy "published reading plans readable" on public.reading_plans for select to anon, authenticated using(is_published or public.is_super_admin());
drop policy if exists "reading plan days readable" on public.reading_plan_days;
create policy "reading plan days readable" on public.reading_plan_days for select to anon, authenticated using(exists(select 1 from public.reading_plans p where p.id=plan_id and p.is_published) or public.is_super_admin());
drop policy if exists "own reading progress" on public.reading_plan_progress;
create policy "own reading progress" on public.reading_plan_progress for all to authenticated using(user_id=auth.uid()) with check(user_id=auth.uid());

drop policy if exists "public prayer wall readable" on public.prayer_requests;
create policy "public prayer wall readable" on public.prayer_requests for select to anon, authenticated using((status='published' and visibility='public') or user_id=auth.uid() or public.is_super_admin());
drop policy if exists "members submit prayer requests" on public.prayer_requests;
create policy "members submit prayer requests" on public.prayer_requests for insert to authenticated with check(user_id=auth.uid());
drop policy if exists "authors update own prayer requests" on public.prayer_requests;
create policy "authors update own prayer requests" on public.prayer_requests for update to authenticated using(user_id=auth.uid() or public.is_super_admin()) with check(user_id=auth.uid() or public.is_super_admin());

drop policy if exists "published testimonies readable" on public.testimonies;
create policy "published testimonies readable" on public.testimonies for select to anon, authenticated using(status='published' or user_id=auth.uid() or public.is_super_admin());
drop policy if exists "members submit testimonies" on public.testimonies;
create policy "members submit testimonies" on public.testimonies for insert to authenticated with check(user_id=auth.uid());
drop policy if exists "authors update own testimonies" on public.testimonies;
create policy "authors update own testimonies" on public.testimonies for update to authenticated using(user_id=auth.uid() or public.is_super_admin()) with check(user_id=auth.uid() or public.is_super_admin());

drop policy if exists "active community groups readable" on public.community_groups;
create policy "active community groups readable" on public.community_groups for select to anon, authenticated using(is_active or public.is_super_admin());
drop policy if exists "group members readable by members" on public.community_group_members;
create policy "group members readable by members" on public.community_group_members for select to authenticated using(user_id=auth.uid() or public.is_super_admin());
drop policy if exists "members join groups" on public.community_group_members;
create policy "members join groups" on public.community_group_members for insert to authenticated with check(user_id=auth.uid());
drop policy if exists "members leave groups" on public.community_group_members;
create policy "members leave groups" on public.community_group_members for delete to authenticated using(user_id=auth.uid());

drop policy if exists "published academy courses readable" on public.academy_courses;
create policy "published academy courses readable" on public.academy_courses for select to anon, authenticated using(is_published or public.is_super_admin());
drop policy if exists "published academy lessons readable" on public.academy_lessons;
create policy "published academy lessons readable" on public.academy_lessons for select to anon, authenticated using(is_published and exists(select 1 from public.academy_courses c where c.id=course_id and c.is_published) or public.is_super_admin());
drop policy if exists "own course enrollments" on public.course_enrollments;
create policy "own course enrollments" on public.course_enrollments for all to authenticated using(user_id=auth.uid()) with check(user_id=auth.uid());
drop policy if exists "own lesson progress" on public.lesson_progress;
create policy "own lesson progress" on public.lesson_progress for all to authenticated using(user_id=auth.uid()) with check(user_id=auth.uid());

grant select on public.devotionals, public.reading_plans, public.reading_plan_days, public.prayer_requests, public.testimonies, public.community_groups, public.academy_courses, public.academy_lessons to anon, authenticated;
grant select, insert, update, delete on public.reading_plan_progress, public.community_group_members, public.course_enrollments, public.lesson_progress to authenticated;
grant insert, update on public.prayer_requests, public.testimonies to authenticated;

-- Starter content gives the new sections something useful to display immediately.
insert into public.devotionals(title, scripture_reference, reflection, prayer, is_published)
select 'Be Doers of the Word', 'James 1:22', 'Faith is expressed not only in what we hear, but in the truth we put into practice. Choose one clear instruction from Scripture today and take a practical step to obey it.', 'Lord, help me to receive Your Word with humility and live it out with love and courage. Amen.', true
where not exists(select 1 from public.devotionals where title='Be Doers of the Word');

insert into public.reading_plans(title, description, duration_days, is_published)
select 'Foundations of Faith', 'A short guided journey through faith, prayer and life with Christ.', 3, true
where not exists(select 1 from public.reading_plans where title='Foundations of Faith');

insert into public.reading_plan_days(plan_id, day_number, scripture_reference, reflection)
select p.id, d.day_number, d.scripture_reference, d.reflection
from public.reading_plans p
cross join (values
 (1, 'Hebrews 11:1–6', 'Reflect on what it means to trust God, even when the outcome is not yet visible.'),
 (2, 'Matthew 6:5–13', 'Consider how Jesus teaches His disciples to pray with sincerity and dependence on the Father.'),
 (3, 'John 15:1–8', 'Reflect on abiding in Christ and allowing His words to shape daily life.')
) as d(day_number, scripture_reference, reflection)
where p.title='Foundations of Faith'
on conflict(plan_id, day_number) do nothing;

insert into public.community_groups(name, description)
select 'Prayer & Intercession', 'Encourage one another to pray and stand in faith.'
where not exists(select 1 from public.community_groups where name='Prayer & Intercession');
insert into public.community_groups(name, description)
select 'Bible Study Fellowship', 'Read Scripture together and grow in understanding.'
where not exists(select 1 from public.community_groups where name='Bible Study Fellowship');
insert into public.community_groups(name, description)
select 'Young Believers', 'A fellowship space for encouragement and discipleship.'
where not exists(select 1 from public.community_groups where name='Young Believers');

insert into public.academy_courses(title, description, instructor, difficulty, duration_label, is_published)
select 'Foundations of Christian Discipleship', 'An introductory course on following Jesus, growing in Scripture, prayer and Christian character.', 'ELIX Academy', 'Beginner', '3 lessons', true
where not exists(select 1 from public.academy_courses where title='Foundations of Christian Discipleship');

insert into public.academy_lessons(course_id, lesson_number, title, content, is_published)
select c.id, l.lesson_number, l.title, l.content, true
from public.academy_courses c
cross join (values
 (1, 'Following Jesus', 'Christian discipleship begins with responding to the call of Jesus. Read Luke 9:23 and reflect on what it means to follow Him daily. Write down one practical way to put His teaching into action.'),
 (2, 'Growing Through Scripture and Prayer', 'A healthy life with God is shaped by Scripture and prayer. Read 2 Timothy 3:16–17 and Matthew 6:5–13. Set aside a regular time to read, reflect and pray.'),
 (3, 'Character and Christian Service', 'Spiritual growth should shape how we love and serve others. Read Galatians 5:22–23 and Mark 10:45. Identify one way to serve someone this week.')
) as l(lesson_number, title, content)
where c.title='Foundations of Christian Discipleship'
on conflict(course_id, lesson_number) do nothing;
