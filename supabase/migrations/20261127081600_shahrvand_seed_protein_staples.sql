-- Protein, oil and dry staple ingredients used in common Tajik cooking.
-- USDA SR Legacy values per 100 g edible portion in the exact state named.
-- International reference data only; local animal breeds and products may differ.

insert into public.shahrvand_food_catalog (
  id, name_tg, name_ru, name_en, category, preparation_state,
  energy_kcal_per_100g, protein_g_per_100g, carbohydrate_g_per_100g,
  fat_g_per_100g, fiber_g_per_100g, sodium_mg_per_100g,
  data_source, source_reference, verified_at, is_locally_verified, is_active
) values
(
  'usda-fdc-169756',
  'Биринҷи сафеди дароздона, хушк',
  'Белый длиннозёрный рис, сухой',
  'White long-grain rice, raw, unenriched',
  'grains', 'dried',
  365, 7.13, 80.00, 0.66, 1.3, 5,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/169756/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-171025',
  'Равғани офтобпараст, навъи линолӣ (тақрибан 65%)',
  'Подсолнечное масло, линолевое (около 65%)',
  'Sunflower oil, linoleic, approximately 65%',
  'oils', 'as_sold',
  884, 0, 0, 100, 0, 0,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/171025/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-172553',
  'Гӯшти гӯсфанд, китф бо равған, хом (маълумоти истинодӣ)',
  'Баранина, лопатка с жиром, сырая (справочные данные)',
  'Lamb shoulder, separable lean and fat, raw',
  'meat_fish_eggs', 'raw',
  244, 17.10, 0, 19.00, 0, 63,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/172553/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-171796',
  'Гӯшти гов, қимаи 85% лоғар ва 15% равған, хом',
  'Говяжий фарш, 85% постного мяса и 15% жира, сырой',
  'Ground beef, 85% lean meat / 15% fat, raw',
  'meat_fish_eggs', 'raw',
  215, 18.60, 0, 15.00, 0, 66,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/171796/nutrients',
  '2026-11-27', false, true
),
(
  'usda-fdc-171077',
  'Гӯшти синаи мурғ, бе пӯст ва устухон, хом',
  'Куриная грудка без кожи и кости, сырая',
  'Chicken breast, skinless and boneless, raw',
  'meat_fish_eggs', 'raw',
  120, 22.50, 0, 2.62, 0, 45,
  'usda_fdc', 'https://fdc.nal.usda.gov/fdc-app.html#/food-details/171077/nutrients',
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
