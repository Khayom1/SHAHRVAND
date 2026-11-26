create or replace function private.shahrvand_get_leaderboard_impl(p_age_band text)
returns table(rank bigint, login text, weekly_xp bigint, tasks_completed bigint, is_me boolean)
language plpgsql security definer set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_today date := (now() at time zone 'Asia/Dushanbe')::date;
  v_week_start date := date_trunc('week', v_today::timestamp)::date;
  v_min_age integer;
  v_max_age integer;
begin
  if v_user_id is null then raise exception 'AUTH_REQUIRED' using errcode = '28000'; end if;
  if p_age_band not in ('7_12','13_17','18_plus') then raise exception 'INVALID_AGE_BAND' using errcode = '22023'; end if;
  v_min_age := case p_age_band when '7_12' then 7 when '13_17' then 13 else 18 end;
  v_max_age := case p_age_band when '7_12' then 12 when '13_17' then 17 else 120 end;
  return query
  with all_scores as (
    select c.user_id,c.xp_awarded::bigint xp,1::bigint completed,c.created_at scored_at
      from public.shahrvand_weekly_completions c where c.week_start=v_week_start
    union all
    select a.user_id,a.xp_awarded::bigint,1::bigint,a.completed_at
      from public.shahrvand_activity_sessions a
      where a.challenge_date between v_week_start and v_week_start+6
        and a.status='verified' and a.xp_awarded>0 and a.completed_at is not null
    union all
    select s.user_id,s.xp_awarded::bigint,1::bigint,s.created_at
      from public.shahrvand_sleep_completions s where s.week_start=v_week_start
  ), totals as (
    select user_id,sum(xp)::bigint xp,sum(completed)::bigint completed,min(scored_at) first_score
      from all_scores group by user_id
  ), eligible as (
    select p.id,p.login,t.xp,t.completed,t.first_score
      from totals t join public.shahrvand_profiles p on p.id=t.user_id
      where p.age between v_min_age and v_max_age
  ), ranked as (
    select row_number() over(order by e.xp desc,e.completed desc,e.first_score asc,e.login asc) pos,
           e.login,e.xp,e.completed,e.id from eligible e
  )
  select r.pos,r.login,r.xp,r.completed,(r.id=v_user_id)
    from ranked r order by r.pos limit 100;
end;
$$;
revoke all on function private.shahrvand_get_leaderboard_impl(text) from public,anon;
grant execute on function private.shahrvand_get_leaderboard_impl(text) to authenticated;
