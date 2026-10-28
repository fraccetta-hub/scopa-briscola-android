-- Add 'banned' as a valid role value for profiles
-- Banned users: authenticated but redirect-blocked in layout; cannot add places or confirmations.

alter table public.profiles
  drop constraint profiles_role_check;

alter table public.profiles
  add constraint profiles_role_check
    check (role in ('user', 'admin', 'superadmin', 'banned'));
