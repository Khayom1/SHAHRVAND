-- SHAHRVAND Health foundation.
-- Keep sensitive body data separate from the public competition/profile model.

create table if not exists public.shahrvand_health_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade default auth.uid(),
  height_cm numeric(5,2) not null check (height_cm between 80 and 250),
  sex_at_birth text not null default 'not_specified'
    check (sex_at_birth in ('female', 'male', 'not_specified')),
  activity_level text not null default 'light'
    check (activity_level in ('sedentary', 'light', 'moderate', 'high')),
  health_goal text not null default 'build_habit'
    check (health_goal in ('maintain', 'build_strength', 'improve_endurance', 'build_habit', 'gain_weight', 'weight_management')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.shahrvand_body_measurements (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
  weight_kg numeric(6,2) not null check (weight_kg between 5 and 500),
  measured_at timestamptz not null default now(),
  source text not null default 'manual'
    check (source in ('manual', 'health_connect', 'connected_device')),
  created_at timestamptz not null default now()
);

create index if not exists shahrvand_body_measurements_user_measured_idx
  on public.shahrvand_body_measurements(user_id, measured_at desc, id desc);

alter table public.shahrvand_health_profiles enable row level security;
alter table public.shahrvand_body_measurements enable row level security;

drop policy if exists shahrvand_health_profiles_select_own
  on public.shahrvand_health_profiles;
create policy shahrvand_health_profiles_select_own
  on public.shahrvand_health_profiles
  for select to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists shahrvand_health_profiles_insert_own
  on public.shahrvand_health_profiles;
create policy shahrvand_health_profiles_insert_own
  on public.shahrvand_health_profiles
  for insert to authenticated
  with check ((select auth.uid()) = user_id);

drop policy if exists shahrvand_health_profiles_update_own
  on public.shahrvand_health_profiles;
create policy shahrvand_health_profiles_update_own
  on public.shahrvand_health_profiles
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

drop policy if exists shahrvand_body_measurements_select_own
  on public.shahrvand_body_measurements;
create policy shahrvand_body_measurements_select_own
  on public.shahrvand_body_measurements
  for select to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists shahrvand_body_measurements_insert_own
  on public.shahrvand_body_measurements;
create policy shahrvand_body_measurements_insert_own
  on public.shahrvand_body_measurements
  for insert to authenticated
  with check ((select auth.uid()) = user_id);

-- Public-schema Data API access is opt-in on current Supabase projects.
-- Expose only the operations required by the signed-in app; RLS still scopes every row.
revoke all on public.shahrvand_health_profiles from anon, public;
revoke all on public.shahrvand_body_measurements from anon, public;

grant select, insert, update
  on public.shahrvand_health_profiles to authenticated;
grant select, insert
  on public.shahrvand_body_measurements to authenticated;
