-- Expand the private health profile with optional body and vital measurements.
-- These fields are self-reported unless a future connected device source is recorded.

alter table public.shahrvand_health_profiles
  add column if not exists waist_cm numeric(5,2)
    check (waist_cm between 30 and 250),
  add column if not exists resting_heart_rate_bpm smallint
    check (resting_heart_rate_bpm between 25 and 240),
  add column if not exists blood_pressure_systolic_mmhg smallint
    check (blood_pressure_systolic_mmhg between 60 and 260),
  add column if not exists blood_pressure_diastolic_mmhg smallint
    check (blood_pressure_diastolic_mmhg between 30 and 180),
  add column if not exists allergies text not null default '',
  add column if not exists health_conditions text not null default '';

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conrelid = 'public.shahrvand_health_profiles'::regclass
      and conname = 'shahrvand_health_profiles_bp_pair_check'
  ) then
    alter table public.shahrvand_health_profiles
      add constraint shahrvand_health_profiles_bp_pair_check
      check (
        (blood_pressure_systolic_mmhg is null and blood_pressure_diastolic_mmhg is null)
        or
        (
          blood_pressure_systolic_mmhg is not null
          and blood_pressure_diastolic_mmhg is not null
          and blood_pressure_systolic_mmhg > blood_pressure_diastolic_mmhg
        )
      );
  end if;
end;
$$;

-- Existing owner-only RLS remains in force; do not grant access to anon/public.
