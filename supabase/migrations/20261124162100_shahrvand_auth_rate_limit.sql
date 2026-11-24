-- Server-side rate limit for public SHAHRVAND username/password endpoints.
-- Only the Edge Function service role may access this table/function.

create schema if not exists private;
revoke all on schema private from public, anon, authenticated;
grant usage on schema private to service_role;

create table private.shahrvand_auth_rate_limits (
    ip_hash text not null,
    action text not null check (action in ('register', 'login')),
    window_start timestamptz not null,
    attempts integer not null default 0 check (attempts >= 0),
    primary key (ip_hash, action, window_start)
);

revoke all on table private.shahrvand_auth_rate_limits from public, anon, authenticated;
grant select, insert, update, delete on table private.shahrvand_auth_rate_limits to service_role;

create or replace function public.shahrvand_check_auth_rate_limit(
    p_ip_hash text,
    p_action text,
    p_limit integer
)
returns boolean
language plpgsql
set search_path = ''
as $$
declare
    v_attempts integer;
begin
    if p_action not in ('register', 'login') or p_limit < 1 or p_limit > 100 then
        return false;
    end if;

    delete from private.shahrvand_auth_rate_limits
    where window_start < now() - interval '1 day';

    insert into private.shahrvand_auth_rate_limits as current_limit
        (ip_hash, action, window_start, attempts)
    values (
        p_ip_hash,
        p_action,
        date_bin('10 minutes', now(), '2000-01-01 00:00:00+00'::timestamptz),
        1
    )
    on conflict (ip_hash, action, window_start)
    do update set attempts = current_limit.attempts + 1
    returning attempts into v_attempts;

    return v_attempts <= p_limit;
end;
$$;

revoke all on function public.shahrvand_check_auth_rate_limit(text, text, integer) from public, anon, authenticated;
grant execute on function public.shahrvand_check_auth_rate_limit(text, text, integer) to service_role;
