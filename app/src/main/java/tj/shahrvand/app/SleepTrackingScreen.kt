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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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


internal object SupabaseSleepRepository {
    private val base get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val key get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    suspend fun award(
        token: String,
        wakeDate: String,
        startedAt: Instant,
        endedAt: Instant,
        sourcePackage: String,
        isManual: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val body = JSONObject()
                .put("p_wake_date", wakeDate)
                .put("p_started_at", startedAt.toString())
                .put("p_ended_at", endedAt.toString())
                .put("p_source_package", sourcePackage)
                .put("p_is_manual", isManual)
            connection = (URL("$base/rest/v1/rpc/shahrvand_award_sleep_xp").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("apikey", key)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(raw).optString("message") }.getOrNull().orEmpty()
                throw IllegalStateException(message.ifBlank { "HTTP $code" })
            }
            val result = JSONObject(raw)
            result.optBoolean("ok") &&
                result.optBoolean("qualified") &&
                !result.optBoolean("already_awarded") &&
                result.optInt("xp_awarded") == SleepRules.REWARD_XP
        } finally {
            connection?.disconnect()
        }
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

            // The server validates the full recorded session interval (end - start).
            // Use the same duration here; summing sleep stages can be shorter and
            // incorrectly prevent an otherwise eligible night from reaching the XP RPC.
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
                // Prefer an automatic Samsung Health record over a longer manual estimate.
                sameDate.filterNot { it.isManual || it.sourcePackage != "com.sec.android.app.shealth" }
                    .maxByOrNull { it.minutes }
                    ?: sameDate.maxByOrNull { it.minutes }
            }
            .sortedByDescending { it.wakeDate }
    }
}

