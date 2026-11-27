-- Let an authenticated owner permanently delete only their own private health data.
-- The function runs as invoker; table grants and RLS remain the enforcement boundary.

drop policy if exists shahrvand_health_profiles_delete_own
  on public.shahrvand_health_profiles;
create policy shahrvand_health_profiles_delete_own
  on public.shahrvand_health_profiles
  for delete to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists shahrvand_body_measurements_delete_own
  on public.shahrvand_body_measurements;
create policy shahrvand_body_measurements_delete_own
  on public.shahrvand_body_measurements
  for delete to authenticated
  using ((select auth.uid()) = user_id);

grant delete on public.shahrvand_health_profiles to authenticated;
grant delete on public.shahrvand_body_measurements to authenticated;

create or replace function public.shahrvand_delete_my_health_data()
returns void
language plpgsql
security invoker
set search_path = ''
as $$
begin
  delete from public.shahrvand_body_measurements
  where user_id = auth.uid();

  delete from public.shahrvand_health_profiles
  where user_id = auth.uid();
end;
$$;

revoke all on function public.shahrvand_delete_my_health_data() from public, anon;
grant execute on function public.shahrvand_delete_my_health_data() to authenticated;
