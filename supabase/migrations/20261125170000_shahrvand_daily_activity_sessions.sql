-- SHAHRVAND daily activity competition: fixed shared tasks, verified sessions, user interests.
-- All six tasks are identical for every age group. The leaderboard remains age-separated.

create table if not exists public.shahrvand_activity_task_catalog (
  id text primary key,
  activity_type text not null check (activity_type in ('gps_walk','gps_run','gps_cycle','pose_squat','pose_pushup','learning')),
  title_tg text not null,
  title_ru text not null,
  title_en text not null,
  description_tg text not null,
  description_ru text not null,
  description_en text not null,
  target_distance_m integer not null default 0 check (target_distance_m >= 0),
  target_reps integer not null default 0 check (target_reps >= 0),
  time_limit_seconds integer not null default 0 check (time_limit_seconds >= 0),
  min_duration_seconds integer not null default 0 check (min_duration_seconds >= 0),
  xp smallint not null check (xp between 5 and 50),
  is_active boolean not null default true,
  sort_order smallint not null default 0
);

insert into public.shahrvand_activity_task_catalog
(id, activity_type, title_tg, title_ru, title_en, description_tg, description_ru, description_en,
 target_distance_m, target_reps, time_limit_seconds, min_duration_seconds, xp, sort_order)
values
('walk_4km','gps_walk',
 '4 км роҳ гард','Пройди 4 км пешком','Walk 4 km',
 'Масофаро бо GPS сабт кун. Қадамҳо аз Health Connect/Samsung Health санҷида мешаванд.',
 'Запиши дистанцию по GPS. Шаги сверяются с Health Connect/Samsung Health.',
 'Record the route with GPS. Steps are cross-checked with Health Connect/Samsung Health.',
 4000,0,0,900,15,1),
('run_1_5km','gps_run',
 '1,5 км давидан — то 15 дақиқа','Пробеги 1,5 км — до 15 минут','Run 1.5 km — within 15 minutes',
 'GPS, суръат, қадамҳо ва ҳаракати сенсорҳо санҷида мешаванд.',
 'Проверяются GPS, скорость, шаги и данные датчиков движения.',
 'GPS, speed, steps, and motion-sensor data are checked.',
 1500,0,900,90,20,2),
('cycle_2_5km','gps_cycle',
 '2,5 км велосипед — то 10 дақиқа','Проедь 2,5 км на велосипеде — до 10 минут','Cycle 2.5 km — within 10 minutes',
 'GPS ва суръати ҳаракат санҷида мешаванд; ҳаракати мошин ё мотосикл далели велосипед нест.',
 'Проверяются GPS и скорость; поездка на машине или мотоцикле не считается велосипедом.',
 'GPS and speed are checked; a car or motorcycle trip does not qualify as cycling.',
 2500,0,600,90,15,3),
('squat_25','pose_squat',
 '25 приседание — то 45 сония','25 приседаний — до 45 секунд','25 squats — within 45 seconds',
 'Танҳо такрорҳои дуруст бо муайянкунии ҳолати бадан дар телефон ҳисоб мешаванд. Кӯшиши нав иҷозат аст.',
 'Считаются только правильные повторения по локальному анализу позы. Попытку можно повторить.',
 'Only correct reps detected by on-device pose analysis count. You may retry.',
 0,25,45,8,15,4),
('pushup_30','pose_pushup',
 '30 отжимание — то 2 дақиқаю 30 сония','30 отжиманий — до 2 минут 30 секунд','30 push-ups — within 2 minutes 30 seconds',
 'Танҳо такрорҳои дуруст ҳисоб мешаванд. Иҷро дар вақти аз меъёр зиёд қабул намешавад.',
 'Считаются только правильные повторения. Выполнение дольше лимита не принимается.',
 'Only correct reps count. Finishing after the time limit is not accepted.',
 0,30,150,15,20,5),
