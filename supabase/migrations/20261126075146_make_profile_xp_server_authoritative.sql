create or replace function private.shahrvand_get_my_achievements_impl()
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_count integer;
  v_total_xp integer;
  v_from integer;
  v_next integer;
  v_rows jsonb;
begin
  if v_user_id is null then raise exception 'AUTH_REQUIRED' using errcode = '28000'; end if;
  insert into public.shahrvand_achievement_awards(user_id, achievement_code, achievement_kind)
    values (v_user_id, 'app_entry', 'badge') on conflict (user_id, achievement_code) do nothing;
  perform private.shahrvand_sync_achievement_awards(v_user_id);
  select
    (select count(*) from public.shahrvand_weekly_completions w where w.user_id = v_user_id)
    + (select count(*) from public.shahrvand_activity_sessions a
       where a.user_id = v_user_id and a.status = 'verified' and a.xp_awarded > 0)
  into v_count;
  select coalesce(sum(s.xp),0)::integer into v_total_xp
  from (
    select w.xp_awarded::integer xp from public.shahrvand_weekly_completions w where w.user_id = v_user_id
    union all
    select a.xp_awarded::integer from public.shahrvand_activity_sessions a
      where a.user_id = v_user_id and a.status = 'verified' and a.xp_awarded > 0
    union all
    select sl.xp_awarded::integer from public.shahrvand_sleep_completions sl where sl.user_id = v_user_id
  ) s;
  v_from := case when v_count < 10 then 0 when v_count < 25 then 10 when v_count < 50 then 25 when v_count < 100 then 50 else 100 end;
  v_next := case when v_count < 10 then 10 when v_count < 25 then 25 when v_count < 50 then 50 when v_count < 100 then 100 else 100 end;
  select coalesce(jsonb_agg(jsonb_build_object('code',achievement_code,'kind',achievement_kind,'awarded_at',awarded_at)
    order by awarded_at,achievement_code),'[]'::jsonb)
    into v_rows from public.shahrvand_achievement_awards where user_id = v_user_id;
  return jsonb_build_object(
    'ok',true,'total_xp',v_total_xp,'verified_tasks',v_count,'progress_from',v_from,
    'next_threshold',v_next,
    'progress',case when v_next=v_from then 1 else least(1,greatest(0,(v_count-v_from)::numeric/(v_next-v_from))) end,
    'achievements',v_rows
  );
end;
$$;
revoke all on function private.shahrvand_get_my_achievements_impl() from public,anon;
grant execute on function private.shahrvand_get_my_achievements_impl() to authenticated;
