package tj.shahrvand.app

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SLEEP_NAVY: Color @Composable get() = LocalShahrvandPalette.current.navy
private val SLEEP_NAVY_TEXT: Color @Composable get() = LocalShahrvandPalette.current.navyText
private val SLEEP_GOLD: Color @Composable get() = LocalShahrvandPalette.current.gold
private val SLEEP_GREEN: Color @Composable get() = LocalShahrvandPalette.current.green
private val SLEEP_MUTED: Color @Composable get() = LocalShahrvandPalette.current.muted
private val SLEEP_BORDER: Color @Composable get() = LocalShahrvandPalette.current.border

private val SLEEP_ZONE: ZoneId = ZoneId.of("Asia/Dushanbe")

private fun sleepTr(language: String, tg: String, ru: String, en: String): String =
    when (language) { "ru" -> ru; "en" -> en; else -> tg }

private data class SleepNight(
    val wakeDate: LocalDate,
    val start: Instant,
    val end: Instant,
    val minutes: Long,
    val sourcePackage: String,
    val hasStages: Boolean,
    val isManual: Boolean
)

/** Human-readable labels for apps that commonly publish sleep records to Health Connect. */
internal object SleepSourceLabels {
    fun label(packageName: String): String = when (packageName) {
        "com.sec.android.app.shealth" -> "Samsung Health"
        "com.google.android.apps.fitness" -> "Google Fit"
        "com.fitbit.FitbitMobile" -> "Fitbit"
        "com.xiaomi.wearable" -> "Mi Fitness"
        "com.xiaomi.hm.health" -> "Zepp Life"
        "com.huawei.health" -> "Huawei Health"
        "com.garmin.android.apps.connectmobile" -> "Garmin Connect"
        "com.withings.wiscale2" -> "Withings"
        "com.ouraring.oura" -> "Oura"
        "fi.polar.polarflow" -> "Polar Flow"
        else -> packageName.substringAfterLast('.').ifBlank { "Health Connect" }
    }
}


private object SleepHealthConnectRepository {
    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    suspend fun hasPermission(context: Context): Boolean {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return false
        val client = HealthConnectClient.getOrCreate(context)
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun readRecentNights(context: Context): List<SleepNight> {
        check(HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            "Health Connect дастрас нест."
        }
        val client = HealthConnectClient.getOrCreate(context)
        check(client.permissionController.getGrantedPermissions().containsAll(permissions)) {
            "Иҷозати хондани маълумоти хоб дода нашудааст."
        }

        val now = Instant.now()
        val from = now.minus(Duration.ofDays(10))
        val zone = SLEEP_ZONE
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, now)
            )
        )

        val candidates = response.records.mapNotNull { record ->
            if (record.endTime.isAfter(now)) return@mapNotNull null

            // Use the full session interval; summing sleep stages can be shorter.
            val minutes = Duration.between(record.startTime, record.endTime).toMinutes()
            if (minutes !in 120L..960L) return@mapNotNull null

            SleepNight(
                wakeDate = record.endTime.atZone(zone).toLocalDate(),
                start = record.startTime,
                end = record.endTime,
                minutes = minutes,
                sourcePackage = record.metadata.dataOrigin.packageName,
                hasStages = record.stages.isNotEmpty(),
                isManual = record.metadata.recordingMethod == Metadata.RECORDING_METHOD_MANUAL_ENTRY
            )
        }

        // Prefer one longest record per wake-up date to avoid duplicate providers
        // counting the same night's sleep more than once.
        return candidates.groupBy { it.wakeDate }
            .values
            .mapNotNull { sameDate ->
                // Prefer device/app-recorded data over manual estimates. Health Connect
                // is the common Android bridge; the originating app varies by device.
                sameDate.filterNot { it.isManual }
                    .maxByOrNull { it.minutes }
                    ?: sameDate.maxByOrNull { it.minutes }
            }
            .sortedByDescending { it.wakeDate }
    }
}