('learning_20m','learning',
 '20 дақиқа омӯзиш','20 минут обучения','20 minutes of learning',
 'Дарс аз рӯи яке аз 5 шавқи интихобкардаат; баъд 5 саволи санҷишӣ. На танҳо таймер.',
 'Урок по одному из 5 выбранных интересов, затем 5 проверочных вопросов. Одного таймера недостаточно.',
 'A lesson from one of your five selected interests, followed by five quiz questions. A timer alone is not enough.',
 0,0,2400,1200,15,6)
on conflict (id) do update set
 activity_type=excluded.activity_type,
 title_tg=excluded.title_tg, title_ru=excluded.title_ru, title_en=excluded.title_en,
 description_tg=excluded.description_tg, description_ru=excluded.description_ru, description_en=excluded.description_en,
 target_distance_m=excluded.target_distance_m, target_reps=excluded.target_reps,
 time_limit_seconds=excluded.time_limit_seconds, min_duration_seconds=excluded.min_duration_seconds,
 xp=excluded.xp, is_active=true, sort_order=excluded.sort_order;

-- Turn off the previous generic 42-task calendar. It is retained for audit/history only.
update public.shahrvand_weekly_tasks set is_active = false where is_active = true;

create table if not exists public.shahrvand_user_interests (
  user_id uuid not null references auth.users(id) on delete cascade,
  interest_code text not null check (interest_code in (
    'science','technology','history','geography','mathematics','languages',
    'health','nature','culture','arts','sports','finance'
  )),
  created_at timestamptz not null default now(),
  primary key (user_id, interest_code)
);
create index if not exists shahrvand_interests_user_idx on public.shahrvand_user_interests(user_id);

create table if not exists public.shahrvand_activity_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  task_id text not null references public.shahrvand_activity_task_catalog(id),
  challenge_date date not null,
  challenge_nonce text not null,
  status text not null default 'pending' check (status in ('pending','verified','rejected','expired','cancelled')),
  started_at timestamptz not null default now(),
  expires_at timestamptz not null,
  completed_at timestamptz,
  xp_awarded smallint not null default 0 check (xp_awarded between 0 and 55),
  evidence jsonb not null default '{}'::jsonb,
  rejection_code text,
  created_at timestamptz not null default now()
);
create index if not exists shahrvand_activity_sessions_user_day_idx
  on public.shahrvand_activity_sessions(user_id, challenge_date, status);
create index if not exists shahrvand_activity_sessions_week_rank_idx
  on public.shahrvand_activity_sessions(challenge_date, status, user_id, xp_awarded);
create unique index if not exists shahrvand_activity_one_verified_task_per_day
  on public.shahrvand_activity_sessions(user_id, task_id, challenge_date)
  where status = 'verified';

alter table public.shahrvand_activity_task_catalog enable row level security;
alter table public.shahrvand_user_interests enable row level security;
alter table public.shahrvand_activity_sessions enable row level security;

revoke all on table public.shahrvand_activity_task_catalog from anon, authenticated;
grant select on table public.shahrvand_activity_task_catalog to authenticated;
drop policy if exists shahrvand_activity_catalog_read on public.shahrvand_activity_task_catalog;
create policy shahrvand_activity_catalog_read on public.shahrvand_activity_task_catalog
  for select to authenticated using (is_active = true);

revoke all on table public.shahrvand_user_interests from anon, authenticated;
grant select on table public.shahrvand_user_interests to authenticated;
drop policy if exists shahrvand_interests_read_own on public.shahrvand_user_interests;
create policy shahrvand_interests_read_own on public.shahrvand_user_interests
  for select to authenticated using ((select auth.uid()) = user_id);

revoke all on table public.shahrvand_activity_sessions from anon, authenticated;
grant select on table public.shahrvand_activity_sessions to authenticated;
drop policy if exists shahrvand_activity_sessions_read_own on public.shahrvand_activity_sessions;
create policy shahrvand_activity_sessions_read_own on public.shahrvand_activity_sessions
  for select to authenticated using ((select auth.uid()) = user_id);

