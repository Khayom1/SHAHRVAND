-- Normalize authenticated Data API privileges to an explicit least-privilege set.
-- This also handles projects where default privileges granted broad access at table creation.

revoke all on public.shahrvand_food_catalog from authenticated;
revoke all on public.shahrvand_recipe_catalog from authenticated;
revoke all on public.shahrvand_recipe_ingredients from authenticated;
revoke all on public.shahrvand_meal_logs from authenticated;
revoke all on public.shahrvand_water_logs from authenticated;

grant select on public.shahrvand_food_catalog to authenticated;
grant select on public.shahrvand_recipe_catalog to authenticated;
grant select on public.shahrvand_recipe_ingredients to authenticated;

-- Meal rows can only be created via the server-side nutrition RPC.
grant select, delete on public.shahrvand_meal_logs to authenticated;

-- Water logs can be added, viewed and removed by their owner.
grant select, insert, delete on public.shahrvand_water_logs to authenticated;
