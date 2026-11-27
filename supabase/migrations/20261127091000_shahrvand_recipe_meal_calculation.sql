-- Save a portion of a user-composed recipe.
-- Nutrient totals are calculated by the database from active catalogue foods.
-- The recipe's measured final cooked yield is required; no guessed dish averages are used.

create or replace function public.shahrvand_add_recipe_meal(
  p_display_name text,
  p_ingredients jsonb,
  p_final_cooked_weight_g numeric,
  p_portion_g numeric
)
returns setof public.shahrvand_meal_logs
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_ingredient jsonb;
  v_food public.shahrvand_food_catalog%rowtype;
  v_food_id text;
  v_amount_g numeric;
  v_count integer := 0;
  v_energy numeric := 0;
  v_protein numeric := 0;
  v_carbohydrate numeric := 0;
  v_fat numeric := 0;
  v_fiber numeric := 0;
  v_sodium numeric := 0;
  v_source_items jsonb := '[]'::jsonb;
begin
  if auth.uid() is null then
    raise exception 'AUTHENTICATION_REQUIRED' using errcode = '28000';
  end if;

  if p_display_name is null or length(trim(p_display_name)) not between 1 and 160 then
    raise exception 'INVALID_RECIPE_NAME' using errcode = '22023';
  end if;

  if jsonb_typeof(p_ingredients) is distinct from 'array' then
    raise exception 'INGREDIENTS_MUST_BE_AN_ARRAY' using errcode = '22023';
  end if;

  if jsonb_array_length(p_ingredients) not between 1 and 30 then
    raise exception 'INGREDIENT_COUNT_OUT_OF_RANGE' using errcode = '22023';
  end if;

  if p_final_cooked_weight_g is null
     or p_final_cooked_weight_g <= 0
     or p_final_cooked_weight_g > 100000 then
    raise exception 'INVALID_FINAL_COOKED_WEIGHT' using errcode = '22023';
  end if;

  if p_portion_g is null or p_portion_g <= 0 or p_portion_g > 10000
     or p_portion_g > p_final_cooked_weight_g then
    raise exception 'INVALID_PORTION' using errcode = '22023';
  end if;

  for v_ingredient in
    select value from jsonb_array_elements(p_ingredients) as ingredient(value)
  loop
    v_food_id := nullif(trim(v_ingredient ->> 'food_id'), '');
    begin
      v_amount_g := (v_ingredient ->> 'amount_g')::numeric;
    exception when others then
      raise exception 'INVALID_INGREDIENT_AMOUNT' using errcode = '22023';
    end;

    if v_food_id is null or v_amount_g is null
       or v_amount_g <= 0 or v_amount_g > 100000 then
      raise exception 'INVALID_INGREDIENT' using errcode = '22023';
    end if;

    select *
      into v_food
      from public.shahrvand_food_catalog
     where id = v_food_id
       and is_active = true;

    if not found then
      raise exception 'FOOD_NOT_AVAILABLE: %', v_food_id using errcode = 'P0002';
    end if;

    v_count := v_count + 1;
    v_energy := v_energy + (v_food.energy_kcal_per_100g * v_amount_g / 100);
    v_protein := v_protein + (v_food.protein_g_per_100g * v_amount_g / 100);
    v_carbohydrate := v_carbohydrate + (v_food.carbohydrate_g_per_100g * v_amount_g / 100);
    v_fat := v_fat + (v_food.fat_g_per_100g * v_amount_g / 100);
    v_fiber := v_fiber + (v_food.fiber_g_per_100g * v_amount_g / 100);
    v_sodium := v_sodium + (v_food.sodium_mg_per_100g * v_amount_g / 100);

    v_source_items := v_source_items || jsonb_build_array(
      jsonb_build_object(
        'food_id', v_food.id,
        'amount_g', v_amount_g,
        'preparation_state', v_food.preparation_state,
        'data_source', v_food.data_source,
        'source_reference', v_food.source_reference
      )
    );
  end loop;

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
    null,
    'user-composed-recipe',
    p_portion_g,
    round((v_energy / p_final_cooked_weight_g) * p_portion_g, 2),
    round((v_protein / p_final_cooked_weight_g) * p_portion_g, 3),
    round((v_carbohydrate / p_final_cooked_weight_g) * p_portion_g, 3),
    round((v_fat / p_final_cooked_weight_g) * p_portion_g, 3),
    round((v_fiber / p_final_cooked_weight_g) * p_portion_g, 3),
    round((v_sodium / p_final_cooked_weight_g) * p_portion_g, 2),
    jsonb_build_object(
      'calculation', 'ingredient_sum_divided_by_measured_final_cooked_weight',
      'final_cooked_weight_g', p_final_cooked_weight_g,
      'ingredients', v_source_items
    )::text
  )
  returning *;
end;
$$;

revoke all on function public.shahrvand_add_recipe_meal(text, jsonb, numeric, numeric) from public, anon;
grant execute on function public.shahrvand_add_recipe_meal(text, jsonb, numeric, numeric) to authenticated;
