-- Calculate logged nutrition values from the trusted food catalogue inside PostgreSQL.
-- The client may preview values, but the persisted numbers are calculated server-side.

create or replace function public.shahrvand_add_food_meal(
  p_food_id text,
  p_portion_g numeric,
  p_display_name text
)
returns setof public.shahrvand_meal_logs
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_food public.shahrvand_food_catalog%rowtype;
begin
  if auth.uid() is null then
    raise exception 'AUTHENTICATION_REQUIRED' using errcode = '28000';
  end if;

  if p_food_id is null or length(trim(p_food_id)) = 0 then
    raise exception 'INVALID_FOOD_ID' using errcode = '22023';
  end if;

  if p_portion_g is null or p_portion_g <= 0 or p_portion_g > 10000 then
    raise exception 'INVALID_PORTION' using errcode = '22023';
  end if;

  if p_display_name is null or length(trim(p_display_name)) not between 1 and 160 then
    raise exception 'INVALID_DISPLAY_NAME' using errcode = '22023';
  end if;

  select *
    into v_food
    from public.shahrvand_food_catalog
   where id = p_food_id
     and is_active = true;

  if not found then
    raise exception 'FOOD_NOT_AVAILABLE' using errcode = 'P0002';
  end if;

  return query
  insert into public.shahrvand_meal_logs (
    user_id,
    item_name_snapshot,
    catalog_item_id,
    recipe_slug_snapshot,
    portion_g,
    energy_kcal,
    protein_g,
    carbohydrate_g,
    fat_g,
    fiber_g,
    sodium_mg,
    source_snapshot
  ) values (
    auth.uid(),
    trim(p_display_name),
    v_food.id,
    null,
    p_portion_g,
    round(v_food.energy_kcal_per_100g * p_portion_g / 100, 2),
    round(v_food.protein_g_per_100g * p_portion_g / 100, 3),
    round(v_food.carbohydrate_g_per_100g * p_portion_g / 100, 3),
    round(v_food.fat_g_per_100g * p_portion_g / 100, 3),
    round(v_food.fiber_g_per_100g * p_portion_g / 100, 3),
    round(v_food.sodium_mg_per_100g * p_portion_g / 100, 2),
    v_food.data_source || ':' || v_food.source_reference
  )
  returning *;
end;
$$;

revoke insert on table public.shahrvand_meal_logs from authenticated;
revoke all on function public.shahrvand_add_food_meal(text, numeric, text) from public, anon;
grant execute on function public.shahrvand_add_food_meal(text, numeric, text) to authenticated;
