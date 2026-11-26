create index if not exists shahrvand_activity_sessions_task_fk_idx
  on public.shahrvand_activity_sessions(task_id);

create or replace function private.shahrvand_cancel_activity_session_impl(p_session_id uuid, p_nonce text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  update public.shahrvand_activity_sessions
    set status='cancelled', completed_at=now(), rejection_code='USER_CANCELLED'
    where id=p_session_id and user_id=v_user and challenge_nonce=p_nonce and status='pending';
  if not found then raise exception 'SESSION_NOT_CANCELLABLE' using errcode='55000'; end if;
  return jsonb_build_object('ok',true,'cancelled',true);
end; $$;
revoke all on function private.shahrvand_cancel_activity_session_impl(uuid,text) from public, anon;
grant execute on function private.shahrvand_cancel_activity_session_impl(uuid,text) to authenticated;

create or replace function public.shahrvand_cancel_activity_session(p_session_id uuid, p_nonce text)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.shahrvand_cancel_activity_session_impl(p_session_id,p_nonce);
$$;
revoke all on function public.shahrvand_cancel_activity_session(uuid,text) from public, anon;
grant execute on function public.shahrvand_cancel_activity_session(uuid,text) to authenticated;
grant execute on function public.shahrvand_cancel_activity_session(uuid,text) to authenticated;
