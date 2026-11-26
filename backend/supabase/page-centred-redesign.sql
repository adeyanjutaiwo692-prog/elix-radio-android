-- ELIX page-centred account and creator-page redesign
alter table public.ministries
  add column if not exists page_type text not null default 'ministry'
  check (page_type in ('ministry','public_figure'));

create index if not exists ministries_page_type_approved_idx
  on public.ministries(page_type, approved, name);

-- Creators may update only the descriptive fields of their own uploaded media.
-- Approval status and publishing controls remain administrator-only.
grant update (title, description, kind, preacher, topic, thumbnail_url)
  on public.media_items to authenticated;

drop policy if exists "media owner edit details" on public.media_items;
create policy "media owner edit details"
  on public.media_items for update to authenticated
  using (uploaded_by = auth.uid())
  with check (uploaded_by = auth.uid());

-- A page owner can update their own page profile. Approval remains unchanged
-- through the app's editable fields; only ELIX admins can approve pages.
grant update (name, slug, description, logo_url, country, city, website_url, contact_email)
  on public.ministries to authenticated;

drop policy if exists "page owner edit profile" on public.ministries;
create policy "page owner edit profile"
  on public.ministries for update to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());
