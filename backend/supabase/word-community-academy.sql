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
