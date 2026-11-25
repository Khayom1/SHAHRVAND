create or replace function private.shahrvand_guard_profile_age_immutable()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
  if new.age is distinct from old.age then
    raise exception 'PROFILE_AGE_IMMUTABLE' using errcode='22023';
  end if;
  return new;
end;
$$;

revoke all on function private.shahrvand_guard_profile_age_immutable() from public, anon, authenticated;

drop trigger if exists shahrvand_profiles_age_immutable on public.shahrvand_profiles;
create trigger shahrvand_profiles_age_immutable
before update of age on public.shahrvand_profiles
for each row execute function private.shahrvand_guard_profile_age_immutable();
