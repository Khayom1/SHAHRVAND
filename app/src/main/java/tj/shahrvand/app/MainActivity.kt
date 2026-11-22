package tj.shahrvand.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val Navy = Color(0xFF173B59)
private val NavyLight = Color(0xFF244F70)
private val Gold = Color(0xFFC49A52)
private val Green = Color(0xFF287A58)
private val Canvas = Color(0xFFF6F3EC)
private val Ink = Color(0xFF1D2B35)
private val Muted = Color(0xFF74818A)
private val Line = Color(0xFFE8E3D9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Navy,
                    secondary = Gold,
                    tertiary = Green,
                    background = Canvas,
                    surface = Color.White,
                    onBackground = Ink,
                    onSurface = Ink
                )
            ) { ShahrvandApp() }
        }
    }
}

private data class Mission(
    val id: String,
    val title: String,
    val category: String,
    val descriptionTg: String,
    val descriptionRu: String,
    val descriptionEn: String,
    val xp: Int,
    val duration: String,
    val icon: String,
    val personal: Boolean = false
) {
    fun description(language: String): String = when (language) {
        "ru" -> descriptionRu
        "en" -> descriptionEn
        else -> descriptionTg
    }
}

private data class PeriodRank(
    val key: String,
    val userXp: Int,
    val userRank: Int,
    val topXp: Int,
    val step: Int
)

private data class BadgeInfo(
    val id: String,
    val icon: String,
    val titleTg: String,
    val titleRu: String,
    val titleEn: String,
    val ruleTg: String,
    val ruleRu: String,
    val ruleEn: String,
    val earned: Boolean
) {
    fun title(language: String) = when (language) {
        "ru" -> titleRu
        "en" -> titleEn
        else -> titleTg
    }
    fun rule(language: String) = when (language) {
        "ru" -> ruleRu
        "en" -> ruleEn
        else -> ruleTg
    }
}

private val initialMissions = listOf(
    Mission("run", "2 км давидан", "Варзиш", "Масофаи 2 километрро бо суръати мувофиқ тай кунед. Бехатариро ба назар гиред.", "Пробегите 2 километра в комфортном темпе. Соблюдайте безопасность.", "Run 2 kilometres at a comfortable pace. Prioritise safety.", 40, "20–35 дақ.", "🏃"),
    Mission("clean", "Тоза кардани кӯча", "Ҷомеа", "Қисми бехатари кӯча ё назди маҳалли истиқоматро тоза кунед.", "Уберите безопасный участок улицы или территорию рядом с домом.", "Clean a safe section of a street or nearby public area.", 80, "30–45 дақ.", "🧹"),
    Mission("tree", "Шинондани дарахт", "Экология", "Дарахтро дар ҷойи мувофиқ ва бо иҷозати зарурӣ шинонед.", "Посадите дерево в подходящем месте с необходимым разрешением.", "Plant a tree in a suitable place with any required permission.", 100, "30–60 дақ.", "🌱"),
    Mission("read", "30 дақиқа китобхонӣ", "Дониш", "30 дақиқа мавзӯи муфидро хонед ва хулоса нависед.", "Читайте полезную тему 30 минут и запишите вывод.", "Read a useful topic for 30 minutes and write a short takeaway.", 30, "30 дақ.", "📚")
)

private val rankNames = listOf(
    "Фирдавс Саидов", "Меҳрона Раҳмон", "Сомон Каримов", "Далер Набиев", "Мадина Юсуфӣ",
    "Шаҳбоз Алиев", "Фарзона Ҳасанова", "Муҳаммад Назаров", "Ситора Давлатова", "Амир Ҷӯраев",
    "Зуҳро Саидова", "Беҳрӯз Шарифов", "Нозим Раҳимов", "Манижа Абдуллоева", "Фаридун Исмоилов",
    "Меҳроҷ Ҳакимов", "Шабнам Қодирова", "Рустам Сафаров", "Нигина Бобоева", "Сорбон Мирзоев",
    "Мавзуна Раҳматова", "Парвиз Саидов", "Дилноза Олимова", "Фирӯз Шарипов", "Шаҳноз Назарова",
    "Комрон Давлатов", "Сабрина Ҳусейнова", "Абдулло Раҳимов", "Малика Нурова", "Ҷамшед Каримов"
)

