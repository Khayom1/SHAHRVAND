package tj.shahrvand.app

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DASHBOARD_ZONE: ZoneId = ZoneId.of("Asia/Dushanbe")
private const val STEP_GOAL = 6000L
private const val ACTIVE_MINUTE_GOAL = 30L
private const val ACTIVE_CALORIE_GOAL = 300.0

private fun dashboardTr(language: String, tg: String, ru: String, en: String): String =
    when (language) {
        "ru" -> ru
        "en" -> en
        else -> tg
    }

internal data class DailyHealthActivity(
    val date: LocalDate,
    val steps: Long,
    val activeCalories: Double,
    val exerciseMinutes: Long
)

internal data class HealthActivityWeek(
    val supported: Boolean,
    val permissionsGranted: Boolean,
    val days: List<DailyHealthActivity>
) {
    val today: DailyHealthActivity?
        get() = days.lastOrNull()
}

internal object HealthConnectDashboardRepository {
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    fun isSupported(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermissions(context: Context): Boolean {
        if (!isSupported(context)) return false
        val client = HealthConnectClient.getOrCreate(context)
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun readWeek(context: Context): HealthActivityWeek = withContext(Dispatchers.IO) {
        if (!isSupported(context)) return@withContext HealthActivityWeek(false, false, emptyList())
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.containsAll(permissions)) {
            return@withContext HealthActivityWeek(true, false, emptyList())
        }

        val now = Instant.now()
        val today = now.atZone(DASHBOARD_ZONE).toLocalDate()
        val startDate = today.minusDays(6)
        val start = startDate.atStartOfDay(DASHBOARD_ZONE).toInstant()
        val range = TimeRangeFilter.between(start, now)

        val stepRecords = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = range,
                pageSize = 1000
            )
        ).records
        val calorieRecords = client.readRecords(
            ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = range,
                pageSize = 1000
            )
        ).records
        val exerciseRecords = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = range,
                pageSize = 1000
            )
        ).records

        val stepsByDay = stepRecords.groupBy { it.endTime.atZone(DASHBOARD_ZONE).toLocalDate() }
            .mapValues { (_, records) -> records.sumOf { it.count } }
        val caloriesByDay = calorieRecords.groupBy { it.endTime.atZone(DASHBOARD_ZONE).toLocalDate() }
            .mapValues { (_, records) -> records.sumOf { it.energy.inKilocalories } }
        val minutesByDay = exerciseRecords.groupBy { it.endTime.atZone(DASHBOARD_ZONE).toLocalDate() }
            .mapValues { (_, records) ->
                records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes().coerceAtLeast(0L) }
            }

        val days = (0L..6L).map { offset ->
            val date = startDate.plusDays(offset)
            DailyHealthActivity(
                date = date,
                steps = stepsByDay[date] ?: 0L,
                activeCalories = caloriesByDay[date] ?: 0.0,
                exerciseMinutes = minutesByDay[date] ?: 0L
            )
        }
        HealthActivityWeek(true, true, days)
    }
}