create or replace function private.shahrvand_set_my_interests_impl(p_codes text[])
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  if p_codes is null or cardinality(p_codes) <> 5
     or (select count(distinct x) from unnest(p_codes) x) <> 5
     or exists (
       select 1 from unnest(p_codes) x
       where x not in ('science','technology','history','geography','mathematics','languages',
                       'health','nature','culture','arts','sports','finance')
     )
  then raise exception 'SELECT_EXACTLY_FIVE_INTERESTS' using errcode='22023'; end if;

  delete from public.shahrvand_user_interests where user_id = v_user;
  insert into public.shahrvand_user_interests(user_id, interest_code)
  select v_user, x from unnest(p_codes) x;
  return jsonb_build_object('ok',true,'interests',to_jsonb(p_codes));
end; $$;
revoke all on function private.shahrvand_set_my_interests_impl(text[]) from public, anon;
grant execute on function private.shahrvand_set_my_interests_impl(text[]) to authenticated;

create or replace function public.shahrvand_set_my_interests(p_codes text[])
returns jsonb language sql security invoker set search_path = '' as $$
  select private.shahrvand_set_my_interests_impl(p_codes);
$$;
revoke all on function public.shahrvand_set_my_interests(text[]) from public, anon;
grant execute on function public.shahrvand_set_my_interests(text[]) to authenticated;

