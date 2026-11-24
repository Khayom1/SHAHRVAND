package tj.shahrvand.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private val Navy = Color(0xFF20384B)
private val NavyLight = Color(0xFF355D70)
private val Gold = Color(0xFFB98A50)
private val Green = Color(0xFF36766D)
private val Page = Color(0xFFF4EFE5)
private val Ink = Color(0xFF263640)
private val Muted = Color(0xFF777F7E)
private val BorderTone = Color(0xFFE4D9C8)
private const val PREFS_NAME = "shahrvand_v1"
private const val DAILY_TARGET = 3

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Navy, secondary = Gold, tertiary = Green,
                    background = Page, surface = Color.White,
                    onSurface = Ink, onBackground = Ink
                )
            ) {
                ShahrvandApp()
            }
        }
    }
}

private fun tr(lang: String, tg: String, ru: String, en: String): String = when (lang) {
    "ru" -> ru
    "en" -> en
    else -> tg
}

private fun ageBandLabel(lang: String, band: String): String = when (band) {
    "7_12" -> tr(lang, "7–12 сола", "7–12 лет", "Ages 7–12")
    "13_17" -> tr(lang, "13–17 сола", "13–17 лет", "Ages 13–17")
    else -> tr(lang, "18+ сола", "18+ лет", "Ages 18+")
}

data class Mission(
    val id: String,
    val titleTg: String,
    val titleRu: String,
    val titleEn: String,
    val descriptionTg: String,
    val descriptionRu: String,
    val descriptionEn: String,
    val category: String,
    val icon: String,
    val xp: Int,
    val minAge: Int = 7
) {
    fun title(lang: String) = tr(lang, titleTg, titleRu, titleEn)
    fun description(lang: String) = tr(lang, descriptionTg, descriptionRu, descriptionEn)
}

private val builtInMissions = listOf(
    Mission("read_10", "10 дақиқа китоб хон", "Читай 10 минут", "Read for 10 minutes",
        "Китоб ё матни омӯзиширо интихоб кун ва 10 дақиқа оромона бихон.",
        "Выбери книгу или обучающий текст и спокойно читай 10 минут.",
        "Choose a book or learning text and read calmly for 10 minutes.", "learning", "📚", 10),
    Mission("learn_one", "Як чизи нав омӯз", "Узнай что-то новое", "Learn one new thing",
        "Як мафҳум, калима ё малакаи навро омӯз ва онро бо суханони худ шарҳ деҳ.",
        "Узнай новое понятие, слово или навык и объясни его своими словами.",
        "Learn a new idea, word, or skill and explain it in your own words.", "learning", "💡", 10),
    Mission("help_home", "Дар хона ёрӣ расон", "Помоги дома", "Help at home",
        "Як кори хурди хонагиро бо мувофиқаи калонсолон иҷро кун.",
        "Выполни небольшое домашнее дело по согласованию со взрослым.",
        "Do a small household task with an adult’s agreement.", "kindness", "🤝", 15),
    Mission("kind_word", "Сухани нек бигӯ", "Скажи доброе слово", "Say something kind",
        "Ба касе сухани самимӣ ва эҳтиромона бигӯ. Ҳеҷ касро маҷбур накун.",
        "Скажи кому-нибудь искренние и уважительные слова. Никого не принуждай.",
        "Say something sincere and respectful to someone. Do not pressure anyone.", "kindness", "💛", 10),
    Mission("water_plant", "Ба растанӣ об деҳ", "Полей растение", "Water a plant",
        "Ба гул ё растании хона об деҳ; миқдори мувофиқро нигоҳ дор.",
        "Полей домашний цветок или растение, не переливая его.",
        "Water a home plant without overwatering it.", "nature", "🌱", 10),
    Mission("clean_space", "Ҷойи худро тоза кун", "Убери своё место", "Tidy your space",
        "Мизи корӣ ё ҷои шахсии худро ба тартиб дарор. Барои корҳои берунӣ аз калонсол иҷозат пурс.",
        "Наведи порядок на своём столе или личном месте. Для работ на улице сначала спроси взрослого.",
        "Tidy your desk or personal space. Ask an adult before doing anything outdoors.", "community", "🧹", 15),
    Mission("focus_5", "5 дақиқа диққатро ҷамъ кун", "Сосредоточься на 5 минут", "Focus for 5 minutes",
        "Як кори ором ва фоиданокро панҷ дақиқа бе парешонӣ анҷом деҳ.",
        "Пять минут спокойно занимайся одним полезным делом без отвлечений.",
        "Spend five calm minutes on one useful task without distractions.", "wellbeing", "🎯", 10)
)

