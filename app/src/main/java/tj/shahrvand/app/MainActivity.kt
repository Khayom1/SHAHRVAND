package tj.shahrvand.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val Navy = Color(0xFF20384B)
private val NavyLight = Color(0xFF355D70)
private val Gold = Color(0xFFB98A50)
private val Green = Color(0xFF36766D)
private val Turquoise = Color(0xFF4C8A86)
private val Copper = Color(0xFFAE6B53)
private val Page = Color(0xFFF4EFE5)
private val Ink = Color(0xFF263640)
private val Muted = Color(0xFF777F7E)
private val BorderTone = Color(0xFFE4D9C8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Navy, secondary = Gold, tertiary = Green,
                background = Page, surface = Color.White, onSurface = Ink, onBackground = Ink
            )) { ShahrvandApp() }
        }
    }
}

private fun tx(lang: String, tg: String, ru: String, en: String): String =
    when (lang) { "ru" -> ru; "en" -> en; else -> tg }

private data class Mission(
    val id: String,
    val tg: String,
    val ru: String,
    val en: String,
    val categoryTg: String,
    val categoryRu: String,
    val categoryEn: String,
    val descTg: String,
    val descRu: String,
    val descEn: String,
    val icon: String,
    val normalXp: Int,
    val testXp: Int,
    val gpsMode: String? = null
) {
    fun title(lang: String) = tx(lang, tg, ru, en)
    fun category(lang: String) = tx(lang, categoryTg, categoryRu, categoryEn)
    fun description(lang: String) = tx(lang, descTg, descRu, descEn)
}

private val missionList = listOf(
    Mission("run", "2 км давидан", "Бег 2 км", "Run 2 km", "Варзиш", "Спорт", "Fitness",
        "2 километр давед. GPS суръати ҳаракатро таҳлил карда, қисми давиданро аз роҳгардӣ ҷудо мекунад.",
        "Пробегите 2 км. GPS анализирует скорость и отделяет бег от ходьбы.",
        "Run 2 km. GPS speed analysis separates running from walking.",
        "🏃", 40, 8, LocationTrackingService.MODE_RUN),
    Mission("walk", "5 км роҳгардӣ дар як рӯз", "Ходьба 5 км за день", "Walk 5 km in one day", "Саломатӣ", "Здоровье", "Wellness",
        "Дар давоми рӯз ҷамъан 5 км роҳ гардед. Метавонед трекингро чанд бор оғозу қатъ кунед; масофа ҷамъ мешавад.",
        "Пройдите суммарно 5 км за день. Трекинг можно запускать несколько раз — дистанция суммируется.",
        "Walk a total of 5 km during the day. Start and stop tracking multiple times; distance accumulates.",
        "👟", 35, 8, LocationTrackingService.MODE_WALK),
    Mission("clean", "Тоза кардани кӯча", "Уборка улицы", "Street cleanup", "Ҷомеа", "Сообщество", "Community",
        "Қисми бехатари кӯча ё маҳалли ҷамъиятиро тоза кунед. Акс ё видео ҳамчун далел илова кунед.",
        "Уберите безопасный участок улицы или общественную территорию. Добавьте фото или видео.",
        "Clean a safe section of a street or public area. Add a photo or video as evidence.",
        "🧹", 80, 6),
    Mission("tree", "Шинондани дарахт", "Посадка дерева", "Plant a tree", "Экология", "Экология", "Environment",
        "Дарахтро дар ҷойи мувофиқ ва бо иҷозати зарурӣ шинонед. Акс ё видео илова кунед.",
        "Посадите дерево в подходящем месте с разрешением. Добавьте фото или видео.",
        "Plant a tree in a suitable place with permission. Add a photo or video.",
        "🌱", 100, 7),
    Mission("read", "30 дақиқа китобхонӣ", "Чтение 30 минут", "Read for 30 minutes", "Дониш", "Знания", "Learning",
        "30 дақиқа мавзӯи муфидро хонед. Акс ё видеои кӯтоҳи далелро илова кунед.",
        "Читайте полезную тему 30 минут. Добавьте фото или короткое видео.",
        "Read a useful topic for 30 minutes. Add a photo or short video.",
        "📚", 30, 5)
)

private data class BadgeInfo(
    val id: String, val icon: String,
    val tg: String, val ru: String, val en: String,
    val ruleTg: String, val ruleRu: String, val ruleEn: String
) {
    fun title(lang: String) = tx(lang, tg, ru, en)
    fun rule(lang: String) = tx(lang, ruleTg, ruleRu, ruleEn)
}

private val badgeList = listOf(
    BadgeInfo("first", "🌟", "Қадами аввал", "Первый шаг", "First Step",
        "Пас аз анҷоми аввалин вазифаи тасдиқшуда дода мешавад.",
        "Выдаётся после первого подтверждённого задания.",
        "Awarded after your first verified mission."),
    BadgeInfo("runner", "🏃", "Давандаи шаҳр", "Городской бегун", "City Runner",
        "2 км давиданро анҷом диҳед. Дар режими тестӣ ҳадаф 120 м аст.",
        "Завершите бег на 2 км. В тестовом режиме цель — 120 м.",
        "Complete a 2 km run. Test mode target is 120 m."),
    BadgeInfo("walker", "👟", "Қадамҳои солим", "Здоровые шаги", "Healthy Steps",
        "Дар як рӯз 5 км роҳ гардед. Дар тест ҳадаф 200 м аст.",
        "Пройдите 5 км за день. В тесте цель — 200 м.",
        "Walk 5 km in a day. Test mode target is 200 m."),
    BadgeInfo("helper", "🤝", "Ёрдамчии ҷомеа", "Помощник сообщества", "Community Helper",
        "Вазифаи тоза кардани кӯчаро иҷро кунед.",
        "Выполните задание по уборке улицы.",
        "Complete the street-cleanup mission."),
    BadgeInfo("nature", "🌳", "Ҳомии табиат", "Защитник природы", "Nature Guardian",
        "Вазифаи шинондани дарахтро иҷро кунед.",
        "Выполните задание по посадке дерева.",
        "Complete the tree-planting mission."),
    BadgeInfo("reader", "📖", "Дӯстдори дониш", "Любитель знаний", "Knowledge Seeker",
        "Вазифаи китобхониро иҷро кунед.",
        "Выполните задание по чтению.",
        "Complete the reading mission."),
    BadgeInfo("citizen", "👑", "Шаҳрванди фаъол", "Активный гражданин", "Active Citizen",
        "Дар тест 2 вазифаи гуногун, дар режими воқеӣ 5 вазифаи тасдиқшуда анҷом диҳед.",
        "В тесте выполните 2 разных задания, в обычном режиме — 5 подтверждённых.",
        "Complete 2 different missions in test mode or 5 verified missions in production mode.")
)

