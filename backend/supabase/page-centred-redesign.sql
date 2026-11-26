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

-- Page owners can publish into their own page without requiring a separate
-- ministry-membership invitation. The authenticated uploader remains the owner.
drop policy if exists "media page owner insert" on public.media_items;
create policy "media page owner insert"
  on public.media_items for insert to authenticated
  with check (
    uploaded_by = auth.uid()
    and ministry_id is not null
    and exists (
      select 1 from public.ministries m
      where m.id = ministry_id and m.owner_id = auth.uid()
    )
  );

drop policy if exists "programme page owner manage" on public.programmes;
create policy "programme page owner manage"
  on public.programmes for all to authenticated
  using (
    ministry_id is not null and exists (
      select 1 from public.ministries m
      where m.id = ministry_id and m.owner_id = auth.uid()
    )
  )
  with check (
    ministry_id is not null and exists (
      select 1 from public.ministries m
      where m.id = ministry_id and m.owner_id = auth.uid()
    )
  );

drop policy if exists "stream page owner manage" on public.live_streams;
create policy "stream page owner manage"
  on public.live_streams for all to authenticated
  using (
    exists (select 1 from public.ministries m where m.id = ministry_id and m.owner_id = auth.uid())
  )
  with check (
    exists (select 1 from public.ministries m where m.id = ministry_id and m.owner_id = auth.uid())
  );