private fun categoryTitle(lang: String, category: String): String = when (category) {
    "learning" -> tr(lang, "Дониш", "Знания", "Learning")
    "kindness" -> tr(lang, "Некӣ", "Доброта", "Kindness")
    "nature" -> tr(lang, "Табиат", "Природа", "Nature")
    "community" -> tr(lang, "Ҷомеа", "Общество", "Community")
    else -> tr(lang, "Диққат", "Внимание", "Focus")
}

private fun safeNickname(raw: String): String =
    raw.trim().filter { it.isLetterOrDigit() || it.isWhitespace() || it == '_' || it == '-' }
        .replace(Regex("\\s+"), " ")
        .take(18)

@Composable
private fun ShahrvandApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }

    var language by remember { mutableStateOf(prefs.getString("language", "tg") ?: "tg") }
    var ageBand by remember { mutableStateOf(prefs.getString("age_band", null)) }
    var isAuthenticated by remember { mutableStateOf(prefs.getBoolean("auth_signed_in", false)) }
    var nickname by remember { mutableStateOf(prefs.getString("nickname", "") ?: "") }
    var tab by remember { mutableIntStateOf(0) }
    var totalXp by remember { mutableIntStateOf(prefs.getInt("total_xp", 0)) }
    var totalCompleted by remember { mutableIntStateOf(prefs.getInt("total_completed", 0)) }
    var missions by remember { mutableStateOf(builtInMissions) }
    var catalogStatus by remember { mutableStateOf("") }
    var isLoadingCatalog by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now().toString() }
    val storedDay = remember { prefs.getString("progress_day", "") ?: "" }
    if (storedDay != today) {
        prefs.edit().putString("progress_day", today)
            .putStringSet("completed_today", emptySet()).apply()
    }
    val completedToday = remember {
        mutableStateListOf<String>().apply {
            addAll(prefs.getStringSet("completed_today", emptySet()) ?: emptySet())
        }
    }

    LaunchedEffect(ageBand) {
        if (ageBand == "18_plus") {
            isLoadingCatalog = true
            catalogStatus = tr(language,
                "Миссияҳо аз сервер санҷида мешаванд…",
                "Проверяем список заданий…",
                "Checking the public mission catalogue…")
            val remote = SupabaseMissionRepository.fetchMissions()
            if (!remote.isNullOrEmpty()) {
                missions = remote
                catalogStatus = tr(language, "Феҳристи миссияҳо нав аст.",
                    "Список заданий обновлён.", "Mission catalogue is up to date.")
            } else {
                missions = builtInMissions
                catalogStatus = tr(language,
                    "Офлайн: феҳристи дохилии миссияҳо истифода мешавад.",
                    "Офлайн: используется встроенный список заданий.",
                    "Offline: using the built-in mission catalogue.")
            }
            isLoadingCatalog = false
        } else {
            missions = builtInMissions
            catalogStatus = tr(language,
                "Маълумот танҳо дар ҳамин телефон мемонад.",
                "Данные остаются только на этом устройстве.",
                "Your game data stays on this device.")
        }
    }

    fun changeLanguage(value: String) {
        language = value
        prefs.edit().putString("language", value).apply()
    }

    fun finishMission(mission: Mission) {
        if (!ProgressRules.canCompleteToday(mission.id, completedToday.toSet())) return
        completedToday.add(mission.id)
        totalXp += mission.xp
        totalCompleted += 1
        prefs.edit()
            .putStringSet("completed_today", completedToday.toSet())
            .putInt("total_xp", totalXp)
            .putInt("total_completed", totalCompleted)
            .putString("progress_day", today)
            .apply()
    }

    if (!isAuthenticated) {
        AuthScreen(language = language, onLanguage = ::changeLanguage) { account ->
            isAuthenticated = true
            ageBand = account.ageBand
            nickname = account.login
            prefs.edit()
                .putBoolean("auth_signed_in", true)
                .putString("auth_access_token", account.accessToken)
                .putString("auth_user_id", account.userId)
                .putString("first_name", account.firstName)
                .putString("last_name", account.lastName ?: "")
                .putInt("account_age", account.age)
                .putString("login", account.login)
                .putString("age_band", account.ageBand)
                .putString("nickname", account.login)
                .putString("language", language)
                .apply()
        }
        return
    }
    if (ageBand == null) ageBand = prefs.getString("age_band", "18_plus") ?: "18_plus"

    Scaffold(
        containerColor = Page,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
                val labels = listOf(
                    tr(language, "Асосӣ", "Главная", "Home"),
                    tr(language, "Миссияҳо", "Задания", "Missions"),
                    tr(language, "Пешрафт", "Прогресс", "Progress"),
                    tr(language, "Оянда", "Будущее", "Future")
                )
                val icons = listOf(Icons.Filled.Home, Icons.Filled.TaskAlt, Icons.Filled.EmojiEvents, Icons.Filled.Explore)
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index, onClick = { tab = index },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    language, nickname, ageBand ?: "7_12", totalXp, totalCompleted,
                    completedToday, missions, catalogStatus, isLoadingCatalog,
                    onOpenMissions = { tab = 1 },
                    onComplete = ::finishMission,
                    onSettings = { settingsOpen = true }
                )
                1 -> MissionsScreen(language, missions, completedToday, onComplete = ::finishMission)
                2 -> ProgressScreen(language, totalXp, totalCompleted, completedToday.size)
                else -> FutureScreen(language, ageBand ?: "7_12", catalogStatus)
            }
        }
    }

    if (settingsOpen) {
        SettingsDialog(
            language = language,
            nickname = nickname,
            ageBand = ageBand ?: "7_12",
            onClose = { settingsOpen = false },
            onLanguage = ::changeLanguage,
            onSaveNickname = { value ->
                nickname = safeNickname(value)
                prefs.edit().putString("nickname", nickname).apply()
            },
            onReset = {
                prefs.edit().clear().apply()
                isAuthenticated = false
                language = "tg"
                nickname = ""
                ageBand = null
                totalXp = 0
                totalCompleted = 0
                completedToday.clear()
                missions = builtInMissions
                tab = 0
                settingsOpen = false
            }
        )
    }
}

