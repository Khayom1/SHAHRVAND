-- Restore the five agreed challenge definitions and stop exposing cycling.
update public.shahrvand_activity_task_catalog
set target_distance_m=4000, target_reps=0, min_duration_seconds=0, time_limit_seconds=0,
    title_tg='4 км роҳ гард', title_ru='Пройди 4 км пешком', title_en='Walk 4 km',
    description_tg='4 километрро бо GPS тай кун. Вақти иҷро маҳдуд нест; масофа ва дақиқии GPS санҷида мешаванд.',
    description_ru='Пройди 4 км с GPS. Ограничения по времени нет; проверяются расстояние и точность GPS.',
    description_en='Walk 4 km using GPS. There is no time limit; distance and GPS accuracy are checked.'
where id='walk_4km';

update public.shahrvand_activity_task_catalog
set target_distance_m=1500, target_reps=0, min_duration_seconds=0, time_limit_seconds=0,
    title_tg='1,5 км давидан', title_ru='Пробеги 1,5 км', title_en='Run 1.5 km',
    description_tg='1,5 километрро бо GPS давида тай кун. Вақти иҷро маҳдуд нест; масофа ва дақиқии GPS санҷида мешаванд.',
    description_ru='Пробеги 1,5 км с GPS. Ограничения по времени нет; проверяются расстояние и точность GPS.',
    description_en='Run 1.5 km using GPS. There is no time limit; distance and GPS accuracy are checked.'
where id='run_1_5km';

update public.shahrvand_activity_task_catalog
set target_reps=25, min_duration_seconds=0, time_limit_seconds=45,
    title_tg='25 нишасту хез дар 45 сония', title_ru='25 приседаний за 45 секунд',
    title_en='25 squats in 45 seconds',
    description_tg='25 такрори пурра ва дурустро дар 45 сония иҷро кун. Ҳисобкунӣ дар худи телефон аст; видео фиристода намешавад.',
    description_ru='Выполни 25 полных правильных приседаний за 45 секунд. Подсчёт выполняется на телефоне; видео не отправляется.',
    description_en='Complete 25 full, correct squats within 45 seconds. Pose counting stays on-device; no video is uploaded.'
where id='squat_25';

update public.shahrvand_activity_task_catalog
set target_reps=30, min_duration_seconds=0, time_limit_seconds=150,
    title_tg='30 отжимание дар 2 дақиқаю 30 сония',
    title_ru='30 отжиманий за 2 минуты 30 секунд',
    title_en='30 push-ups in 2 minutes 30 seconds',
    description_tg='30 такрори пурра ва дурустро дар 150 сония иҷро кун. Ҳисобкунӣ дар худи телефон аст; видео фиристода намешавад.',
    description_ru='Выполни 30 полных правильных отжиманий за 150 секунд. Подсчёт выполняется на телефоне; видео не отправляется.',
    description_en='Complete 30 full, correct push-ups within 150 seconds. Pose counting stays on-device; no video is uploaded.'
where id='pushup_30';

update public.shahrvand_activity_task_catalog
set min_duration_seconds=1200, time_limit_seconds=0,
    description_tg='20 дақиқа омӯзиши фаъол аз рӯи ҳар 5 шавқи интихобкардаат, баъд 5 саволи санҷишӣ.',
    description_ru='20 минут активного обучения по всем 5 выбранным интересам, затем 5 проверочных вопросов.',
    description_en='20 minutes of active learning across all 5 selected interests, followed by a 5-question quiz.'
where id='learning_20m';

update public.shahrvand_activity_task_catalog
set is_active=false where id='cycle_2_5km';

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
  select age into v_age from public.shahrvand_profiles where id=v_user;
  if v_age is null then raise exception 'PROFILE_REQUIRED' using errcode='P0002'; end if;

  select * into v_task from public.shahrvand_activity_task_catalog
  where id=p_task_id and is_active=true;
  if not found then raise exception 'TASK_NOT_AVAILABLE' using errcode='22023'; end if;

  if exists (
    select 1 from public.shahrvand_activity_sessions
    where user_id=v_user and task_id=p_task_id and challenge_date=v_today and status='verified'
  ) then raise exception 'TASK_ALREADY_COMPLETED' using errcode='23505'; end if;

  update public.shahrvand_activity_sessions
  set status='expired',rejection_code='SESSION_EXPIRED'
  where user_id=v_user and status='pending' and expires_at<=now();

  if exists (
    select 1 from public.shahrvand_activity_sessions
    where user_id=v_user and status='pending' and expires_at>now()
  ) then raise exception 'ANOTHER_SESSION_ACTIVE' using errcode='55000'; end if;

  select count(*) into v_attempts from public.shahrvand_activity_sessions
  where user_id=v_user and task_id=p_task_id and challenge_date=v_today;
  if v_attempts>=8 then raise exception 'DAILY_ATTEMPT_LIMIT' using errcode='54000'; end if;

  v_limit := case
    when v_task.id='squat_25' then interval '50 seconds'
    when v_task.id='pushup_30' then interval '155 seconds'
    else interval '24 hours'
  end;

  insert into public.shahrvand_activity_sessions
    (user_id,task_id,challenge_date,challenge_nonce,expires_at)
  values (v_user,p_task_id,v_today,gen_random_uuid()::text,now()+v_limit)
  returning * into v_session;

  return jsonb_build_object('ok',true,'session_id',v_session.id,'nonce',v_session.challenge_nonce,
    'started_at',v_session.started_at,'expires_at',v_session.expires_at,
    'task_id',v_session.task_id,'server_date',v_today);
