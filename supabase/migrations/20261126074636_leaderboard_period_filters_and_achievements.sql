create table if not exists public.shahrvand_achievement_awards (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  achievement_code text not null check (achievement_code in ('app_entry','first_task','steady_10','dedicated_25','expert_50','legend_100')),
  achievement_kind text not null check (achievement_kind in ('badge','title')),
  awarded_at timestamptz not null default now(),
  constraint shahrvand_achievement_awards_user_code_key unique (user_id, achievement_code)
);
create index if not exists shahrvand_achievement_awards_user_idx on public.shahrvand_achievement_awards(user_id, awarded_at);
alter table public.shahrvand_achievement_awards enable row level security;
drop policy if exists "Users can read their own achievement awards" on public.shahrvand_achievement_awards;
create policy "Users can read their own achievement awards" on public.shahrvand_achievement_awards
  for select to authenticated using ((select auth.uid()) = user_id);
revoke all on public.shahrvand_achievement_awards from anon, authenticated;
grant select on public.shahrvand_achievement_awards to authenticated;

create or replace function private.shahrvand_sync_achievement_awards(p_user_id uuid)
returns void language plpgsql security definer set search_path = ''
as $$
declare v_verified integer;
begin
  if p_user_id is null then return; end if;
  select
    (select count(*) from public.shahrvand_weekly_completions w where w.user_id = p_user_id)
    + (select count(*) from public.shahrvand_activity_sessions a where a.user_id = p_user_id and a.status = 'verified' and a.xp_awarded > 0)
  into v_verified;
  insert into public.shahrvand_achievement_awards(user_id, achievement_code, achievement_kind)
  select p_user_id, earned.code, earned.kind
  from (values
    ('first_task', 'badge', 1),
    ('steady_10', 'title', 10),
    ('dedicated_25', 'title', 25),
    ('expert_50', 'title', 50),
    ('legend_100', 'title', 100)
  ) as earned(code, kind, required_count)
  where v_verified >= earned.required_count
  on conflict (user_id, achievement_code) do nothing;
end;
$$;
revoke all on function private.shahrvand_sync_achievement_awards(uuid) from public, anon, authenticated;

create or replace function private.shahrvand_award_achievement_after_weekly_task()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
  perform private.shahrvand_sync_achievement_awards(new.user_id);
  return new;
end;
$$;
revoke all on function private.shahrvand_award_achievement_after_weekly_task() from public, anon, authenticated;
drop trigger if exists shahrvand_award_after_weekly_task on public.shahrvand_weekly_completions;
create trigger shahrvand_award_after_weekly_task after insert on public.shahrvand_weekly_completions
for each row execute function private.shahrvand_award_achievement_after_weekly_task();

create or replace function private.shahrvand_award_achievement_after_activity()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
  if new.status = 'verified' and new.xp_awarded > 0 then
    perform private.shahrvand_sync_achievement_awards(new.user_id);
  end if;
  return new;
end;
$$;
revoke all on function private.shahrvand_award_achievement_after_activity() from public, anon, authenticated;
drop trigger if exists shahrvand_award_after_activity_insert on public.shahrvand_activity_sessions;
create trigger shahrvand_award_after_activity_insert after insert on public.shahrvand_activity_sessions
for each row execute function private.shahrvand_award_achievement_after_activity();
drop trigger if exists shahrvand_award_after_activity_verify on public.shahrvand_activity_sessions;
create trigger shahrvand_award_after_activity_verify after update of status on public.shahrvand_activity_sessions
for each row when (new.status = 'verified' and old.status is distinct from new.status)
execute function private.shahrvand_award_achievement_after_activity();

create or replace function private.shahrvand_get_my_achievements_impl()
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_count integer;
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
    + (select count(*) from public.shahrvand_activity_sessions a where a.user_id = v_user_id and a.status = 'verified' and a.xp_awarded > 0)
  into v_count;
  v_from := case when v_count < 10 then 0 when v_count < 25 then 10 when v_count < 50 then 25 when v_count < 100 then 50 else 100 end;
  v_next := case when v_count < 10 then 10 when v_count < 25 then 25 when v_count < 50 then 50 when v_count < 100 then 100 else 100 end;
  select coalesce(jsonb_agg(jsonb_build_object('code', achievement_code, 'kind', achievement_kind, 'awarded_at', awarded_at)
    order by awarded_at, achievement_code), '[]'::jsonb)
  into v_rows from public.shahrvand_achievement_awards where user_id = v_user_id;
  return jsonb_build_object(
    'ok', true, 'verified_tasks', v_count, 'progress_from', v_from, 'next_threshold', v_next,
    'progress', case when v_next = v_from then 1 else least(1, greatest(0, (v_count - v_from)::numeric / (v_next - v_from))) end,
    'achievements', v_rows
  );