@Composable
fun SleepTrackingScreen(
    language: String,
    age: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var refreshCounter by remember { mutableIntStateOf(0) }
    // Re-check provider availability after returning from system settings or installing Health Connect.
    val sdkStatus = remember(context, refreshCounter) { HealthConnectClient.getSdkStatus(context) }
    var nights by remember { mutableStateOf<List<SleepNight>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        permissionGranted = SleepHealthConnectRepository.permissions.all { it in granted }
        if (permissionGranted) refreshCounter++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshCounter++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshCounter, sdkStatus) {
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) return@LaunchedEffect
        loading = true
        error = ""
        try {
            permissionGranted = SleepHealthConnectRepository.hasPermission(context)
            nights = if (permissionGranted) {
                SleepHealthConnectRepository.readRecentNights(context)
            } else emptyList()
        } catch (e: Exception) {
            error = e.message ?: sleepTr(language, "Маълумоти хоб гирифта нашуд.", "Не удалось прочитать сон.", "Could not read sleep data.")
        } finally {
            loading = false
        }
    }

    val latest = nights.firstOrNull()
    val targetMinutes = SleepRules.minimumTargetMinutes(age)
    val sleepInsight = remember(nights, targetMinutes) {
        SleepRules.analyzeRecentNights(nights.map { it.minutes }, targetMinutes)
    }

    Column(
        modifier = Modifier.fillMaxSize().shahrvandCanvas().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(sleepTr(language, "САЛОМАТИИ ХОБ", "ЗДОРОВЫЙ СОН", "SLEEP HEALTH"),
                color = SLEEP_GOLD, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(sleepTr(language, "Хоб барои саломатӣ ва барқароршавии организм муҳим аст.", "Сон важен для здоровья и восстановления организма.", "Sleep is essential for health and recovery."),
                color = SLEEP_NAVY_TEXT, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                sleepTr(
                    language,
                    "SHAHRVAND маълумоти хобро мустақиман аз Health Connect мехонад. Маълумот танҳо барои назорати саломатӣ ва барқароршавӣ дар ҳамин дастгоҳ истифода мешавад.",
                    "SHAHRVAND читает данные сна напрямую из Health Connect. Они используются только для контроля здоровья и восстановления на этом устройстве.",
                    "SHAHRVAND reads sleep data directly from Health Connect. It is used only for health and recovery insights on this device."
                ), color = SLEEP_MUTED, fontSize = 12.sp, lineHeight = 18.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = LocalShahrvandPalette.current.surface.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, LocalShahrvandPalette.current.border),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = LocalShahrvandPalette.current.goldSurface) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Bedtime, contentDescription = null,
                            tint = SLEEP_GOLD, modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(sleepTr(language, "Ҳадафи хоб", "Цель сна", "Sleep target"),
                            color = SLEEP_NAVY_TEXT, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                age in 7..12 -> sleepTr(language, "9–12 соат дар шабонарӯз", "9–12 часов в сутки", "9–12 hours per day")
                                age in 13..17 -> sleepTr(language, "8–10 соат дар шабонарӯз", "8–10 часов в сутки", "8–10 hours per day")
                                else -> sleepTr(language, "Ҳадди ақал 7 соат", "Не менее 7 часов", "At least 7 hours")
                            }, color = SLEEP_MUTED, fontSize = 11.sp
                        )
                    }
                }
                HorizontalDivider(color = SLEEP_BORDER)
                when {
                    sdkStatus != HealthConnectClient.SDK_AVAILABLE -> {
                        Text(
                            sleepTr(language,
                                "Health Connect дастрас нест. Онро аз Google Play насб ё навсозӣ кун.",
                                "Health Connect недоступен. Установи или обнови его через Google Play.",
                                "Health Connect is unavailable. Install or update it from Google Play."),
                            color = SLEEP_MUTED, fontSize = 12.sp, lineHeight = 18.sp
                        )
                    }
                    !permissionGranted -> {
                        Text(
                            sleepTr(language,
                                "Барои хондани давомнокии хоб иҷозати READ_SLEEP лозим аст. SHAHRVAND танҳо сабтҳоро мехонад.",
                                "Для чтения продолжительности сна нужно разрешение READ_SLEEP. SHAHRVAND только читает записи.",
                                "The READ_SLEEP permission is needed to read sleep duration. SHAHRVAND only reads records."),
                            color = SLEEP_MUTED, fontSize = 12.sp, lineHeight = 18.sp
                        )
                        Button(onClick = { permissionLauncher.launch(SleepHealthConnectRepository.permissions) },
                            modifier = Modifier.fillMaxWidth()) {
                            Text(sleepTr(language, "Пайваст кардани хоб", "Подключить сон", "Connect sleep"))
                        }
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sleepTr(language, "Health Connect пайваст аст", "Health Connect подключён", "Health Connect connected"),
                                color = SLEEP_GREEN, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { refreshCounter++ }) {
                                androidx.compose.material3.Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text(sleepTr(language, "Навсозӣ", "Обновить", "Refresh"), fontSize = 11.sp)
                            }
                        }
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (nights.isEmpty()) {
                            Text(
                                sleepTr(language,
                                    "Сабти хоб ёфт нашуд. Дар барномаи соат ё тандурустии худ ҳамоҳангсозии Sleep бо Health Connect-ро фаъол кун, иҷозати Sleep-ро деҳ ва боз навсозӣ намо.",
                                    "Запись сна не найдена. В приложении часов или здоровья включи синхронизацию Sleep с Health Connect, разреши доступ к сну и обнови экран.",
                                    "No sleep record found. Enable Sleep sync with Health Connect in your watch or health app, grant sleep access, then refresh."),
                                color = SLEEP_MUTED, fontSize = 12.sp, lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LocalShahrvandPalette.current.dangerSurface),
                border = BorderStroke(1.dp, LocalShahrvandPalette.current.border),
                shape = RoundedCornerShape(19.dp)
            ) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                    modifier = Modifier.padding(14.dp))
            }
        }

        if (permissionGranted && latest != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LocalShahrvandPalette.current.surface.copy(alpha = 0.98f)),
                border = BorderStroke(1.dp, LocalShahrvandPalette.current.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(sleepTr(language, "САБТИ ОХИРИН", "ПОСЛЕДНЯЯ ЗАПИСЬ", "LATEST SLEEP RECORD"),
                        color = SLEEP_GOLD, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        latest.minutes.div(60).toString() + " " + sleepTr(language, "соат", "ч", "h") + " " +
                            (latest.minutes % 60).toString() + " " + sleepTr(language, "дақиқа", "мин", "min"),
                        color = SLEEP_NAVY, fontSize = 30.sp, fontWeight = FontWeight.Black
                    )
                    val dateLabel = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault()).format(latest.wakeDate)
                    val startLabel = DateTimeFormatter.ofPattern("HH:mm").withZone(SLEEP_ZONE).format(latest.start)
                    val endLabel = DateTimeFormatter.ofPattern("HH:mm").withZone(SLEEP_ZONE).format(latest.end)
                    Text(dateLabel + " · " + startLabel + " – " + endLabel, color = SLEEP_MUTED, fontSize = 12.sp)
                    Text(
                        sleepTr(language, "Манба: ", "Источник: ", "Source: ") +
                            SleepSourceLabels.label(latest.sourcePackage),
                        color = SLEEP_MUTED, fontSize = 10.sp
                    )
                    Text(
                        if (latest.hasStages)
                            sleepTr(language, "Давомнокӣ аз марҳилаҳои хоб ҳисоб шудааст.", "Длительность рассчитана по стадиям сна.", "Duration is calculated from sleep stages.")
                        else
                            sleepTr(language, "Марҳилаҳо дастрас нестанд; вақти сессия истифода шуд.", "Стадии недоступны; использовано время сессии.", "Stages are unavailable; session duration is used."),
                        color = SLEEP_MUTED, fontSize = 10.sp
                    )
                    if (latest.isManual) {
                        Text(
                            sleepTr(language,
                                "Ин вақти тахминӣ ё аз тарафи корбар тасдиқшуда аст.",
                                "Это примерное время или время, подтверждённое пользователем.",
                                "This is an estimated or user-confirmed sleep time."
                            ),
                            color = SLEEP_GOLD, fontSize = 10.sp
                        )
                    }
                    HorizontalDivider(color = SLEEP_BORDER)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (latest.minutes >= targetMinutes)
                                sleepTr(language, "Ҳадафи хоб иҷро шуд", "Цель сна достигнута", "Sleep goal reached")
                            else
                                sleepTr(language, "То ҳадаф: ", "До цели: ", "Remaining: ") +
                                    ((targetMinutes - latest.minutes + 59) / 60).toString() +
                                    " " + sleepTr(language, "соат", "ч", "h"),
                            color = if (latest.minutes >= targetMinutes) SLEEP_GREEN else SLEEP_MUTED,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (sleepInsight != null && sleepInsight.pattern != SleepPattern.ADEQUATE) {
            val insight = sleepInsight
            val averageLabel = "" + (insight.averageMinutes / 60L) + " " +
                sleepTr(language, "соат", "ч", "h") + " " +
                (insight.averageMinutes % 60L) + " " +
                sleepTr(language, "дақ", "мин", "min")
            val title = when (insight.pattern) {
                SleepPattern.REPEATED_SHORT_SLEEP -> sleepTr(
                    language,
                    "КАМХОБИИ ТАКРОРШАВАНДА",
                    "ПОВТОРЯЮЩИЙСЯ НЕДОСЫП",
                    "REPEATED SHORT SLEEP"
                )
                SleepPattern.LAST_NIGHT_SHORT -> sleepTr(
                    language,
                    "ХОБИ ШАБИ ОХИР КАМ БУД",
                    "ПОСЛЕДНЯЯ НОЧЬ БЫЛА КОРОТКОЙ",
                    "LAST NIGHT'S SLEEP WAS SHORT"
                )
                SleepPattern.MIXED_RECENT_SLEEP -> sleepTr(
                    language,
                    "РЕҶАИ ХОБ НОМУНТАЗАМ АСТ",
                    "РЕЖИМ СНА НЕСТАБИЛЕН",
                    "RECENT SLEEP IS INCONSISTENT"
                )
                SleepPattern.ADEQUATE -> ""
            }
            val summary = when (insight.pattern) {
                SleepPattern.REPEATED_SHORT_SLEEP -> sleepTr(
                    language,
                    "Дар 7 сабти охир ${insight.belowTargetCount} аз ${insight.recentNightCount} шаб аз ҳадафи синну сол камтар хоб рафтӣ. Давомнокии миёна: ",
                    "В последних 7 записях ${insight.belowTargetCount} из ${insight.recentNightCount} ночей были короче возрастной цели. Средняя длительность: ",
                    "In the latest 7 records, ${insight.belowTargetCount} of ${insight.recentNightCount} nights were below your age-based target. Average duration: "
                ) + averageLabel + "."
                SleepPattern.LAST_NIGHT_SHORT -> sleepTr(
                    language,
                    "Сабти охирин аз ҳадафи синну сол камтар аст. Аз рӯи сабтҳои дастрас ҳоло камхобии такроршаванда муайян нашудааст.",
                    "Последняя запись ниже возрастной цели. По доступным записям повторяющийся недосып пока не выявлен.",
                    "The latest record is below your age-based target. The available records do not yet show a repeated pattern."
                )
                SleepPattern.MIXED_RECENT_SLEEP -> sleepTr(
                    language,
                    "Шаби охир ба ҳадаф расид, аммо дар ${insight.belowTargetCount} аз ${insight.recentNightCount} сабти охир хоб кам буд. Давомнокии миёна: ",
                    "Последняя ночь достигла цели, но в ${insight.belowTargetCount} из ${insight.recentNightCount} последних записей сна было недостаточно. Средняя длительность: ",
                    "The latest night met the target, but ${insight.belowTargetCount} of the latest ${insight.recentNightCount} records were short. Average duration: "
                ) + averageLabel + "."
                SleepPattern.ADEQUATE -> ""
            }
            val tips = when (insight.pattern) {
                SleepPattern.REPEATED_SHORT_SLEEP -> listOf(
                    sleepTr(language,
                        "Барои 7 шаби оянда вақти кофии хобро пешакӣ дар тақвим ҷудо кун; вақти бедоршавиро ҳар рӯз тақрибан яксон нигоҳ дор.",
                        "На ближайшие 7 ночей заранее выдели достаточно времени для сна и старайся вставать примерно в одно время.",
                        "For the next 7 nights, protect enough time for sleep and keep your wake-up time consistent."),
                    sleepTr(language,
                        "Қаҳва, чойи сиёҳи сахт ва энергетикро дар 8 соати пеш аз хоб истеъмол накун; кофеин метавонад таъсири дароз дошта бошад.",
                        "Не употребляй кофе, крепкий чёрный чай и энергетики в течение 8 часов перед сном: действие кофеина может быть длительным.",
                        "Avoid coffee, strong black tea and energy drinks during the 8 hours before bedtime; caffeine can have lasting effects."),
                    sleepTr(language,
                        "30–60 дақиқа пеш аз хоб телефонро канор гузор, равшаниро кам кун ва кори ором интихоб намо.",
                        "За 30–60 минут до сна отложи телефон, приглуши свет и выбери спокойное занятие.",
                        "Put your phone away, dim the lights and choose a quiet activity 30–60 minutes before bed."),
                    sleepTr(language,
                        "Ҷои хобро ором, торик ва каме салқин нигоҳ дор; рӯзона дар рӯшноии табиӣ сайр кун.",
                        "Сделай спальню тихой, тёмной и прохладной; днём выходи на естественный свет.",
                        "Keep the bedroom quiet, dark and comfortably cool; get natural daylight during the day.")
                )
                SleepPattern.LAST_NIGHT_SHORT -> buildList {
                    add(sleepTr(language,
                        "Имрӯз вақти иловагӣ барои хоби шаби оянда ҷудо кун; кӯшиш кун вақти бедоршавӣ доимӣ монад.",
                        "Сегодня заранее выдели больше времени для сна следующей ночью и сохрани привычное время подъёма.",
                        "Set aside enough opportunity for a longer night of sleep, while keeping your usual wake-up time."))
                    add(sleepTr(language,
                        "Агар хоболуд бошӣ, ронандагӣ ё кори хатарнокро анҷом надеҳ; аввал бехатариро интихоб кун.",
                        "При сонливости не садись за руль и не выполняй опасную работу; сначала позаботься о безопасности.",
                        "Do not drive or do hazardous tasks when sleepy; put safety first."))
                    if (age >= 18) add(sleepTr(language,
                        "Агар рӯзона сахт хоболуд бошӣ, хоби кӯтоҳи то 20 дақиқа дар аввали нисфирӯзӣ метавонад ҳушёриро беҳтар кунад; дер хоб накун.",
                        "Если днём сильно хочется спать, короткий сон до 20 минут в первой половине дня может повысить бодрость; не спи поздно.",
                        "If needed, an early-afternoon nap of up to 20 minutes may improve alertness; avoid napping late."))
                    add(sleepTr(language,
                        "Чойи сиёҳи сахт, қаҳва ва энергетикро наздики вақти хоб нанӯш.",
                        "Не пей крепкий чёрный чай, кофе и энергетики близко ко времени сна.",
                        "Avoid strong tea, coffee and energy drinks close to bedtime."))
                }
                SleepPattern.MIXED_RECENT_SLEEP -> listOf(
                    sleepTr(language,
                        "Шаби охир хубтар буд — ҳамин реҷаро нигоҳ дор: вақти хоб ва бедоршавиро ҳар рӯз тақрибан яксон интихоб кун.",
                        "Последняя ночь была лучше — закрепи результат: ложись и вставай примерно в одно время каждый день.",
                        "The latest night was better. Build on it by keeping bedtime and wake time consistent."),
                    sleepTr(language,
                        "Рӯзҳои дарсӣ ё кориро пешакӣ ба нақша гир, то вазифаҳо вақти хобро кам накунанд.",
                        "Планируй учёбу и работу заранее, чтобы дела не сокращали время сна.",
                        "Plan study and work ahead so they do not keep cutting into your sleep window."),
                    sleepTr(language,
                        "Пеш аз хоб экран ва равшании зиёдро кам кун; чойи сахт ва кофеинро дер нанӯш.",
                        "Перед сном сократи экранное время и яркий свет; не пей крепкий чай и кофеин поздно.",
                        "Reduce screen time and bright light before bed; avoid late caffeine.")
                )
                SleepPattern.ADEQUATE -> emptyList()
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = LocalShahrvandPalette.current.goldSurface),
                border = BorderStroke(1.dp, SLEEP_BORDER),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(title, color = SLEEP_NAVY_TEXT, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Text(summary, color = SLEEP_NAVY_TEXT, fontSize = 12.sp, lineHeight = 18.sp)
                    Text(
                        sleepTr(language, "ПЕШНИҲОДҲОИ МУВОФИҚ БА САБТҲОИ ТУ", "РЕКОМЕНДАЦИИ ПО ТВОИМ ЗАПИСЯМ", "RECOMMENDATIONS BASED ON YOUR RECORDS"),
                        color = SLEEP_GOLD, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp
                    )
                    tips.forEach { tip ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Text("•", color = SLEEP_GOLD, fontWeight = FontWeight.Black)
                            Text(tip, color = SLEEP_NAVY_TEXT, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                    Text(
                        sleepTr(language,
                            "Агар камхобӣ чанд ҳафта давом кунад, ё бо вуҷуди вақти кофӣ хоб рафтан рӯзона сахт хоболуд бошӣ, ба духтур муроҷиат кун. Агар нишонаҳо ногаҳон сахт бошанд, зудтар ёрии тиббӣ гир.",
                            "Если недосып продолжается несколько недель или сильная сонливость сохраняется даже при достаточном времени для сна, обратись к врачу. При внезапных выраженных симптомах обратись за медицинской помощью раньше.",
                            "If short sleep continues for several weeks, or you remain very sleepy despite allowing enough time for sleep, consult a clinician. Seek care sooner for sudden or severe symptoms."
                        ), color = SLEEP_MUTED, fontSize = 10.sp, lineHeight = 15.sp
                    )
                    Text(
                        sleepTr(language,
                            "Ин таҳлил танҳо давомнокии сабтшудаи хобро истифода мебарад; он сифати хоб ё сабаби камхобиро муайян намекунад.",
                            "Анализ учитывает только записанную продолжительность сна; он не определяет качество сна или причину недосыпа.",
                            "This analysis uses recorded sleep duration only; it does not determine sleep quality or the cause of short sleep."
                        ), color = SLEEP_MUTED, fontSize = 10.sp, lineHeight = 15.sp
                    )
                }
            }
        }

        Text(
            sleepTr(language,
                "Маълумоти хоб метавонад бо таъхир ба Health Connect ҳамоҳанг шавад. Барномаи манбаи хобро кушо, баъд ин саҳифаро навсозӣ кун. Маълумоти хоб барои назорати саломатӣ истифода мешавад.",
                "Данные сна могут поступать в Health Connect с задержкой. Открой приложение-источник сна, затем обнови эту страницу. Данные сна нужны для контроля здоровья.",
                "Sleep data may sync to Health Connect with a delay. Open the app that records your sleep, then refresh this screen. Sleep data is used for health insights."
            ), color = SLEEP_MUTED, fontSize = 10.sp, lineHeight = 15.sp
        )
    }
}
