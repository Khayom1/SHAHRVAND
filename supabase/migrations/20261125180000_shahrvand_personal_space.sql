create table if not exists public.shahrvand_notes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  kind text not null check (kind in ('note','plan','report','event','spiritual')),
  title text not null check (char_length(btrim(title)) between 1 and 120),
  body text not null check (char_length(btrim(body)) between 1 and 10000),
  planned_on date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.shahrvand_financial_entries (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  entry_type text not null check (entry_type in ('income','expense')),
  category text not null check (category in (
    'transport','groceries','household','utilities','rent','communication',
    'education','health','medicine','clothing','personal','entertainment',
    'family','loan','salary','business','freelance','scholarship','gift','refund','other'
  )),
  custom_category text,
  amount numeric(14,2) not null check (amount > 0),
  currency text not null default 'TJS' check (currency = 'TJS'),
  comment text check (comment is null or char_length(comment) <= 500),
  occurred_on date not null default current_date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint shahrvand_finance_other_category_check check (
    (category = 'other' and custom_category is not null and char_length(btrim(custom_category)) between 1 and 60)
    or (category <> 'other' and custom_category is null)
  )
);

create index if not exists shahrvand_notes_user_created_idx
  on public.shahrvand_notes (user_id, created_at desc);
create index if not exists shahrvand_notes_user_planned_idx
  on public.shahrvand_notes (user_id, planned_on);
create index if not exists shahrvand_finance_user_date_idx
  on public.shahrvand_financial_entries (user_id, occurred_on desc, created_at desc);

alter table public.shahrvand_notes enable row level security;
alter table public.shahrvand_financial_entries enable row level security;

revoke all on table public.shahrvand_notes from anon, authenticated;
revoke all on table public.shahrvand_financial_entries from anon, authenticated;
grant select, insert, update, delete on table public.shahrvand_notes to authenticated;
grant select, insert, update, delete on table public.shahrvand_financial_entries to authenticated;

drop policy if exists shahrvand_notes_owner_all on public.shahrvand_notes;
create policy shahrvand_notes_owner_all
  on public.shahrvand_notes
  for all to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

drop policy if exists shahrvand_finance_owner_all on public.shahrvand_financial_entries;
create policy shahrvand_finance_owner_all
  on public.shahrvand_financial_entries
  for all to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
