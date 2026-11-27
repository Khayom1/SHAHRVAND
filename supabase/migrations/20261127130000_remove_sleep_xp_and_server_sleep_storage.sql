-- Sleep remains a health feature only. Remove sleep-derived rewards and all server-side
-- sleep-session storage while preserving age-based sleep guidance in the Android app.

drop function if exists public.shahrvand_award_sleep_xp(
  date, timestamptz, timestamptz, text, boolean
);
drop function if exists private.shahrvand_award_sleep_xp_impl(
  date, timestamptz, timestamptz, text, boolean
);

create or replace function private.shahrvand_get_leaderboard_impl(
  p_period text,
  p_age_band text
)
returns table(
  rank bigint,
  login text,
  weekly_xp bigint,
  tasks_completed bigint,
  is_me boolean,
  achievement_codes text[]
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_today date := (now() at time zone 'Asia/Dushanbe')::date;
  v_week_start date := date_trunc('week', v_today::timestamp)::date;
  v_month_start date := date_trunc('month', v_today::timestamp)::date;
  v_min_age integer;
  v_max_age integer;
begin
  if v_user_id is null then raise exception 'AUTH_REQUIRED' using errcode = '28000'; end if;
  if p_period not in ('week','month','all_time') then raise exception 'INVALID_PERIOD' using errcode = '22023'; end if;
  if p_age_band not in ('all','7_12','13_17','18_plus') then raise exception 'INVALID_AGE_BAND' using errcode = '22023'; end if;

  v_min_age := case p_age_band when '7_12' then 7 when '13_17' then 13 when '18_plus' then 18 else 7 end;
  v_max_age := case p_age_band when '7_12' then 12 when '13_17' then 17 when '18_plus' then 120 else 120 end;

  return query
  with all_scores as (
    select c.user_id, c.xp_awarded::bigint as xp, 1::bigint as completed, c.created_at as scored_at
      from public.shahrvand_weekly_completions c
      where (p_period = 'all_time')
         or (p_period = 'week' and c.completed_on between v_week_start and v_today)
         or (p_period = 'month' and c.completed_on between v_month_start and v_today)
    union all
    select a.user_id, a.xp_awarded::bigint, 1::bigint, a.completed_at
      from public.shahrvand_activity_sessions a
      where a.status = 'verified' and a.xp_awarded > 0 and a.completed_at is not null
        and ((p_period = 'all_time')
          or (p_period = 'week' and a.challenge_date between v_week_start and v_today)
          or (p_period = 'month' and a.challenge_date between v_month_start and v_today))
  ),
  totals as (
    select a.user_id, sum(a.xp)::bigint as xp, sum(a.completed)::bigint as completed,
           min(a.scored_at) as first_score
      from all_scores a group by a.user_id
  ),
  eligible as (
    select p.id, p.login, t.xp, t.completed, t.first_score
      from totals t join public.shahrvand_profiles p on p.id = t.user_id
      where p.age between v_min_age and v_max_age
  ),
  ranked as (
    select row_number() over (
      order by e.xp desc, e.completed desc, e.first_score asc, e.login asc
    ) as pos, e.login, e.xp, e.completed, e.id
    from eligible e
  )
  select r.pos, r.login, r.xp, r.completed, (r.id = v_user_id),
    coalesce((
      select array_agg(aw.achievement_code order by aw.achievement_code)
      from public.shahrvand_achievement_awards aw where aw.user_id = r.id
    ), array[]::text[])
  from ranked r order by r.pos limit 100;
end;
$$;

create or replace function private.shahrvand_get_my_achievements_impl()
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_count integer;
  v_total_xp integer;
  v_from integer;
  v_next integer;
  v_rows jsonb;
begin
  if v_user_id is null then
    raise exception 'AUTH_REQUIRED' using errcode = '28000';
  end if;

  insert into public.shahrvand_achievement_awards(user_id, achievement_code, achievement_kind)
  values (v_user_id, 'app_entry', 'badge')
  on conflict (user_id, achievement_code) do nothing;

  perform private.shahrvand_sync_achievement_awards(v_user_id);

  select
    (select count(*) from public.shahrvand_weekly_completions w where w.user_id = v_user_id)
    + (select count(*) from public.shahrvand_activity_sessions a
       where a.user_id = v_user_id and a.status = 'verified' and a.xp_awarded > 0)
  into v_count;

  select coalesce(sum(s.xp), 0)::integer
  into v_total_xp
  from (
    select w.xp_awarded::integer as xp
      from public.shahrvand_weekly_completions w where w.user_id = v_user_id
    union all
    select a.xp_awarded::integer
      from public.shahrvand_activity_sessions a
      where a.user_id = v_user_id and a.status = 'verified' and a.xp_awarded > 0
  ) s;

  v_from := case when v_count < 10 then 0 when v_count < 25 then 10 when v_count < 50 then 25 when v_count < 100 then 50 else 100 end;
  v_next := case when v_count < 10 then 10 when v_count < 25 then 25 when v_count < 50 then 50 when v_count < 100 then 100 else 100 end;

  select coalesce(jsonb_agg(
    jsonb_build_object('code', achievement_code, 'kind', achievement_kind, 'awarded_at', awarded_at)
    order by awarded_at, achievement_code
  ), '[]'::jsonb)
  into v_rows
  from public.shahrvand_achievement_awards
  where user_id = v_user_id;

  return jsonb_build_object(
    'ok', true,
    'total_xp', v_total_xp,
    'verified_tasks', v_count,
    'progress_from', v_from,
    'next_threshold', v_next,
    'progress', case when v_next = v_from then 1 else least(1, greatest(0, (v_count - v_from)::numeric / (v_next - v_from))) end,
    'achievements', v_rows
  );
end;
$$;

-- This table only existed to store sleep-based XP completions.
drop table if exists public.shahrvand_sleep_completions;