private val periods = listOf(
    PeriodRank("rank_today", 120, 86, 520, 11),
    PeriodRank("rank_week", 980, 112, 5200, 85),
    PeriodRank("rank_month", 1640, 147, 18000, 260),
    PeriodRank("rank_year", 2450, 203, 65000, 900)
)

private val badges = listOf(
    BadgeInfo("first", "⭐", "Оғози роҳ", "Первый шаг", "First Step",
        "Иҷрои 1 вазифаи тасдиқшуда.", "Выполнить 1 подтверждённое задание.", "Complete 1 verified mission.", true),
    BadgeInfo("nature", "🌳", "Ҳомии табиат", "Защитник природы", "Nature Guardian",
        "Дарахт шинонед ва далели онро пешниҳод кунед; тасдиқи маъмурият зарур аст.", "Посадите дерево и отправьте подтверждение; требуется проверка администратора.", "Plant a tree and submit evidence; administrator verification is required.", false),
    BadgeInfo("reader", "📖", "Китобхон", "Книголюб", "Reader",
        "5 вазифаи бахши донишро бо далели тасдиқшуда иҷро кунед.", "Выполните 5 заданий раздела знаний с подтверждением.", "Complete 5 learning missions with verified evidence.", false),
    BadgeInfo("runner", "🏃", "Варзишгар", "Спортсмен", "Athlete",
        "Аз фаъолиятҳои варзишӣ 500 XP-и тасдиқшуда ҷамъ кунед.", "Наберите 500 подтверждённых XP за спортивные активности.", "Earn 500 verified XP from fitness activities.", false),
    BadgeInfo("helper", "🤝", "Ёрдамчии ҷомеа", "Помощник сообщества", "Community Helper",
        "Дар 5 фаъолияти ҷомеавӣ иштирок кунед; ҳамаи онҳо бояд тасдиқ шаванд.", "Участвуйте в 5 общественных активностях; все должны быть подтверждены.", "Take part in 5 community activities; all must be verified.", false),
    BadgeInfo("citizen", "🏆", "Шаҳрванди рӯз", "Гражданин дня", "Citizen of the Day",
        "Маъмурият барои саҳми махсуси рӯзона ва фаъолияти тасдиқшуда интихоб мекунад.", "Администрация выбирает участника за особый вклад и подтверждённую активность дня.", "Administrators select a participant for a special daily contribution and verified activity.", false)
)

