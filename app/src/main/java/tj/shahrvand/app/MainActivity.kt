package tj.shahrvand.app

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
            val colors = lightColorScheme(
                primary = Navy,
                secondary = Gold,
                tertiary = Green,
                background = Canvas,
                surface = Color.White,
                onBackground = Ink,
                onSurface = Ink
            )
            MaterialTheme(colorScheme = colors) {
                ShahrvandApp()
            }
        }
    }
}

private data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val xp: Int,
    val duration: String,
    val icon: ImageVector,
    val personal: Boolean = false
)

private data class Leader(
    val position: Int,
    val name: String,
    val xp: Int
)

private val initialMissions = listOf(
    Mission("run", "2 км давидан", "Масофаи 2 километрро бо суръати мувофиқ тай кунед. Саломатӣ ва бехатариро ба назар гиред.", "Варзиш", 40, "20–35 дақиқа", Icons.Filled.DirectionsRun),
    Mission("clean", "Тоза кардани кӯча", "Қисми бехатари кӯча ё назди маҳалли истиқоматро тоза кунед. Ба партовҳои хатарнок даст нарасонед.", "Ҷомеа", 80, "30–45 дақиқа", Icons.Filled.CleaningServices),
    Mission("tree", "Шинондани дарахт", "Дарахтро дар ҷойи мувофиқ ва бо иҷозати зарурӣ шинонед. Навъи дарахтро ба иқлими маҳал мутобиқ интихоб кунед.", "Экология", 100, "30–60 дақиқа", Icons.Filled.Park),
    Mission("read", "30 дақиқа китобхонӣ", "Як мавзӯи муфидро хонед ва санҷиши кӯтоҳи донишро гузаред.", "Дониш", 30, "30 дақиқа", Icons.Filled.MenuBook)
)

