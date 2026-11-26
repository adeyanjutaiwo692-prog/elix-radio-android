-- Run after the first ELIX Control Room account has been created.
-- 1) Find your account UUID:
select id, email from auth.users order by created_at desc;
-- 2) Replace YOUR_AUTH_USER_UUID below with the UUID shown above, then run:
-- update public.profiles set role = 'super_admin' where id = 'YOUR_AUTH_USER_UUID';
