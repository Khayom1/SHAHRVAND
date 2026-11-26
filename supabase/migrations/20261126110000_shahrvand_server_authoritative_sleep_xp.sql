create table if not exists public.shahrvand_sleep_completions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  wake_date date not null,
  week_start date not null,
  started_at timestamptz not null,
  ended_at timestamptz not null,
  sleep_minutes smallint not null check (sleep_minutes between 120 and 960),
  source_package text not null,
  xp_awarded smallint not null check (xp_awarded = 15),
  created_at timestamptz not null default now(),
  constraint shahrvand_sleep_completions_user_date_key unique (user_id, wake_date),
  constraint shahrvand_sleep_completions_valid_range check (ended_at > started_at)
);

create index if not exists shahrvand_sleep_completions_rank_idx
  on public.shahrvand_sleep_completions (week_start, user_id, xp_awarded);

alter table public.shahrvand_sleep_completions enable row level security;
drop policy if exists "Users can read their own sleep completions" on public.shahrvand_sleep_completions;
create policy "Users can read their own sleep completions"
  on public.shahrvand_sleep_completions for select to authenticated
  using ((select auth.uid()) = user_id);
revoke all on public.shahrvand_sleep_completions from anon, authenticated;
grant select on public.shahrvand_sleep_completions to authenticated;

create or replace function private.shahrvand_award_sleep_xp_impl(
  p_wake_date date, p_started_at timestamptz, p_ended_at timestamptz,
  p_source_package text, p_is_manual boolean
) returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_age smallint;
  v_minutes integer;
  v_target integer;
  v_today date := (now() at time zone 'Asia/Dushanbe')::date;
  v_week_start date;
  v_xp smallint := 15;
  v_inserted uuid;
begin
  if v_user_id is null then raise exception 'AUTH_REQUIRED' using errcode = '28000'; end if;
  select age into v_age from public.shahrvand_profiles where id = v_user_id;
  if v_age is null then raise exception 'PROFILE_REQUIRED' using errcode = 'P0002'; end if;
  if p_wake_date is null or p_started_at is null or p_ended_at is null or p_ended_at <= p_started_at then
    raise exception 'INVALID_SLEEP_SESSION' using errcode = '22023';
  end if;
  if p_wake_date > v_today or p_wake_date < v_today - 9
     or (p_ended_at at time zone 'Asia/Dushanbe')::date <> p_wake_date then
    raise exception 'SLEEP_DATE_OUT_OF_RANGE' using errcode = '22023';
  end if;
  if coalesce(p_is_manual, true) then raise exception 'MANUAL_SLEEP_RECORD_NOT_ELIGIBLE' using errcode = '22023'; end if;
  if p_source_package <> 'com.sec.android.app.shealth' then
    raise exception 'UNSUPPORTED_SLEEP_SOURCE' using errcode = '22023';
  end if;
  v_minutes := floor(extract(epoch from (p_ended_at - p_started_at)) / 60)::integer;
  if v_minutes < 120 or v_minutes > 960 then raise exception 'SLEEP_DURATION_INVALID' using errcode = '22023'; end if;
  v_target := case when v_age between 7 and 12 then 540 when v_age between 13 and 17 then 480 else 420 end;
  if v_minutes < v_target then
    return jsonb_build_object('ok', true, 'qualified', false, 'xp_awarded', 0,
      'sleep_minutes', v_minutes, 'target_minutes', v_target);
  end if;
  v_week_start := date_trunc('week', p_wake_date::timestamp)::date;
  insert into public.shahrvand_sleep_completions (
    user_id, wake_date, week_start, started_at, ended_at, sleep_minutes, source_package, xp_awarded
  ) values (v_user_id, p_wake_date, v_week_start, p_started_at, p_ended_at, v_minutes, p_source_package, v_xp)
  on conflict (user_id, wake_date) do nothing returning id into v_inserted;
  if v_inserted is null then
    return jsonb_build_object('ok', true, 'qualified', true, 'already_awarded', true,
      'xp_awarded', 0, 'sleep_minutes', v_minutes, 'target_minutes', v_target);
  end if;
  return jsonb_build_object('ok', true, 'qualified', true, 'already_awarded', false,
    'xp_awarded', v_xp, 'sleep_minutes', v_minutes, 'target_minutes', v_target,
    'wake_date', p_wake_date, 'week_start', v_week_start);
end;
$$;

revoke all on function private.shahrvand_award_sleep_xp_impl(date,timestamptz,timestamptz,text,boolean) from public, anon;
grant usage on schema private to authenticated;
grant execute on function private.shahrvand_award_sleep_xp_impl(date,timestamptz,timestamptz,text,boolean) to authenticated;

create or replace function public.shahrvand_award_sleep_xp(
  p_wake_date date, p_started_at timestamptz, p_ended_at timestamptz,
  p_source_package text, p_is_manual boolean
) returns jsonb language sql security invoker set search_path = ''
as $$
  select private.shahrvand_award_sleep_xp_impl(
    p_wake_date, p_started_at, p_ended_at, p_source_package, p_is_manual
  );
$$;
revoke all on function public.shahrvand_award_sleep_xp(date,timestamptz,timestamptz,text,boolean) from public, anon;
grant execute on function public.shahrvand_award_sleep_xp(date,timestamptz,timestamptz,text,boolean) to authenticated;

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
    select c.user_id, c.xp_awarded::bigint as xp, 1::bigint as completed, c.created_at as scored_at
      from public.shahrvand_weekly_completions c where c.week_start = v_week_start
    union all
    select s.user_id, s.xp_awarded::bigint as xp, 1::bigint as completed, s.created_at as scored_at
      from public.shahrvand_sleep_completions s where s.week_start = v_week_start
  ), totals as (
    select a.user_id, sum(a.xp)::bigint as xp, sum(a.completed)::bigint as completed,
           min(a.scored_at) as first_score
      from all_scores a group by a.user_id
  ), eligible as (
    select p.id, p.login, t.xp, t.completed, t.first_score
      from totals t join public.shahrvand_profiles p on p.id = t.user_id
      where p.age between v_min_age and v_max_age
  ), ranked as (
    select row_number() over (order by e.xp desc, e.completed desc, e.first_score asc, e.login asc) as pos,
           e.login, e.xp, e.completed, e.id
      from eligible e
  )
  select r.pos, r.login, r.xp, r.completed, (r.id = v_user_id)
    from ranked r order by r.pos limit 100;
end;
$$;
revoke all on function private.shahrvand_get_leaderboard_impl(text) from public, anon;
grant execute on function private.shahrvand_get_leaderboard_impl(text) to authenticated;