@Composable
private fun WelcomeScreen(
    language: String,
    nickname: String,
    onNickname: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onContinue: (String) -> Unit
) {
    var selectedBand by remember { mutableStateOf("7_12") }
    Column(
        Modifier.fillMaxSize().background(Page).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            listOf("tg", "ru", "en").forEach { code ->
                FilterChip(selected = language == code, onClick = { onLanguage(code) }, label = { Text(code.uppercase()) })
                Spacer(Modifier.width(5.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        BrandHeader(
            tr(language, "ҲАР ҚАДАМ — ЯК ПЕШРАФТ", "КАЖДЫЙ ШАГ — ПРОГРЕСС", "EVERY STEP IS PROGRESS"),
            tr(language, "БОЗИИ МАҲОРАТ ВА НЕКӢ", "ИГРА НАВЫКОВ И ДОБРА", "A GAME OF SKILLS AND KINDNESS")
        )
        Spacer(Modifier.height(18.dp))
        Text(tr(language, "Хуш омадӣ!", "Добро пожаловать!", "Welcome!"),
            color = Navy, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text(
            tr(language, "Барои оғоз ҳисоб, рақами телефон ё парол лозим нест.",
                "Для начала не нужны аккаунт, номер телефона или пароль.",
                "No account, phone number, or password is needed to start."),
            color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 7.dp, bottom = 16.dp)
        )
        OutlinedTextField(
            value = nickname, onValueChange = { onNickname(it.take(30)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(tr(language, "Номи бозӣ (ихтиёрӣ)", "Игровое имя (необязательно)", "Nickname (optional)")) },
            supportingText = { Text(tr(language, "Номи ҳақиқӣ навиштан шарт нест.",
                "Настоящее имя указывать не нужно.", "You do not need to use your real name.")) }
        )
        Spacer(Modifier.height(16.dp))
        Text(tr(language, "Гурӯҳи синнусолиро интихоб кун", "Выбери возрастную группу", "Choose an age group"),
            color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        AgeChoice(selectedBand == "7_12", tr(language, "7–12 сола", "7–12 лет", "Ages 7–12"),
            tr(language, "Бозӣ дар телефон мемонад. Барои корҳои берунӣ аз калонсол иҷозат пурс.",
                "Данные остаются на телефоне. Для занятий на улице спроси разрешение взрослого.",
                "Game data stays on this device. Ask an adult before outdoor activities.")) { selectedBand = "7_12" }
        AgeChoice(selectedBand == "13_17", tr(language, "13–17 сола", "13–17 лет", "Ages 13–17"),
            tr(language, "Режими маҳаллӣ: ҳисоб ва пешрафт танҳо дар ҳамин телефон нигоҳ дошта мешавад.",
                "Локальный режим: прогресс хранится только на этом устройстве.",
                "Local mode: progress stays on this device.")) { selectedBand = "13_17" }
        AgeChoice(selectedBand == "18_plus", tr(language, "18+ сола", "18+ лет", "Ages 18+"),
            tr(language, "Метавонӣ феҳристи оммавии миссияҳоро аз сервер нав кунӣ. Профил ва холҳо фиристода намешаванд.",
                "Можно обновлять публичный список заданий с сервера. Профиль и очки не отправляются.",
                "You can refresh the public mission list. Your profile and score are not uploaded.")) { selectedBand = "18_plus" }
        Spacer(Modifier.height(15.dp))
        Button(onClick = { onContinue(selectedBand) },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text(tr(language, "Оғоз кардан", "Начать", "Start"), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            tr(language,
                "Версияи аввал: бе реклама, чат, геолокатсия ва боркунии акс. Барои кӯдакон маълумот ба сервер намеравад.",
                "Первая версия: без рекламы, чата, геолокации и загрузки фото. Данные детей не отправляются на сервер.",
                "Version 1: no ads, chat, location tracking, or photo uploads. Children's data is not sent to a server."),
            color = Muted, fontSize = 10.sp, lineHeight = 15.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AgeChoice(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFFFF8E8) else Color.White),
        border = BorderStroke(1.dp, if (selected) Gold else BorderTone)
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Navy, fontWeight = FontWeight.Bold)
                Text(description, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun BrandHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(RoundedCornerShape(13.dp)).background(Navy), contentAlignment = Alignment.Center) {
                Text("Ш", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("ШАҲРВАНД", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text("✦", color = Gold, fontSize = 25.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFF0E7D8)),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(19) {
                Box(Modifier.size(if (it % 4 == 0) 7.dp else 5.dp).clip(CircleShape)
                    .background(if (it % 2 == 0) Gold else Green))
            }
        }
        Text(title, color = NavyLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun HomeScreen(
    language: String,
    nickname: String,
    ageBand: String,
    totalXp: Int,
    totalCompleted: Int,
    completedToday: List<String>,
    missions: List<Mission>,
    catalogStatus: String,
    isLoading: Boolean,
    onOpenMissions: () -> Unit,
    onComplete: (Mission) -> Unit,
    onSettings: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                BrandHeader(tr(language, "Ҳар қадам — як пешрафт", "Каждый шаг — прогресс", "Every step is progress"),
                    tr(language, "БОЗИИ МАҲОРАТ ВА НЕКӢ", "ИГРА НАВЫКОВ И ДОБРА", "A GAME OF SKILLS AND KINDNESS"))
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = tr(language, "Танзимот", "Настройки", "Settings"), tint = Navy)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(tr(language, "Салом", "Привет", "Hello") + ", " +
            nickname.ifBlank { tr(language, "Шаҳрванд", "Гражданин", "Citizen") } + "!",
            color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(ageBandLabel(language, ageBand), color = Muted, fontSize = 10.sp)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Text(tr(language, "ПЕШРАФТИ ТУ", "ТВОЙ ПРОГРЕСС", "YOUR PROGRESS"), color = Color(0xFFDCE6ED), fontSize = 10.sp)
                Text(totalXp.toString() + " XP", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text(tr(language, "Сатҳ", "Уровень", "Level") + " " + ProgressRules.levelFor(totalXp),
                    color = Color(0xFFE7D4A8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(
                    progress = { ProgressRules.levelProgress(totalXp) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Gold, trackColor = NavyLight
                )
                Text(tr(language, "То сатҳи дигар", "До следующего уровня", "Progress to next level") +
                    ": " + ProgressRules.xpToNextLevel(totalXp) + " XP",
                    color = Color(0xFFDCE6ED), fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(tr(language, "Имрӯз", "Сегодня", "Today"), completedToday.size.toString() + "/" + DAILY_TARGET.toString(), Modifier.weight(1f))
            SummaryCard(tr(language, "Ҳамаи миссияҳо", "Всего заданий", "Completed"), totalCompleted.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Text(tr(language, "Қадами навбатӣ", "Следующий шаг", "Your next step"),
            color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "Се кори хурд — пешрафти ҳаррӯза", "Три небольших дела — ежедневный прогресс", "Three small actions make a daily goal"),
            color = Muted, fontSize = 11.sp)
        missions.take(3).forEach { mission ->
            MissionCard(language, mission, mission.id in completedToday, onComplete)
        }
        OutlinedButton(onClick = onOpenMissions, modifier = Modifier.fillMaxWidth()) {
            Text(tr(language, "Ҳамаи миссияҳо", "Все задания", "All missions"))
        }
        if (catalogStatus.isNotBlank()) {
            Text(if (isLoading) tr(language, "Навсозӣ…", "Обновление…", "Refreshing…") else catalogStatus,
                color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Text(label, color = Muted, fontSize = 10.sp)
            Text(value, color = Navy, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun MissionsScreen(
    language: String,
    missions: List<Mission>,
    completedToday: List<String>,
    onComplete: (Mission) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 15.dp)) {
        Spacer(Modifier.height(15.dp))
        BrandHeader(tr(language, "Миссияҳо", "Задания", "Missions"),
            tr(language, "Қадамҳои хурд, натиҷаи воқеӣ", "Маленькие шаги, реальный результат", "Small steps, real progress"))
        Text(tr(language, "Ҳар миссияро имрӯз як бор анҷом дода метавонӣ.",
            "Каждое задание можно выполнить один раз в день.",
            "Each mission can be completed once per day."),
            color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 14.dp)) {
            items(missions, key = { it.id }) { mission ->
                MissionCard(language, mission, mission.id in completedToday, onComplete)
            }
        }
    }
}

@Composable
private fun MissionCard(language: String, mission: Mission, completed: Boolean, onComplete: (Mission) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, if (completed) Green.copy(alpha = .35f) else BorderTone)
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2EFE7)),
                    contentAlignment = Alignment.Center) { Text(mission.icon, fontSize = 23.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(mission.title(language), color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(categoryTitle(language, mission.category), color = Gold, fontSize = 10.sp)
                }
                Text("+" + mission.xp + " XP", color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            Text(mission.description(language), color = Muted, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            Button(
                onClick = { onComplete(mission) }, enabled = !completed,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (completed) Green else Navy,
                    disabledContainerColor = Color(0xFFE7EFEA),
                    disabledContentColor = Green
                ),
                shape = RoundedCornerShape(11.dp)
            ) {
                Text(if (completed) tr(language, "Иҷро шуд ✓", "Выполнено ✓", "Completed ✓")
                    else tr(language, "Иҷро кардам", "Выполнено", "I completed it"))
            }
        }
    }
}

@Composable
private fun ProgressScreen(language: String, xp: Int, completed: Int, doneToday: Int) {
    val badges = listOf(
        Triple(1, "🌟", tr(language, "Қадами аввал", "Первый шаг", "First step")),
        Triple(5, "🤝", tr(language, "Ёрдамчӣ", "Помощник", "Helper")),
        Triple(20, "📚", tr(language, "Ҷӯяндаи дониш", "Искатель знаний", "Knowledge seeker")),
        Triple(50, "🏅", tr(language, "Шаҳрванди фаъол", "Активный гражданин", "Active citizen"))
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        BrandHeader(tr(language, "Пешрафти ман", "Мой прогресс", "My progress"),
            tr(language, "Натиҷаи шахсӣ — бе мусобиқаи сунъӣ", "Личный результат — без искусственного соревнования", "Personal progress, without fake competition"))
        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(tr(language, "ҲОЛҲОИ ҶАМЪШУДА", "НАКОПЛЕННЫЕ ОЧКИ", "TOTAL POINTS"), color = Color(0xFFDCE6ED), fontSize = 10.sp)
                Text("$xp XP", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
                Text(tr(language, "Сатҳ", "Уровень", "Level") + " " + ProgressRules.levelFor(xp),
                    color = Color(0xFFE7D4A8), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        SummaryCard(tr(language, "Миссияҳои иҷрошуда", "Выполнено заданий", "Missions completed"), completed.toString(), Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SummaryCard(tr(language, "Имрӯз", "Сегодня", "Today"), "$doneToday/$DAILY_TARGET", Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text(tr(language, "Нишонҳо", "Достижения", "Achievements"), color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        badges.forEach { (threshold, icon, title) ->
            val unlocked = completed >= threshold
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = if (unlocked) Color(0xFFFFF8E8) else Color.White),
                border = BorderStroke(1.dp, if (unlocked) Gold.copy(alpha = .5f) else BorderTone),
                shape = RoundedCornerShape(13.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (unlocked) icon else "🔒", fontSize = 25.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(tr(language, "Пас аз $threshold миссия", "После $threshold заданий", "After $threshold missions"),
                            color = Muted, fontSize = 10.sp)
                    }
                    if (unlocked) Text("✓", color = Green, fontWeight = FontWeight.Black)
                }
            }
        }
        Text(tr(language,
            "Холҳо танҳо нишондиҳандаи пешрафти шахсӣ мебошанд. Онҳо мукофоти пулӣ ё тасдиқи фаъолияти расмӣ нестанд.",
            "Очки показывают только личный прогресс. Это не денежное вознаграждение и не официальное подтверждение действий.",
            "Points only represent personal progress. They are not cash rewards or official verification."),
            color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun FutureScreen(language: String, ageBand: String, catalogStatus: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        BrandHeader(tr(language, "Мақсади бузург", "Большая цель", "The bigger vision"),
            tr(language, "Роҳи рушди ШАҲРВАНД", "Путь развития SHAHRVAND", "The SHAHRVAND roadmap"))
        Spacer(Modifier.height(12.dp))
        Text(tr(language,
            "Шаҳрванд бояд ба платформае табдил ёбад, ки одатҳои хуб, дониш, масъулият ва саҳм ба ҷомеаро ба пешрафти равшан табдил медиҳад.",
            "SHAHRVAND должен стать платформой, которая превращает полезные привычки, знания, ответственность и вклад в общество в понятный прогресс.",
            "SHAHRVAND aims to turn helpful habits, learning, responsibility, and community contribution into visible personal progress."),
            color = Ink, fontSize = 14.sp, lineHeight = 21.sp)
        RoadmapItem(language, "1", tr(language, "Версияи аввал — ҳозир", "Первая версия — сейчас", "Version 1 — now"),
            tr(language, "Миссияҳои бехатар, пешрафти маҳаллӣ, нишонҳо ва се забон.",
                "Безопасные задания, локальный прогресс, достижения и три языка.",
                "Safe missions, local progress, achievements, and three languages."), true)
        RoadmapItem(language, "2", tr(language, "Тасдиқи шаффоф", "Прозрачная проверка", "Transparent verification"),
            tr(language, "Системаи воқеии баррасии далелҳо ва қоидаҳои равшани тасдиқ.",
                "Реальная система проверки доказательств и понятные правила подтверждения.",
                "A real evidence-review system with clear verification rules."), false)
        RoadmapItem(language, "3", tr(language, "Лоиҳаҳои маҳаллӣ", "Местные проекты", "Local community projects"),
            tr(language, "Ҳамкорӣ бо мактабҳо, оилаҳо ва ташкилотҳо бо иҷозат ва назорати зарурӣ.",
                "Сотрудничество со школами, семьями и организациями с необходимыми разрешениями и контролем.",
                "Work with schools, families, and organisations with appropriate permission and oversight."), false)
        RoadmapItem(language, "4", tr(language, "Рейтинги боэътимод", "Надёжный рейтинг", "A trustworthy leaderboard"),
            tr(language, "Рейтинг танҳо баъди санҷиши серверӣ, зиддиқаллобӣ ва сиёсати ҳифзи маълумот.",
                "Рейтинг появится только после серверной проверки, защиты от накрутки и политики конфиденциальности.",
                "A leaderboard only after server-side verification, anti-abuse controls, and privacy safeguards."), false)
        RoadmapItem(language, "5", tr(language, "Ҳамкории миллӣ", "Национальное участие", "National participation"),
            tr(language, "Нишон додани саҳми воқеии шаҳрвандон бо ҳисоботи ҷамъбастӣ ва ченшаванда.",
                "Показывать реальный вклад граждан с помощью измеримых сводных отчётов.",
                "Show measurable, aggregated reports of real civic contribution."), false)
        Spacer(Modifier.height(12.dp))
        Text(tr(language, "Ҳоло фаъол", "Сейчас работает", "Active now"), color = Navy, fontWeight = FontWeight.Bold)
        Text(
            if (ageBand == "18_plus") catalogStatus else tr(language,
                "Режими маҳаллӣ. Барои синни то 18 маълумот ба сервер фиристода намешавад.",
                "Локальный режим. Данные пользователей младше 18 лет не отправляются на сервер.",
                "Local mode. Data from users under 18 is not sent to a server."),
            color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(13.dp)) {
                Text(tr(language, "Қоидаҳои версияи 1", "Правила версии 1", "Version 1 principles"),
                    color = Navy, fontWeight = FontWeight.Bold)
                PrivacyLine(tr(language, "Маълумоти профил дар телефон мемонад.",
                    "Данные профиля остаются на устройстве.", "Profile data stays on device."))
                PrivacyLine(tr(language, "Барои кӯдакон ва наврасон пайвасти шабакавӣ истифода намешавад.",
                    "Для детей и подростков сетевое подключение не используется.", "Children and teens do not use network features."))
                PrivacyLine(tr(language, "GPS, микрофон, камера, чат ва реклама нест.",
                    "Нет GPS, микрофона, камеры, чата и рекламы.", "No GPS, microphone, camera, chat, or ads."))
                PrivacyLine(tr(language, "Холҳо мукофоти пулӣ ё тасдиқи расмӣ нестанд.",
                    "Очки — не деньги и не официальное подтверждение.", "Points are not money or official verification."))
            }
        }
    }
}

@Composable
private fun RoadmapItem(language: String, number: String, title: String, description: String, active: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(if (active) Green else Color(0xFFE7E2D8)),
            contentAlignment = Alignment.Center) {
            Text(number, color = if (active) Color.White else Muted, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(if (active) tr(language, "ФАЪОЛ", "АКТИВНО", "ACTIVE")
                    else tr(language, "НАҚША", "ПЛАН", "PLANNED"),
                    color = if (active) Green else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Text(description, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun PrivacyLine(text: String) {
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
        Text("•", color = Green, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(6.dp))
        Text(text, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun SettingsDialog(
    language: String,
    nickname: String,
    ageBand: String,
    onClose: () -> Unit,
    onLanguage: (String) -> Unit,
    onSaveNickname: (String) -> Unit,
    onReset: () -> Unit
) {
    var editName by remember { mutableStateOf(nickname) }
    var confirmReset by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(tr(language, "Танзимот", "Настройки", "Settings"), color = Navy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr(language, "Номи бозӣ", "Игровое имя", "Nickname"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(value = editName, onValueChange = { editName = it.take(30) },
                    singleLine = true, label = { Text(tr(language, "Ихтиёрӣ", "Необязательно", "Optional")) })
                Text(tr(language, "Гурӯҳи синнусолӣ", "Возрастная группа", "Age group") + ": " + ageBandLabel(language, ageBand),
                    color = Muted, fontSize = 11.sp)
                Text(tr(language, "Забон", "Язык", "Language"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("tg", "ru", "en").forEach { code ->
                        FilterChip(selected = language == code, onClick = { onLanguage(code) }, label = { Text(code.uppercase()) })
                    }
                }
                TextButton(onClick = { confirmReset = true }) {
                    Text(tr(language, "Нест кардани ҳамаи маълумоти маҳаллӣ",
                        "Удалить все локальные данные", "Delete all local data"),
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveNickname(editName); onClose() }) {
                Text(tr(language, "Нигоҳ доштан", "Сохранить", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text(tr(language, "Пӯшидан", "Закрыть", "Close")) }
        }
    )
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr(language, "Ҳамаи маълумот нест шавад?", "Удалить все данные?", "Delete all data?")) },
            text = { Text(tr(language,
                "Профил, холҳо ва пешрафти ҳамин телефон пок мешаванд.",
                "Профиль, очки и прогресс на этом устройстве будут удалены.",
                "The profile, points, and progress on this device will be deleted.")) },
            confirmButton = {
                TextButton(onClick = onReset) {
                    Text(tr(language, "Нест кардан", "Удалить", "Delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(tr(language, "Бекор", "Отмена", "Cancel"))
                }
            }
        )
    }
}


@Composable
private fun AuthScreen(
    language: String,
    onLanguage: (String) -> Unit,
    onAuthenticated: (ShahrvandAccount) -> Unit
) {
    var isRegister by remember { mutableStateOf(true) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Page).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            listOf("tg", "ru", "en").forEach { code ->
                FilterChip(selected = language == code, onClick = { onLanguage(code) }, label = { Text(code.uppercase()) })
                Spacer(Modifier.width(5.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        BrandHeader(tr(language, "ҲАР ҚАДАМ — ЯК ПЕШРАФТ", "КАЖДЫЙ ШАГ — ПРОГРЕСС", "EVERY STEP IS PROGRESS"),
            tr(language, "БОЗИИ МАҲОРАТ ВА НЕКӢ", "ИГРА НАВЫКОВ И ДОБРА", "A GAME OF SKILLS AND KINDNESS"))
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text(tr(language, "Ҳисоби ШАҲРВАНД", "Аккаунт SHAHRVAND", "SHAHRVAND account"), color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(tr(language, "Барои нигоҳ доштани профил ва ворид шудан аз дастгоҳҳои дигар.", "Создай аккаунт, чтобы сохранить профиль и входить с других устройств.", "Create an account to keep your profile and sign in on other devices."), color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Page).padding(4.dp)) {
                    listOf(true, false).forEach { register ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (isRegister == register) Navy else Color.Transparent)
                            .clickable(enabled = !loading) { isRegister = register; errorText = "" }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                            Text(if (register) tr(language, "Регистрация", "Регистрация", "Sign up") else tr(language, "Вход", "Вход", "Sign in"),
                                color = if (isRegister == register) Color.White else Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                if (isRegister) {
                    OutlinedTextField(firstName, { firstName = it.take(60); errorText = "" }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr(language, "Ном", "Имя", "First name")) })
                    OutlinedTextField(lastName, { lastName = it.take(60) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr(language, "Насаб (ихтиёрӣ)", "Фамилия (необязательно)", "Last name (optional)")) })
                    OutlinedTextField(ageText, { ageText = it.filter(Char::isDigit).take(3); errorText = "" }, Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), label = { Text(tr(language, "Синну сол", "Возраст", "Age")) })
                }
                OutlinedTextField(login, { login = it.filter { ch -> ch.isLetterOrDigit() || ch in "._-" }.take(24); errorText = "" },
                    Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr(language, "Логин", "Логин", "Username")) },
                    supportingText = if (isRegister) ({ Text(tr(language, "4–24 аломат; a–z, 0–9, нуқта, _ ё -", "4–24 символа; a–z, 0–9, точка, _ или -", "4–24 characters; a–z, 0–9, dot, _ or -")) }) else null)
                OutlinedTextField(password, { password = it; errorText = "" }, Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), label = { Text(tr(language, "Парол", "Пароль", "Password")) })
                if (errorText.isNotBlank()) Text(errorText, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, lineHeight = 17.sp)
                Button(onClick = {
                    if (loading) return@Button
                    loading = true
                    errorText = ""
                    scope.launch {
                        val result = if (isRegister) {
                            val age = ageText.toIntOrNull()
                            if (age == null) Result.failure(IllegalArgumentException("Синну солро дуруст ворид кунед."))
                            else SupabaseAuthRepository.register(firstName, lastName, age, login, password)
                        } else SupabaseAuthRepository.signIn(login, password)
                        loading = false
                        result.onSuccess(onAuthenticated).onFailure { errorText = it.message ?: tr(language, "Хатогӣ рух дод.", "Произошла ошибка.", "Something went wrong.") }
                    }
                }, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(13.dp)) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(if (isRegister) tr(language, "Сохтани ҳисоб", "Создать аккаунт", "Create account") else tr(language, "Ворид шудан", "Войти", "Sign in"), fontWeight = FontWeight.Bold)
                }
                Text(tr(language, "Маълумоти профил дар Supabase нигоҳ дошта мешавад. Парол танҳо тавассути Supabase Auth ҳифз мешавад.", "Данные профиля хранятся в Supabase. Пароль защищён Supabase Auth.", "Your profile is stored in Supabase. Passwords are handled by Supabase Auth."), color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("SHAHRVAND • " + tr(language, "Ҳисоби ягонаи корбар", "Единый аккаунт пользователя", "One user account"), color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
