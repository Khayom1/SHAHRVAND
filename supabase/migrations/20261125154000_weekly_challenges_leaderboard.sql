
create table if not exists public.shahrvand_weekly_tasks (
 id text primary key, weekday smallint not null check(weekday between 1 and 7),
 title_tg text not null,title_ru text not null,title_en text not null,
 description_tg text not null,description_ru text not null,description_en text not null,
 category text not null check(category in ('logic','learning','memory','creativity','focus','kindness','wellbeing','community')),
 icon text not null default '🧠',xp smallint not null check(xp between 5 and 30),
 min_age smallint not null default 7 check(min_age between 7 and 18),
 sort_order smallint not null default 0,is_active boolean not null default true
);
alter table public.shahrvand_weekly_tasks enable row level security;
create policy "Authenticated users can read weekly tasks" on public.shahrvand_weekly_tasks for select to authenticated using(is_active=true);
grant select(id,weekday,title_tg,title_ru,title_en,description_tg,description_ru,description_en,category,icon,xp,min_age,sort_order,is_active) on public.shahrvand_weekly_tasks to authenticated;
create table if not exists public.shahrvand_weekly_completions (
 id uuid primary key default gen_random_uuid(),user_id uuid not null references auth.users(id) on delete cascade,
 task_id text not null references public.shahrvand_weekly_tasks(id),completed_on date not null,week_start date not null,
 xp_awarded smallint not null check(xp_awarded between 5 and 30),created_at timestamptz not null default now(),
 unique(user_id,task_id,completed_on)
);
create index if not exists shahrvand_weekly_completions_rank_idx on public.shahrvand_weekly_completions(week_start,user_id,xp_awarded);
create index if not exists shahrvand_weekly_completions_user_idx on public.shahrvand_weekly_completions(user_id,completed_on desc);
create index if not exists shahrvand_weekly_completions_task_idx on public.shahrvand_weekly_completions(task_id);
alter table public.shahrvand_weekly_completions enable row level security;
create policy "Users can read their own weekly completions" on public.shahrvand_weekly_completions for select to authenticated using((select auth.uid())=user_id);
revoke all on public.shahrvand_weekly_completions from anon,authenticated;
grant select on public.shahrvand_weekly_completions to authenticated;
insert into public.shahrvand_weekly_tasks (id,weekday,title_tg,title_ru,title_en,description_tg,description_ru,description_en,category,icon,xp,min_age,sort_order,is_active) values ('w1_1',1,'Пайдарпаии 2, 4, 8, 16-ро идома деҳ ва қоидаашро шарҳ деҳ.','Продолжи ряд 2, 4, 8, 16 и объясни правило.','Continue 2, 4, 8, 16 and explain the rule.','Кадом амал ҳар дафъа такрор мешавад?','Какое действие повторяется каждый раз?','What operation repeats each time?','logic','🧩',20,7,1,'t'),
('w1_2',1,'Як мавзӯи навро 15 дақиқа омӯз ва се нуқтаи асосиро навис.','Изучи новую тему 15 минут и запиши три главные мысли.','Study a new topic for 15 minutes and write three key points.','Мавзӯъро худат интихоб кун.','Выбери тему самостоятельно.','Choose any topic.','learning','📚',15,7,2,'t'),
('w1_3',1,'Панҷ ашёи атрофро 30 сония мушоҳида кун, баъд рӯйхатро аз ёд навис.','30 секунд изучай пять предметов вокруг, затем перечисли их по памяти.','Observe five nearby objects for 30 seconds, then list them from memory.','Бе аксбардорӣ иҷро кун.','Выполни без фотографирования.','Do it without taking a photo.','memory','🧠',15,7,3,'t'),
('w1_4',1,'10 дақиқа як кори муҳимро бе гузаштан ба барномаҳои дигар анҷом деҳ.','10 минут выполняй одно важное дело, не переключаясь на другие приложения.','Work on one important task for 10 minutes without switching apps.','Вақтсанҷ истифода бурдан мумкин аст.','Можно использовать таймер.','A timer is allowed.','focus','🎯',10,7,4,'t'),
('w1_5',1,'Ба як нафар дар кори хурд кӯмак кун ва эҳтиромона рафтор намо.','Помоги кому-нибудь с небольшим делом и прояви уважение.','Help someone with a small task and act respectfully.','Кӯмак бояд ихтиёрӣ ва бехатар бошад.','Помощь должна быть добровольной и безопасной.','Keep the help voluntary and safe.','kindness','🤝',15,7,5,'t'),
('w1_6',1,'10 дақиқа сайругашт ё ҳаракати сабук кун; агар хаста бошӣ, истироҳат кун.','10 минут прогуляйся или сделай лёгкую разминку; если устал — отдохни.','Take a 10-minute walk or do light movement; rest if tired.','Фаъолиятро ба имкониятат мутобиқ кун.','Подстрой активность под свои возможности.','Adapt the activity to your ability.','wellbeing','🚶',10,7,6,'t'),
('w2_1',2,'Агар 3 қалам 12 сомонӣ бошад, 5 қалам бо ҳамон нарх чанд мешавад? Ҳисобро навис.','Если 3 ручки стоят 12 сомони, сколько стоят 5 по той же цене? Запиши расчёт.','If 3 pens cost 12 somoni, what do 5 cost at the same rate? Show the calculation.','Нархро барои як қалам ёб.','Найди цену одной ручки.','Find the price of one pen.','logic','🔢',20,7,1,'t'),
('w2_2',2,'Дар бораи як мушкили маҳалла се роҳи ҳал пешниҳод кун.','Предложи три решения одной проблемы своего района.','Suggest three solutions to one problem in your neighbourhood.','Роҳҳои амалӣ ва бехатар интихоб кун.','Выбирай практичные и безопасные решения.','Choose practical, safe solutions.','creativity','✍️',20,7,2,'t'),
('w2_3',2,'Пойтахти се кишвари Осиёи Марказиро ёб ва аз ёд бигӯ.','Найди столицы трёх стран Центральной Азии и запомни их.','Find the capitals of three Central Asian countries and memorise them.','Номҳоро бо харита санҷ.','Проверь названия по карте.','Check the names on a map.','learning','🌍',15,7,3,'t'),
('w2_4',2,'Панҷ калимаи нав омӯз ва баъди 10 дақиқа бе нигоҳ кардан такрор кун.','Выучи пять новых слов и повтори их через 10 минут без подсказки.','Learn five new words and recall them after 10 minutes without looking.','Забонро худат интихоб кун.','Выбери язык сам.','Choose the language.','memory','🗣️',15,7,4,'t'),
('w2_5',2,'Як роҳи кам кардани партовро дар хона амалӣ кун.','Примени дома один способ уменьшить количество отходов.','Apply one way to reduce waste at home.','Аввал бо аҳли хона мувофиқа кун.','Сначала согласуй с семьёй.','Agree with your household first.','community','♻️',15,7,5,'t'),
('w2_6',2,'Як матнро 10 дақиқа бихон ва мазмунашро дар ду ҷумла ҷамъбаст кун.','Читай текст 10 минут и изложи его смысл в двух предложениях.','Read a text for 10 minutes and summarise it in two sentences.','Матнро худат интихоб кун.','Выбери текст самостоятельно.','Choose the text yourself.','focus','📖',10,7,6,'t'),
('w3_1',3,'Се рақами пайдарпай ҷамъ 36 мешаванд. Он рақамҳоро ёб.','Сумма трёх последовательных чисел равна 36. Найди эти числа.','Three consecutive numbers add up to 36. Find them.','Аз рақами миёна оғоз кун.','Начни со среднего числа.','Start with the middle number.','logic','🧠',20,7,1,'t'),
('w3_2',3,'Як далели илмиро омӯз ва фарқи далелу тахминро шарҳ деҳ.','Изучи один научный факт и объясни разницу между фактом и предположением.','Study one scientific fact and explain the difference between fact and assumption.','Манбаи боэътимод истифода бар.','Используй надёжный источник.','Use a reliable source.','learning','🔬',20,7,2,'t'),
('w3_3',3,'Шаш рақамро интихоб кун, 20 сония нигоҳ кун ва аз охир ба аввал такрор намо.','Выбери шесть цифр, смотри 20 секунд и повтори их в обратном порядке.','Choose six digits, study them for 20 seconds, then recall them backwards.','Рақамҳои тасодуфӣ истифода бар.','Используй случайные цифры.','Use random digits.','memory','🧩',15,7,3,'t'),
('w3_4',3,'Аз як ашёи оддӣ се истифодаи нави муфид фикр кун.','Придумай три новых полезных применения обычного предмета.','Think of three useful new ways to use an ordinary object.','Ашёро вайрон накун.','Не повреждай предмет.','Do not damage the object.','creativity','🎨',15,7,4,'t'),
('w3_5',3,'Ба як нафар бодиққат гӯш кун ва фикрашро бе қатъ кардан фаҳм.','Внимательно выслушай человека, не перебивая, и постарайся понять его мысль.','Listen carefully to someone without interrupting and try to understand their point.','Сӯҳбатро маҷбурӣ накун.','Не навязывай разговор.','Do not force a conversation.','kindness','💛',15,7,5,'t'),
('w3_6',3,'Дар давоми рӯз ба нӯшидани об диққат деҳ ва як одати солимро интихоб кун.','В течение дня следи за питьевым режимом и выбери одну здоровую привычку.','Pay attention to hydration today and choose one healthy habit.','Миқдорро ба эҳтиёҷи бадан мутобиқ кун.','Подстраивай количество под потребности организма.','Adapt intake to your body''s needs.','wellbeing','💧',10,7,6,'t'),
('w4_1',4,'Периметри росткунҷа 30 см, дарозӣ 9 см аст. Барашро ёб.','Периметр прямоугольника 30 см, длина 9 см. Найди ширину.','A rectangle has perimeter 30 cm and length 9 cm. Find its width.','Формулаи периметрро истифода бар.','Используй формулу периметра.','Use the perimeter formula.','logic','📐',20,7,1,'t'),
('w4_2',4,'Як хабари рӯзро аз ду манбаъ санҷ ва фарқи маълумотро муайян кун.','Проверь одну новость дня по двум источникам и найди различия.','Check one current news item against two sources and identify differences.','Сарлавҳаро танҳо далел ҳисоб накун.','Не считай один заголовок доказательством.','Do not treat a headline alone as proof.','learning','📰',15,7,2,'t'),
('w4_3',4,'15 дақиқа нақшаи як кори муҳимро иҷро кун ва натиҷаро қайд намо.','15 минут выполняй план важного дела и отметь результат.','Spend 15 minutes following a plan for an important task and record the result.','Як ҳадафи мушаххас гузор.','Поставь конкретную цель.','Set one specific goal.','focus','⏱️',15,7,3,'t'),
('w4_4',4,'Ҳикояи кӯтоҳи 5 ҷумлаӣ бо ибтидо, мушкил ва анҷом навис.','Напиши короткую историю из 5 предложений с началом, проблемой и концом.','Write a five-sentence story with a beginning, problem, and ending.','Мавзӯъ озод аст.','Тема свободная.','Any topic.','creativity','📝',20,7,4,'t'),
('w4_5',4,'Як гул ё растаниро нигоҳубин кун ва тағйироташро мушоҳида намо.','Позаботься о растении и понаблюдай за его состоянием.','Care for a plant and observe its condition.','Обёриро аз ҳад зиёд накун.','Не переливай растение.','Do not overwater it.','community','🌱',15,7,5,'t'),
('w4_6',4,'Номи панҷ китоб, шаҳр ё шахсияти таърихиро аз як мавзӯъ ба ёд биёр.','Вспомни пять книг, городов или исторических личностей по одной теме.','Recall five books, cities, or historical figures from one topic.','Мавзӯъро худат интихоб кун.','Выбери тему сам.','Choose the topic yourself.','memory','📚',15,7,6,'t'),
('w5_1',5,'Агар имрӯз чоршанбе бошад, пас аз 100 рӯз кадом рӯзи ҳафта мешавад?','Если сегодня среда, какой день недели будет через 100 дней?','If today is Wednesday, what day will it be in 100 days?','Боқимондаи тақсим ба 7-ро ёб.','Найди остаток от деления на 7.','Find the remainder when dividing by 7.','logic','🔍',20,7,1,'t'),
('w5_2',5,'Як мафҳуми рақамиро омӯз: рамзгузорӣ, алгоритм ё махфият. Бо мисол фаҳмон.','Изучи цифровое понятие: кодирование, алгоритм или приватность. Объясни на примере.','Learn a digital concept: encoding, algorithm, or privacy. Explain with an example.','Манбаи омӯзиширо истифода бар.','Используй учебный источник.','Use a learning source.','learning','💻',15,7,2,'t'),
('w5_3',5,'Як рӯйхати ҳафт унсуриро аз ёд кун ва баъди 5 дақиқа такрор намо.','Запомни список из семи элементов и повтори его через 5 минут.','Memorise a seven-item list and recall it after five minutes.','Рӯйхатро худат соз.','Составь список сам.','Create your own list.','memory','🎭',15,7,3,'t'),
('w5_4',5,'Ба кори муштараки хона ё гурӯҳ саҳми хурд гузор.','Внеси небольшой вклад в общее домашнее или групповое дело.','Make a small contribution to a shared household or group task.','Саҳм бояд мувофиқи синну сол бошад.','Учитывай возраст и безопасность.','Keep it age-appropriate and safe.','kindness','🙌',15,7,4,'t'),
('w5_5',5,'Панҷ дақиқа ором нишин, нафасро табиӣ нигоҳ дор ва баъд ҳолататро арзёбӣ кун.','Пять минут спокойно посиди, дыши естественно, затем оцени своё состояние.','Sit quietly for five minutes, breathe naturally, then check how you feel.','Нафасро нигоҳ надор.','Не задерживай дыхание.','Do not hold your breath.','wellbeing','🧘',10,7,5,'t'),
('w5_6',5,'Барои беҳтар кардани як қисми мактаб ё маҳалла идея ва се қадами иҷро навис.','Придумай улучшение для школы или района и запиши три шага реализации.','Create an idea to improve a school or neighbourhood and write three steps to implement it.','Идея бояд бехатар ва воқеӣ бошад.','Идея должна быть безопасной и реалистичной.','Keep it safe and realistic.','creativity','💡',20,7,6,'t'),
('w6_1',6,'Нисфи адад 18 аст. Сеяки ҳамон адад чанд мешавад?','Половина числа равна 18. Чему равна треть этого числа?','Half of a number is 18. What is one third of that number?','Аввал ададро пурра ёб.','Сначала найди целое число.','Find the whole number first.','logic','🧮',20,7,1,'t'),
('w6_2',6,'Як мавзӯи душворро интихоб кун ва онро ба кӯдаки хурдсол бо суханони сода фаҳмон.','Выбери сложную тему и объясни её младшему ребёнку простыми словами.','Choose a difficult topic and explain it to a younger child in simple words.','Ба касе маҷбурӣ дарс надеҳ.','Не навязывай объяснение.','Do not force anyone to listen.','learning','📘',15,7,2,'t'),
('w6_3',6,'Як масъалаи мантиқиро 15 дақиқа ҳал кун; қадамҳои фикратро навис.','Решай логическую задачу 15 минут и запиши ход рассуждений.','Spend 15 minutes solving a logic problem and write down your reasoning.','Ҷавобро бе шарҳ нагузор.','Не ограничивайся ответом.','Do not provide only the answer.','focus','🧠',15,7,3,'t'),
('w6_4',6,'Ҷойи умумии истифодашавандаро бо иҷозат ба тартиб дарор.','С разрешения наведи порядок в общем пространстве.','With permission, tidy a shared space.','Кори хатарнокро иҷро накун.','Не выполняй опасную работу.','Do not do hazardous work.','community','🧹',15,7,4,'t'),
('w6_5',6,'Як идеяро дар 60 сония равшан пешниҳод кун: мушкил, ҳалли он ва фоида.','За 60 секунд ясно представь идею: проблема, решение и польза.','Present an idea clearly in 60 seconds: problem, solution, and benefit.','Метавонӣ танҳо машқ кунӣ.','Можно потренироваться одному.','You may practise alone.','creativity','🎤',20,7,5,'t'),
('w6_6',6,'Аз хотира се вазифаи ҳафтаи гузашта ва натиҷаи онҳоро ба ёд биёр.','Вспомни три задания прошлой недели и их результаты.','Recall three tasks from last week and their outcomes.','Ростқавлона арзёбӣ кун.','Оцени честно.','Be honest in your reflection.','memory','🔁',15,7,6,'t'),
('w7_1',7,'Як муаммои мантиқӣ ё шоҳмотро ҳал кун ва роҳи ҳалро шарҳ деҳ.','Реши логическую или шахматную задачу и объясни ход решения.','Solve a logic or chess puzzle and explain your solution.','Душвориро мувофиқи сатҳ интихоб кун.','Выбери подходящий уровень сложности.','Choose an appropriate difficulty.','logic','♟️',25,7,1,'t'),
('w7_2',7,'Се далелро дар бораи як мавзӯъ ёд гир ва якеашро бо манбаи дигар тасдиқ кун.','Узнай три факта по одной теме и проверь один из них по другому источнику.','Learn three facts on one topic and verify one using another source.','Сарчашмаро қайд намо.','Укажи источник.','Note the source.','learning','🌐',15,7,2,'t'),
('w7_3',7,'Як чизи муфидро аз маводи дастрас тарҳрезӣ кун ё нақшаашро каш.','Спроектируй полезную вещь из доступных материалов или нарисуй план.','Design something useful from available materials or sketch a plan.','Бе асбоби хатарнок.','Без опасных инструментов.','No dangerous tools.','creativity','🛠️',20,7,3,'t'),
('w7_4',7,'Як амали некро бе интизори подош анҷом деҳ.','Сделай доброе дело без ожидания награды.','Do one kind act without expecting a reward.','Эҳтироми дигаронро нигоҳ дор.','Уважай личные границы других.','Respect other people''s boundaries.','kindness','🌟',15,7,4,'t'),
('w7_5',7,'10 дақиқа ҳаракати сабук ё сайругашти бехатар анҷом деҳ.','Сделай 10 минут лёгкой активности или безопасной прогулки.','Do 10 minutes of light activity or a safe walk.','Ҳангоми нороҳатӣ қатъ кун.','Остановись при дискомфорте.','Stop if uncomfortable.','wellbeing','🚶',10,7,5,'t'),
('w7_6',7,'Барои ҳафтаи оянда се ҳадафи воқеӣ гузор ва қадами аввалро муайян кун.','Поставь три реалистичные цели на следующую неделю и определи первый шаг.','Set three realistic goals for next week and identify the first step.','Ҳадафҳоро ченшаванда соз.','Сделай цели измеримыми.','Make the goals measurable.','focus','📅',20,7,6,'t') on conflict (id) do update set title_tg=excluded.title_tg,title_ru=excluded.title_ru,title_en=excluded.title_en,description_tg=excluded.description_tg,description_ru=excluded.description_ru,description_en=excluded.description_en,category=excluded.category,icon=excluded.icon,xp=excluded.xp;
create schema if not exists private;
revoke all on schema private from public,anon;
grant usage on schema private to authenticated;
create function private.shahrvand_complete_weekly_task_impl(p_task_id text) returns jsonb language plpgsql security definer set search_path='' as $$
declare v_user_id uuid:=auth.uid(); v_today date:=(now() at time zone 'Asia/Dushanbe')::date;
v_week_start date:=date_trunc('week',v_today::timestamp)::date; v_weekday smallint:=extract(isodow from v_today)::smallint;
v_age smallint; v_task public.shahrvand_weekly_tasks%rowtype; v_inserted public.shahrvand_weekly_completions%rowtype; v_total_xp integer;
begin
if v_user_id is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
select age into v_age from public.shahrvand_profiles where id=v_user_id;
if v_age is null then raise exception 'PROFILE_REQUIRED' using errcode='P0002'; end if;
select * into v_task from public.shahrvand_weekly_tasks where id=p_task_id and is_active=true and weekday=v_weekday and min_age<=v_age;
if not found then raise exception 'TASK_NOT_AVAILABLE_TODAY' using errcode='22023'; end if;
insert into public.shahrvand_weekly_completions(user_id,task_id,completed_on,week_start,xp_awarded)
values(v_user_id,v_task.id,v_today,v_week_start,v_task.xp) on conflict(user_id,task_id,completed_on) do nothing returning * into v_inserted;
if v_inserted.id is null then raise exception 'TASK_ALREADY_COMPLETED' using errcode='23505'; end if;
select coalesce(sum(xp_awarded),0)::integer into v_total_xp from public.shahrvand_weekly_completions where user_id=v_user_id and week_start=v_week_start;
return jsonb_build_object('ok',true,'xp_awarded',v_task.xp,'weekly_xp',v_total_xp,'completed_on',v_today,'week_start',v_week_start);
end; $$;
revoke all on function private.shahrvand_complete_weekly_task_impl(text) from public,anon;
grant execute on function private.shahrvand_complete_weekly_task_impl(text) to authenticated;
create function public.shahrvand_complete_weekly_task(p_task_id text) returns jsonb language sql security invoker set search_path='' as $$ select private.shahrvand_complete_weekly_task_impl(p_task_id); $$;
revoke all on function public.shahrvand_complete_weekly_task(text) from public,anon;
grant execute on function public.shahrvand_complete_weekly_task(text) to authenticated;
create function private.shahrvand_get_leaderboard_impl(p_age_band text)
returns table(rank bigint,login text,weekly_xp bigint,tasks_completed bigint,is_me boolean)
language plpgsql security definer set search_path='' as $$
declare v_user_id uuid:=auth.uid(); v_today date:=(now() at time zone 'Asia/Dushanbe')::date;
v_week_start date:=date_trunc('week',v_today::timestamp)::date; v_min_age integer; v_max_age integer;
begin
if v_user_id is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
if p_age_band not in ('7_12','13_17','18_plus') then raise exception 'INVALID_AGE_BAND' using errcode='22023'; end if;
v_min_age:=case p_age_band when '7_12' then 7 when '13_17' then 13 else 18 end;
v_max_age:=case p_age_band when '7_12' then 12 when '13_17' then 17 else 120 end;
return query with totals as (
select c.user_id,sum(c.xp_awarded)::bigint xp,count(*)::bigint completed,min(c.created_at) first_score
from public.shahrvand_weekly_completions c where c.week_start=v_week_start group by c.user_id
), eligible as (
select p.id,p.login,t.xp,t.completed,t.first_score from totals t join public.shahrvand_profiles p on p.id=t.user_id where p.age between v_min_age and v_max_age
), ranked as (
select row_number() over(order by e.xp desc,e.completed desc,e.first_score asc,e.login asc) pos,e.login,e.xp,e.completed,e.id from eligible e
) select r.pos,r.login,r.xp,r.completed,(r.id=v_user_id) from ranked r order by r.pos limit 100;
end; $$;
revoke all on function private.shahrvand_get_leaderboard_impl(text) from public,anon;
grant execute on function private.shahrvand_get_leaderboard_impl(text) to authenticated;
create function public.shahrvand_get_leaderboard(p_age_band text)
returns table(rank bigint,login text,weekly_xp bigint,tasks_completed bigint,is_me boolean)
language sql security invoker set search_path='' as $$ select * from private.shahrvand_get_leaderboard_impl(p_age_band); $$;
revoke all on function public.shahrvand_get_leaderboard(text) from public,anon;
grant execute on function public.shahrvand_get_leaderboard(text) to authenticated;
