-- ELIX open publishing, profile photos and creator-owned page profiles.
-- Run once in the Supabase SQL Editor.

-- Pages become public immediately; there is no page approval queue.
alter table public.ministries alter column approved set default true;
update public.ministries set approved=true where approved=false;

-- Creator uploads publish immediately instead of waiting for moderation.
alter table public.media_items alter column status set default 'approved';
update public.media_items set status='approved' where status='pending';

-- Page owners can edit their own page profile (including the page logo).
drop policy if exists "ministry admin update only" on public.ministries;
drop policy if exists "ELIX page owner update own page" on public.ministries;
create policy "ELIX page owner update own page" on public.ministries
  for update to authenticated using (owner_id=auth.uid() or public.is_super_admin())
  with check (owner_id=auth.uid() or public.is_super_admin());
grant update (name, description, city, logo_url, website_url, contact_email) on public.ministries to authenticated;

-- Public profile photos are stored in elix-public under the signed-in user's folder.
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('elix-public','elix-public',true,10485760,array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public=true, file_size_limit=10485760,
  allowed_mime_types=array['image/jpeg','image/png','image/webp'];

drop policy if exists "ELIX profile image upload" on storage.objects;
create policy "ELIX profile image upload" on storage.objects
  for insert to authenticated with check (
    bucket_id='elix-public' and (storage.foldername(name))[1]=auth.uid()::text
  );
drop policy if exists "ELIX profile image update" on storage.objects;
create policy "ELIX profile image update" on storage.objects
  for update to authenticated using (
    bucket_id='elix-public' and (storage.foldername(name))[1]=auth.uid()::text
  ) with check (
    bucket_id='elix-public' and (storage.foldername(name))[1]=auth.uid()::text
  );
drop policy if exists "ELIX profile image delete" on storage.objects;
create policy "ELIX profile image delete" on storage.objects
  for delete to authenticated using (
    bucket_id='elix-public' and (storage.foldername(name))[1]=auth.uid()::text
  );

grant select on public.ministries to anon, authenticated;
grant select on public.media_items to anon, authenticated;


-- Creators may update their own display metadata (including generated thumbnails),
-- but cannot approve their own media or alter moderation status.
drop policy if exists "ELIX media owner metadata update" on public.media_items;
create policy "ELIX media owner metadata update" on public.media_items
  for update to authenticated using (uploaded_by=auth.uid())
  with check (uploaded_by=auth.uid());
grant update (title, description, thumbnail_url) on public.media_items to authenticated;
