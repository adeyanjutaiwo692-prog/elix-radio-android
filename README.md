# ELIX Radio — multi-ministry Christian broadcasting platform

Android-first listener app plus a ministry/admin portal.

## Repository
- `app/`: Android listener application.
- `portal/index.html`: responsive ELIX Control Room for Super Admins and ministry teams.
- `portal/config.example.js`: Supabase browser configuration template.
- `backend/supabase/schema.sql`: database schema, roles, RLS policies and storage buckets.

## Backend setup
1. Create a Supabase project and run `backend/supabase/schema.sql` in SQL Editor.
2. Enable Email and Google providers in Supabase Authentication; configure Google OAuth redirect URLs.
3. Copy `portal/config.example.js` to `portal/config.js` and add the project URL and anon/public key.
4. Host `portal/` over HTTPS. Do not open the page as a local file.
5. Create your account, then promote your profile to Super Admin in SQL Editor: `update public.profiles set role='super_admin' where id='YOUR_AUTH_USER_UUID';`
6. Ministry users apply through the portal. Super Admin approves channels and assigns ministry team membership.

## Production integrations required
The portal records authorised streams, uploads and schedules; it is not itself a broadcast ingest/transcoding server. Configure RTMP/SRT ingest, HLS packaging and a server-side stream health worker. Connect a 24/7 audio automation service such as AzuraCast/Icecast. Configure Firebase Cloud Messaging and secure payment webhooks (Paystack/Flutterwave) before activating push alerts or subscriptions.

Never commit Supabase service-role keys, payment secrets, OAuth client secrets or RTMP passwords. Use written distribution permissions for every ministry feed and archived item. Content is pending until approved by ELIX.
