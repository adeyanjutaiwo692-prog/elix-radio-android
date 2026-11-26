-- ELIX open member uploads and content-policy acceptance.
-- Run this migration in Supabase SQL Editor after the original schema.sql.

alter table public.profiles
  add column if not exists content_policy_accepted_at timestamptz,
  add column if not exists content_policy_version text;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path=public
as $$
begin
  insert into public.profiles(
    id, display_name, avatar_url,
    content_policy_accepted_at, content_policy_version
  )
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name'),
    new.raw_user_meta_data->>'avatar_url',
    case when new.raw_user_meta_data->>'content_policy_accepted' = 'true' then now() else null end,
    case when new.raw_user_meta_data->>'content_policy_accepted' = 'true' then '2026-11-26' else null end
  )
  on conflict(id) do nothing;
  return new;
end;
$$;

-- Uploaded media is published immediately, without a review queue.
create or replace function public.publish_member_upload()
returns trigger
language plpgsql
set search_path=public
as $$
begin
  new.status := 'approved';
  new.published_at := now();
  return new;
end;
$$;

drop trigger if exists publish_member_upload_trigger on public.media_items;
create trigger publish_member_upload_trigger
before insert on public.media_items
for each row execute function public.publish_member_upload();

-- Publicly readable media files; only signed-in users can upload into their own folder.
insert into storage.buckets(id, name, public, file_size_limit, allowed_mime_types)
values (
  'elix-media', 'elix-media', true, 5368709120,
  array['audio/mpeg','audio/mp4','audio/aac','audio/wav','audio/x-wav','audio/ogg',
        'video/mp4','video/quicktime','application/pdf','image/jpeg','image/png','image/webp']
)
on conflict (id) do update
set public = true,
    file_size_limit = 5368709120,
    allowed_mime_types = array[
      'audio/mpeg','audio/mp4','audio/aac','audio/wav','audio/x-wav','audio/ogg',
      'video/mp4','video/quicktime','application/pdf','image/jpeg','image/png','image/webp'
    ];

-- The global Storage upload limit in the Supabase dashboard must also be at least 5 GB.

drop policy if exists "ELIX media files are publicly readable" on storage.objects;
create policy "ELIX media files are publicly readable"
on storage.objects for select
using (bucket_id = 'elix-media');

drop policy if exists "ELIX members upload to own folder" on storage.objects;
create policy "ELIX members upload to own folder"
on storage.objects for insert to authenticated
with check (
  bucket_id = 'elix-media'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "ELIX super admins delete media files" on storage.objects;
create policy "ELIX super admins delete media files"
on storage.objects for delete to authenticated
using (bucket_id = 'elix-media' and public.is_super_admin());

-- Only authenticated members can create media catalogue records for themselves.
drop policy if exists "media insert" on public.media_items;
create policy "media insert"
on public.media_items for insert to authenticated
with check (
  uploaded_by = auth.uid()
  and rights_confirmed = true
  and (ministry_id is null or public.is_ministry_member(ministry_id) or public.is_super_admin())
);
