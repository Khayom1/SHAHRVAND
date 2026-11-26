package tj.shahrvand.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private val Navy = Color(0xFF183247)
private val NavyLight = Color(0xFF315A72)
private val Gold = Color(0xFFC8954A)
private val Green = Color(0xFF2F7969)
private val Page = Color(0xFFF1F6FA)
private val Ink = Color(0xFF1D2C38)
private val Muted = Color(0xFF687987)
private val BorderTone = Color(0xFFE0E7EC)
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
    var accessToken by remember { mutableStateOf(prefs.getString("access_token", "") ?: "") }
    var refreshToken by remember { mutableStateOf(prefs.getString("refresh_token", "") ?: "") }
    var expiresAt by remember { mutableLongStateOf(prefs.getLong("session_expires_at", 0L)) }
    var isAuthenticated by remember {
        mutableStateOf(prefs.getBoolean("auth_signed_in", false) && accessToken.isNotBlank() && refreshToken.isNotBlank())
    }
    var nickname by remember { mutableStateOf(prefs.getString("nickname", "") ?: "") }
    var accountFirstName by remember { mutableStateOf(prefs.getString("first_name", "") ?: "") }
    var accountLastName by remember { mutableStateOf(prefs.getString("last_name", "") ?: "") }
    var accountAge by remember { mutableIntStateOf(prefs.getInt("account_age", 18)) }
    var accountLogin by remember { mutableStateOf(prefs.getString("login", "") ?: "") }
    var fontScale by remember { mutableFloatStateOf(prefs.getFloat("font_scale", 1.0f).coerceIn(0.90f, 1.15f)) }
    var fontFamilyChoice by remember { mutableStateOf(prefs.getString("font_family", "system") ?: "system") }
    var financeEntries by remember { mutableStateOf(emptyList<MoneyEntry>()) }
    var tab by remember { mutableIntStateOf(0) }
    var totalXp by remember { mutableIntStateOf(prefs.getInt("total_xp", 0)) }
    var totalCompleted by remember { mutableIntStateOf(0) }
    var achievementSummary by remember { mutableStateOf<AchievementSummary?>(null) }
    var achievementSummaryLoading by remember { mutableStateOf(true) }
    var missions by remember { mutableStateOf(builtInMissions) }
    var catalogStatus by remember { mutableStateOf("") }
    var isLoadingCatalog by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var homeDestination by remember { mutableStateOf("dashboard") }
    var personalSection by remember { mutableStateOf("notes") }

    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, fontScale) {
        Density(baseDensity.density, baseDensity.fontScale * fontScale)
    }
    val appFontFamily = when (fontFamilyChoice) {
        "sans" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        else -> FontFamily.Default
    }

    val today = remember { ProgressRules.competitionDate().toString() }
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

    suspend fun getValidAccessToken(): String {
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            throw IllegalStateException("Сессияи корбар ёфт нашуд. Дубора ворид шавед.")
        }
        if (expiresAt <= System.currentTimeMillis() / 1000L + 120L) {
            val tokens = SupabaseAuthRepository.refreshSession(refreshToken).getOrThrow()
            accessToken = tokens.accessToken
            refreshToken = tokens.refreshToken
            expiresAt = tokens.expiresAt
            prefs.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putLong("session_expires_at", expiresAt)
                .apply()
        }
        return accessToken
    }

    LaunchedEffect(isAuthenticated, tab) {
        if (isAuthenticated && tab != 3) {
            val token = try {
                getValidAccessToken()
            } catch (_: Exception) {
                prefs.edit().putBoolean("auth_signed_in", false)
                    .remove("access_token").remove("refresh_token").remove("session_expires_at").apply()
                accessToken = ""
                refreshToken = ""
                expiresAt = 0L
                isAuthenticated = false
                return@LaunchedEffect
            }

            achievementSummaryLoading = true
            val serverSummary = runCatching {
                SupabaseCompetitionRepository.myAchievements(token)
            }.getOrNull()
            achievementSummary = serverSummary
            achievementSummaryLoading = false
            if (serverSummary != null) {
                totalXp = serverSummary.totalXp
                totalCompleted = serverSummary.verifiedTasks
                prefs.edit()
                    .putInt("total_xp", serverSummary.totalXp)
                    .putInt("total_completed", serverSummary.verifiedTasks)
                    .apply()
            }

            if (tab == 0) {
                runCatching { PersonalSpaceRepository.fetchMoneyEntries(token) }
                    .onSuccess { financeEntries = it }
            }
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
                "Профил дар Supabase нигоҳ дошта мешавад; пешрафт дар ҳамин телефон мемонад.",
                "Профиль хранится в Supabase; игровой прогресс остаётся на этом устройстве.",
                "Your profile is stored in Supabase; game progress stays on this device.")
        }
    }

    fun changeLanguage(value: String) {
        language = value
        prefs.edit().putString("language", value).apply()
    }

    fun finishMission(mission: Mission) {
        if (!ProgressRules.canCompleteToday(mission.id, completedToday.toSet())) return
        completedToday.add(mission.id)
        // Local checklist state is separate from server-verified XP and task totals.
        prefs.edit()
            .putStringSet("completed_today", completedToday.toSet())
            .putString("progress_day", today)
            .apply()
    }

    if (!isAuthenticated) {
        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = appFontFamily)
        ) {
        AuthScreen(language = language, onLanguage = ::changeLanguage) { account ->
            isAuthenticated = true
            ageBand = account.ageBand
            nickname = account.login
            accountFirstName = account.firstName
            accountLastName = account.lastName.orEmpty()
            accountAge = account.age
            accountLogin = account.login
            accessToken = account.accessToken
            refreshToken = account.refreshToken
            expiresAt = account.expiresAt
            prefs.edit()
                .putBoolean("auth_signed_in", true)
                .putString("access_token", account.accessToken)
                .putString("refresh_token", account.refreshToken)
                .putLong("session_expires_at", account.expiresAt)
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
        }
        return
    }
    if (ageBand == null) ageBand = prefs.getString("age_band", "18_plus") ?: "18_plus"

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = appFontFamily)
    ) {
    Scaffold(
        containerColor = Page,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.78f),
                contentColor = Ink,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.95f)),
                shadowElevation = 13.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val labels = listOf(
                        tr(language, "Асосӣ", "Главная", "Home"),
                        tr(language, "Вазифаҳо", "Задания", "Tasks"),
                        tr(language, "Рейтинг", "Рейтинг", "Ranking"),
                        tr(language, "Профил", "Профиль", "Profile")
                    )
                    val icons = listOf(
                        Icons.Filled.Home, Icons.Filled.Explore,
                        Icons.Filled.EmojiEvents, Icons.Filled.AccountCircle
                    )
                    labels.forEachIndexed { index, label ->
                        val selected = tab == index
                        Column(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(17.dp))
                                .background(if (selected) Color(0xFFEDE5F8) else Color.Transparent)
                                .clickable {
                                    tab = index
                                    if (index != 0) homeDestination = "dashboard"
                                }
                                .padding(horizontal = 2.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                icons[index],
                                contentDescription = label,
                                tint = if (selected) Navy else Muted,
                                modifier = Modifier.size(21.dp)
                            )
                            Text(
                                label,
                                color = if (selected) Gold else Muted,
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> when (homeDestination) {
                    "personal" -> PersonalSpaceHubScreen(
                        language = language,
                        userId = prefs.getString("auth_user_id", "") ?: "",
                        initialSection = personalSection,
                        entries = financeEntries,
                        onEntriesChanged = { financeEntries = it },
                        getAccessToken = ::getValidAccessToken,
                        onBack = { homeDestination = "dashboard" }
                    )
                    "sleep" -> Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { homeDestination = "dashboard" }) {
                                Text("‹ " + tr(language, "Асосӣ", "Главная", "Home"))
                            }
                            Text(
                                tr(language, "Хоб ва барқароршавӣ", "Сон и восстановление", "Sleep and recovery"),
                                color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            SleepTrackingScreen(
                                language = language,
                                age = accountAge,
                                onReward = { date, startedAt, endedAt, sourcePackage, isManual ->
                                    val awarded = SupabaseSleepRepository.award(
                                        token = getValidAccessToken(),
                                        wakeDate = date,
                                        startedAt = startedAt,
                                        endedAt = endedAt,
                                        sourcePackage = sourcePackage,
                                        isManual = isManual
                                    )
                                    if (awarded) {
                                        val summary = SupabaseCompetitionRepository.myAchievements(getValidAccessToken())
                                        totalXp = summary.totalXp
                                        totalCompleted = summary.verifiedTasks
                                        achievementSummary = summary
                                        achievementSummaryLoading = false
                                        prefs.edit()
                                            .putInt("total_xp", summary.totalXp)
                                            .putInt("total_completed", summary.verifiedTasks)
                                            .apply()
                                    }
                                    awarded
                                }
                            )
                        }
                    }
                    else -> HomeScreen(
                        language = language,
                        nickname = nickname,
                        ageBand = ageBand ?: "7_12",
                        summary = achievementSummary,
                        summaryLoading = achievementSummaryLoading,
                        onOpenMissions = { tab = 1 },
                        onOpenLeaderboard = { tab = 2 },
                        onOpenNotes = {
                            personalSection = "notes"
                            homeDestination = "personal"
                        },
                        onOpenSleep = { homeDestination = "sleep" },
                        onOpenProfile = { tab = 3 }
                    )
                }
                1 -> DailyActivitiesScreen(language, ::getValidAccessToken)
                2 -> LeaderboardScreen(language, ageBand ?: "18_plus", ::getValidAccessToken)
                3 -> ShahrvandProfileScreen(
                    language = language,
                    firstName = accountFirstName,
                    lastName = accountLastName,
                    age = accountAge,
                    login = accountLogin,
                    totalXp = totalXp,
                    totalCompleted = totalCompleted,
                    getAccessToken = ::getValidAccessToken,
                    onServerSummary = { summary ->
                        achievementSummary = summary
                        achievementSummaryLoading = false
                        totalXp = summary.totalXp
                        totalCompleted = summary.verifiedTasks
                        prefs.edit()
                            .putInt("total_xp", summary.totalXp)
                            .putInt("total_completed", summary.verifiedTasks)
                            .apply()
                    },
                    onBack = {
                        tab = 0
                        homeDestination = "dashboard"
                    },
                    onSettings = { settingsOpen = true }
                )
                else -> HomeScreen(
                    language = language,
                    nickname = nickname,
                    ageBand = ageBand ?: "7_12",
                    summary = achievementSummary,
                    summaryLoading = achievementSummaryLoading,
                    onOpenMissions = { tab = 1 },
                    onOpenLeaderboard = { tab = 2 },
                    onOpenNotes = {
                        personalSection = "notes"
                        homeDestination = "personal"
                    },
                    onOpenSleep = { homeDestination = "sleep" },
                    onOpenProfile = { tab = 3 }
                )
            }
        }
    }

    if (settingsOpen) {
        ShahrvandSettingsDialog(
            language = language,
            firstName = accountFirstName,
            lastName = accountLastName,
            age = accountAge,
            login = accountLogin,
            getAccessToken = ::getValidAccessToken,
            fontScale = fontScale,
            fontFamilyChoice = fontFamilyChoice,
            onClose = { settingsOpen = false },
            onLanguage = ::changeLanguage,
            onFontScale = { value ->
                fontScale = value.coerceIn(0.90f, 1.15f)
                prefs.edit().putFloat("font_scale", fontScale).apply()
            },
            onFontFamily = { value ->
                fontFamilyChoice = value
                prefs.edit().putString("font_family", value).apply()
            },
            onProfileSaved = { profile ->
                accountFirstName = profile.firstName
                accountLastName = profile.lastName.orEmpty()
                accountAge = profile.age
                accountLogin = profile.login
                nickname = profile.login
                ageBand = profile.ageBand
                prefs.edit()
                    .putString("first_name", profile.firstName)
                    .putString("last_name", profile.lastName ?: "")
                    .putInt("account_age", profile.age)
                    .putString("login", profile.login)
                    .putString("nickname", profile.login)
                    .putString("age_band", profile.ageBand)
                    .apply()
            },
            onSignOut = {
                prefs.edit()
                    .putBoolean("auth_signed_in", false)
                    .remove("access_token")
                    .remove("refresh_token")
                    .remove("session_expires_at")
                    .remove("auth_user_id")
                    .remove("first_name")
                    .remove("last_name")
                    .remove("account_age")
                    .remove("login")
                    .remove("age_band")
                    .remove("nickname")
                    .putInt("total_xp", 0)
                    .putInt("total_completed", 0)
                    .putStringSet("completed_today", emptySet())
                    .apply()
                isAuthenticated = false
                accessToken = ""
                refreshToken = ""
                expiresAt = 0L
                financeEntries = emptyList()
                nickname = ""
                accountFirstName = ""
                accountLastName = ""
                accountAge = 18
                accountLogin = ""
                totalXp = 0
                totalCompleted = 0
                completedToday.clear()
                homeDestination = "dashboard"
                settingsOpen = false
                tab = 0
            },
            onClearLocalProgress = {
                prefs.edit()
                    .putInt("total_xp", 0)
                    .putInt("total_completed", 0)
                    .putStringSet("completed_today", emptySet())
                    .putString("progress_day", today)
                    .apply()
                totalXp = 0
                totalCompleted = 0
                completedToday.clear()
            }
        )
    }
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
            tr(language, "ПЛАТФОРМАИ РУШДИ ШАҲРВАНД", "ПЛАТФОРМА РАЗВИТИЯ ГРАЖДАН", "CITIZEN DEVELOPMENT PLATFORM")
        )
        Spacer(Modifier.height(18.dp))
        Text(tr(language, "Хуш омадӣ!", "Добро пожаловать!", "Welcome!"),
            color = Navy, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text(
            tr(language, "Барои истифода сабти ном бо ном, синну сол, логин ва парол ҳатмист. Email ва телефон талаб намешавад.",
                "Для использования обязательна регистрация: имя, возраст, логин и пароль. Email и телефон не нужны.",
                "Registration is required: first name, age, username, and password. No email or phone number is needed."),
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
            tr(language, "Профил дар Supabase нигоҳ дошта мешавад; пешрафти бозӣ дар телефон мемонад. Барои корҳои берунӣ аз калонсол иҷозат пурс.",
                "Профиль хранится в Supabase, игровой прогресс — на телефоне. Для занятий на улице спроси разрешение взрослого.",
                "Your profile is stored in Supabase; game progress stays on this device. Ask an adult before outdoor activities.")) { selectedBand = "7_12" }
        AgeChoice(selectedBand == "13_17", tr(language, "13–17 сола", "13–17 лет", "Ages 13–17"),
            tr(language, "Профил дар Supabase, пешрафти бозӣ дар ҳамин телефон нигоҳ дошта мешавад.",
                "Профиль хранится в Supabase, игровой прогресс — на этом устройстве.",
                "Your profile is stored in Supabase; game progress stays on this device.")) { selectedBand = "13_17" }
        AgeChoice(selectedBand == "18_plus", tr(language, "18+ сола", "18+ лет", "Ages 18+"),
            tr(language, "Феҳристи миссияҳо аз сервер нав мешавад; профили регистрация дар Supabase нигоҳ дошта мешавад, холҳо ҳоло дар телефон мемонанд.",
                "Список заданий обновляется с сервера; регистрационный профиль хранится в Supabase, очки пока остаются на устройстве.",
                "The public mission list is refreshed from the server; your registration profile is stored in Supabase, while scores remain on this device for now.")) { selectedBand = "18_plus" }
        Spacer(Modifier.height(15.dp))
        Button(onClick = { onContinue(selectedBand) },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text(tr(language, "Оғоз кардан", "Начать", "Start"), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            tr(language,
                "Версияи аввал: бе реклама, чат, геолокатсия ва боркунии акс. Маълумоти ҳисоб дар Supabase, пешрафти бозӣ дар телефон нигоҳ дошта мешавад.",
                "Первая версия: без рекламы, чата, геолокации и загрузки фото. Данные аккаунта хранятся в Supabase, игровой прогресс — на устройстве.",
                "Version 1: no ads, chat, location tracking, or photo uploads. Account data is stored in Supabase; game progress stays on the device."),
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
    summary: AchievementSummary?,
    summaryLoading: Boolean,
    onOpenMissions: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val xp = summary?.totalXp ?: 0
    val tasks = summary?.verifiedTasks ?: 0
    val badges = summary?.unlockedCodes.orEmpty()
    val nextTarget = summary?.nextThreshold ?: 10
    val progress = summary?.progress ?: 0f
    val animatedXp by animateIntAsState(xp, tween(850), label = "home_xp_count")
    val animatedTasks by animateIntAsState(tasks, tween(850), label = "home_task_count")
    val animatedProgress by animateFloatAsState(progress, tween(1000), label = "home_title_progress")
    val unlockedBadgeCount = badges.size

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "SHAHRVAND",
                    color = Navy,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    tr(language, "Салом, ", "Привет, ", "Hello, ") +
                        nickname.ifBlank { tr(language, "Шаҳрванд", "Гражданин", "Citizen") },
                    color = Ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(ageBandLabel(language, ageBand), color = Muted, fontSize = 10.sp)
            }
            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier.size(43.dp).background(Color.White, CircleShape)
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Navy)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = Navy),
            shape = RoundedCornerShape(23.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            tr(language, "ҲОЛАТИ ҲОЗИРАИ ТУ", "ТВОЙ ТЕКУЩИЙ СТАТУС", "YOUR CURRENT STATUS"),
                            color = Color(0xFFE6C991),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            tr(language, "Пешрафти шахсӣ", "Личный прогресс", "Personal progress"),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) { Text("✦", fontSize = 24.sp, color = Color(0xFFE6C991)) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    HomeMetricCard(
                        Modifier.weight(1f),
                        tr(language, "ХОЛҲО", "ОЧКИ", "XP"),
                        if (summaryLoading) "…" else if (summary == null) "—" else animatedXp.toString(),
                        "⚡"
                    )
                    HomeMetricCard(
                        Modifier.weight(1f),
                        tr(language, "ВАЗИФАҲО", "ЗАДАНИЯ", "TASKS"),
                        if (summaryLoading) "…" else if (summary == null) "—" else animatedTasks.toString(),
                        "✓"
                    )
                    HomeMetricCard(
                        Modifier.weight(1f),
                        tr(language, "НИШОНҲО", "ЗНАЧКИ", "BADGES"),
                        if (summaryLoading) "…" else if (summary == null) "—" else unlockedBadgeCount.toString(),
                        "🏅"
                    )
                }

                if (summaryLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                        color = Color(0xFFE6C991),
                        trackColor = Color.White.copy(alpha = 0.16f)
                    )
                } else if (summary == null) {
                    Text(
                        tr(language, "Маълумот аз сервер дастрас нест. Барои навсозӣ ба профил даро.",
                            "Не удалось получить данные сервера. Открой профиль для обновления.",
                            "Server data is unavailable. Open Profile to retry."),
                        color = Color(0xFFDCE6ED),
                        fontSize = 10.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tr(language, "То унвони навбатӣ", "До следующего титула", "Next title"),
                                color = Color(0xFFDCE6ED),
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "$tasks/$nextTarget",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = Color(0xFFE6C991),
                            trackColor = Color.White.copy(alpha = 0.16f)
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr(language, "Нишонҳои кушодашуда", "Открытые значки", "Unlocked badges"),
                color = Navy,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenProfile, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text(tr(language, "Ҳамааш", "Все", "View all"), color = Gold, fontSize = 11.sp)
            }
        }

        if (summaryLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(17.dp)
            ) {
                Box(Modifier.fillMaxWidth().height(62.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Gold,
                        strokeWidth = 2.dp
                    )
                }
            }
        } else if (summary != null && badges.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                badges.take(3).forEachIndexed { index, code ->
                    val (symbol, title) = homeBadgeDetails(language, code)
                    AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.weight(1f),
                        enter = fadeIn(tween(350, delayMillis = index * 110)) +
                            scaleIn(tween(450, delayMillis = index * 110), initialScale = 0.55f) +
                            slideInVertically(tween(450, delayMillis = index * 110)) { it / 3 }
                    ) {
                        HomeBadgeCard(symbol, title)
                    }
                }
                repeat((3 - badges.take(3).size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        } else if (summary != null) {
            Text(
                tr(language, "Аввалин нишони худро бо иҷрои вазифа гир.", "Получи первый значок за выполнение задания.",
                    "Complete your first task to earn a badge."),
                color = Muted,
                fontSize = 11.sp
            )
        }

        Text(
            tr(language, "ҚИСМҲОИ АСОСӢ", "ОСНОВНЫЕ РАЗДЕЛЫ", "QUICK ACCESS"),
            color = Muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeActionTile(
                Modifier.weight(1f),
                "☑",
                tr(language, "Вазифаҳо", "Задания", "Tasks"),
                onOpenMissions
            )
            HomeActionTile(
                Modifier.weight(1f),
                "🏆",
                tr(language, "Рейтинг", "Рейтинг", "Ranking"),
                onOpenLeaderboard
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeActionTile(
                Modifier.weight(1f),
                "🗒",
                tr(language, "Ёддоштҳо", "Заметки", "Notes"),
                onOpenNotes
            )
            HomeActionTile(
                Modifier.weight(1f),
                "🌙",
                tr(language, "Хоб", "Сон", "Sleep"),
                onOpenSleep
            )
        }

        Text(
            tr(language, "Холҳо ва унвонҳо танҳо аз натиҷаҳои тасдиқшудаи сервер ҳисоб мешаванд.",
                "Очки и титулы рассчитываются только по подтверждённым сервером результатам.",
                "XP and titles are calculated only from server-verified results."),
            color = Muted,
            fontSize = 9.sp,
            lineHeight = 13.sp,
            modifier = Modifier.padding(bottom = 5.dp)
        )
    }
}

