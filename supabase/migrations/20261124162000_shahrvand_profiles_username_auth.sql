-- SHAHRVAND account profiles linked to Supabase Auth.
-- Passwords are managed exclusively by Supabase Auth; this table never stores passwords.

create table public.shahrvand_profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    first_name text not null check (char_length(btrim(first_name)) between 1 and 60),
    last_name text not null check (char_length(btrim(last_name)) between 1 and 60),
    age smallint not null check (age between 7 and 120),
    login text not null,
    created_at timestamptz not null default now(),
    constraint shahrvand_profiles_login_format
        check (login = lower(login) and login ~ '^[a-z0-9][a-z0-9._-]{3,23}$'),
    constraint shahrvand_profiles_login_unique unique (login)
);

alter table public.shahrvand_profiles enable row level security;

revoke all on table public.shahrvand_profiles from anon, authenticated;
grant select on table public.shahrvand_profiles to authenticated;

create policy "shahrvand_profiles_read_own"
on public.shahrvand_profiles
for select
to authenticated
using ((select auth.uid()) = id);