private val words = mapOf(
    "tg" to mapOf(
        "tagline" to "Ҳар қадам — як пешрафт", "prototype" to "ВЕРСИЯИ ТЕСТӢ • МАЪЛУМОТИ НАМУНАВӢ",
        "language" to "Забон", "create_profile" to "Сохтани профил", "first_name" to "Ном", "last_name" to "Насаб",
        "age" to "Синну сол", "add_photo" to "Иловаи акс", "photo_optional" to "Акси профил (ихтиёрӣ)",
        "continue" to "Идома додан", "invalid_form" to "Ном, насаб ва синну соли дурустро ворид кунед. Синну сол аз 7 боло бошад.",
        "home" to "Асосӣ", "missions" to "Вазифаҳо", "ranking" to "Рейтинг", "profile" to "Профил",
        "hello" to "Салом", "progress" to "ПЕШРАФТИ ШУМО", "completed" to "Иҷрошуда", "pending" to "Интизори тасдиқ",
        "remaining" to "Боқимонда", "today_mission" to "Вазифаи имрӯз", "view_mission" to "Дидан",
        "citizen_day" to "Шаҳрванди рӯз", "citizen_day_note" to "Ғолиб баъди санҷиши натиҷаҳо муайян мешавад.",
        "recent" to "Фаъолияти охирин", "empty" to "Ҳоло пешниҳод нест.", "all_missions" to "Вазифаҳои дастрас",
        "personal" to "Пешниҳоди фаъолияти шахсӣ", "personal_note" to "XP танҳо баъди санҷиш муайян мешавад.",
        "task_title" to "Номи фаъолият", "task_description" to "Тавсифи кӯтоҳ", "send" to "Ирсоли пешниҳод",
        "duration" to "Давомнокӣ", "reward" to "Мукофот", "after_review" to "Пас аз санҷиш",
        "proof" to "Далели иҷро", "photo" to "Акс", "video" to "Видео", "proof_selected" to "Далел интихоб шуд",
        "send_review" to "Ирсол барои санҷиш", "waiting" to "Дар интизори санҷиш",
        "no_file" to "Аввал акс ё видео интихоб кунед.", "submitted" to "Пешниҳод дар ин дастгоҳ сабт шуд.",
        "top30" to "ТОП 30", "your_position" to "Ҷойи шумо", "rank_today" to "Имрӯз", "rank_week" to "Ҳафта",
        "rank_month" to "Моҳ", "rank_year" to "Сол", "xp" to "XP", "level" to "Сатҳ", "achievements" to "Титулҳо ва нишонҳо",
        "history" to "Таърихи фаъолият", "settings" to "Танзимот", "edit" to "Таҳрири профил",
        "save" to "Нигоҳ доштан", "cancel" to "Бекор кардан", "earned" to "Гирифташуда",
        "locked" to "Ҳанӯз гирифта нашудааст", "how_to_get" to "Чӣ гуна гирифтан мумкин аст",
        "close" to "Фаҳмо", "reset" to "Пок кардани профили тестӣ", "reset_note" to "Маълумоти маҳаллии тестӣ пок мешавад.",
        "age_short" to "сола", "period_xp" to "Холҳои ин давра", "lifetime_xp" to "Ҳамаи XP",
        "rank_note" to "Хол ва мақом танҳо барои давраи интихобшуда мебошанд. XP-и умумӣ алоҳида аст.",
        "demo_note" to "Тестӣ: маълумот маҳаллӣ нигоҳ дошта мешавад; сервер ва тасдиқи воқеӣ пайваст нест."
    ),
    "ru" to mapOf(
        "tagline" to "Каждый шаг — это прогресс", "prototype" to "ТЕСТОВАЯ ВЕРСИЯ • ДЕМО-ДАННЫЕ",
        "language" to "Язык", "create_profile" to "Создать профиль", "first_name" to "Имя", "last_name" to "Фамилия",
        "age" to "Возраст", "add_photo" to "Добавить фото", "photo_optional" to "Фото профиля (необязательно)",
        "continue" to "Продолжить", "invalid_form" to "Введите имя, фамилию и возраст от 7 лет.",
        "home" to "Главная", "missions" to "Задания", "ranking" to "Рейтинг", "profile" to "Профиль",
        "hello" to "Здравствуйте", "progress" to "ВАШ ПРОГРЕСС", "completed" to "Выполнено", "pending" to "На проверке",
        "remaining" to "Осталось", "today_mission" to "Задание дня", "view_mission" to "Открыть",
        "citizen_day" to "Гражданин дня", "citizen_day_note" to "Победитель определяется после проверки результатов.",
        "recent" to "Последняя активность", "empty" to "Пока нет отправок.", "all_missions" to "Доступные задания",
        "personal" to "Предложить свою активность", "personal_note" to "XP назначается только после проверки.",
        "task_title" to "Название активности", "task_description" to "Краткое описание", "send" to "Отправить предложение",
        "duration" to "Длительность", "reward" to "Награда", "after_review" to "После проверки",
        "proof" to "Подтверждение", "photo" to "Фото", "video" to "Видео", "proof_selected" to "Материал выбран",
        "send_review" to "Отправить на проверку", "waiting" to "Ожидает проверки",
        "no_file" to "Сначала выберите фото или видео.", "submitted" to "Отправка сохранена на устройстве.",
        "top30" to "ТОП 30", "your_position" to "Ваше место", "rank_today" to "День", "rank_week" to "Неделя",
        "rank_month" to "Месяц", "rank_year" to "Год", "xp" to "XP", "level" to "Уровень", "achievements" to "Титулы и значки",
        "history" to "История активности", "settings" to "Настройки", "edit" to "Изменить профиль",
        "save" to "Сохранить", "cancel" to "Отмена", "earned" to "Получено",
        "locked" to "Ещё не получено", "how_to_get" to "Как получить",
        "close" to "Понятно", "reset" to "Сбросить тестовый профиль", "reset_note" to "Локальные тестовые данные будут удалены.",
        "age_short" to "лет", "period_xp" to "Баллы за период", "lifetime_xp" to "Всего XP",
        "rank_note" to "Баллы и место относятся только к выбранному периоду. Общий XP показан отдельно.",
        "demo_note" to "Тестовый режим: данные хранятся локально; сервер и реальная проверка не подключены."
    ),
    "en" to mapOf(
        "tagline" to "Every step is progress", "prototype" to "TEST BUILD • SAMPLE DATA",
        "language" to "Language", "create_profile" to "Create profile", "first_name" to "First name", "last_name" to "Surname",
        "age" to "Age", "add_photo" to "Add photo", "photo_optional" to "Profile photo (optional)",
        "continue" to "Continue", "invalid_form" to "Enter your first name, surname and age of at least 7.",
        "home" to "Home", "missions" to "Missions", "ranking" to "Ranking", "profile" to "Profile",
        "hello" to "Hello", "progress" to "YOUR PROGRESS", "completed" to "Completed", "pending" to "Pending review",
        "remaining" to "Remaining", "today_mission" to "Today's mission", "view_mission" to "Open",
        "citizen_day" to "Citizen of the Day", "citizen_day_note" to "The title is awarded after results are reviewed.",
        "recent" to "Recent activity", "empty" to "No submissions yet.", "all_missions" to "Available missions",
        "personal" to "Suggest an activity", "personal_note" to "XP is assigned only after review.",
        "task_title" to "Activity title", "task_description" to "Short description", "send" to "Submit proposal",
        "duration" to "Duration", "reward" to "Reward", "after_review" to "After review",
        "proof" to "Evidence", "photo" to "Photo", "video" to "Video", "proof_selected" to "Evidence selected",
        "send_review" to "Send for review", "waiting" to "Pending review",
        "no_file" to "Choose a photo or video first.", "submitted" to "Submission saved on this device.",
        "top30" to "TOP 30", "your_position" to "Your rank", "rank_today" to "Today", "rank_week" to "Week",
        "rank_month" to "Month", "rank_year" to "Year", "xp" to "XP", "level" to "Level", "achievements" to "Titles and badges",
        "history" to "Activity history", "settings" to "Settings", "edit" to "Edit profile",
        "save" to "Save changes", "cancel" to "Cancel", "earned" to "Earned",
        "locked" to "Not earned yet", "how_to_get" to "How to earn",
        "close" to "Got it", "reset" to "Reset test profile", "reset_note" to "Local test data will be removed.",
        "age_short" to "years old", "period_xp" to "Period XP", "lifetime_xp" to "Lifetime XP",
        "rank_note" to "Points and rank refer only to the selected period. Lifetime XP is separate.",
        "demo_note" to "Test mode: data is stored locally; no server or real review is connected."
    )
)

