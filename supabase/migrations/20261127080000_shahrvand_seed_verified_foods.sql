-- Initial international reference foods for the SHAHRVAND nutrition UI.
-- Values are USDA SR Legacy per 100 g edible portion; they are not Tajik laboratory values.
-- Local dishes are intentionally not seeded until their regional recipes are documented.

insert into public.shahrvand_food_catalog (
  id, name_tg, name_ru, name_en, category, preparation_state,
  energy_kcal_per_100g, protein_g_per_100g, carbohydrate_g_per_100g,
  fat_g_per_100g, fiber_g_per_100g, sodium_mg_per_100g,
  data_source, source_reference, verified_at, is_locally_verified, is_active
) values
(
  'usda-fdc-171688',
  'Себ бо пӯст, хом',
  'Яблоко с кожурой, сырое',
  'Apple with skin, raw',
  'fruits', 'raw',
  52, 0.26, 13.81, 0.17, 2.4, 1,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/171688/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-173944',
  'Банан, хом',
  'Банан, сырой',
  'Banana, raw',
  'fruits', 'raw',
  89, 1.09, 22.84, 0.33, 2.6, 1,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/173944/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-168878',
  'Биринҷи сафеди дароздона, пухта',
  'Белый длиннозёрный рис, варёный',
  'White long-grain rice, cooked',
  'grains', 'cooked',
  130, 2.69, 28.20, 0.28, 0.4, 1,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/168878/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-171284',
  'Ҷурғоти одии шири пурравған',
  'Йогурт натуральный из цельного молока',
  'Plain whole-milk yogurt',
  'dairy', 'raw',
  61, 3.47, 4.66, 3.25, 0, 46,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/171284/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-173424',
  'Тухми обпаз, пурра',
  'Яйцо варёное вкрутую, целое',
  'Whole egg, hard-boiled',
  'meat_fish_eggs', 'cooked',
  155, 12.58, 1.12, 10.61, 0, 124,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/173424/nutrients',
  '2026-11-27', false, true
)
on conflict (id) do update set
  name_tg = excluded.name_tg,
  name_ru = excluded.name_ru,
  name_en = excluded.name_en,
  category = excluded.category,
  preparation_state = excluded.preparation_state,
  energy_kcal_per_100g = excluded.energy_kcal_per_100g,
  protein_g_per_100g = excluded.protein_g_per_100g,
  carbohydrate_g_per_100g = excluded.carbohydrate_g_per_100g,
  fat_g_per_100g = excluded.fat_g_per_100g,
  fiber_g_per_100g = excluded.fiber_g_per_100g,
  sodium_mg_per_100g = excluded.sodium_mg_per_100g,
  data_source = excluded.data_source,
  source_reference = excluded.source_reference,
  verified_at = excluded.verified_at,
  is_locally_verified = excluded.is_locally_verified,
  is_active = excluded.is_active,
  updated_at = now();