create or replace function private.shahrvand_start_activity_session_impl(p_task_id text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_user uuid := auth.uid();
  v_today date := (now() at time zone 'Asia/Dushanbe')::date;
  v_age smallint;
  v_task public.shahrvand_activity_task_catalog%rowtype;
  v_session public.shahrvand_activity_sessions%rowtype;
  v_limit interval;
  v_attempts integer;
begin
  if v_user is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  select age into v_age from public.shahrvand_profiles where id = v_user;
  if v_age is null then raise exception 'PROFILE_REQUIRED' using errcode='P0002'; end if;
  select * into v_task from public.shahrvand_activity_task_catalog where id=p_task_id and is_active=true;
  if not found then raise exception 'TASK_NOT_AVAILABLE' using errcode='22023'; end if;

  if exists (select 1 from public.shahrvand_activity_sessions
             where user_id=v_user and task_id=p_task_id and challenge_date=v_today and status='verified')
  then raise exception 'TASK_ALREADY_COMPLETED' using errcode='23505'; end if;

  update public.shahrvand_activity_sessions
    set status='expired', rejection_code='SESSION_EXPIRED'
    where user_id=v_user and status='pending' and expires_at <= now();

  if exists (select 1 from public.shahrvand_activity_sessions
             where user_id=v_user and status='pending' and expires_at > now())
  then raise exception 'ANOTHER_SESSION_ACTIVE' using errcode='55000'; end if;

  select count(*) into v_attempts from public.shahrvand_activity_sessions
    where user_id=v_user and task_id=p_task_id and challenge_date=v_today;
  if v_attempts >= 8 then raise exception 'DAILY_ATTEMPT_LIMIT' using errcode='54000'; end if;

  v_limit := case
    when v_task.id='walk_4km' then interval '4 hours'
    when v_task.id='run_1_5km' then interval '16 minutes'
    when v_task.id='cycle_2_5km' then interval '11 minutes'
    when v_task.id='squat_25' then interval '60 seconds'
    when v_task.id='pushup_30' then interval '165 seconds'
    else interval '40 minutes'
  end;

  insert into public.shahrvand_activity_sessions
    (user_id,task_id,challenge_date,challenge_nonce,expires_at)
  values (v_user,p_task_id,v_today,gen_random_uuid()::text,now()+v_limit)
  returning * into v_session;

  return jsonb_build_object('ok',true,'session_id',v_session.id,'nonce',v_session.challenge_nonce,
    'started_at',v_session.started_at,'expires_at',v_session.expires_at,
    'task_id',v_session.task_id,'server_date',v_today);
end; $$;
revoke all on function private.shahrvand_start_activity_session_impl(text) from public, anon;
grant execute on function private.shahrvand_start_activity_session_impl(text) to authenticated;

create or replace function public.shahrvand_start_activity_session(p_task_id text)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.shahrvand_start_activity_session_impl(p_task_id);
$$;
revoke all on function public.shahrvand_start_activity_session(text) from public, anon;
grant execute on function public.shahrvand_start_activity_session(text) to authenticated;

create or replace function private.shahrvand_finish_activity_session_impl(
  p_session_id uuid, p_nonce text, p_evidence jsonb
) returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_user uuid := auth.uid();
  v_session public.shahrvand_activity_sessions%rowtype;
  v_task public.shahrvand_activity_task_catalog%rowtype;
  v_elapsed numeric;
  v_distance integer := coalesce((p_evidence->>'distance_m')::integer,0);
  v_steps integer := coalesce((p_evidence->>'step_count')::integer,0);
  v_gps_samples integer := coalesce((p_evidence->>'gps_samples')::integer,0);
  v_accuracy numeric := coalesce((p_evidence->>'avg_accuracy_m')::numeric,999);
  v_max_speed numeric := coalesce((p_evidence->>'max_speed_kmh')::numeric,999);
  v_reps integer := coalesce((p_evidence->>'valid_reps')::integer,0);
  v_frames integer := coalesce((p_evidence->>'camera_frames')::integer,0);
  v_pose_visible numeric := coalesce((p_evidence->>'pose_visible_ratio')::numeric,0);
  v_pose_confidence numeric := coalesce((p_evidence->>'pose_confidence')::numeric,0);
  v_foreground_ms bigint := coalesce((p_evidence->>'foreground_ms')::bigint,0);
  v_quiz_total integer := coalesce((p_evidence->>'quiz_total')::integer,0);
  v_quiz_correct integer := coalesce((p_evidence->>'quiz_correct')::integer,0);
  v_interest text := coalesce(p_evidence->>'interest_code','');
  v_avg_speed numeric;
  v_xp integer;
  v_reject text := null;
  v_week_start date;
  v_weekly_xp integer;
begin
  if v_user is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  if p_evidence is null or jsonb_typeof(p_evidence) <> 'object' then
    raise exception 'INVALID_EVIDENCE' using errcode='22023';
  end if;

  select * into v_session from public.shahrvand_activity_sessions
   where id=p_session_id and user_id=v_user for update;
  if not found then raise exception 'SESSION_NOT_FOUND' using errcode='P0002'; end if;
  if v_session.status <> 'pending' then raise exception 'SESSION_NOT_PENDING' using errcode='55000'; end if;
  if p_nonce is null or p_nonce <> v_session.challenge_nonce then raise exception 'INVALID_NONCE' using errcode='22023'; end if;

  v_elapsed := extract(epoch from (now() - v_session.started_at));
  if now() > v_session.expires_at then v_reject := 'TIME_LIMIT_EXCEEDED'; end if;

  select * into v_task from public.shahrvand_activity_task_catalog where id=v_session.task_id;
  if not found or not v_task.is_active then v_reject := coalesce(v_reject,'TASK_DISABLED'); end if;

  if v_task.activity_type in ('gps_walk','gps_run','gps_cycle') then
    if v_distance < v_task.target_distance_m then v_reject := coalesce(v_reject,'DISTANCE_NOT_REACHED'); end if;
    if v_gps_samples < 12 then v_reject := coalesce(v_reject,'INSUFFICIENT_GPS_SAMPLES'); end if;
    if v_accuracy > 25 then v_reject := coalesce(v_reject,'GPS_ACCURACY_TOO_LOW'); end if;
    if v_max_speed > 55 then v_reject := coalesce(v_reject,'GPS_SPEED_IMPLAUSIBLE'); end if;
    if length(coalesce(p_evidence->>'route_hash','')) < 32 then v_reject := coalesce(v_reject,'ROUTE_EVIDENCE_MISSING'); end if;
    if v_elapsed > 0 then v_avg_speed := (v_distance::numeric / 1000) / (v_elapsed / 3600); else v_avg_speed := 999; end if;

    if v_task.id='walk_4km' then
      if v_elapsed < 900 or v_elapsed > 14400 then v_reject := coalesce(v_reject,'WALK_DURATION_IMPLAUSIBLE'); end if;
      if v_steps < 3500 then v_reject := coalesce(v_reject,'STEP_COUNT_TOO_LOW'); end if;
      if v_avg_speed > 15 or v_max_speed > 16 then v_reject := coalesce(v_reject,'WALK_SPEED_IMPLAUSIBLE'); end if;
    elsif v_task.id='run_1_5km' then
      if v_elapsed < 90 or v_elapsed > 900 then v_reject := coalesce(v_reject,'RUN_TIME_LIMIT_EXCEEDED'); end if;
      if v_avg_speed < 6 or v_avg_speed > 30 or v_max_speed > 32 then v_reject := coalesce(v_reject,'RUN_SPEED_IMPLAUSIBLE'); end if;
      if v_steps < 1000 then v_reject := coalesce(v_reject,'RUN_STEP_EVIDENCE_LOW'); end if;
    elsif v_task.id='cycle_2_5km' then
      if v_elapsed < 90 or v_elapsed > 600 then v_reject := coalesce(v_reject,'CYCLE_TIME_LIMIT_EXCEEDED'); end if;
      if v_avg_speed < 15 or v_avg_speed > 45 or v_max_speed > 50 then v_reject := coalesce(v_reject,'CYCLE_SPEED_IMPLAUSIBLE'); end if;
    end if;
  elsif v_task.activity_type='pose_squat' or v_task.activity_type='pose_pushup' then
    if v_reps < v_task.target_reps then v_reject := coalesce(v_reject,'VALID_REPS_NOT_REACHED'); end if;
    if v_frames < 30 or v_pose_visible < 0.65 or v_pose_confidence < 0.65 then
      v_reject := coalesce(v_reject,'POSE_EVIDENCE_LOW');
    end if;
    if v_elapsed < v_task.min_duration_seconds or v_elapsed > v_task.time_limit_seconds then
      v_reject := coalesce(v_reject,'EXERCISE_TIME_LIMIT_INVALID');
    end if;
  elsif v_task.activity_type='learning' then
    if v_elapsed < 1200 or v_foreground_ms < 1140000 then v_reject := coalesce(v_reject,'LEARNING_TIME_INSUFFICIENT'); end if;
    if v_quiz_total <> 5 or v_quiz_correct < 3 then v_reject := coalesce(v_reject,'QUIZ_REQUIREMENT_NOT_MET'); end if;
    if not exists (select 1 from public.shahrvand_user_interests i where i.user_id=v_user and i.interest_code=v_interest) then
      v_reject := coalesce(v_reject,'INTEREST_NOT_SELECTED');
    end if;
  end if;

  if v_reject is not null then
    update public.shahrvand_activity_sessions set status='rejected',completed_at=now(),
      evidence=p_evidence,rejection_code=v_reject
      where id=v_session.id;
    return jsonb_build_object('ok',false,'rejected',true,'reason',v_reject,'retry_allowed',true);
  end if;

  v_xp := v_task.xp;
  if v_task.id='learning_20m' and v_quiz_correct >= 4 then v_xp := v_xp + 5; end if;

  update public.shahrvand_activity_sessions set status='verified',completed_at=now(),
    xp_awarded=v_xp,evidence=p_evidence
    where id=v_session.id;

  v_week_start := date_trunc('week',v_session.challenge_date::timestamp)::date;
  select coalesce(sum(xp_awarded),0)::integer into v_weekly_xp
    from public.shahrvand_activity_sessions
    where user_id=v_user and challenge_date between v_week_start and v_week_start+6 and status='verified';

  return jsonb_build_object('ok',true,'verified',true,'xp_awarded',v_xp,
    'weekly_xp',v_weekly_xp,'completed_on',v_session.challenge_date,'activity_type',v_task.activity_type);
end; $$;
revoke all on function private.shahrvand_finish_activity_session_impl(uuid,text,jsonb) from public, anon;
grant execute on function private.shahrvand_finish_activity_session_impl(uuid,text,jsonb) to authenticated;

create or replace function public.shahrvand_finish_activity_session(p_session_id uuid, p_nonce text, p_evidence jsonb)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.shahrvand_finish_activity_session_impl(p_session_id,p_nonce,p_evidence);
$$;
revoke all on function public.shahrvand_finish_activity_session(uuid,text,jsonb) from public, anon;
grant execute on function public.shahrvand_finish_activity_session(uuid,text,jsonb) to authenticated;

-- Switch leaderboard totals to server-verified activity sessions only.
create or replace function private.shahrvand_get_leaderboard_impl(p_age_band text)
returns table(rank bigint,login text,weekly_xp bigint,tasks_completed bigint,is_me boolean)
language plpgsql security definer set search_path='' as $$
declare
  v_user_id uuid:=auth.uid();
  v_today date:=(now() at time zone 'Asia/Dushanbe')::date;
  v_week_start date:=date_trunc('week',v_today::timestamp)::date;
  v_min_age integer;
  v_max_age integer;
begin
  if v_user_id is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  if p_age_band not in ('7_12','13_17','18_plus') then raise exception 'INVALID_AGE_BAND' using errcode='22023'; end if;
  v_min_age:=case p_age_band when '7_12' then 7 when '13_17' then 13 else 18 end;
  v_max_age:=case p_age_band when '7_12' then 12 when '13_17' then 17 else 120 end;
  return query
  with totals as (
    select s.user_id,sum(s.xp_awarded)::bigint xp,count(*)::bigint completed,min(s.completed_at) first_score
    from public.shahrvand_activity_sessions s
    where s.challenge_date between v_week_start and v_week_start+6 and s.status='verified'
    group by s.user_id
  ), eligible as (
    select p.id,p.login,t.xp,t.completed,t.first_score
    from totals t join public.shahrvand_profiles p on p.id=t.user_id
    where p.age between v_min_age and v_max_age
  ), ranked as (
    select row_number() over(order by e.xp desc,e.completed desc,e.first_score asc,e.login asc) pos,
      e.login,e.xp,e.completed,e.id
    from eligible e
  )
  select r.pos,r.login,r.xp,r.completed,(r.id=v_user_id) from ranked r order by r.pos limit 100;
end; $$;
revoke all on function private.shahrvand_get_leaderboard_impl(text) from public, anon;
grant execute on function private.shahrvand_get_leaderboard_impl(text) to authenticated;

-- Explicit grants are required for newly-created public tables/functions on current Supabase projects.
grant select on public.shahrvand_activity_task_catalog, public.shahrvand_user_interests,
  public.shahrvand_activity_sessions to authenticated;
grant execute on function public.shahrvand_set_my_interests(text[]),
  public.shahrvand_start_activity_session(text),
  public.shahrvand_finish_activity_session(uuid,text,jsonb),
  public.shahrvand_get_leaderboard(text) to authenticated;
revoke all on function public.shahrvand_set_my_interests(text[]),
  public.shahrvand_start_activity_session(text),
  public.shahrvand_finish_activity_session(uuid,text,jsonb) from anon;