private val rankingNames = listOf(
    "Фирдавс Саидов", "Меҳрона Раҳмон", "Сомон Каримов", "Далер Набиев", "Мадина Юсуфӣ",
    "Шаҳбоз Алиев", "Фарзона Ҳасанова", "Муҳаммад Назаров", "Ситора Давлатова", "Амир Ҷӯраев",
    "Зуҳро Саидова", "Беҳрӯз Шарифов", "Нозим Раҳимов", "Манижа Абдуллоева", "Фаридун Исмоилов",
    "Меҳроҷ Ҳакимов", "Шабнам Қодирова", "Рустам Сафаров", "Нигина Бобоева", "Сорбон Мирзоев",
    "Мавзуна Раҳматова", "Парвиз Саидов", "Дилноза Олимова", "Фирӯз Шарипов", "Шаҳноз Назарова",
    "Комрон Давлатов", "Сабрина Ҳусейнова", "Абдулло Раҳимов", "Малика Нурова", "Ҷамшед Каримов"
)

private fun formatDistance(m: Float): String =
    if (m >= 1000f) String.format(Locale.getDefault(), "%.2f км", m / 1000f) else m.toInt().toString() + " м"

@Composable
private fun CulturalPatternBand() {
    Row(
        Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFF0E7D8)).padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(23) { index ->
            Box(
                Modifier.size(if (index % 4 == 0) 8.dp else 6.dp)
                    .rotate(45f)
                    .background(
                        when (index % 3) {
                            0 -> Turquoise.copy(alpha = .85f)
                            1 -> Gold.copy(alpha = .85f)
                            else -> Copper.copy(alpha = .72f)
                        },
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

private fun badgeAccent(id: String): Color = when (id) {
    "first" -> Color(0xFFB98A50)
    "runner" -> Color(0xFF36766D)
    "walker" -> Color(0xFF4C8A86)
    "helper" -> Color(0xFF54758A)
    "nature" -> Color(0xFF65845B)
    "reader" -> Color(0xFF81709B)
    "citizen" -> Color(0xFFAE6B53)
    else -> Navy
}

private fun badgeTint(id: String): Color = badgeAccent(id).copy(alpha = .11f)

@Composable
private fun BrandHeader(title: String, subtitle: String? = null) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(13.dp)).background(Navy), contentAlignment = Alignment.Center) {
                Text("Ш", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Navy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                if (subtitle != null) Text(subtitle, color = Gold, fontSize = 10.sp)
            }
            Text("♛", color = Gold, fontSize = 27.sp)
        }
        Spacer(Modifier.height(8.dp))
        CulturalPatternBand()
    }
}

@Composable
private fun ShahrvandApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(LocationTrackingService.PREFS, Context.MODE_PRIVATE) }
    var lang by remember { mutableStateOf(prefs.getString("language", "tg") ?: "tg") }
    var registered by remember { mutableStateOf(prefs.getBoolean("registered", false)) }
    var firstName by remember { mutableStateOf(prefs.getString("first_name", "") ?: "") }
    var lastName by remember { mutableStateOf(prefs.getString("last_name", "") ?: "") }
    var age by remember { mutableStateOf(prefs.getString("age", "") ?: "") }
    var avatar by remember { mutableStateOf(prefs.getString("avatar", null)?.let(Uri::parse)) }
    var tab by remember { mutableIntStateOf(0) }
    var selectedMission by remember { mutableStateOf<Mission?>(null) }
    var testMode by remember { mutableStateOf(prefs.getBoolean("test_mode", true)) }
    var xp by remember { mutableIntStateOf(prefs.getInt("total_xp", 0)) }
    val completed = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("completed_v2", emptySet()) ?: emptySet()) } }
    val pending = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("pending_v2", emptySet()) ?: emptySet()) } }
    val customMissions = remember { mutableStateListOf<Mission>() }
    var award by remember { mutableStateOf<BadgeInfo?>(null) }
    var awardQueue by remember { mutableStateOf<List<BadgeInfo>>(emptyList()) }

    fun saveProgress() {
        prefs.edit().putStringSet("completed_v2", completed.toSet())
            .putStringSet("pending_v2", pending.toSet())
            .putInt("total_xp", xp).putBoolean("test_mode", testMode).apply()
    }

    fun evaluateBadges() {
        val ids = mutableListOf<String>()
        if (completed.isNotEmpty()) ids.add("first")
        if ("run" in completed) ids.add("runner")
        if ("walk" in completed) ids.add("walker")
        if ("clean" in completed) ids.add("helper")
        if ("tree" in completed) ids.add("nature")
        if ("read" in completed) ids.add("reader")
        if (completed.size >= (if (testMode) 2 else 5)) ids.add("citizen")
        val stored = prefs.getStringSet("earned_badges_v2", emptySet())?.toMutableSet() ?: mutableSetOf()
        val newIds = ids.filter { it !in stored }
        if (newIds.isNotEmpty()) {
            stored.addAll(newIds)
            prefs.edit().putStringSet("earned_badges_v2", stored).apply()
            val fresh = newIds.mapNotNull { id -> badgeList.find { it.id == id } }
            awardQueue = awardQueue + fresh
            if (award == null) award = awardQueue.firstOrNull()
        }
    }

    fun completeMission(id: String) {
        if (id in completed) return
        val savedBefore = prefs.getStringSet("completed_v2", emptySet()) ?: emptySet()
        completed.add(id)
        if (id in savedBefore) {
            xp = prefs.getInt("total_xp", xp)
        } else {
            val mission = missionList.find { it.id == id } ?: return
            xp += if (testMode) mission.testXp else mission.normalXp
        }
        saveProgress()
        evaluateBadges()
    }

    fun submitPending(id: String) {
        if (id !in pending && id !in completed) pending.add(id)
        saveProgress()
    }

    LaunchedEffect(Unit) {
        while (true) {
            val stored = prefs.getStringSet("completed_v2", emptySet()) ?: emptySet()
            val newlySynced = stored.filter { it !in completed }
            if (newlySynced.isNotEmpty()) {
                completed.addAll(newlySynced)
                xp = prefs.getInt("total_xp", xp)
                evaluateBadges()
            } else {
                val storedXp = prefs.getInt("total_xp", xp)
                if (storedXp != xp) xp = storedXp
            }
            delay(900)
        }
    }

    SideEffect {
        prefs.edit().putString("language", lang).putBoolean("registered", registered)
            .putString("first_name", firstName).putString("last_name", lastName)
            .putString("age", age).putString("avatar", avatar?.toString())
            .putBoolean("test_mode", testMode).apply()
    }

    if (!registered) {
        RegistrationScreen(lang, firstName, { firstName = it }, lastName, { lastName = it },
            age, { age = it }, avatar, { avatar = it }, { lang = it }, { registered = true })
        return
    }

    if (selectedMission != null) {
        val mission = selectedMission!!
        if (mission.gpsMode != null) {
            GpsMissionScreen(
                mission, lang, testMode, mission.id in completed,
                onBack = { selectedMission = null },
                onComplete = { completeMission(mission.id); selectedMission = null }
            )
        } else {
            ProofMissionScreen(
                mission, lang, testMode,
                mission.id in completed, mission.id in pending,
                onBack = { selectedMission = null },
                onTestComplete = { completeMission(mission.id); selectedMission = null },
                onPending = { submitPending(mission.id); selectedMission = null }
            )
        }
        if (award != null) {
            AnimatedAwardDialog(award!!, lang) {
                awardQueue = awardQueue.drop(1)
                award = awardQueue.firstOrNull()
            }
        }
        return
    }

    Scaffold(
        containerColor = Page,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 7.dp) {
                val labels = listOf(
                    tx(lang, "Асосӣ", "Главная", "Home"),
                    tx(lang, "Вазифаҳо", "Задания", "Missions"),
                    tx(lang, "Рейтинг", "Рейтинг", "Ranking"),
                    tx(lang, "Профил", "Профиль", "Profile")
                )
                val icons = listOf(Icons.Filled.Home, Icons.Filled.Assignment, Icons.Filled.EmojiEvents, Icons.Filled.Person)
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
                0 -> HomeScreen(lang, firstName, xp, completed.size, pending.size, testMode, prefs,
                    onMission = { selectedMission = it }, onAllMissions = { tab = 1 })
                1 -> MissionsScreen(lang, testMode, completed, pending, customMissions,
                    onMission = { selectedMission = it },
                    onAdd = { name, desc ->
                        customMissions.add(Mission(
                            "custom_" + System.currentTimeMillis(), name, name, name,
                            "Шахсӣ", "Личное", "Personal", desc, desc, desc, "✨", 0, 1
                        ))
                    })
                2 -> RankingScreen(lang, firstName, lastName, xp, completed.toList())
                else -> ProfileScreen(
                    lang, firstName, lastName, age, avatar, xp,
                    completed.toList(), pending.size, testMode,
                    onLanguage = { lang = it }, onTestMode = { testMode = it; saveProgress() },
                    onSave = { f, l, a -> firstName = f; lastName = l; age = a },
                    onReset = {
                        prefs.edit().clear().apply()
                        firstName = ""; lastName = ""; age = ""; avatar = null
                        completed.clear(); pending.clear(); xp = 0; registered = false; tab = 0
                        testMode = true
                    }
                )
            }
        }
    }

    if (award != null) {
        AnimatedAwardDialog(award!!, lang) {
            awardQueue = awardQueue.drop(1)
            award = awardQueue.firstOrNull()
        }
    }
}

