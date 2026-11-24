-- Applied to the connected Supabase project as migration
-- 20261124153012_shahrvand_v1_public_missions.
-- Public content only: no user accounts, profiles, location, or progress.

create table public.shahrvand_missions (
    id text primary key,
    title_tg text not null,
    title_ru text not null,
    title_en text not null,
    description_tg text not null,
    description_ru text not null,
    description_en text not null,
    category text not null check (category in ('learning','kindness','community','nature','wellbeing')),
    icon text not null default '⭐',
    xp smallint not null check (xp between 5 and 20),
    min_age smallint not null default 7 check (min_age >= 7 and min_age <= 18),
    sort_order smallint not null default 0,
    is_active boolean not null default true,
    created_at timestamptz not null default now()
);

alter table public.shahrvand_missions enable row level security;
revoke all on table public.shahrvand_missions from anon, authenticated;
grant select on table public.shahrvand_missions to anon, authenticated;

create policy "shahrvand_active_missions_read_only"
on public.shahrvand_missions
for select to anon, authenticated
using (is_active = true);

insert into public.shahrvand_missions
(id, title_tg, title_ru, title_en, description_tg, description_ru, description_en, category, icon, xp, min_age, sort_order)
values
('read_10','10 дақиқа китоб хон','Читай 10 минут','Read for 10 minutes','Китоб ё матни омӯзиширо интихоб кун ва 10 дақиқа оромона бихон.','Выбери книгу или обучающий текст и спокойно читай 10 минут.','Choose a book or learning text and read calmly for 10 minutes.','learning','📚',10,7,10),
('learn_one','Як чизи нав омӯз','Узнай что-то новое','Learn one new thing','Як мафҳум, калима ё малакаи навро омӯз ва онро бо суханони худ шарҳ деҳ.','Узнай новое понятие, слово или навык и объясни его своими словами.','Learn a new idea, word, or skill and explain it in your own words.','learning','💡',10,7,20),
('help_home','Дар хона ёрӣ расон','Помоги дома','Help at home','Як кори хурди хонагиро бо мувофиқаи калонсолон иҷро кун.','Выполни небольшое домашнее дело по согласованию со взрослым.','Do a small household task with an adult’s agreement.','kindness','🤝',15,7,30),
('kind_word','Сухани нек бигӯ','Скажи доброе слово','Say something kind','Ба касе сухани самимӣ ва эҳтиромона бигӯ. Ҳеҷ касро маҷбур накун.','Скажи кому-нибудь искренние и уважительные слова. Никого не принуждай.','Say something sincere and respectful to someone. Do not pressure anyone.','kindness','💛',10,7,40),
('water_plant','Ба растанӣ об деҳ','Полей растение','Water a plant','Ба гул ё растании хона об деҳ; миқдори мувофиқро нигоҳ дор.','Полей домашний цветок или растение, не переливая его.','Water a home plant without overwatering it.','nature','🌱',10,7,50),
('clean_space','Ҷойи худро тоза кун','Убери своё место','Tidy your space','Мизи корӣ ё ҷои шахсии худро ба тартиб дарор. Барои корҳои берунӣ аз калонсол иҷозат пурс.','Наведи порядок на своём столе или личном месте. Для работ на улице сначала спроси взрослого.','Tidy your desk or personal space. Ask an adult before doing anything outdoors.','community','🧹',15,7,60),
('focus_5','5 дақиқа диққатро ҷамъ кун','Сосредоточься на 5 минут','Focus for 5 minutes','Як кори ором ва фоиданокро панҷ дақиқа бе парешонӣ анҷом деҳ.','Пять минут спокойно занимайся одним полезным делом без отвлечений.','Spend five calm minutes on one useful task without distractions.','wellbeing','🎯',10,7,70)
on conflict (id) do nothing;