private fun tr(language: String, key: String): String =
    words[language]?.get(key) ?: words["tg"]?.get(key) ?: key

@Composable
private fun ShahrvandApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("shahrvand_local", Context.MODE_PRIVATE) }
    var language by remember { mutableStateOf(prefs.getString("language", "tg") ?: "tg") }
    var registered by remember { mutableStateOf(prefs.getBoolean("registered", false)) }
    var firstName by remember { mutableStateOf(prefs.getString("first_name", "") ?: "") }
    var lastName by remember { mutableStateOf(prefs.getString("last_name", "") ?: "") }
    var age by remember { mutableStateOf(prefs.getString("age", "") ?: "") }
    var avatarUri by remember { mutableStateOf(prefs.getString("avatar", null)?.let(Uri::parse)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedMission by remember { mutableStateOf<Mission?>(null) }
    val missions = remember { mutableStateListOf<Mission>().apply { addAll(initialMissions) } }
    val submittedIds = remember {
        mutableStateListOf<String>().apply {
            addAll((prefs.getString("submitted", "") ?: "").split("|").filter { it.isNotBlank() })
        }
    }

    SideEffect {
        prefs.edit()
            .putString("language", language)
            .putBoolean("registered", registered)
            .putString("first_name", firstName)
            .putString("last_name", lastName)
            .putString("age", age)
            .putString("avatar", avatarUri?.toString())
            .putString("submitted", submittedIds.joinToString("|"))
            .apply()
    }

    if (!registered) {
        RegistrationScreen(
            language = language,
            onLanguage = { language = it },
            firstName = firstName,
            onFirstName = { firstName = it },
            lastName = lastName,
            onLastName = { lastName = it },
            age = age,
            onAge = { age = it },
            avatarUri = avatarUri,
            onAvatar = { avatarUri = it },
            onContinue = { registered = true }
        )
        return
    }

    if (selectedMission != null) {
        MissionDetailScreen(
            mission = selectedMission!!,
            language = language,
            alreadySubmitted = submittedIds.contains(selectedMission!!.id),
            onBack = { selectedMission = null },
            onSubmitted = {
                if (!submittedIds.contains(selectedMission!!.id)) submittedIds.add(selectedMission!!.id)
                selectedMission = null
            }
        )
        return
    }

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val labels = listOf("home", "missions", "ranking", "profile")
                val icons = listOf(Icons.Filled.Home, Icons.Filled.Assignment, Icons.Filled.EmojiEvents, Icons.Filled.Person)
                labels.forEachIndexed { index, key ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icons[index], contentDescription = tr(language, key)) },
                        label = { Text(tr(language, key), fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(language, firstName, missions, submittedIds, onOpenMission = { selectedMission = it }, onOpenMissions = { selectedTab = 1 })
                1 -> MissionsScreen(
                    language, missions, submittedIds,
                    onOpenMission = { selectedMission = it },
                    onAddPersonal = { title, description ->
                        missions.add(0, Mission(
                            "personal_" + System.currentTimeMillis(), title, "Шахсӣ",
                            description, description, description, 0, "Пас аз санҷиш", "✨", true
                        ))
                    }
                )
                2 -> RankingScreen(language, firstName, lastName)
                else -> ProfileScreen(
                    language, firstName, lastName, age, avatarUri, submittedIds.size,
                    onLanguage = { language = it },
                    onSaveProfile = { f, l, a -> firstName = f; lastName = l; age = a },
                    onReset = {
                        prefs.edit().clear().apply()
                        firstName = ""; lastName = ""; age = ""; avatarUri = null
                        submittedIds.clear(); language = "tg"; registered = false; selectedTab = 0
                    }
                )
            }
        }
    }
}

