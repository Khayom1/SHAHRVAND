-- Additional common cooking ingredients for the Tajik food catalogue.
-- Values are USDA SR Legacy per 100 g in the exact state named below.
-- These are international reference data, not Tajik laboratory measurements.

alter table public.shahrvand_food_catalog
  drop constraint if exists shahrvand_food_catalog_preparation_state_check;

alter table public.shahrvand_food_catalog
  add constraint shahrvand_food_catalog_preparation_state_check
  check (preparation_state in ('raw', 'cooked', 'dried', 'baked', 'fermented', 'canned', 'as_sold'));

insert into public.shahrvand_food_catalog (
  id, name_tg, name_ru, name_en, category, preparation_state,
  energy_kcal_per_100g, protein_g_per_100g, carbohydrate_g_per_100g,
  fat_g_per_100g, fiber_g_per_100g, sodium_mg_per_100g,
  data_source, source_reference, verified_at, is_locally_verified, is_active
) values
(
  'usda-fdc-170457',
  'Помидори сурх, хом',
  'Томаты красные, сырые',
  'Tomatoes, red, ripe, raw',
  'vegetables', 'raw',
  18, 0.88, 3.89, 0.20, 1.2, 5,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/170457/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-170000',
  'Пиёз, хом',
  'Лук, сырой',
  'Onions, raw',
  'vegetables', 'raw',
  40, 1.10, 9.34, 0.10, 1.7, 4,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/170000/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-169230',
  'Сирпиёз, хом',
  'Чеснок, сырой',
  'Garlic, raw',
  'vegetables', 'raw',
  149, 6.36, 33.10, 0.50, 2.1, 17,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/169230/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-168462',
  'Исфаноҷ, хом',
  'Шпинат, сырой',
  'Spinach, raw',
  'vegetables', 'raw',
  23, 2.86, 3.63, 0.39, 2.2, 79,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/168462/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-173800',
  'Нахӯди консервшуда, обаш рехташуда',
  'Нут консервированный, без жидкости',
  'Chickpeas, canned, drained solids',
  'legumes_nuts', 'canned',
  139, 7.05, 22.50, 2.77, 6.4, 246,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/173800/nutrients',
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
