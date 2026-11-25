update public.shahrvand_activity_task_catalog
set title_tg='2 км роҳ гард', title_ru='Пройди 2 км пешком', title_en='Walk 2 km',
    description_tg='2 километр роҳро бо GPS сабт кун. Барои тасдиқ вақт, қадамҳо ва дақиқии GPS санҷида мешаванд.',
    description_ru='Запиши 2 километра пешком по GPS. Для проверки учитываются время, шаги и точность GPS.',
    description_en='Record a 2 km walk with GPS. Time, steps, and GPS accuracy are checked.'
where id='walk_4km';

update public.shahrvand_activity_task_catalog
set title_tg='15 приседание бо техникаи дуруст', title_ru='15 правильных приседаний', title_en='15 correct squats',
    description_tg='15 ҳаракати пурраро бо муайянкунии ҳолати бадан иҷро кун. Ҳисобкунӣ дар телефон мегузарад.',
    description_ru='Выполни 15 полных повторений с анализом положения тела. Подсчёт выполняется на телефоне.',
    description_en='Complete 15 full repetitions with on-device pose analysis.'
where id='squat_25';

update public.shahrvand_activity_task_catalog
set title_tg='8 отжимание бо техникаи дуруст', title_ru='8 правильных отжиманий', title_en='8 correct push-ups',
    description_tg='8 такрори пурраро бо нигоҳ доштани ҳолати дурусти бадан иҷро кун. Видео ба сервер фиристода намешавад.',
    description_ru='Выполни 8 полных повторений с правильным положением тела. Видео не отправляется на сервер.',
    description_en='Complete 8 full repetitions with correct body alignment. No video is uploaded.'
where id='pushup_30';
