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