@Composable
private fun RegistrationScreen(
    language: String,
    onLanguage: (String) -> Unit,
    firstName: String,
    onFirstName: (String) -> Unit,
    lastName: String,
    onLastName: (String) -> Unit,
    age: String,
    onAge: (String) -> Unit,
    avatarUri: Uri?,
    onAvatar: (Uri?) -> Unit,
    onContinue: () -> Unit
) {
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val firstFocus = remember { FocusRequester() }
    val lastFocus = remember { FocusRequester() }
    val ageFocus = remember { FocusRequester() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            onAvatar(uri)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Canvas).imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(tr(language, "language"), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.width(7.dp))
            listOf("tg", "ru", "en").forEach { code ->
                Text(
                    code.uppercase(),
                    color = if (language == code) Gold else Muted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onLanguage(code) }.padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.size(68.dp).clip(RoundedCornerShape(21.dp)).background(Navy), contentAlignment = Alignment.Center) {
            Text("Ш", color = Color.White, fontSize = 39.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))
        Text("ШАҲРВАНД", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
        Text(tr(language, "tagline"), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Text(tr(language, "create_profile"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "prototype"), color = Muted, fontSize = 9.sp, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(15.dp))
        if (avatarUri == null) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White).border(1.dp, Line, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, null, tint = Muted, modifier = Modifier.size(36.dp))
            }
        } else {
            AsyncImage(model = avatarUri, contentDescription = null, modifier = Modifier.size(72.dp).clip(CircleShape))
        }
        TextButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text(tr(language, "add_photo")) }
        Text(tr(language, "photo_optional"), color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                firstName, onFirstName,
                modifier = Modifier.weight(1f).focusRequester(firstFocus),
                label = { Text(tr(language, "first_name")) },
                singleLine = true, shape = RoundedCornerShape(13.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { lastFocus.requestFocus() })
            )
            OutlinedTextField(
                lastName, onLastName,
                modifier = Modifier.weight(1f).focusRequester(lastFocus),
                label = { Text(tr(language, "last_name")) },
                singleLine = true, shape = RoundedCornerShape(13.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { ageFocus.requestFocus() })
            )
        }
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(
            age, { onAge(it.filter(Char::isDigit).take(3)) },
            modifier = Modifier.fillMaxWidth().focusRequester(ageFocus),
            label = { Text(tr(language, "age")) },
            singleLine = true, shape = RoundedCornerShape(13.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() })
        )
        if (error) Text(tr(language, "invalid_form"), color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
        Spacer(Modifier.height(13.dp))
        Button(
            onClick = {
                val parsedAge = age.toIntOrNull()
                error = firstName.isBlank() || lastName.isBlank() || parsedAge == null || parsedAge !in 7..120
                if (!error) { keyboard?.hide(); onContinue() }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy)
        ) { Text(tr(language, "continue"), fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        Text(tr(language, "demo_note"), color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HomeScreen(
    language: String,
    firstName: String,
    missions: List<Mission>,
    submittedIds: List<String>,
    onOpenMission: (Mission) -> Unit,
    onOpenMissions: () -> Unit
) {
    val next = missions.firstOrNull { !submittedIds.contains(it.id) } ?: missions.first()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("ШАҲРВАНД", color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp)
                Text(tr(language, "tagline"), color = Gold, fontSize = 11.sp)
            }
            Surface(shape = CircleShape, color = Color.White) {
                Icon(Icons.Filled.NotificationsNone, null, tint = Navy, modifier = Modifier.padding(10.dp))
            }
        }
        Spacer(Modifier.height(17.dp))
        Text(tr(language, "hello") + ", " + firstName + "!", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "prototype"), color = Muted, fontSize = 9.sp)
        Spacer(Modifier.height(13.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(21.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(tr(language, "progress"), color = Color(0xFFDCE6ED), fontSize = 10.sp, letterSpacing = 1.2.sp)
                Text("2 450 XP", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr(language, "level") + " 3 • Амалгар", color = Color(0xFFE7D4A8), fontSize = 12.sp)
                    Text("65%", color = Color.White, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { 0.65f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Gold, trackColor = NavyLight)
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            StatCard(tr(language, "completed"), "12", Icons.Filled.CheckCircle, Green, Modifier.weight(1f))
            StatCard(tr(language, "pending"), submittedIds.size.toString(), Icons.Filled.AccessTime, Gold, Modifier.weight(1f))
            StatCard(tr(language, "remaining"), (missions.size - submittedIds.size).coerceAtLeast(0).toString(), Icons.Filled.Assignment, NavyLight, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        SectionHeader(tr(language, "today_mission"), tr(language, "view_mission"), onOpenMissions)
        MissionCard(next, language, submittedIds.contains(next.id), onClick = { onOpenMission(next) })
        Spacer(Modifier.height(15.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E8)), shape = RoundedCornerShape(17.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(tr(language, "citizen_day"), fontWeight = FontWeight.Bold, color = Navy)
                    Text(tr(language, "citizen_day_note"), color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        Text(tr(language, "recent"), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
        if (submittedIds.isEmpty()) Text(tr(language, "empty"), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        else missions.filter { submittedIds.contains(it.id) }.forEach { ActivityRow(it.title, tr(language, "waiting"), "—") }
        Spacer(Modifier.height(12.dp))
        Text(tr(language, "demo_note"), color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Ink)
            Text(label, fontSize = 9.sp, color = Muted, lineHeight = 11.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
        TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) { Text(action, color = Navy, fontSize = 11.sp) }
    }
}

@Composable
private fun ActivityRow(title: String, status: String, xp: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, null, tint = Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(status, color = Muted, fontSize = 10.sp)
        }
        Text(xp, color = Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun MissionsScreen(
    language: String,
    missions: SnapshotStateList<Mission>,
    submittedIds: List<String>,
    onOpenMission: (Mission) -> Unit,
    onAddPersonal: (String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Text(tr(language, "missions"), color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "all_missions"), color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF0F2)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = Navy)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr(language, "personal"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(tr(language, "personal_note"), color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = Navy)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(missions, key = { it.id }) { mission ->
                MissionCard(mission, language, submittedIds.contains(mission.id), onClick = { onOpenMission(mission) })
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(tr(language, "personal")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(tr(language, "personal_note"), fontSize = 12.sp, color = Muted)
                    OutlinedTextField(title, { title = it }, label = { Text(tr(language, "task_title")) }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text(tr(language, "task_description")) }, minLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onAddPersonal(title.trim(), description.trim())
                        title = ""; description = ""; showDialog = false
                    }
                }) { Text(tr(language, "send")) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(tr(language, "cancel")) } }
        )
    }
}

@Composable
private fun MissionCard(mission: Mission, language: String, submitted: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFF1EEE6)), contentAlignment = Alignment.Center) {
                Text(mission.icon, fontSize = 23.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(mission.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(mission.category + " • " + mission.duration, color = Muted, fontSize = 10.sp)
                if (submitted) Text(tr(language, "waiting"), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(if (mission.personal) tr(language, "after_review") else "+" + mission.xp + " XP", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MissionDetailScreen(
    mission: Mission,
    language: String,
    alreadySubmitted: Boolean,
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            proofUri = uri
        }
    }
    Column(Modifier.fillMaxSize().background(Canvas).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = tr(language, "cancel"), tint = Navy) }
            Text(tr(language, "missions"), color = Navy, fontWeight = FontWeight.Bold)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(21.dp)) {
                Text(mission.icon, fontSize = 36.sp)
                Text(mission.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(mission.category, color = Color(0xFFE7D4A8), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(mission.description(language), color = Ink, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            InfoPill(tr(language, "duration"), mission.duration)
            InfoPill(tr(language, "reward"), if (mission.personal) tr(language, "after_review") else mission.xp.toString() + " XP")
        }
        Spacer(Modifier.height(20.dp))
        Text(tr(language, "proof"), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(5.dp)); Text(tr(language, "photo"), fontSize = 11.sp) }
            OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.VideoLibrary, null); Spacer(Modifier.width(5.dp)); Text(tr(language, "video"), fontSize = 11.sp) }
        }
        if (proofUri != null) Text(tr(language, "proof_selected"), color = Green, fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp))
        if (alreadySubmitted) {
            Text(tr(language, "waiting"), color = Gold, modifier = Modifier.padding(top = 15.dp), fontWeight = FontWeight.Bold)
        } else {
            Button(
                onClick = {
                    if (proofUri == null) message = tr(language, "no_file")
                    else { message = tr(language, "submitted"); onSubmitted() }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 15.dp).height(51.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                shape = RoundedCornerShape(13.dp)
            ) { Text(tr(language, "send_review"), fontWeight = FontWeight.Bold) }
        }
        if (message != null) Text(message!!, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp))
        Spacer(Modifier.height(16.dp))
        Text(tr(language, "demo_note"), color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun InfoPill(label: String, value: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
            Text(label, color = Muted, fontSize = 10.sp)
            Text(value, color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RankingScreen(language: String, firstName: String, lastName: String) {
    var period by remember { mutableIntStateOf(0) }
    val keys = listOf("rank_today", "rank_week", "rank_month", "rank_year")
    val selected = periods[period]
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(17.dp))
        Text(tr(language, "ranking"), color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "top30"), color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            keys.forEachIndexed { index, key ->
                FilterChip(
                    selected = period == index,
                    onClick = { period = index },
                    label = { Text(tr(language, key), fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Navy, selectedLabelColor = Color.White)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(vertical = 9.dp)) {
            PeriodStat(tr(language, "period_xp"), selected.userXp.toString() + " XP", Modifier.weight(1f))
            PeriodStat(tr(language, "your_position"), "#" + selected.userRank, Modifier.weight(1f))
            PeriodStat(tr(language, "lifetime_xp"), "2 450 XP", Modifier.weight(1f))
        }
        Text(tr(language, "rank_note"), color = Muted, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(bottom = 7.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 9.dp)) {
            itemsIndexed(rankNames) { index, name ->
                val xp = selected.topXp - index * selected.step
                LeaderRow(index + 1, name, xp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr(language, "your_position") + " • " + tr(language, keys[period]), color = Color(0xFFDCE6ED), fontSize = 9.sp)
                    Text("#" + selected.userRank + "  •  " + (firstName + " " + lastName).trim(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(selected.userXp.toString() + " XP", color = Color(0xFFE7D4A8), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun PeriodStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 9.dp)) {
            Text(label, color = Muted, fontSize = 8.sp, maxLines = 1)
            Text(value, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LeaderRow(position: Int, name: String, xp: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(13.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(if (position <= 3) Color(0xFFFFF1D5) else Color(0xFFF1F2F2)), contentAlignment = Alignment.Center) {
                Text(position.toString(), color = if (position <= 3) Gold else Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(if (position == 1) "Шаҳрванди фаъол" else "Иштирокчӣ", color = Muted, fontSize = 9.sp)
            }
            Text("%,d XP".format(xp), color = Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileScreen(
    language: String,
    firstName: String,
    lastName: String,
    age: String,
    avatarUri: Uri?,
    submittedCount: Int,
    onLanguage: (String) -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onReset: () -> Unit
) {
    var selectedBadge by remember { mutableStateOf<BadgeInfo?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var editFirst by remember(firstName) { mutableStateOf(firstName) }
    var editLast by remember(lastName) { mutableStateOf(lastName) }
    var editAge by remember(age) { mutableStateOf(age) }
    var error by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(17.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tr(language, "profile"), color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.Settings, contentDescription = tr(language, "settings"), tint = Navy) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(21.dp)) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                if (avatarUri == null) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                } else {
                    AsyncImage(model = avatarUri, contentDescription = null, modifier = Modifier.size(64.dp).clip(CircleShape))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text((firstName + " " + lastName).trim(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(age + " " + tr(language, "age_short") + " • " + tr(language, "level") + " 3", color = Color(0xFFDCE6ED), fontSize = 10.sp)
                    Text("Амалгар", color = Color(0xFFE7D4A8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            StatCard(tr(language, "xp"), "2 450", Icons.Filled.Star, Gold, Modifier.weight(1f))
            StatCard(tr(language, "completed"), "12", Icons.Filled.CheckCircle, Green, Modifier.weight(1f))
            StatCard(tr(language, "ranking"), "#147", Icons.Filled.EmojiEvents, NavyLight, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Text(tr(language, "achievements"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(tr(language, "earned"), color = Muted, fontSize = 10.sp)
        BadgeGrid(badges.filter { it.earned }, language, onSelect = { selectedBadge = it })
        Spacer(Modifier.height(9.dp))
        Text(tr(language, "locked"), color = Muted, fontSize = 10.sp)
        BadgeGrid(badges.filter { !it.earned }, language, onSelect = { selectedBadge = it })
        Spacer(Modifier.height(16.dp))
        Text(tr(language, "history"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(tr(language, "completed") + ": 12", color = Muted, fontSize = 12.sp)
        Text(tr(language, "pending") + ": " + submittedCount, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(17.dp))
        Text(tr(language, "demo_note"), color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
    }

    if (selectedBadge != null) {
        val badge = selectedBadge!!
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            icon = { Text(badge.icon, fontSize = 34.sp) },
            title = { Text(badge.title(language)) },
            text = {
                Column {
                    Text(tr(language, "how_to_get"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(badge.rule(language), color = Ink, fontSize = 13.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(if (badge.earned) tr(language, "earned") else tr(language, "locked"), color = Muted, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = { selectedBadge = null }) { Text(tr(language, "close")) } }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(tr(language, "settings")) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr(language, "edit"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(editFirst, { editFirst = it }, label = { Text(tr(language, "first_name")) }, singleLine = true)
                    OutlinedTextField(editLast, { editLast = it }, label = { Text(tr(language, "last_name")) }, singleLine = true)
                    OutlinedTextField(
                        editAge, { editAge = it.filter(Char::isDigit).take(3) },
                        label = { Text(tr(language, "age")) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (error) Text(tr(language, "invalid_form"), color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    Text(tr(language, "language"), color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("tg", "ru", "en").forEach { code ->
                            FilterChip(selected = language == code, onClick = { onLanguage(code) }, label = { Text(code.uppercase()) })
                        }
                    }
                    Text(tr(language, "demo_note"), color = Muted, fontSize = 10.sp)
                    TextButton(onClick = { showSettings = false; onReset() }) { Text(tr(language, "reset"), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = editAge.toIntOrNull()
                    error = editFirst.isBlank() || editLast.isBlank() || parsed == null || parsed !in 7..120
                    if (!error) {
                        onSaveProfile(editFirst.trim(), editLast.trim(), editAge)
                        showSettings = false
                    }
                }) { Text(tr(language, "save")) }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text(tr(language, "cancel")) } }
        )
    }
}

@Composable
private fun BadgeGrid(items: List<BadgeInfo>, language: String, onSelect: (BadgeInfo) -> Unit) {
    items.chunked(3).forEach { rowItems ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowItems.forEach { badge ->
                Card(
                    modifier = Modifier.weight(1f).alpha(if (badge.earned) 1f else 0.48f).clickable { onSelect(badge) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(badge.icon, fontSize = 25.sp)
                        Text(badge.title(language), color = Ink, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp, maxLines = 2)
                    }
                }
            }
            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(7.dp))
    }
}