private val translations = mapOf(
    "tg" to mapOf(
        "tagline" to "Ҳар қадам — як пешрафт",
        "prototype" to "ВЕРСИЯИ ТЕСТӢ • МАЪЛУМОТИ НАМУНАВӢ",
        "language" to "Забон",
        "create_profile" to "Сохтани профил",
        "first_name" to "Ном",
        "last_name" to "Насаб",
        "age" to "Синну сол",
        "photo_optional" to "Акси профил (ихтиёрӣ)",
        "add_photo" to "Илова кардани акс",
        "continue" to "Идома додан",
        "home" to "Асосӣ",
        "missions" to "Вазифаҳо",
        "ranking" to "Рейтинг",
        "profile" to "Профил",
        "hello" to "Салом",
        "your_progress" to "ПЕШРАФТИ ШУМО",
        "completed" to "Иҷрошуда",
        "pending" to "Интизори тасдиқ",
        "remaining" to "Боқимонда",
        "today_mission" to "Вазифаи имрӯз",
        "view_mission" to "Дидани вазифа",
        "recent_activity" to "Фаъолияти охирин",
        "citizen_day" to "Шаҳрванди рӯз",
        "citizen_day_empty" to "Ғолиби рӯз баъди санҷиши натиҷаҳо муайян мешавад",
        "all_missions" to "Вазифаҳои дастрас",
        "personal_mission" to "Пешниҳоди фаъолияти шахсӣ",
        "personal_note" to "Фаъолияти худро пешниҳод кунед. XP танҳо баъди санҷиш муайян мешавад.",
        "task_title" to "Номи фаъолият",
        "task_description" to "Тавсифи кӯтоҳ",
        "send_proposal" to "Ирсоли пешниҳод",
        "duration" to "Давомнокӣ",
        "reward" to "Мукофот",
        "after_review" to "Пас аз санҷиш",
        "submit_proof" to "Пешниҳоди далел",
        "photo" to "Иловаи акс",
        "video" to "Иловаи видео",
        "proof_selected" to "Далел интихоб шуд",
        "send_review" to "Ирсол барои санҷиш",
        "waiting" to "Дар интизори санҷиш",
        "approved" to "Тасдиқ шуд",
        "rejected" to "Рад шуд",
        "top30" to "ТОП 30",
        "your_position" to "Ҷойи шумо",
        "rank_today" to "Имрӯз",
        "rank_week" to "Ҳафта",
        "rank_month" to "Моҳ",
        "rank_year" to "Сол",
        "xp" to "XP",
        "level" to "Сатҳи",
        "achievements" to "Дастовардҳо",
        "history" to "Таърихи фаъолият",
        "joined" to "Иштирокчӣ аз",
        "settings" to "Танзимот",
        "privacy" to "Махфият ва аккаунт",
        "empty_history" to "Ҳоло фаъолияти тасдиқшуда нест",
        "back" to "Бозгашт",
        "choose_task" to "Вазифаро оғоз кунед",
        "status_submitted" to "Пешниҳод фиристода шуд. Дар прототип ҳолат маҳаллӣ нигоҳ дошта мешавад.",
        "personal_pending" to "Пешниҳоди шахсӣ — дар интизори санҷиш",
        "invalid_form" to "Ном, насаб ва синну соли дурустро ворид кунед.",
        "no_file" to "Аввал акс ё видеоро интихоб кунед.",
        "test_disclaimer" to "Ин прототип аст: аккаунт, XP ва пешниҳодҳо ҳоло танҳо дар дохили телефон намунавӣ мебошанд."
    ),
    "ru" to mapOf(
        "tagline" to "Каждый шаг — это прогресс",
        "prototype" to "ТЕСТОВАЯ ВЕРСИЯ • ДЕМО-ДАННЫЕ",
        "language" to "Язык",
        "create_profile" to "Создать профиль",
        "first_name" to "Имя",
        "last_name" to "Фамилия",
        "age" to "Возраст",
        "photo_optional" to "Фото профиля (необязательно)",
        "add_photo" to "Добавить фото",
        "continue" to "Продолжить",
        "home" to "Главная",
        "missions" to "Задания",
        "ranking" to "Рейтинг",
        "profile" to "Профиль",
        "hello" to "Здравствуйте",
        "your_progress" to "ВАШ ПРОГРЕСС",
        "completed" to "Выполнено",
        "pending" to "На проверке",
        "remaining" to "Осталось",
        "today_mission" to "Задание дня",
        "view_mission" to "Открыть задание",
        "recent_activity" to "Последняя активность",
        "citizen_day" to "Гражданин дня",
        "citizen_day_empty" to "Победитель дня будет определён после проверки результатов",
        "all_missions" to "Доступные задания",
        "personal_mission" to "Предложить свою активность",
        "personal_note" to "Предложите свою активность. XP будет назначен после проверки.",
        "task_title" to "Название активности",
        "task_description" to "Краткое описание",
        "send_proposal" to "Отправить предложение",
        "duration" to "Длительность",
        "reward" to "Награда",
        "after_review" to "После проверки",
        "submit_proof" to "Отправить подтверждение",
        "photo" to "Добавить фото",
        "video" to "Добавить видео",
        "proof_selected" to "Материал выбран",
        "send_review" to "Отправить на проверку",
        "waiting" to "Ожидает проверки",
        "approved" to "Подтверждено",
        "rejected" to "Отклонено",
        "top30" to "ТОП 30",
        "your_position" to "Ваше место",
        "rank_today" to "День",
        "rank_week" to "Неделя",
        "rank_month" to "Месяц",
        "rank_year" to "Год",
        "xp" to "XP",
        "level" to "Уровень",
        "achievements" to "Достижения",
        "history" to "История активности",
        "joined" to "Участник с",
        "settings" to "Настройки",
        "privacy" to "Конфиденциальность и аккаунт",
        "empty_history" to "Пока нет подтверждённой активности",
        "back" to "Назад",
        "choose_task" to "Начать задание",
        "status_submitted" to "Материал отправлен. В прототипе статус хранится локально.",
        "personal_pending" to "Личное предложение — ожидает проверки",
        "invalid_form" to "Введите корректные имя, фамилию и возраст.",
        "no_file" to "Сначала выберите фото или видео.",
        "test_disclaimer" to "Это прототип: аккаунт, XP и отправки пока демонстрационные и хранятся только на устройстве."
    ),
    "en" to mapOf(
        "tagline" to "Every step is progress",
        "prototype" to "TEST BUILD • SAMPLE DATA",
        "language" to "Language",
        "create_profile" to "Create profile",
        "first_name" to "First name",
        "last_name" to "Last name",
        "age" to "Age",
        "photo_optional" to "Profile photo (optional)",
        "add_photo" to "Add photo",
        "continue" to "Continue",
        "home" to "Home",
        "missions" to "Missions",
        "ranking" to "Ranking",
        "profile" to "Profile",
        "hello" to "Hello",
        "your_progress" to "YOUR PROGRESS",
        "completed" to "Completed",
        "pending" to "Pending review",
        "remaining" to "Remaining",
        "today_mission" to "Today's mission",
        "view_mission" to "View mission",
        "recent_activity" to "Recent activity",
        "citizen_day" to "Citizen of the Day",
        "citizen_day_empty" to "The daily title is awarded after results are reviewed",
        "all_missions" to "Available missions",
        "personal_mission" to "Suggest your own activity",
        "personal_note" to "Submit your activity. XP is assigned only after review.",
        "task_title" to "Activity title",
        "task_description" to "Short description",
        "send_proposal" to "Submit proposal",
        "duration" to "Duration",
        "reward" to "Reward",
        "after_review" to "After review",
        "submit_proof" to "Submit evidence",
        "photo" to "Add photo",
        "video" to "Add video",
        "proof_selected" to "Evidence selected",
        "send_review" to "Send for review",
        "waiting" to "Pending review",
        "approved" to "Approved",
        "rejected" to "Rejected",
        "top30" to "TOP 30",
        "your_position" to "Your position",
        "rank_today" to "Today",
        "rank_week" to "Week",
        "rank_month" to "Month",
        "rank_year" to "Year",
        "xp" to "XP",
        "level" to "Level",
        "achievements" to "Achievements",
        "history" to "Activity history",
        "joined" to "Member since",
        "settings" to "Settings",
        "privacy" to "Privacy & account",
        "empty_history" to "No approved activity yet",
        "back" to "Back",
        "choose_task" to "Start mission",
        "status_submitted" to "Evidence submitted. In this prototype, status is stored locally.",
        "personal_pending" to "Personal proposal — pending review",
        "invalid_form" to "Enter a valid first name, last name and age.",
        "no_file" to "Choose a photo or video first.",
        "test_disclaimer" to "This is a prototype: accounts, XP and submissions are demo-only and stored on this device."
    )
)