@Composable
private fun HomeMetricCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: String
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 15.sp)
            Text(
                value,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                label,
                color = Color(0xFFDCE6ED),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HomeBadgeCard(icon: String, title: String) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE7EAF0)),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 11.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 23.sp)
            Text(title, color = Navy, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HomeActionTile(
    modifier: Modifier,
    icon: String,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderTone),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(icon, fontSize = 19.sp)
            Text(title, color = Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun homeBadgeDetails(language: String, code: String): Pair<String, String> = when (code) {
    "app_entry" -> "🌟" to tr(language, "Воридшавӣ", "Вход", "Joined")
    "first_task" -> "⚡" to tr(language, "Вазифаи аввал", "Первое задание", "First task")
    "steady_10" -> "🛡️" to tr(language, "Устувор", "Постоянный", "Steady")
    "dedicated_25" -> "💠" to tr(language, "Садоқатманд", "Преданный", "Dedicated")
    "expert_50" -> "🏆" to tr(language, "Устод", "Эксперт", "Expert")
    "legend_100" -> "👑" to tr(language, "Афсона", "Легенда", "Legend")
    else -> "🏅" to tr(language, "Нишон", "Значок", "Badge")
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
        colors = CardDefaults.cardColors(containerColor = if (completed) Color(0xFFEAF5EF).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.84f)),
        shape = RoundedCornerShape(21.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
        border = BorderStroke(1.dp, if (completed) Green.copy(alpha = .42f) else Color.White.copy(alpha = 0.96f))
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
                Text(tr(language, "Бе хол", "Без очков", "No XP"), color = Muted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
        Card(
            colors = CardDefaults.cardColors(containerColor = Navy),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 9.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
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
                PrivacyLine(tr(language, "Маълумоти профил дар Supabase, пешрафт дар телефон нигоҳ дошта мешавад.",
                    "Данные профиля хранятся в Supabase, игровой прогресс — на устройстве.", "Profile data is stored in Supabase; game progress stays on device."))
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
                "Сессияи воридшавӣ ва пешрафти маҳаллӣ пок мешаванд. Ҳисоби серверӣ боқӣ мемонад.",
                "Состояние входа и локальный прогресс будут удалены. Серверный аккаунт сохранится.",
                "Your sign-in state and local progress will be cleared. Your server account will remain.")) },
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
            tr(language, "ПЛАТФОРМАИ РУШДИ ШАҲРВАНД", "ПЛАТФОРМА РАЗВИТИЯ ГРАЖДАН", "CITIZEN DEVELOPMENT PLATFORM"))
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
                OutlinedTextField(login, { login = it.filter { ch -> ch in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-" }.take(24); errorText = "" },
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
                TextButton(
                    onClick = { isRegister = !isRegister; errorText = "" },
                    enabled = !loading
                ) {
                    Text(
                        if (isRegister) tr(language,
                            "Ҳисоб доред? Ворид шавед",
                            "Уже есть аккаунт? Войти",
                            "Already have an account? Sign in")
                        else tr(language,
                            "Ҳисоб надоред? Сабти ном кунед",
                            "Нет аккаунта? Зарегистрироваться",
                            "Don’t have an account? Sign up"),
                        color = Navy,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(tr(language, "Маълумоти профил дар Supabase нигоҳ дошта мешавад. Парол танҳо тавассути Supabase Auth ҳифз мешавад.", "Данные профиля хранятся в Supabase. Пароль защищён Supabase Auth.", "Your profile is stored in Supabase. Passwords are handled by Supabase Auth."), color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("SHAHRVAND • " + tr(language, "Ҳисоби ягонаи корбар", "Единый аккаунт пользователя", "One user account"), color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
