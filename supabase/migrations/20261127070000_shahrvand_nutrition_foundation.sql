-- SHAHRVAND Nutrition foundation.
-- Food composition is source-aware. Local dishes must be recipe-derived or lab-verified;
-- do not seed guessed calorie values for Tajik meals.

create table if not exists public.shahrvand_food_catalog (
  id text primary key,
  name_tg text not null check (length(trim(name_tg)) between 1 and 120),
  name_ru text not null check (length(trim(name_ru)) between 1 and 120),
  name_en text not null check (length(trim(name_en)) between 1 and 120),
  category text not null check (category in (
    'grains', 'meat_fish_eggs', 'dairy', 'vegetables', 'fruits',
    'legumes_nuts', 'oils', 'prepared_food', 'other'
  )),
  preparation_state text not null check (preparation_state in (
    'raw', 'cooked', 'dried', 'baked'
  )),
  energy_kcal_per_100g numeric(8,2) not null check (energy_kcal_per_100g >= 0),
  protein_g_per_100g numeric(8,3) not null check (protein_g_per_100g >= 0),
  carbohydrate_g_per_100g numeric(8,3) not null check (carbohydrate_g_per_100g >= 0),
  fat_g_per_100g numeric(8,3) not null check (fat_g_per_100g >= 0),
  fiber_g_per_100g numeric(8,3) not null check (fiber_g_per_100g >= 0),
  sodium_mg_per_100g numeric(9,2) not null check (sodium_mg_per_100g >= 0),
  data_source text not null check (data_source in (
    'usda_fdc', 'tajik_lab', 'manufacturer_label', 'recipe_estimate'
  )),
  source_reference text not null check (length(trim(source_reference)) > 0),
  verified_at date,
  is_locally_verified boolean not null default false,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists shahrvand_food_catalog_search_idx
  on public.shahrvand_food_catalog (is_active, category, name_tg);

create table if not exists public.shahrvand_recipe_catalog (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  name_tg text not null check (length(trim(name_tg)) between 1 and 120),
  name_ru text not null check (length(trim(name_ru)) between 1 and 120),
  name_en text not null check (length(trim(name_en)) between 1 and 120),
  method text not null check (method in ('recipe_calculated', 'laboratory_analyzed')),
  final_cooked_weight_g numeric(9,2) not null check (final_cooked_weight_g > 0),
  source_reference text not null check (length(trim(source_reference)) > 0),
  verified_at date,
  is_active boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.shahrvand_recipe_ingredients (
  recipe_id uuid not null references public.shahrvand_recipe_catalog(id) on delete cascade,
  food_id text not null references public.shahrvand_food_catalog(id) on delete restrict,
  amount_g numeric(9,2) not null check (amount_g > 0),
  primary key (recipe_id, food_id)
);

create table if not exists public.shahrvand_meal_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
  item_name_snapshot text not null check (length(trim(item_name_snapshot)) between 1 and 160),
  catalog_item_id text,
  recipe_slug_snapshot text,
  portion_g numeric(9,2) not null check (portion_g > 0 and portion_g <= 10000),
  energy_kcal numeric(9,2) not null check (energy_kcal >= 0),
  protein_g numeric(9,3) not null check (protein_g >= 0),
  carbohydrate_g numeric(9,3) not null check (carbohydrate_g >= 0),
  fat_g numeric(9,3) not null check (fat_g >= 0),
  fiber_g numeric(9,3) not null check (fiber_g >= 0),
  sodium_mg numeric(10,2) not null check (sodium_mg >= 0),
  source_snapshot text not null,
  logged_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  check ((catalog_item_id is not null) <> (recipe_slug_snapshot is not null))
);

create index if not exists shahrvand_meal_logs_user_logged_idx
  on public.shahrvand_meal_logs(user_id, logged_at desc);

create table if not exists public.shahrvand_water_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
  volume_ml integer not null check (volume_ml between 1 and 5000),
  logged_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index if not exists shahrvand_water_logs_user_logged_idx
  on public.shahrvand_water_logs(user_id, logged_at desc);

alter table public.shahrvand_food_catalog enable row level security;
alter table public.shahrvand_recipe_catalog enable row level security;
alter table public.shahrvand_recipe_ingredients enable row level security;
alter table public.shahrvand_meal_logs enable row level security;
alter table public.shahrvand_water_logs enable row level security;

drop policy if exists shahrvand_food_catalog_authenticated_read on public.shahrvand_food_catalog;
create policy shahrvand_food_catalog_authenticated_read
  on public.shahrvand_food_catalog for select to authenticated
  using (is_active);

drop policy if exists shahrvand_recipe_catalog_authenticated_read on public.shahrvand_recipe_catalog;
create policy shahrvand_recipe_catalog_authenticated_read
  on public.shahrvand_recipe_catalog for select to authenticated
  using (is_active);

drop policy if exists shahrvand_recipe_ingredients_authenticated_read on public.shahrvand_recipe_ingredients;
create policy shahrvand_recipe_ingredients_authenticated_read
  on public.shahrvand_recipe_ingredients for select to authenticated
  using (
    exists (
      select 1 from public.shahrvand_recipe_catalog r
      where r.id = recipe_id and r.is_active
    )
  );

drop policy if exists shahrvand_meal_logs_select_own on public.shahrvand_meal_logs;
create policy shahrvand_meal_logs_select_own
  on public.shahrvand_meal_logs for select to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists shahrvand_meal_logs_insert_own on public.shahrvand_meal_logs;
create policy shahrvand_meal_logs_insert_own
  on public.shahrvand_meal_logs for insert to authenticated
  with check ((select auth.uid()) = user_id);

drop policy if exists shahrvand_meal_logs_delete_own on public.shahrvand_meal_logs;
create policy shahrvand_meal_logs_delete_own
  on public.shahrvand_meal_logs for delete to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists shahrvand_water_logs_select_own on public.shahrvand_water_logs;
create policy shahrvand_water_logs_select_own
  on public.shahrvand_water_logs for select to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists shahrvand_water_logs_insert_own on public.shahrvand_water_logs;
create policy shahrvand_water_logs_insert_own
  on public.shahrvand_water_logs for insert to authenticated
  with check ((select auth.uid()) = user_id);

drop policy if exists shahrvand_water_logs_delete_own on public.shahrvand_water_logs;
create policy shahrvand_water_logs_delete_own
  on public.shahrvand_water_logs for delete to authenticated
  using ((select auth.uid()) = user_id);

revoke all on public.shahrvand_food_catalog from anon, public;
revoke all on public.shahrvand_recipe_catalog from anon, public;
revoke all on public.shahrvand_recipe_ingredients from anon, public;
revoke all on public.shahrvand_meal_logs from anon, public;
revoke all on public.shahrvand_water_logs from anon, public;

grant select on public.shahrvand_food_catalog to authenticated;
grant select on public.shahrvand_recipe_catalog to authenticated;
grant select on public.shahrvand_recipe_ingredients to authenticated;
grant select, delete on public.shahrvand_meal_logs to authenticated;
grant select, insert, delete on public.shahrvand_water_logs to authenticated;


-- Extend the existing owner-scoped deletion RPC to cover nutrition history too.
create or replace function public.shahrvand_delete_my_health_data()
returns void
language plpgsql
security invoker
set search_path = ''
as $$
begin
  delete from public.shahrvand_meal_logs
  where user_id = auth.uid();

  delete from public.shahrvand_water_logs
  where user_id = auth.uid();

  delete from public.shahrvand_body_measurements
  where user_id = auth.uid();

  delete from public.shahrvand_health_profiles
  where user_id = auth.uid();
end;
$$;

revoke all on function public.shahrvand_delete_my_health_data() from public, anon;
grant execute on function public.shahrvand_delete_my_health_data() to authenticated;
