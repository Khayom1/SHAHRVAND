alter table public.shahrvand_financial_entries
  add constraint shahrvand_finance_category_matches_type check (
    (entry_type = 'income' and category in ('salary','business','freelance','scholarship','gift','refund','other'))
    or
    (entry_type = 'expense' and category in (
      'transport','groceries','household','utilities','rent','communication',
      'education','health','medicine','clothing','personal','entertainment',
      'family','loan','other'
    ))
  );