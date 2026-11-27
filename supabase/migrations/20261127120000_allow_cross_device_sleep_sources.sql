-- Allow sleep sessions from any connected Health Connect provider.
-- The Android client still rejects manually entered sessions before requesting XP.
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
  if coalesce(p_is_manual, true) then
    raise exception 'MANUAL_SLEEP_RECORD_NOT_ELIGIBLE' using errcode = '22023';
  end if;
  -- Do not bind rewards to one manufacturer's package name. Health Connect
  -- normalizes sleep sessions from compatible vendor and third-party apps.
  if nullif(btrim(p_source_package), '') is null then
    raise exception 'SLEEP_SOURCE_REQUIRED' using errcode = '22023';
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