private fun tr(language: String, key: String): String =
    translations[language]?.get(key) ?: translations["tg"]?.get(key) ?: key

@Composable
private fun ShahrvandApp() {
    var language by remember { mutableStateOf("tg") }
    var registered by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedMission by remember { mutableStateOf<Mission?>(null) }
    val missions = remember { mutableStateListOf<Mission>().apply { addAll(initialMissions) } }
    val submittedIds = remember { mutableStateListOf<String>() }
    val personalTitles = remember { mutableStateListOf<String>() }

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
                0 -> HomeScreen(
                    language, firstName, missions, submittedIds,
                    onOpenMission = { selectedMission = it },
                    onOpenMissions = { selectedTab = 1 }
                )
                1 -> MissionsScreen(
                    language, missions, submittedIds,
                    onOpenMission = { selectedMission = it },
                    onAddPersonal = { title, description ->
                        val id = "personal_" + System.currentTimeMillis().toString()
                        missions.add(0, Mission(id, title, description, "Шахсӣ", 0, "Пас аз санҷиш", Icons.Filled.Star, true))
                        personalTitles.add(title)
                    }
                )
                2 -> RankingScreen(language)
                else -> ProfileScreen(
                    language, firstName, lastName, age, avatarUri, submittedIds.size,
                    onLanguage = { language = it }
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
    avatarUri: android.net.Uri?,
    onAvatar: (android.net.Uri?) -> Unit,
    onContinue: () -> Unit
) {
    var error by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onAvatar(it) }
    Column(
        Modifier.fillMaxSize().background(Canvas).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(tr(language, "language"), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            listOf("tg", "ru", "en").forEach { code ->
                Text(
                    code.uppercase(),
                    color = if (language == code) Gold else Muted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onLanguage(code) }.padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(Navy),
            contentAlignment = Alignment.Center
        ) {
            Text("Ш", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text("ШАҲРВАНД", color = Navy, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Text(tr(language, "tagline"), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(26.dp))
        Text(tr(language, "create_profile"), color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "prototype"), color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(20.dp))
        if (avatarUri == null) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(Color.White).border(1.dp, Line, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Person, null, tint = Muted, modifier = Modifier.size(42.dp)) }
        } else {
            AsyncImage(model = avatarUri, contentDescription = null, modifier = Modifier.size(88.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        }
        TextButton(onClick = { picker.launch("image/*") }) { Text(tr(language, "add_photo")) }
        Text(tr(language, "photo_optional"), color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(firstName, onFirstName, label = { Text(tr(language, "first_name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(lastName, onLastName, label = { Text(tr(language, "last_name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            age, onAge, label = { Text(tr(language, "age")) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        if (error) {
            Text(tr(language, "invalid_form"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val parsedAge = age.toIntOrNull()
                error = firstName.isBlank() || lastName.isBlank() || parsedAge == null || parsedAge !in 1..120
                if (!error) onContinue()
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy)
        ) { Text(tr(language, "continue"), fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(16.dp))
        Text(tr(language, "test_disclaimer"), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(20.dp))
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
    val mainMission = missions.firstOrNull { !submittedIds.contains(it.id) } ?: missions.first()
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
        Spacer(Modifier.height(20.dp))
        Text(tr(language, "hello") + ", " + firstName + "!", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "prototype"), color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(tr(language, "your_progress"), color = Color(0xFFDCE6ED), fontSize = 10.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(6.dp))
                Text("2 450 XP", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr(language, "level") + " 3 • Амалгар", color = Color(0xFFE7D4A8), fontSize = 12.sp)
                    Text("65%", color = Color.White, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { 0.65f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Gold, trackColor = NavyLight)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(tr(language, "completed"), "12", Icons.Filled.CheckCircle, Green, Modifier.weight(1f))
            StatCard(tr(language, "pending"), submittedIds.size.toString(), Icons.Filled.AccessTime, Gold, Modifier.weight(1f))
            StatCard(tr(language, "remaining"), (missions.size - submittedIds.size).coerceAtLeast(0).toString(), Icons.Filled.Assignment, NavyLight, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        SectionHeader(tr(language, "today_mission"), tr(language, "view_mission"), onOpenMissions)
        MissionCard(mainMission, language, submittedIds.contains(mainMission.id), onClick = { onOpenMission(mainMission) })
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9EC)), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, null, tint = Gold, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr(language, "citizen_day"), fontWeight = FontWeight.Bold, color = Navy)
                    Text(tr(language, "citizen_day_empty"), color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(tr(language, "recent_activity"), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
        Spacer(Modifier.height(8.dp))
        ActivityRow("Тести дониш", tr(language, "approved"), "+40 XP", Green)
        ActivityRow("2 км роҳгардӣ", tr(language, "waiting"), "—", Gold)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(5.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink)
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
private fun ActivityRow(title: String, status: String, xp: String, tint: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(status, color = Muted, fontSize = 10.sp)
        }
        Text(xp, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    Text(tr(language, "personal_mission"), color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(tr(language, "personal_note"), color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = Navy)
            }
        }
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(missions, key = { it.id }) { mission ->
                MissionCard(mission, language, submittedIds.contains(mission.id), onClick = { onOpenMission(mission) })
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(tr(language, "personal_mission")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr(language, "personal_note"), fontSize = 12.sp, color = Muted)
                    OutlinedTextField(title, { title = it }, label = { Text(tr(language, "task_title")) }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text(tr(language, "task_description")) }, minLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onAddPersonal(title.trim(), description.trim())
                        title = ""
                        description = ""
                        showDialog = false
                    }
                }) { Text(tr(language, "send_proposal")) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(tr(language, "back")) } }
        )
    }
}

@Composable
private fun MissionCard(mission: Mission, language: String, submitted: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF1EEE6)), contentAlignment = Alignment.Center) {
                Icon(mission.icon, null, tint = Navy, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(mission.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(mission.category + " • " + mission.duration, color = Muted, fontSize = 10.sp)
                if (submitted) Text(tr(language, "waiting"), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (mission.personal) tr(language, "after_review") else "+" + mission.xp + " XP", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.ChevronRight, null, tint = Muted)
            }
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
    var proofUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { proofUri = it }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { proofUri = it }

    Column(Modifier.fillMaxSize().background(Canvas).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = tr(language, "back"), tint = Navy) }
            Text(tr(language, "missions"), color = Navy, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Navy).padding(22.dp)) {
            Column {
                Icon(mission.icon, null, tint = Gold, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(12.dp))
                Text(mission.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(mission.category, color = Color(0xFFE7D4A8), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(mission.description, color = Ink, fontSize = 15.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoPill(tr(language, "duration"), mission.duration)
            InfoPill(tr(language, "reward"), if (mission.personal) tr(language, "after_review") else mission.xp.toString() + " XP")
        }
        Spacer(Modifier.height(22.dp))
        Text(tr(language, "submit_proof"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Акс ё видеоеро интихоб кунед, ки иҷрои вазифаро нишон медиҳад.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CameraAlt, null)
                Spacer(Modifier.width(5.dp))
                Text(tr(language, "photo"), fontSize = 11.sp)
            }
            OutlinedButton(onClick = { videoPicker.launch("video/*") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.VideoLibrary, null)
                Spacer(Modifier.width(5.dp))
                Text(tr(language, "video"), fontSize = 11.sp)
            }
        }
        if (proofUri != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F1EB))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Green)
                    Spacer(Modifier.width(8.dp))
                    Text(tr(language, "proof_selected"), color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (alreadySubmitted) {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5DF))) {
                Text(tr(language, "waiting"), color = Gold, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (proofUri == null) message = tr(language, "no_file")
                    else {
                        message = tr(language, "status_submitted")
                        onSubmitted()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                shape = RoundedCornerShape(14.dp)
            ) { Text(tr(language, "send_review"), fontWeight = FontWeight.Bold) }
        }
        if (message != null) Text(message!!, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(20.dp))
        Text(tr(language, "test_disclaimer"), color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun InfoPill(label: String, value: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, color = Muted, fontSize = 10.sp)
            Text(value, color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RankingScreen(language: String) {
    var period by remember { mutableIntStateOf(0) }
    val periodKeys = listOf("rank_today", "rank_week", "rank_month", "rank_year")
    val names = listOf(
        "Фирдавс Саидов", "Меҳрона Раҳмон", "Сомон Каримов", "Далер Набиев", "Мадина Юсуфӣ",
        "Шаҳбоз Алиев", "Фарзона Ҳасанова", "Муҳаммад Назаров", "Ситора Давлатова", "Амир Ҷӯраев",
        "Зуҳро Саидова", "Беҳрӯз Шарифов", "Нозим Раҳимов", "Манижа Абдуллоева", "Фаридун Исмоилов",
        "Меҳроҷ Ҳакимов", "Шабнам Қодирова", "Рустам Сафаров", "Нигина Бобоева", "Сорбон Мирзоев",
        "Мавзуна Раҳматова", "Парвиз Саидов", "Дилноза Олимова", "Фирӯз Шарипов", "Шаҳноз Назарова",
        "Комрон Давлатов", "Сабрина Ҳусейнова", "Абдулло Раҳимов", "Малика Нурова", "Ҷамшед Каримов"
    )
    val multiplier = listOf(1, 7, 30, 365)[period]
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Text(tr(language, "ranking"), color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "top30"), color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            periodKeys.forEachIndexed { index, key ->
                FilterChip(
                    selected = period == index,
                    onClick = { period = index },
                    label = { Text(tr(language, key), fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Navy, selectedLabelColor = Color.White)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            items(names.size) { index ->
                val xp = (520 - index * 11).coerceAtLeast(90) * multiplier
                LeaderRow(Leader(index + 1, names[index], xp))
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr(language, "your_position"), color = Color(0xFFDCE6ED), fontSize = 10.sp)
                    Text("#147  •  Хайём", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("2 450 XP", color = Color(0xFFE7D4A8), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LeaderRow(leader: Leader) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(13.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(if (leader.position <= 3) Color(0xFFFFF1D5) else Color(0xFFF1F2F2)), contentAlignment = Alignment.Center) {
                Text(leader.position.toString(), color = if (leader.position <= 3) Gold else Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(leader.name, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(if (leader.position == 1) "Шаҳрванди фаъол" else "Иштирокчӣ", color = Muted, fontSize = 9.sp)
            }
            Text(leader.xp.toString() + " XP", color = Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileScreen(
    language: String,
    firstName: String,
    lastName: String,
    age: String,
    avatarUri: android.net.Uri?,
    submittedCount: Int,
    onLanguage: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text(tr(language, "profile"), color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                if (avatarUri == null) {
                    Box(Modifier.size(66.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                } else {
                    AsyncImage(model = avatarUri, contentDescription = null, modifier = Modifier.size(66.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text((firstName + " " + lastName).trim(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(age + " • " + tr(language, "level") + " 3", color = Color(0xFFDCE6ED), fontSize = 11.sp)
                    Text("Амалгар", color = Color(0xFFE7D4A8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(tr(language, "xp"), "2 450", Icons.Filled.Star, Gold, Modifier.weight(1f))
            StatCard(tr(language, "completed"), "12", Icons.Filled.CheckCircle, Green, Modifier.weight(1f))
            StatCard(tr(language, "ranking"), "#147", Icons.Filled.EmojiEvents, NavyLight, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text(tr(language, "achievements"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Achievement(Icons.Filled.EmojiEvents, "Шаҳрванди рӯз")
            Achievement(Icons.Filled.Park, "Ҳомии табиат")
            Achievement(Icons.Filled.MenuBook, "Донишҷӯ")
        }
        Spacer(Modifier.height(20.dp))
        Text(tr(language, "history"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(tr(language, "completed") + ": 12", color = Muted, fontSize = 12.sp)
        Text(tr(language, "pending") + ": " + submittedCount, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Text(tr(language, "language"), fontWeight = FontWeight.Bold, color = Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("tg", "ru", "en").forEach { code ->
                FilterChip(selected = language == code, onClick = { onLanguage(code) }, label = { Text(code.uppercase()) })
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, null, tint = Muted)
            Spacer(Modifier.width(8.dp))
            Text(tr(language, "privacy"), color = Ink, fontSize = 13.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text(tr(language, "test_disclaimer"), color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun Achievement(icon: ImageVector, title: String) {
    Card(modifier = Modifier.width(100.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = Ink, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp)
        }
    }
}