@Composable
private fun RegistrationScreen(
    lang: String,
    first: String, setFirst: (String) -> Unit,
    last: String, setLast: (String) -> Unit,
    age: String, setAge: (String) -> Unit,
    avatar: Uri?, setAvatar: (Uri?) -> Unit,
    setLang: (String) -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val firstFocus = remember { FocusRequester() }
    val lastFocus = remember { FocusRequester() }
    val ageFocus = remember { FocusRequester() }
    var error by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            setAvatar(uri)
        }
    }
    Column(Modifier.fillMaxSize().background(Page).imePadding().verticalScroll(rememberScrollState()).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            listOf("tg", "ru", "en").forEach { code ->
                Text(code.uppercase(), color = if (lang == code) Gold else Muted,
                    modifier = Modifier.clickable { setLang(code) }.padding(8.dp), fontWeight = FontWeight.Bold)
            }
        }
        BrandHeader("ШАҲРВАНД", tx(lang, "Ҳар қадам — як пешрафт", "Каждый шаг — это прогресс", "Every step is progress"))
        Spacer(Modifier.height(17.dp))
        Text(tx(lang, "Сохтани профил", "Создать профиль", "Create profile"), color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (avatar == null) {
            Box(Modifier.size(68.dp).clip(CircleShape).background(Color.White).border(1.dp, BorderTone, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, null, tint = Muted, modifier = Modifier.size(35.dp))
            }
        } else AsyncImage(model = avatar, contentDescription = null, modifier = Modifier.size(68.dp).clip(CircleShape))
        TextButton(onClick = { photoPicker.launch(arrayOf("image/*")) }) {
            Text(tx(lang, "Иловаи акс", "Добавить фото", "Add photo"))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(first, setFirst, modifier = Modifier.weight(1f).focusRequester(firstFocus),
                label = { Text(tx(lang, "Ном", "Имя", "First name")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { lastFocus.requestFocus() }))
            OutlinedTextField(last, setLast, modifier = Modifier.weight(1f).focusRequester(lastFocus),
                label = { Text(tx(lang, "Насаб", "Фамилия", "Surname")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { ageFocus.requestFocus() }))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(age, { setAge(it.filter(Char::isDigit).take(3)) },
            modifier = Modifier.fillMaxWidth().focusRequester(ageFocus),
            label = { Text(tx(lang, "Синну сол", "Возраст", "Age")) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }))
        if (error) Text(tx(lang, "Ном, насаб ва синни 7–120-ро дуруст ворид кун.", "Введите имя, фамилию и возраст 7–120 лет.", "Enter your name and age from 7 to 120."),
            color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        Spacer(Modifier.height(13.dp))
        Button(onClick = {
            val a = age.toIntOrNull()
            error = first.isBlank() || last.isBlank() || a == null || a !in 7..120
            if (!error) { keyboard?.hide(); onContinue() }
        }, modifier = Modifier.fillMaxWidth().height(51.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy)) {
            Text(tx(lang, "Идома додан", "Продолжить", "Continue"), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(tx(lang, "Маълумот дар дохили ҳамин телефон нигоҳ дошта мешавад.", "Данные сохраняются на этом телефоне.", "Data is stored locally on this phone."),
            color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun HomeScreen(
    lang: String, firstName: String, xp: Int, completedCount: Int, pendingCount: Int,
    testMode: Boolean, prefs: android.content.SharedPreferences,
    onMission: (Mission) -> Unit, onAllMissions: () -> Unit
) {
    var walk by remember { mutableFloatStateOf(prefs.getFloat("walk_today_m", 0f)) }
    var run by remember { mutableFloatStateOf(prefs.getFloat("run_today_m", 0f)) }
    LaunchedEffect(Unit) {
        while (true) { walk = prefs.getFloat("walk_today_m", 0f); run = prefs.getFloat("run_today_m", 0f); delay(1100) }
    }
    val walkGoal = if (testMode) 200f else 5000f
    val runGoal = if (testMode) 120f else 2000f
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        BrandHeader("ШАҲРВАНД", tx(lang, "Ҳар қадам — як пешрафт", "Каждый шаг — это прогресс", "Every step is progress"))
        Spacer(Modifier.height(13.dp))
        Text(tx(lang, "Салом", "Здравствуйте", "Hello") + ", " + firstName + "!", color = Ink, fontWeight = FontWeight.Bold, fontSize = 21.sp)
        Surface(color = if (testMode) Color(0xFFFFF0D5) else Color(0xFFE4F1E9), shape = RoundedCornerShape(18.dp)) {
            Text(if (testMode) tx(lang, "РЕЖИМИ ТЕСТӢ • ҲАДАФҲОИ КӮТОҲ", "ТЕСТОВЫЙ РЕЖИМ • КОРОТКИЕ ЦЕЛИ", "TEST MODE • SHORT TARGETS")
                else tx(lang, "РЕЖИМИ ВОҚЕӢ", "ОБЫЧНЫЙ РЕЖИМ", "PRODUCTION MODE"),
                color = Navy, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(tx(lang, "ХОЛҲОИ ШУМО", "ВАШИ БАЛЛЫ", "YOUR POINTS"), color = Color(0xFFDCE6ED), fontSize = 10.sp)
                Text(xp.toString() + " XP", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
                Text(tx(lang, "Титулҳо бо иҷрои вазифаҳо кушода мешаванд", "Титулы открываются за задания", "Titles unlock as missions are completed"),
                    color = Color(0xFFE7D4A8), fontSize = 10.sp)
                Spacer(Modifier.height(9.dp))
                CulturalPatternBand()
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            StatCard(tx(lang, "Иҷрошуда", "Выполнено", "Completed"), completedCount.toString(), Icons.Filled.CheckCircle, Green, Modifier.weight(1f))
            StatCard(tx(lang, "Интизор", "На проверке", "Pending"), pendingCount.toString(), Icons.Filled.AccessTime, Gold, Modifier.weight(1f))
            StatCard("XP", xp.toString(), Icons.Filled.Star, NavyLight, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Text(tx(lang, "Саломатии ҳаррӯза", "Ежедневная активность", "Daily activity"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        DistanceCard(tx(lang, "Роҳгардӣ", "Ходьба", "Walking"), walk, walkGoal, "👟")
        DistanceCard(tx(lang, "Давидан", "Бег", "Running"), run, runGoal, "🏃")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tx(lang, "Вазифаҳо", "Задания", "Missions"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            TextButton(onClick = onAllMissions) { Text(tx(lang, "Ҳама", "Все", "All")) }
        }
        missionList.take(2).forEach { mission ->
            MissionTile(mission, lang, mission.id in listOf("run", "walk"), testMode, false, false) { onMission(mission) }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E8)), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👑", fontSize = 27.sp); Spacer(Modifier.width(9.dp))
                Column {
                    Text(tx(lang, "Шаҳрванди рӯз", "Гражданин дня", "Citizen of the Day"), color = Navy, fontWeight = FontWeight.Bold)
                    Text(tx(lang, "Титули махсус барои саҳми арзишманд.", "Особый титул за важный вклад.", "A special title for meaningful contribution."), color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun DistanceCard(title: String, distance: Float, goal: Float, icon: String) {
    val progress = (distance / goal).coerceIn(0f, 1f)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(top = 7.dp)) {
        Column(Modifier.fillMaxWidth().padding(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 22.sp); Spacer(Modifier.width(8.dp))
                Text(title, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(formatDistance(distance) + " / " + formatDistance(goal), color = Navy, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = if (progress >= 1f) Green else Gold, trackColor = BorderTone)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(13.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
            Text(value, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Text(label, color = Muted, fontSize = 8.sp, lineHeight = 10.sp)
        }
    }
}

@Composable
private fun MissionsScreen(
    lang: String, testMode: Boolean, completed: List<String>, pending: List<String>,
    customMissions: List<Mission>, onMission: (Mission) -> Unit, onAdd: (String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 15.dp)) {
        Spacer(Modifier.height(15.dp))
        BrandHeader(tx(lang, "Вазифаҳо", "Задания", "Missions"), tx(lang, "Ҳар вазифа — як қадам", "Каждое задание — шаг", "Every mission is a step"))
        Spacer(Modifier.height(8.dp))
        Surface(color = if (testMode) Color(0xFFFFF0D5) else Color(0xFFE4F1E9), shape = RoundedCornerShape(11.dp)) {
            Text(if (testMode) tx(lang, "ТЕСТ: ҳадафҳои кӯтоҳ, XP-и кам", "ТЕСТ: короткие цели, мало XP", "TEST: short targets, low XP")
            else tx(lang, "Меъёрҳои воқеӣ фаъоланд", "Действуют обычные цели", "Production targets are active"),
                color = Navy, fontSize = 10.sp, modifier = Modifier.padding(9.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 9.dp)) {
            items(missionList + customMissions, key = { it.id }) { mission ->
                MissionTile(mission, lang, mission.gpsMode != null, testMode, mission.id in completed, mission.id in pending) { onMission(mission) }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF0F2)), shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { showDialog = true }) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, tint = Navy); Spacer(Modifier.width(8.dp))
                        Column {
                            Text(tx(lang, "Фаъолияти шахсӣ", "Личная активность", "Personal activity"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(tx(lang, "Фаъолияти нав пешниҳод кун", "Предложите новое задание", "Suggest a new activity"), color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
    if (showDialog) AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text(tx(lang, "Фаъолияти шахсӣ", "Личная активность", "Personal activity")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(tx(lang, "Ном", "Название", "Title")) }, singleLine = true)
                OutlinedTextField(desc, { desc = it }, label = { Text(tx(lang, "Тавсиф", "Описание", "Description")) }, minLines = 3)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank() && desc.isNotBlank()) { onAdd(title.trim(), desc.trim()); title = ""; desc = ""; showDialog = false } }) {
                Text(tx(lang, "Илова", "Добавить", "Add"))
            }
        },
        dismissButton = { TextButton(onClick = { showDialog = false }) { Text(tx(lang, "Бекор", "Отмена", "Cancel")) } }
    )
}

@Composable
private fun MissionTile(
    mission: Mission, lang: String, gps: Boolean, testMode: Boolean,
    completed: Boolean, pending: Boolean, onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFF2EFE7)), contentAlignment = Alignment.Center) {
                Text(mission.icon, fontSize = 23.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(mission.title(lang), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(mission.category(lang), color = Muted, fontSize = 10.sp)
                if (gps) Text(tx(lang, "GPS • бе акс/видео", "GPS • без фото/видео", "GPS • no photo/video"), color = Green, fontSize = 9.sp)
                if (completed) Text(tx(lang, "Иҷро шуд ✓", "Выполнено ✓", "Completed ✓"), color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                else if (pending) Text(tx(lang, "Интизори санҷиш", "Ожидает проверки", "Pending review"), color = Gold, fontSize = 9.sp)
            }
            Text(if (completed) "✓" else "+" + (if (testMode) mission.testXp else mission.normalXp) + " XP",
                color = if (completed) Green else Navy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun GpsMissionScreen(
    mission: Mission, lang: String, testMode: Boolean, completed: Boolean,
    onBack: () -> Unit, onComplete: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(LocationTrackingService.PREFS, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val mode = mission.gpsMode ?: LocationTrackingService.MODE_WALK
    val isRun = mode == LocationTrackingService.MODE_RUN
    val goal = if (testMode) { if (isRun) 120f else 200f } else { if (isRun) 2000f else 5000f }
    var active by remember { mutableStateOf(prefs.getBoolean("tracker_active", false) && prefs.getString("tracker_mode", "") == mode) }
    var walkToday by remember { mutableFloatStateOf(prefs.getFloat("walk_today_m", 0f)) }
    var runToday by remember { mutableFloatStateOf(prefs.getFloat("run_today_m", 0f)) }
    var accuracy by remember { mutableFloatStateOf(prefs.getFloat("tracker_accuracy_m", -1f)) }
    var speed by remember { mutableFloatStateOf(prefs.getFloat("tracker_speed_mps", 0f)) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun refresh() {
        active = prefs.getBoolean("tracker_active", false) && prefs.getString("tracker_mode", "") == mode
        walkToday = prefs.getFloat("walk_today_m", 0f)
        runToday = prefs.getFloat("run_today_m", 0f)
        accuracy = prefs.getFloat("tracker_accuracy_m", -1f)
        speed = prefs.getFloat("tracker_speed_mps", 0f)
    }

    fun startTracking() {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            error = tx(lang, "GPS-и телефонро фаъол кун.", "Включите GPS телефона.", "Turn on phone GPS.")
            return
        }
        try {
            prefs.edit().putBoolean("tracker_goal_reached_$mode", false).apply()
            val intent = Intent(context, LocationTrackingService::class.java)
                .setAction(LocationTrackingService.ACTION_START)
                .putExtra(LocationTrackingService.EXTRA_MODE, mode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
            error = ""; message = ""; refresh()
        } catch (e: Exception) {
            error = e.message ?: "GPS error"
        }
    }

    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startTracking()
        else error = tx(lang, "Барои трекинг иҷозати ҷойгиршавии дақиқ зарур аст.", "Нужно разрешение на точную геолокацию.", "Precise location permission is required.")
    }

    LaunchedEffect(mode, completed) {
        while (true) {
            refresh()
            if (!completed && prefs.getBoolean("tracker_goal_reached_$mode", false)) {
                message = tx(lang, "Ҳадаф иҷро шуд! Вазифа ба таври худкор анҷом ёфт.", "Цель достигнута! Задание завершено автоматически.", "Target reached! Mission completed automatically.")
                onComplete()
                break
            }
            delay(700)
        }
    }

    val distance = if (isRun) runToday else walkToday
    val progress = (distance / goal).coerceIn(0f, 1f)
    Column(Modifier.fillMaxSize().background(Page).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Navy) }
            Text(tx(lang, "Ба вазифаҳо", "К заданиям", "Back to missions"), color = Navy, fontWeight = FontWeight.Bold)
        }
        BrandHeader(mission.title(lang), tx(lang, "GPS фаъолиятро сабт мекунад", "GPS записывает активность", "GPS records your activity"))
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(21.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isRun) "🏃" else "👟", fontSize = 44.sp)
                Text(formatDistance(distance), color = Color.White, fontWeight = FontWeight.Black, fontSize = 38.sp)
                Text(tx(lang, "аз ҳадаф", "из цели", "of target") + " " + formatDistance(goal), color = Color(0xFFE7D4A8), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Gold, trackColor = NavyLight)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tx(lang, "Роҳгардии имрӯз", "Ходьба сегодня", "Today's walking"), color = Color(0xFFDCE6ED), fontSize = 9.sp)
                        Text(formatDistance(walkToday), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tx(lang, "Давидани имрӯз", "Бег сегодня", "Today's running"), color = Color(0xFFDCE6ED), fontSize = 9.sp)
                        Text(formatDistance(runToday), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
            Column(Modifier.padding(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MyLocation, null, tint = if (active) Green else Navy)
                    Spacer(Modifier.width(7.dp))
                    Text(if (active) tx(lang, "GPS фаъол аст", "GPS активен", "GPS is active") else tx(lang, "GPS омода аст", "GPS готов", "GPS is ready"),
                        color = if (active) Green else Navy, fontWeight = FontWeight.Bold)
                }
                Text(tx(lang, "Таснифи ҳаракат аз суръати GPS истифода мебарад; панҷ нуқтаи охирин ҳамвор карда мешаванд.",
                    "Классификация использует скорость GPS с усреднением пяти последних точек.",
                    "Movement classification uses GPS speed smoothed over the last five points."),
                    color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                Text(tx(lang, "Муайян шуд: ", "Определено: ", "Detected: ") +
                    if (speed >= 1.72f) tx(lang, "давидан", "бег", "running") else tx(lang, "роҳгардӣ", "ходьба", "walking"),
                    color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(tx(lang, "Суръат", "Скорость", "Speed") + ": %.1f км/соат".format(speed * 3.6f) +
                    if (accuracy > 0f) " • ±" + accuracy.toInt() + " м" else "", color = Muted, fontSize = 10.sp)
            }
        }
        if (testMode) {
            Spacer(Modifier.height(8.dp))
            Surface(color = Color(0xFFFFF0D5), shape = RoundedCornerShape(11.dp)) {
                Text(tx(lang, "Режими тестӣ: ҳадаф " + formatDistance(goal) + ". Дар режими воқеӣ: " + if (isRun) "2 км." else "5 км.",
                    "Тестовая цель: " + formatDistance(goal) + ". Обычная цель: " + if (isRun) "2 км." else "5 км.",
                    "Test target: " + formatDistance(goal) + ". Production target: " + if (isRun) "2 km." else "5 km."),
                    modifier = Modifier.padding(10.dp), color = Navy, fontSize = 10.sp)
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        if (message.isNotBlank()) Text(message, color = Green, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        if (!completed) {
            if (!active) {
                Button(onClick = {
                    if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) startTracking()
                    else permissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Navy), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(7.dp))
                    Text(tx(lang, "Оғози GPS", "Начать GPS", "Start GPS"), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = {
                    context.startService(Intent(context, LocationTrackingService::class.java).setAction(LocationTrackingService.ACTION_STOP))
                    scope.launch {
                        delay(500)
                        refresh()
                        val current = if (isRun) prefs.getFloat("run_today_m", 0f) else prefs.getFloat("walk_today_m", 0f)
                        if (current >= goal) onComplete()
                        else message = tx(lang,
                            "Ҳанӯз " + formatDistance(goal - current) + " мондааст. Баъдтар идома деҳ.",
                            "Осталось " + formatDistance(goal - current) + ". Можно продолжить позже.",
                            formatDistance(goal - current) + " remaining. Continue later.")
                    }
                }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Copper), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Filled.Stop, null); Spacer(Modifier.width(7.dp))
                    Text(tx(lang, "Қатъ ва санҷиш", "Стоп и проверить", "Stop and check"), fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Text(tx(lang, "Вазифа иҷро шудааст ✓", "Задание выполнено ✓", "Mission completed ✓"), color = Green, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(tx(lang, "Барои ҷамъ кардани 5 км роҳгардӣ трекингро пеш аз сайругашт оғоз кун. Метавонӣ онро қатъ ва баъдтар боз оғоз намоӣ. Хизмат ҳангоми хомӯш будани экран кор мекунад ва огоҳиномаи доимӣ нишон медиҳад.",
            "Для накопления 5 км начните трекинг перед прогулкой. Можно остановить и запустить снова. Сервис продолжает работу при выключенном экране и показывает уведомление.",
            "Start tracking before walking to accumulate the daily 5 km. You can stop and restart it. The foreground service continues with the screen off and displays a persistent notification."),
            color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun ProofMissionScreen(
    mission: Mission, lang: String, testMode: Boolean,
    completed: Boolean, pending: Boolean,
    onBack: () -> Unit, onTestComplete: () -> Unit, onPending: () -> Unit
) {
    val context = LocalContext.current
    var proof by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            proof = uri; error = ""
        }
    }
    Column(Modifier.fillMaxSize().background(Page).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Navy) }
            Text(tx(lang, "Ба вазифаҳо", "К заданиям", "Back to missions"), color = Navy, fontWeight = FontWeight.Bold)
        }
        BrandHeader(mission.title(lang), mission.category(lang))
        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(19.dp)) {
            Column(Modifier.padding(17.dp)) {
                Text(mission.icon, fontSize = 38.sp)
                Text(mission.title(lang), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(mission.description(lang), color = Color(0xFFDCE6ED), fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("+" + (if (testMode) mission.testXp else mission.normalXp) + " XP", color = Color(0xFFE7D4A8), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(17.dp))
        Text(tx(lang, "Далели иҷро", "Подтверждение", "Evidence"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(5.dp)); Text(tx(lang, "Акс", "Фото", "Photo"))
            }
            OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.VideoLibrary, null); Spacer(Modifier.width(5.dp)); Text(tx(lang, "Видео", "Видео", "Video"))
            }
        }
        if (proof != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = Green)
                Spacer(Modifier.width(6.dp))
                Text(tx(lang, "Далел интихоб шуд", "Файл выбран", "Evidence selected"), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (testMode) {
            Spacer(Modifier.height(9.dp))
            Surface(color = Color(0xFFFFF0D5), shape = RoundedCornerShape(11.dp)) {
                Text(tx(lang, "Тест: баъди интихоби акс ё видео XP фавран дода мешавад.",
                    "Тест: после выбора фото или видео XP начисляется сразу.",
                    "Test: XP is granted immediately after selecting photo or video."),
                    color = Navy, fontSize = 10.sp, modifier = Modifier.padding(10.dp))
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        Spacer(Modifier.height(13.dp))
        when {
            completed -> Text(tx(lang, "Вазифа иҷро шуд ✓", "Задание выполнено ✓", "Mission completed ✓"), color = Green, fontWeight = FontWeight.Bold)
            pending -> Text(tx(lang, "Интизорӣ барои тасдиқ", "Ожидает проверки", "Awaiting review"), color = Gold, fontWeight = FontWeight.Bold)
            else -> Button(onClick = {
                if (proof == null) error = tx(lang, "Аввал акс ё видео интихоб кун.", "Сначала выберите фото или видео.", "Choose a photo or video first.")
                else if (testMode) onTestComplete() else onPending()
            }, modifier = Modifier.fillMaxWidth().height(51.dp), colors = ButtonDefaults.buttonColors(containerColor = Navy), shape = RoundedCornerShape(14.dp)) {
                Text(if (testMode) tx(lang, "Гирифтани холҳои тестӣ", "Получить тестовые XP", "Grant test XP")
                    else tx(lang, "Ирсол барои санҷиш", "Отправить на проверку", "Submit for review"), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(tx(lang, "Дар ин версия файл маҳаллӣ интихоб мешавад; боркунии серверӣ ҳанӯз пайваст нест.",
            "В этой версии файл выбирается локально; загрузка на сервер ещё не подключена.",
            "In this version the file is selected locally; server upload is not connected."),
            color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

private data class BoardMember(
    val id: String,
    val name: String,
    val xp: Int,
    val mainTitleId: String?,
    val badgeIds: List<String>,
    val isCurrentUser: Boolean = false
)

@Composable
private fun TitlePill(title: String, accent: Color, compact: Boolean = false) {
    Surface(color = accent.copy(alpha = .12f), shape = RoundedCornerShape(8.dp)) {
        Row(
            Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 3.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(if (compact) 5.dp else 6.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(5.dp))
            Text(title, color = accent, fontSize = if (compact) 9.sp else 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun RankingScreen(lang: String, first: String, last: String, totalXp: Int, completedIds: List<String>) {
    var selectedMember by remember { mutableStateOf<BoardMember?>(null) }
    val earnedIds = buildList {
        if (completedIds.isNotEmpty()) add("first")
        if ("run" in completedIds) add("runner")
        if ("walk" in completedIds) add("walker")
        if ("clean" in completedIds) add("helper")
        if ("tree" in completedIds) add("nature")
        if ("read" in completedIds) add("reader")
        if (completedIds.size >= 2) add("citizen")
    }
    val mainTitleId = listOf("citizen", "runner", "walker", "nature", "helper", "reader", "first")
        .firstOrNull { it in earnedIds }
    val ownName = (first + " " + last).trim().ifBlank { tx(lang, "Шумо", "Вы", "You") }
    val members = listOf(
        BoardMember("p1", "Фирдавс Саидов", 18, "runner", listOf("first", "runner")),
        BoardMember("p2", "Меҳрона Раҳмон", 14, "nature", listOf("first", "nature", "reader")),
        BoardMember("p3", "Сомон Каримов", 9, "helper", listOf("first", "helper")),
        BoardMember("p4", "Мадина Юсуфӣ", 4, "reader", listOf("first", "reader")),
        BoardMember("self", ownName, totalXp, mainTitleId, earnedIds, true)
    ).sortedWith(compareByDescending<BoardMember> { it.xp }.thenBy { if (it.isCurrentUser) 1 else 0 })
    val ownRank = members.indexOfFirst { it.isCurrentUser } + 1
    val nextMember = members.getOrNull(ownRank - 2)
    val xpToNext = if (nextMember == null) 0 else (nextMember.xp - totalXp + 1).coerceAtLeast(0)

    Column(Modifier.fillMaxSize().padding(horizontal = 15.dp)) {
        Spacer(Modifier.height(15.dp))
        BrandHeader(tx(lang, "Рейтинг", "Рейтинг", "Ranking"),
            tx(lang, "5 иштирокчӣ • режими тестӣ", "5 участников • тестовый режим", "5 participants • test leaderboard"))
        Spacer(Modifier.height(11.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(17.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tx(lang, "ҶОЙИ ШУМО", "ВАШЕ МЕСТО", "YOUR POSITION"), color = Color(0xFFDCE5E4), fontSize = 9.sp, letterSpacing = 1.sp)
                    Text("#" + ownRank + "  •  " + ownName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (ownRank == 1) tx(lang, "Пешсафи рейтинг!", "Вы лидер рейтинга!", "You lead the ranking!")
                        else tx(lang, "Барои боло рафтан: " + xpToNext + " XP", "До следующего места: " + xpToNext + " XP", xpToNext.toString() + " XP to climb one place"),
                        color = Color(0xFFE2C99E), fontSize = 10.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(totalXp.toString(), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("XP", color = Color(0xFFE2C99E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tx(lang, "ИШТИРОКЧИЁН", "УЧАСТНИКИ", "PARTICIPANTS"), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text(tx(lang, "Барои дидани профил пахш кун", "Нажмите для профиля", "Tap a person to view profile"), color = Muted, fontSize = 9.sp)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 9.dp)
        ) {
            items(members, key = { it.id }) { member ->
                val pos = members.indexOf(member) + 1
                LeaderProfileRow(pos, member, lang) { selectedMember = member }
            }
        }
        Text(tx(lang, "Холҳо аз вазифаҳои иҷрошуда ҷамъ мешаванд. Бо ҳар вазифа мақом метавонад боло ё поён равад.",
            "Баллы накапливаются за задания. После каждого задания место может измениться.",
            "Points accumulate from completed missions. Your position can move after each mission."),
            color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
    }
    if (selectedMember != null) {
        val member = selectedMember!!
        val mainBadge = member.mainTitleId?.let { id -> badgeList.find { it.id == id } }
        val badges = member.badgeIds.mapNotNull { id -> badgeList.find { it.id == id } }
        AlertDialog(
            onDismissRequest = { selectedMember = null },
            icon = {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFECE5D9)), contentAlignment = Alignment.Center) {
                    Text(member.name.take(1), color = Navy, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
            },
            title = { Text(member.name, color = Navy, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(member.xp.toString() + " XP", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text(tx(lang, "Титули асосӣ", "Основной титул", "Main title"), color = Muted, fontSize = 10.sp)
                    if (mainBadge != null) {
                        TitlePill(mainBadge.title(lang), badgeAccent(mainBadge.id))
                        Text(mainBadge.rule(lang), color = Ink, fontSize = 11.sp, lineHeight = 15.sp)
                    } else {
                        TitlePill(tx(lang, "Навкор", "Новичок", "Newcomer"), Turquoise)
                    }
                    Text(tx(lang, "Нишонҳои гирифташуда", "Полученные значки", "Earned badges"), color = Muted, fontSize = 10.sp)
                    if (badges.isEmpty()) {
                        Text(tx(lang, "Ҳоло нишон надорад.", "Пока нет значков.", "No badges yet."), color = Muted, fontSize = 11.sp)
                    } else {
                        badges.forEach { badge ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(badge.icon, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(badge.title(lang), color = badgeAccent(badge.id), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(badge.rule(lang), color = Muted, fontSize = 9.sp, lineHeight = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMember = null }) {
                    Text(tx(lang, "Пӯшидан", "Закрыть", "Close"))
                }
            }
        )
    }
}

@Composable
private fun LeaderProfileRow(position: Int, member: BoardMember, lang: String, onClick: () -> Unit) {
    val title = member.mainTitleId?.let { id -> badgeList.find { it.id == id } }
    val accent = title?.let { badgeAccent(it.id) } ?: Turquoise
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (member.isCurrentUser) Color(0xFFF0E7D8) else Color.White),
        shape = RoundedCornerShape(15.dp),
        border = if (member.isCurrentUser) androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .75f)) else null
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(if (position == 1) Color(0xFFECE0C9) else Color(0xFFECEFED)),
                contentAlignment = Alignment.Center
            ) {
                Text(position.toString(), color = if (position == 1) Gold else Muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(9.dp))
            Box(Modifier.size(37.dp).clip(CircleShape).background(accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Text(member.name.take(1), color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(member.name, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                TitlePill(title?.title(lang) ?: tx(lang, "Навкор", "Новичок", "Newcomer"), accent, compact = true)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(member.xp.toString() + " XP", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (member.isCurrentUser) tx(lang, "Шумо", "Вы", "You") else tx(lang, "Иштирокчӣ", "Участник", "Participant"),
                    color = Muted, fontSize = 8.sp)
            }
            Spacer(Modifier.width(3.dp))
            Icon(Icons.Filled.ChevronRight, null, tint = Muted, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun PeriodStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(11.dp)) {
        Column(Modifier.padding(8.dp)) {
            Text(label, color = Muted, fontSize = 8.sp, maxLines = 1)
            Text(value, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LeaderRow(position: Int, name: String, xp: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(13.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(if (position <= 3) Color(0xFFFFF1D5) else Color(0xFFF1F2F2)), contentAlignment = Alignment.Center) {
                Text(position.toString(), color = if (position <= 3) Gold else Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(name, modifier = Modifier.weight(1f), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("%,d XP".format(xp), color = Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileScreen(
    lang: String, first: String, last: String, age: String, avatar: Uri?, xp: Int,
    completed: List<String>, pendingCount: Int, testMode: Boolean,
    onLanguage: (String) -> Unit, onTestMode: (Boolean) -> Unit,
    onSave: (String, String, String) -> Unit, onReset: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedBadge by remember { mutableStateOf<BadgeInfo?>(null) }
    val earned = remember(completed.toSet(), testMode) {
        buildSet {
            if (completed.isNotEmpty()) add("first")
            if ("run" in completed) add("runner")
            if ("walk" in completed) add("walker")
            if ("clean" in completed) add("helper")
            if ("tree" in completed) add("nature")
            if ("read" in completed) add("reader")
            if (completed.size >= (if (testMode) 2 else 5)) add("citizen")
        }
    }
    var editFirst by remember(first) { mutableStateOf(first) }
    var editLast by remember(last) { mutableStateOf(last) }
    var editAge by remember(age) { mutableStateOf(age) }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(15.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { BrandHeader(tx(lang, "Профил", "Профиль", "Profile")) }
            IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.Settings, null, tint = Navy) }
        }
        Spacer(Modifier.height(11.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(19.dp)) {
            Column {
                CulturalPatternBand()
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (avatar == null) {
                        Box(Modifier.size(62.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    } else AsyncImage(model = avatar, contentDescription = null, modifier = Modifier.size(62.dp).clip(CircleShape))
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(first + " " + last, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(age + " " + tx(lang, "сола", "лет", "years old"), color = Color(0xFFDCE6ED), fontSize = 10.sp)
                        Text(xp.toString() + " XP", color = Color(0xFFE7D4A8), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        Text(tx(lang, "Титулҳо ва нишонҳо", "Титулы и значки", "Titles and badges"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(tx(lang, "Гирифташуда", "Получено", "Earned"), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        BadgeGrid(badgeList.filter { it.id in earned }, lang, true) { selectedBadge = it }
        Spacer(Modifier.height(8.dp))
        Text(tx(lang, "Ҳанӯз гирифта нашудааст", "Ещё не получено", "Not earned yet"), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        BadgeGrid(badgeList.filter { it.id !in earned }, lang, false) { selectedBadge = it }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(tx(lang, "Таърихи фаъолият", "История активности", "Activity history"), color = Navy, fontWeight = FontWeight.Bold)
                Text(tx(lang, "Иҷрошуда", "Выполнено", "Completed") + ": " + completed.size, color = Muted, fontSize = 11.sp)
                Text(tx(lang, "Интизори тасдиқ", "Ожидает проверки", "Pending review") + ": " + pendingCount, color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showSettings = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Settings, null); Spacer(Modifier.width(6.dp)); Text(tx(lang, "Танзимот", "Настройки", "Settings"))
        }
    }
    if (selectedBadge != null) {
        val b = selectedBadge!!
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            icon = { Text(b.icon, fontSize = 34.sp) },
            title = { Text(b.title(lang), color = Navy, fontWeight = FontWeight.ExtraBold) },
            text = { Column {
                Text(tx(lang, "Чӣ гуна гирифтан мумкин аст:", "Как получить:", "How to earn:"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp)); Text(b.rule(lang), color = Ink, fontSize = 13.sp)
                Spacer(Modifier.height(7.dp))
                Text(if (b.id in earned) tx(lang, "Аллакай гирифтӣ ✓", "Уже получено ✓", "Already earned ✓") else tx(lang, "Ҳоло баста аст", "Пока закрыто", "Still locked"),
                    color = if (b.id in earned) Green else Muted, fontSize = 11.sp)
            } },
            confirmButton = { TextButton(onClick = { selectedBadge = null }) { Text(tx(lang, "Фаҳмо", "Понятно", "Got it")) } }
        )
    }
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(tx(lang, "Танзимот", "Настройки", "Settings")) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tx(lang, "Маълумоти профил", "Данные профиля", "Profile details"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(editFirst, { editFirst = it }, label = { Text(tx(lang, "Ном", "Имя", "First name")) }, singleLine = true)
                    OutlinedTextField(editLast, { editLast = it }, label = { Text(tx(lang, "Насаб", "Фамилия", "Surname")) }, singleLine = true)
                    OutlinedTextField(editAge, { editAge = it.filter(Char::isDigit).take(3) }, label = { Text(tx(lang, "Синну сол", "Возраст", "Age")) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    if (error) Text(tx(lang, "Маълумотро санҷ.", "Проверьте данные.", "Check your details."), color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    Text(tx(lang, "Забон", "Язык", "Language"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("tg", "ru", "en").forEach { code ->
                            FilterChip(selected = lang == code, onClick = { onLanguage(code) }, label = { Text(code.uppercase()) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tx(lang, "Режими тестӣ", "Тестовый режим", "Test mode"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(tx(lang, "Ҳадафи кӯтоҳ ва XP-и хурд", "Короткие цели и небольшой XP", "Short goals and small XP"), color = Muted, fontSize = 10.sp)
                        }
                        Switch(checked = testMode, onCheckedChange = onTestMode)
                    }
                    TextButton(onClick = { showSettings = false; onReset() }) {
                        Text(tx(lang, "Пок кардани профили тестӣ", "Сбросить тестовый профиль", "Reset test profile"), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val a = editAge.toIntOrNull()
                    error = editFirst.isBlank() || editLast.isBlank() || a == null || a !in 7..120
                    if (!error) { onSave(editFirst.trim(), editLast.trim(), editAge); showSettings = false }
                }) { Text(tx(lang, "Нигоҳ доштан", "Сохранить", "Save")) }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text(tx(lang, "Бекор", "Отмена", "Cancel")) } }
        )
    }
}

@Composable
private fun BadgeGrid(items: List<BadgeInfo>, lang: String, earned: Boolean, onSelect: (BadgeInfo) -> Unit) {
    items.chunked(3).forEach { rowItems ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            rowItems.forEach { b ->
                val accent = badgeAccent(b.id)
                Card(
                    modifier = Modifier.weight(1f).alpha(if (earned) 1f else .62f).clickable { onSelect(b) },
                    colors = CardDefaults.cardColors(containerColor = badgeTint(b.id)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .35f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(b.icon, fontSize = 25.sp)
                        Text(b.title(lang), color = accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 12.sp, maxLines = 2)
                        Text(if (earned) "✓" else "🔒", color = accent, fontSize = 10.sp)
                    }
                }
            }
            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun AnimatedAwardDialog(badge: BadgeInfo, lang: String, onDismiss: () -> Unit) {
    val scale by animateFloatAsState(1f, animationSpec = spring(dampingRatio = .48f, stiffness = 250f), label = "badgeScale")
    val rotate by animateFloatAsState(360f, animationSpec = tween(900), label = "badgeRotate")
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF4)), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(19.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CulturalPatternBand()
                Spacer(Modifier.height(13.dp))
                Text("✦  ШАҲРВАНД  ✦", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.size(98.dp).clip(CircleShape).background(Color(0xFFF0E7D8)).border(2.dp, Gold, CircleShape), contentAlignment = Alignment.Center) {
                    Text(badge.icon, fontSize = 54.sp, modifier = Modifier.graphicsLayer { rotationZ = rotate })
                }
                Spacer(Modifier.height(16.dp))
                Text(tx(lang, "ТАБРИК! ТУ ГИРИФТӢ", "ПОЗДРАВЛЯЕМ! ПОЛУЧЕНО", "CONGRATULATIONS! EARNED"),
                    color = Gold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(badge.title(lang), color = badgeAccent(badge.id), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(badge.rule(lang), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(15.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Navy),
                    shape = RoundedCornerShape(13.dp)) { Text(tx(lang, "Ифтихор кун!", "Так держать!", "Keep it up!"), fontWeight = FontWeight.Bold) }
            }
        }
    }
}