@Composable
fun SleepTrackingScreen(
    language: String,
    age: Int,
    onReward: suspend (date: String, startedAt: Instant, endedAt: Instant, sourcePackage: String, isManual: Boolean) -> Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sdkStatus = remember { HealthConnectClient.getSdkStatus(context) }
    var permissionGranted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var refreshCounter by remember { mutableIntStateOf(0) }
    var nights by remember { mutableStateOf<List<SleepNight>>(emptyList()) }
    var rewardMessage by remember { mutableStateOf("") }

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

    val today = remember { LocalDate.now(SLEEP_ZONE) }
    val latest = nights.firstOrNull()
    val todaysNight = nights.firstOrNull { it.wakeDate == today }
    val targetMinutes = SleepRules.minimumTargetMinutes(age)
    val durationQualifies = todaysNight?.let { SleepRules.qualifies(age, it.minutes) } == true
    val qualifies = todaysNight?.let {
        !it.isManual && it.sourcePackage == "com.sec.android.app.shealth" &&
            SleepRules.qualifies(age, it.minutes)
    } == true

    LaunchedEffect(todaysNight?.wakeDate, todaysNight?.minutes, todaysNight?.sourcePackage, todaysNight?.isManual, qualifies) {
        rewardMessage = ""
        if (todaysNight != null && durationQualifies && !qualifies) {
            rewardMessage = when {
                todaysNight.isManual -> sleepTr(language,
                    "Сабти дастӣ барои гирифтани XP қабул намешавад.",
                    "Ручная запись не подходит для начисления XP.",
                    "Manual sleep entries are not eligible for XP.")
                else -> sleepTr(language,
                    "Барои XP сабти автоматии Samsung Health лозим аст.",
                    "Для XP нужна автоматическая запись Samsung Health.",
                    "An automatic Samsung Health record is required for XP.")
            }
        } else if (qualifies && todaysNight != null) {
            try {
                rewardMessage = if (onReward(
                    todaysNight.wakeDate.toString(),
                    todaysNight.start,
                    todaysNight.end,
                    todaysNight.sourcePackage,
                    todaysNight.isManual
                )) {
                    sleepTr(language, "Офарин! +15 XP барои хоби кофӣ.", "Отлично! +15 XP за достаточный сон.", "Great! +15 XP for sufficient sleep.")
                } else {
                    sleepTr(language, "Холи ин шаб аллакай ҳисоб шудааст.", "Очки за эту ночь уже начислены.", "This night's points have already been awarded.")
                }
            } catch (e: Exception) {
                error = e.message ?: sleepTr(language,
                    "Сервер XP-ро тасдиқ накард. Баъдтар навсозӣ кун.",
                    "Сервер не подтвердил XP. Повтори позже.",
                    "The server could not verify XP. Try refreshing later.")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(LocalShahrvandPalette.current.background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(sleepTr(language, "ОДАТИ ХОБ", "ПРИВЫЧКА СНА", "SLEEP HABIT"),
                color = SLEEP_GOLD, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(sleepTr(language, "Хоби кофӣ — пешрафти ҳаррӯза.", "Достаточный сон — ежедневный прогресс.", "Enough sleep is daily progress."),
                color = SLEEP_NAVY_TEXT, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                sleepTr(
                    language,
                    "SHAHRVAND сабти хобро аз Health Connect мехонад. Барои ҳисоб кардани XP сана, вақти оғозу анҷом ва манбаи сабт ба сервер фиристода мешаванд; худи сабти Health Connect нусхабардорӣ намешавад.",
                    "SHAHRVAND читает записи сна через Health Connect. Для начисления XP на сервер отправляются дата, время начала и окончания, а также источник записи; сама запись Health Connect не копируется.",
                    "SHAHRVAND reads sleep records through Health Connect. To award XP, the date, start and end times, and record source are sent to the server; the Health Connect record itself is not copied."
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
                    Text("+15 XP", color = SLEEP_GOLD, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                HorizontalDivider(color = SLEEP_BORDER)
                when {
                    sdkStatus != HealthConnectClient.SDK_AVAILABLE -> {
                        Text(
                            sleepTr(language,
                                "Health Connect дастрас нест. Онро дар телефон насб ё навсозӣ кун ва Samsung Health-ро низ навсозӣ намо.",
                                "Health Connect недоступен. Установи или обнови его на телефоне и обнови Samsung Health.",
                                "Health Connect is unavailable. Install or update it on your phone and update Samsung Health."),
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
                                    "Сабти хоб ёфт нашуд. Дар Samsung Health ба Settings → Health Connect даромада, дастрасии Sleep-ро фаъол кун. Баъд саҳифаро навсозӣ намо.",
                                    "Запись сна не найдена. В Samsung Health открой Settings → Health Connect и разреши доступ к Sleep. Затем обнови экран.",
                                    "No sleep record found. In Samsung Health, open Settings → Health Connect and enable Sleep sharing, then refresh this screen."),
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
                            if (latest.sourcePackage == "com.sec.android.app.shealth") "Samsung Health"
                            else latest.sourcePackage.substringAfterLast('.').ifBlank { "Health Connect" },
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
                        if (latest.wakeDate == today && qualifies) {
                            Text("+15 XP", color = SLEEP_GREEN, fontWeight = FontWeight.Black)
                        }
                    }
                    if (rewardMessage.isNotBlank()) {
                        Text(rewardMessage, color = SLEEP_GREEN, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Text(
            sleepTr(language,
                "Маълумоти хоб метавонад дертар ба Health Connect ҳамоҳанг шавад. Баъди кушодани Samsung Health ин саҳифаро навсозӣ кун. XP-и хоб ба рейтинги ҳафтаинаи серверӣ низ дохил мешавад.",
                "Данные сна могут синхронизироваться с Health Connect с задержкой. После открытия Samsung Health обнови эту страницу. Очки за сон также входят в недельный серверный рейтинг.",
                "Sleep data may sync to Health Connect with a delay. After opening Samsung Health, refresh this page. Sleep XP is also included in the server weekly leaderboard."
            ), color = SLEEP_MUTED, fontSize = 10.sp, lineHeight = 15.sp
        )
    }
}
