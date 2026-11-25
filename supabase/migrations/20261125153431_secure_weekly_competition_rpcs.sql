-- Applied to Supabase as migration 20261125153431_secure_weekly_competition_rpcs.
create schema if not exists private;
revoke all on schema private from public,anon;
grant usage on schema private to authenticated;
create function private.shahrvand_complete_weekly_task_impl(p_task_id text) returns jsonb language plpgsql security definer set search_path='' as $$
declare v_user_id uuid:=auth.uid(); v_today date:=(now() at time zone 'Asia/Dushanbe')::date;
v_week_start date:=date_trunc('week',v_today::timestamp)::date; v_weekday smallint:=extract(isodow from v_today)::smallint;
v_age smallint; v_task public.shahrvand_weekly_tasks%rowtype; v_inserted public.shahrvand_weekly_completions%rowtype; v_total_xp integer;
begin
if v_user_id is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
select age into v_age from public.shahrvand_profiles where id=v_user_id;
if v_age is null then raise exception 'PROFILE_REQUIRED' using errcode='P0002'; end if;
select * into v_task from public.shahrvand_weekly_tasks where id=p_task_id and is_active=true and weekday=v_weekday and min_age<=v_age;
if not found then raise exception 'TASK_NOT_AVAILABLE_TODAY' using errcode='22023'; end if;
insert into public.shahrvand_weekly_completions(user_id,task_id,completed_on,week_start,xp_awarded)
values(v_user_id,v_task.id,v_today,v_week_start,v_task.xp) on conflict(user_id,task_id,completed_on) do nothing returning * into v_inserted;
if v_inserted.id is null then raise exception 'TASK_ALREADY_COMPLETED' using errcode='23505'; end if;
select coalesce(sum(xp_awarded),0)::integer into v_total_xp from public.shahrvand_weekly_completions where user_id=v_user_id and week_start=v_week_start;
return jsonb_build_object('ok',true,'xp_awarded',v_task.xp,'weekly_xp',v_total_xp,'completed_on',v_today,'week_start',v_week_start);
end; $$;
revoke all on function private.shahrvand_complete_weekly_task_impl(text) from public,anon;
grant execute on function private.shahrvand_complete_weekly_task_impl(text) to authenticated;
create function public.shahrvand_complete_weekly_task(p_task_id text) returns jsonb language sql security invoker set search_path='' as $$ select private.shahrvand_complete_weekly_task_impl(p_task_id); $$;
revoke all on function public.shahrvand_complete_weekly_task(text) from public,anon;
grant execute on function public.shahrvand_complete_weekly_task(text) to authenticated;
create function private.shahrvand_get_leaderboard_impl(p_age_band text)
returns table(rank bigint,login text,weekly_xp bigint,tasks_completed bigint,is_me boolean)
language plpgsql security definer set search_path='' as $$
declare v_user_id uuid:=auth.uid(); v_today date:=(now() at time zone 'Asia/Dushanbe')::date;
v_week_start date:=date_trunc('week',v_today::timestamp)::date; v_min_age integer; v_max_age integer;
begin
if v_user_id is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
if p_age_band not in ('7_12','13_17','18_plus') then raise exception 'INVALID_AGE_BAND' using errcode='22023'; end if;
v_min_age:=case p_age_band when '7_12' then 7 when '13_17' then 13 else 18 end;
v_max_age:=case p_age_band when '7_12' then 12 when '13_17' then 17 else 120 end;
return query with totals as (
select c.user_id,sum(c.xp_awarded)::bigint xp,count(*)::bigint completed,min(c.created_at) first_score
from public.shahrvand_weekly_completions c where c.week_start=v_week_start group by c.user_id
), eligible as (
select p.id,p.login,t.xp,t.completed,t.first_score from totals t join public.shahrvand_profiles p on p.id=t.user_id where p.age between v_min_age and v_max_age
), ranked as (
select row_number() over(order by e.xp desc,e.completed desc,e.first_score asc,e.login asc) pos,e.login,e.xp,e.completed,e.id from eligible e
) select r.pos,r.login,r.xp,r.completed,(r.id=v_user_id) from ranked r order by r.pos limit 100;
end; $$;
revoke all on function private.shahrvand_get_leaderboard_impl(text) from public,anon;
grant execute on function private.shahrvand_get_leaderboard_impl(text) to authenticated;
create function public.shahrvand_get_leaderboard(p_age_band text)
returns table(rank bigint,login text,weekly_xp bigint,tasks_completed bigint,is_me boolean)
language sql security invoker set search_path='' as $$ select * from private.shahrvand_get_leaderboard_impl(p_age_band); $$;
revoke all on function public.shahrvand_get_leaderboard(text) from public,anon;
grant execute on function public.shahrvand_get_leaderboard(text) to authenticated;
