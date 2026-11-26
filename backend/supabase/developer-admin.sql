-- ELIX Developer / Super Admin permissions.
-- The account role must be assigned to the owner's verified email from Supabase SQL Editor.

-- Allow a verified ELIX super admin to read and update all profile records.
drop policy if exists "profiles super admin manage" on public.profiles;
create policy "profiles super admin manage"
on public.profiles
for all
to authenticated
using (public.is_super_admin())
with check (public.is_super_admin());

-- Ensure the role lookup function remains available to authenticated app users.
grant execute on function public.is_super_admin() to authenticated;

-- OWNER ACCOUNT ACTIVATION:
-- Replace the email below with the exact email used to register/sign in to ELIX,
-- then run this statement in Supabase SQL Editor.
-- update public.profiles
-- set role = 'super_admin'
-- where id = (select id from auth.users where lower(email) = lower('YOUR-EMAIL@example.com'));