end;
$$;

create or replace function private.shahrvand_finish_activity_session_impl(
  p_session_id uuid,p_nonce text,p_evidence jsonb
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_user uuid := auth.uid();
  v_session public.shahrvand_activity_sessions%rowtype;
  v_task public.shahrvand_activity_task_catalog%rowtype;
  v_elapsed numeric;
  v_distance numeric := coalesce((p_evidence->>'distance_m')::numeric,0);
  v_gps_samples numeric := coalesce((p_evidence->>'gps_samples')::numeric,0);
  v_accuracy numeric := coalesce((p_evidence->>'avg_accuracy_m')::numeric,999);
  v_max_speed numeric := coalesce((p_evidence->>'max_speed_kmh')::numeric,999);
  v_reps numeric := coalesce((p_evidence->>'valid_reps')::numeric,0);
  v_frames numeric := coalesce((p_evidence->>'camera_frames')::numeric,0);
  v_pose_visible numeric := coalesce((p_evidence->>'pose_visible_ratio')::numeric,0);
  v_pose_confidence numeric := coalesce((p_evidence->>'pose_confidence')::numeric,0);
  v_foreground_ms numeric := coalesce((p_evidence->>'foreground_ms')::numeric,0);
  v_quiz_total numeric := coalesce((p_evidence->>'quiz_total')::numeric,0);
  v_quiz_correct numeric := coalesce((p_evidence->>'quiz_correct')::numeric,0);
  v_xp integer;
  v_reject text := null;
  v_week_start date;
  v_weekly_xp integer;
begin
  if v_user is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  if p_evidence is null or jsonb_typeof(p_evidence)<>'object'
     or octet_length(p_evidence::text)>8192 then
    raise exception 'INVALID_EVIDENCE' using errcode='22023';
  end if;

  if exists (
    select 1 from jsonb_each(p_evidence) e
    where e.key=any(array[
      'distance_m','gps_samples','avg_accuracy_m','max_speed_kmh','valid_reps',
      'camera_frames','pose_visible_ratio','pose_confidence','foreground_ms','quiz_total','quiz_correct'
    ]) and jsonb_typeof(e.value)<>'number'
  ) then raise exception 'INVALID_EVIDENCE_TYPE' using errcode='22023'; end if;

  if v_distance<0 or v_distance>100000
     or v_gps_samples<0 or v_gps_samples>10000
     or v_accuracy<0 or v_accuracy>1000
     or v_max_speed<0 or v_max_speed>200
     or v_reps<0 or v_reps>2000
     or v_frames<0 or v_frames>200000
     or v_pose_visible<0 or v_pose_visible>1
     or v_pose_confidence<0 or v_pose_confidence>1
     or v_foreground_ms<0 or v_foreground_ms>14400000
     or v_quiz_total<0 or v_quiz_total>20
     or v_quiz_correct<0 or v_quiz_correct>v_quiz_total then
    raise exception 'EVIDENCE_OUT_OF_RANGE' using errcode='22023';
  end if;

  select * into v_session from public.shahrvand_activity_sessions
  where id=p_session_id and user_id=v_user for update;
  if not found then raise exception 'SESSION_NOT_FOUND' using errcode='P0002'; end if;
  if v_session.status<>'pending' then raise exception 'SESSION_NOT_PENDING' using errcode='55000'; end if;
  if p_nonce is null or p_nonce<>v_session.challenge_nonce then
    raise exception 'INVALID_NONCE' using errcode='22023';
  end if;

  v_elapsed := extract(epoch from (now()-v_session.started_at));
  if now()>v_session.expires_at then v_reject:='TIME_LIMIT_EXCEEDED'; end if;

  select * into v_task from public.shahrvand_activity_task_catalog where id=v_session.task_id;
  if not found or not v_task.is_active then
    v_reject:=coalesce(v_reject,'TASK_DISABLED');
  elsif v_task.activity_type in ('gps_walk','gps_run') then
    if v_distance<v_task.target_distance_m then v_reject:=coalesce(v_reject,'DISTANCE_NOT_REACHED'); end if;
    if v_gps_samples<12 then v_reject:=coalesce(v_reject,'INSUFFICIENT_GPS_SAMPLES'); end if;
    if v_accuracy>25 then v_reject:=coalesce(v_reject,'GPS_ACCURACY_TOO_LOW'); end if;
    if v_max_speed>55 then v_reject:=coalesce(v_reject,'GPS_SPEED_IMPLAUSIBLE'); end if;
    if coalesce(p_evidence->>'route_hash','') !~ '^[0-9a-f]{64}$' then
      v_reject:=coalesce(v_reject,'ROUTE_EVIDENCE_INVALID');
    end if;
  elsif v_task.activity_type in ('pose_squat','pose_pushup') then
    if v_reps<v_task.target_reps then v_reject:=coalesce(v_reject,'VALID_REPS_NOT_REACHED'); end if;
    if v_frames<30 or v_pose_visible<0.65 or v_pose_confidence<0.65 then
      v_reject:=coalesce(v_reject,'POSE_EVIDENCE_LOW');
    end if;
    if v_elapsed>v_task.time_limit_seconds then
      v_reject:=coalesce(v_reject,'EXERCISE_TIME_LIMIT_EXCEEDED');
    end if;
  elsif v_task.activity_type='learning' then
    if v_elapsed<1200 or v_foreground_ms<1200000 then
      v_reject:=coalesce(v_reject,'LEARNING_TIME_INSUFFICIENT');
    end if;
    if v_foreground_ms>(v_elapsed*1000)+3000 then
      v_reject:=coalesce(v_reject,'LEARNING_FOREGROUND_TIME_INVALID');
    end if;
    if v_quiz_total<>5 or v_quiz_correct<3 then
      v_reject:=coalesce(v_reject,'QUIZ_REQUIREMENT_NOT_MET');
    end if;
    if jsonb_typeof(p_evidence->'interest_codes') is distinct from 'array' then
      v_reject:=coalesce(v_reject,'INTEREST_SELECTION_INVALID');
    elsif jsonb_array_length(p_evidence->'interest_codes')<>5 then
      v_reject:=coalesce(v_reject,'INTEREST_SELECTION_INVALID');
    elsif (
      select count(distinct x.code)
      from jsonb_array_elements_text(p_evidence->'interest_codes') as x(code)
    )<>5 then
      v_reject:=coalesce(v_reject,'INTEREST_SELECTION_INVALID');
    elsif exists (
      select 1 from jsonb_array_elements_text(p_evidence->'interest_codes') as x(code)
      where not exists (
        select 1 from public.shahrvand_user_interests i
        where i.user_id=v_user and i.interest_code=x.code
      )
    ) then
      v_reject:=coalesce(v_reject,'INTEREST_SELECTION_INVALID');
    end if;
  end if;

  if v_reject is not null then
    update public.shahrvand_activity_sessions
    set status='rejected',completed_at=now(),evidence=p_evidence,rejection_code=v_reject
    where id=v_session.id;
    return jsonb_build_object('ok',false,'rejected',true,'reason',v_reject,'retry_allowed',true);
  end if;

  v_xp:=v_task.xp;
  if v_task.id='learning_20m' and v_quiz_correct>=4 then v_xp:=v_xp+5; end if;

  update public.shahrvand_activity_sessions
  set status='verified',completed_at=now(),xp_awarded=v_xp,evidence=p_evidence
  where id=v_session.id;

  v_week_start:=date_trunc('week',v_session.challenge_date::timestamp)::date;
  select coalesce(sum(xp_awarded),0)::integer into v_weekly_xp
  from public.shahrvand_activity_sessions
  where user_id=v_user and challenge_date between v_week_start and v_week_start+6 and status='verified';

  return jsonb_build_object('ok',true,'verified',true,'xp_awarded',v_xp,'weekly_xp',v_weekly_xp,
    'completed_on',v_session.challenge_date,'activity_type',v_task.activity_type);
end;
$$;

revoke all on function private.shahrvand_start_activity_session_impl(text) from public,anon;
grant execute on function private.shahrvand_start_activity_session_impl(text) to authenticated;
revoke all on function private.shahrvand_finish_activity_session_impl(uuid,text,jsonb) from public,anon;
grant execute on function private.shahrvand_finish_activity_session_impl(uuid,text,jsonb) to authenticated;

create or replace function public.shahrvand_start_activity_session(p_task_id text)
returns jsonb language sql security invoker set search_path='' as $$
  select private.shahrvand_start_activity_session_impl(p_task_id);
$$;
revoke all on function public.shahrvand_start_activity_session(text) from public,anon;
grant execute on function public.shahrvand_start_activity_session(text) to authenticated;

create or replace function public.shahrvand_finish_activity_session(p_session_id uuid,p_nonce text,p_evidence jsonb)
returns jsonb language sql security invoker set search_path='' as $$
  select private.shahrvand_finish_activity_session_impl(p_session_id,p_nonce,p_evidence);
$$;
revoke all on function public.shahrvand_finish_activity_session(uuid,text,jsonb) from public,anon;
grant execute on function public.shahrvand_finish_activity_session(uuid,text,jsonb) to authenticated;
