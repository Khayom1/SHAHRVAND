-- Fermented dairy foods such as plain yogurt are neither raw nor cooked.
alter table public.shahrvand_food_catalog
  drop constraint if exists shahrvand_food_catalog_preparation_state_check;

alter table public.shahrvand_food_catalog
  add constraint shahrvand_food_catalog_preparation_state_check
  check (preparation_state in ('raw', 'cooked', 'dried', 'baked', 'fermented'));
