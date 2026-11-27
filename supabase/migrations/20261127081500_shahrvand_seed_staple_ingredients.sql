-- Additional staple ingredients used in common Tajik home cooking.
-- USDA SR Legacy values per 100 g edible portion in the precise state named.
-- These are reference values and are not local Tajik laboratory analyses.

insert into public.shahrvand_food_catalog (
  id, name_tg, name_ru, name_en, category, preparation_state,
  energy_kcal_per_100g, protein_g_per_100g, carbohydrate_g_per_100g,
  fat_g_per_100g, fiber_g_per_100g, sodium_mg_per_100g,
  data_source, source_reference, verified_at, is_locally_verified, is_active
) values
(
  'usda-fdc-170393',
  'Сабзӣ, хом',
  'Морковь, сырая',
  'Carrots, raw',
  'vegetables', 'raw',
  41, 0.93, 9.58, 0.24, 2.8, 69,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/170393/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-170026',
  'Картошка бо пӯст, хом',
  'Картофель с кожурой, сырой',
  'Potatoes, flesh and skin, raw',
  'vegetables', 'raw',
  77, 2.05, 17.49, 0.09, 2.1, 6,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/170026/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-170438',
  'Картошкаи обпаз, бе намак',
  'Картофель отварной, без соли',
  'Potatoes, boiled, cooked in skin, flesh, without salt',
  'vegetables', 'cooked',
  87, 1.87, 20.10, 0.10, 1.8, 4,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/170438/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-169761',
  'Орди гандуми сафед, навъи умумӣ, ғанигардонданашуда',
  'Пшеничная мука высшего сорта, не обогащённая',
  'Wheat flour, white, all-purpose, unenriched',
  'grains', 'raw',
  364, 10.30, 76.30, 0.98, 2.7, 2,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/169761/nutrients',
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