end;
$$;
revoke all on function private.shahrvand_get_my_achievements_impl() from public, anon;
grant usage on schema private to authenticated;
grant execute on function private.shahrvand_get_my_achievements_impl() to authenticated;
create or replace function public.shahrvand_get_my_achievements()
returns jsonb language sql security invoker set search_path = ''
as $$ select private.shahrvand_get_my_achievements_impl(); $$;
revoke all on function public.shahrvand_get_my_achievements() from public, anon;
grant execute on function public.shahrvand_get_my_achievements() to authenticated;

do $$
declare v_user record;
begin
  for v_user in
    select user_id from public.shahrvand_weekly_completions
    union
    select user_id from public.shahrvand_activity_sessions where status = 'verified' and xp_awarded > 0
  loop
    perform private.shahrvand_sync_achievement_awards(v_user.user_id);
  end loop;
end;
$$;

drop function if exists public.shahrvand_get_leaderboard(text);
drop function if exists private.shahrvand_get_leaderboard_impl(text);
create function private.shahrvand_get_leaderboard_impl(p_period text, p_age_band text)
returns table(rank bigint, login text, weekly_xp bigint, tasks_completed bigint, is_me boolean, achievement_codes text[])
language plpgsql security definer set search_path = ''
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
    select c.user_id, c.xp_awarded::bigint xp, 1::bigint completed, c.created_at scored_at
      from public.shahrvand_weekly_completions c
      where p_period = 'all_time'
         or (p_period = 'week' and c.completed_on between v_week_start and v_today)
         or (p_period = 'month' and c.completed_on between v_month_start and v_today)
    union all
    select a.user_id, a.xp_awarded::bigint, 1::bigint, a.completed_at
      from public.shahrvand_activity_sessions a
      where a.status = 'verified' and a.xp_awarded > 0 and a.completed_at is not null
        and (p_period = 'all_time'
          or (p_period = 'week' and a.challenge_date between v_week_start and v_today)
          or (p_period = 'month' and a.challenge_date between v_month_start and v_today))
    union all
    select s.user_id, s.xp_awarded::bigint, 0::bigint, s.created_at
      from public.shahrvand_sleep_completions s
      where p_period = 'all_time'
         or (p_period = 'week' and s.wake_date between v_week_start and v_today)
         or (p_period = 'month' and s.wake_date between v_month_start and v_today)
  ), totals as (
    select a.user_id, sum(a.xp)::bigint xp, sum(a.completed)::bigint completed, min(a.scored_at) first_score
      from all_scores a group by a.user_id
  ), eligible as (
    select p.id, p.login, t.xp, t.completed, t.first_score
      from totals t join public.shahrvand_profiles p on p.id = t.user_id
      where p.age between v_min_age and v_max_age
  ), ranked as (
    select row_number() over(order by e.xp desc, e.completed desc, e.first_score asc, e.login asc) pos,
      e.login, e.xp, e.completed, e.id from eligible e
  )
  select r.pos, r.login, r.xp, r.completed, (r.id = v_user_id),
    coalesce((select array_agg(aw.achievement_code order by aw.achievement_code)
      from public.shahrvand_achievement_awards aw where aw.user_id = r.id), array[]::text[])
  from ranked r order by r.pos limit 100;
end;
$$;
revoke all on function private.shahrvand_get_leaderboard_impl(text,text) from public, anon;
grant execute on function private.shahrvand_get_leaderboard_impl(text,text) to authenticated;

create function public.shahrvand_get_leaderboard(p_period text, p_age_band text)
returns table(rank bigint, login text, weekly_xp bigint, tasks_completed bigint, is_me boolean, achievement_codes text[])
language sql security invoker set search_path = ''
as $$ select * from private.shahrvand_get_leaderboard_impl(p_period, p_age_band); $$;
revoke all on function public.shahrvand_get_leaderboard(text,text) from public, anon;
grant execute on function public.shahrvand_get_leaderboard(text,text) to authenticated;

create function public.shahrvand_get_leaderboard(p_age_band text)
returns table(rank bigint, login text, weekly_xp bigint, tasks_completed bigint, is_me boolean, achievement_codes text[])
language sql security invoker set search_path = ''
as $$ select * from private.shahrvand_get_leaderboard_impl('week', p_age_band); $$;
revoke all on function public.shahrvand_get_leaderboard(text) from public, anon;
grant execute on function public.shahrvand_get_leaderboard(text) to authenticated;