@Composable
internal fun HealthActivityDashboard(
    language: String,
    onOpenActivity: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalShahrvandPalette.current
    val supported = remember { HealthConnectDashboardRepository.isSupported(context) }
    var permissionGranted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var week by remember { mutableStateOf<HealthActivityWeek?>(null) }
    var error by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        permissionGranted = HealthConnectDashboardRepository.permissions.all { it in granted }
        if (permissionGranted) refresh++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refresh, supported) {
        if (!supported) return@LaunchedEffect
        loading = true
        error = ""
        try {
            permissionGranted = HealthConnectDashboardRepository.hasPermissions(context)
            week = if (permissionGranted) HealthConnectDashboardRepository.readWeek(context) else null
        } catch (e: Exception) {
            error = e.message ?: dashboardTr(language, "Маълумоти фаъолият дастрас нест.", "Данные активности недоступны.", "Activity data is unavailable.")
        } finally {
            loading = false
        }
    }

    val today = week?.today
    val steps = today?.steps ?: 0L
    val activeCalories = today?.activeCalories ?: 0.0
    val activeMinutes = today?.exerciseMinutes ?: 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        dashboardTr(language, "ФАЪОЛИЯТИ ИМРӮЗА", "АКТИВНОСТЬ СЕГОДНЯ", "TODAY'S ACTIVITY"),
                        color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        dashboardTr(language, "Ҳаракати ҳаррӯза", "Ежедневная активность", "Daily activity"),
                        color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold
                    )
                }
                Surface(color = palette.surfaceVariant, shape = RoundedCornerShape(11.dp)) {
                    Text(
                        dashboardTr(language, "7 рӯз", "7 дней", "7 days"),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        color = palette.navy, fontSize = 9.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!supported) {
                Text(
                    dashboardTr(language, "Health Connect дар ин дастгоҳ дастрас нест.", "Health Connect недоступен на этом устройстве.", "Health Connect is unavailable on this device."),
                    color = palette.muted, fontSize = 11.sp
                )
            } else if (!permissionGranted) {
                Text(
                    dashboardTr(
                        language,
                        "Барои нишон додани қадамҳо, калорияҳои фаъол ва вақти машқ иҷозати Health Connect лозим аст. Маълумот танҳо хонда мешавад.",
                        "Для отображения шагов, активных калорий и времени тренировок нужен доступ Health Connect. Данные доступны только для чтения.",
                        "Allow Health Connect to read steps, active calories and exercise time. SHAHRVAND only reads this data."
                    ),
                    color = palette.muted, fontSize = 10.sp, lineHeight = 15.sp
                )
                Button(
                    onClick = { permissionLauncher.launch(HealthConnectDashboardRepository.permissions) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(dashboardTr(language, "Пайваст кардани Health Connect", "Подключить Health Connect", "Connect Health Connect"))
                }
            } else if (loading) {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            } else if (week != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActivityGoalRing(
                        modifier = Modifier.weight(1f),
                        value = steps.toDouble(),
                        target = STEP_GOAL.toDouble(),
                        displayValue = String.format(Locale.US, "%,d", steps),
                        label = dashboardTr(language, "Қадам", "Шаги", "Steps"),
                        color = palette.green
                    )
                    ActivityGoalRing(
                        modifier = Modifier.weight(1f),
                        value = activeMinutes.toDouble(),
                        target = ACTIVE_MINUTE_GOAL.toDouble(),
                        displayValue = activeMinutes.toString(),
                        label = dashboardTr(language, "Дақиқа", "Минуты", "Minutes"),
                        color = palette.navyLight
                    )
                    ActivityGoalRing(
                        modifier = Modifier.weight(1f),
                        value = activeCalories,
                        target = ACTIVE_CALORIE_GOAL,
                        displayValue = String.format(Locale.US, "%.0f", activeCalories),
                        label = dashboardTr(language, "ккал", "ккал", "kcal"),
                        color = palette.gold
                    )
                }

                Text(
                    dashboardTr(language, "ФАЪОЛИЯТИ 7 РӮЗИ ОХИР", "АКТИВНОСТЬ ЗА 7 ДНЕЙ", "LAST 7 DAYS"),
                    color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                )
                WeeklyStepsChart(language, week.days, palette)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dashboardTr(language, "Таърихи машқҳо ва масофа", "Тренировки и дистанция", "Workouts and distance"),
                        modifier = Modifier.weight(1f),
                        color = palette.ink, fontSize = 10.sp
                    )
                    androidx.compose.material3.TextButton(onClick = onOpenActivity) {
                        Text(dashboardTr(language, "Кушодан", "Открыть", "Open"), fontSize = 10.sp)
                    }
                }
            } else {
                Text(
                    error.ifBlank { dashboardTr(language, "Маълумот ҳоло дастрас нест.", "Данные пока недоступны.", "Data is not available yet.") },
                    color = palette.muted, fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ActivityGoalRing(
    modifier: Modifier,
    value: Double,
    target: Double,
    displayValue: String,
    label: String,
    color: Color
) {
    val palette = LocalShahrvandPalette.current
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (target <= 0.0) 0f else (value / target).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.size(68.dp),
                color = color,
                trackColor = palette.surfaceVariant,
                strokeWidth = 6.dp
            )
            Text(
                when {
                    target <= 0.0 -> "—"
                    value >= target -> "✓"
                    else -> String.format(Locale.US, "%.0f%%", value / target * 100.0)
                },
                color = palette.ink, fontSize = 10.sp, fontWeight = FontWeight.Bold
            )
        }
        Text(displayValue, color = palette.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = palette.muted, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun WeeklyStepsChart(
    language: String,
    days: List<DailyHealthActivity>,
    palette: ShahrvandPalette
) {
    val maxSteps = maxOf(STEP_GOAL, days.maxOfOrNull { it.steps } ?: STEP_GOAL).toFloat()
    Row(
        Modifier.fillMaxWidth().height(94.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, day ->
            val barHeight = (day.steps / maxSteps * 65f).coerceIn(3f, 65f)
            val dayLabel = day.date.dayOfWeek.getDisplayName(
                TextStyle.SHORT,
                when (language) {
                    "ru" -> Locale("ru")
                    "en" -> Locale.ENGLISH
                    else -> Locale("tg", "TJ")
                }
            ).take(2)
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    Modifier.fillMaxWidth().height(68.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier.fillMaxWidth(0.58f).height(barHeight.dp)
                            .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                            .background(if (index == days.lastIndex) palette.green else palette.navyLight.copy(alpha = 0.55f))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(dayLabel, color = palette.muted, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}
