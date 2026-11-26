-- Allow a broad range of learning interests instead of requiring exactly five.
alter table public.shahrvand_user_interests
  drop constraint if exists shahrvand_user_interests_interest_code_check;

alter table public.shahrvand_user_interests
  add constraint shahrvand_user_interests_interest_code_check
  check (interest_code = any (array['science', 'technology', 'history', 'geography', 'mathematics', 'languages', 'health', 'nature', 'culture', 'arts', 'sports', 'finance', 'physics', 'chemistry', 'biology', 'astronomy', 'medicine', 'psychology', 'philosophy', 'programming', 'ai', 'engineering', 'robotics', 'cybersecurity', 'environment', 'animals', 'space', 'politics', 'economics', 'business', 'law', 'sociology', 'literature', 'music', 'design', 'cinema', 'photography', 'architecture', 'cooking', 'travel', 'aviation', 'agriculture', 'dentistry', 'gaming']::text[]));

create or replace function private.shahrvand_set_my_interests_impl(p_codes text[])
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception 'AUTH_REQUIRED' using errcode='28000';
  end if;

  if p_codes is null
     or cardinality(p_codes) < 1
     or cardinality(p_codes) > 44
     or (select count(distinct x) from unnest(p_codes) x) <> cardinality(p_codes)
     or exists (
       select 1 from unnest(p_codes) x
       where x is null or x not in ('science', 'technology', 'history', 'geography', 'mathematics', 'languages', 'health', 'nature', 'culture', 'arts', 'sports', 'finance', 'physics', 'chemistry', 'biology', 'astronomy', 'medicine', 'psychology', 'philosophy', 'programming', 'ai', 'engineering', 'robotics', 'cybersecurity', 'environment', 'animals', 'space', 'politics', 'economics', 'business', 'law', 'sociology', 'literature', 'music', 'design', 'cinema', 'photography', 'architecture', 'cooking', 'travel', 'aviation', 'agriculture', 'dentistry', 'gaming')
     )
  then
    raise exception 'INVALID_INTEREST_SELECTION' using errcode='22023';
  end if;

  delete from public.shahrvand_user_interests where user_id = v_user;

  insert into public.shahrvand_user_interests(user_id, interest_code)
  select v_user, x from unnest(p_codes) x;

  return jsonb_build_object('ok', true, 'interests', to_jsonb(p_codes));
end;
$$;

update public.shahrvand_activity_task_catalog
set description_tg = 'Дарс аз рӯи мавзӯъҳои интихобкардаат; баъд 5 саволи санҷишӣ. Танҳо таймер кофӣ нест.',
    description_ru = 'Урок по выбранным интересам, затем 5 проверочных вопросов. Одного таймера недостаточно.',
    description_en = 'A lesson based on your selected interests, followed by five quiz questions. A timer alone is not enough.'
where id = 'learning_20m';
