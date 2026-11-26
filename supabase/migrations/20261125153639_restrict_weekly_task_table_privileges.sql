-- Applied to Supabase as migration 20261125153639_restrict_weekly_task_table_privileges.
revoke all on table public.shahrvand_weekly_tasks from anon, authenticated;
grant select(id,weekday,title_tg,title_ru,title_en,description_tg,description_ru,description_en,category,icon,xp,min_age,sort_order,is_active) on table public.shahrvand_weekly_tasks to authenticated;
