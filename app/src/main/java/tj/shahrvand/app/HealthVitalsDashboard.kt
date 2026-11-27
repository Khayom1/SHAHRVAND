package tj.shahrvand.app

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val VITALS_ZONE = ZoneId.of("Asia/Dushanbe")

private fun vitalsTr(language: String, tg: String, ru: String, en: String): String =
    when (language) { "ru" -> ru; "en" -> en; else -> tg }

private data class HealthVitalsSnapshot(
    val restingHeartRate: Long?,
    val bloodPressure: Pair<Double, Double>?,
    val oxygenSaturation: Double?,
    val latestTime: Instant?
)

private object HealthVitalsRepository {
    val permissions = setOf(
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    )

    fun isSupported(context: Context) =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermissions(context: Context): Boolean {
        if (!isSupported(context)) return false
        val client = HealthConnectClient.getOrCreate(context)
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun load(context: Context): HealthVitalsSnapshot {
        val client = HealthConnectClient.getOrCreate(context)
        val now = Instant.now()
        val range = TimeRangeFilter.between(now.minus(Duration.ofDays(30)), now)
        val heart = client.readRecords(
            ReadRecordsRequest(RestingHeartRateRecord::class, range, pageSize = 100)
        ).records.maxByOrNull { it.time }
        val pressure = client.readRecords(
            ReadRecordsRequest(BloodPressureRecord::class, range, pageSize = 100)
        ).records.maxByOrNull { it.time }
        val oxygen = client.readRecords(
            ReadRecordsRequest(OxygenSaturationRecord::class, range, pageSize = 100)
        ).records.maxByOrNull { it.time }

        val times = listOfNotNull(heart?.time, pressure?.time, oxygen?.time)
        return HealthVitalsSnapshot(
            restingHeartRate = heart?.beatsPerMinute,
            bloodPressure = pressure?.let {
                it.systolic.inMillimetersOfMercury to it.diastolic.inMillimetersOfMercury
            },
            oxygenSaturation = oxygen?.percentage?.value,
            latestTime = times.maxOrNull()
        )
    }
}

@Composable
internal fun HealthVitalsDashboardCard(language: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalShahrvandPalette.current
    val supported = remember { HealthVitalsRepository.isSupported(context) }
    var granted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<HealthVitalsSnapshot?>(null) }
    var error by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { result ->
        granted = HealthVitalsRepository.permissions.all { it in result }
        if (granted) refresh++
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
            granted = HealthVitalsRepository.hasPermissions(context)
            snapshot = if (granted) HealthVitalsRepository.load(context) else null
        } catch (e: Exception) {
            error = e.message ?: vitalsTr(language, "Маълумоти нишондиҳандаҳо дастрас нест.", "Данные показателей недоступны.", "Vital data is unavailable.")
        } finally {
            loading = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    vitalsTr(language, "НИШОНДИҲАНДАҲОИ ҶИСМ", "ПОКАЗАТЕЛИ ОРГАНИЗМА", "BODY VITALS"),
                    color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(
                    vitalsTr(language, "Нишондиҳандаҳои охирин", "Последние показатели", "Latest measurements"),
                    color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
            when {
                !supported -> Text(
                    vitalsTr(language, "Health Connect дар ин дастгоҳ дастрас нест.", "Health Connect недоступен на этом устройстве.", "Health Connect is unavailable on this device."),
                    color = palette.muted, fontSize = 10.sp
                )
                !granted -> {
                    Text(
                        vitalsTr(
                            language,
                            "Набзи оромӣ, фишори хун ва SpO₂ танҳо аз дастгоҳи ченкунанда ё сабти воқеии пайвастшуда хонда мешаванд.",
                            "Пульс в покое, давление и SpO₂ считываются только с подключённого измерительного устройства или реальной записи.",
                            "Resting heart rate, blood pressure and SpO₂ are read only from a connected measurement device or an actual recorded reading."
                        ),
                        color = palette.muted, fontSize = 10.sp, lineHeight = 15.sp
                    )
                    Button(
                        onClick = { permissionLauncher.launch(HealthVitalsRepository.permissions) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text(vitalsTr(language, "Пайваст кардани нишондиҳандаҳо", "Подключить показатели", "Connect vitals"))
                    }
                }
                loading -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                snapshot != null -> {
                    val data = snapshot!!
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VitalMetric(
                            Modifier.weight(1f),
                            data.restingHeartRate?.toString() ?: "—",
                            "bpm",
                            vitalsTr(language, "Набзи оромӣ", "Пульс покоя", "Resting HR"),
                            palette
                        )
                        VitalMetric(
                            Modifier.weight(1f),
                            data.bloodPressure?.let { "${it.first.toInt()}/${it.second.toInt()}" } ?: "—",
                            "mmHg",
                            vitalsTr(language, "Фишори хун", "Давление", "Blood pressure"),
                            palette
                        )
                        VitalMetric(
                            Modifier.weight(1f),
                            data.oxygenSaturation?.let { String.format(Locale.US, "%.0f", it) } ?: "—",
                            "%",
                            "SpO₂",
                            palette
                        )
                    }
                    if (data.latestTime != null) {
                        Text(
                            vitalsTr(language, "Маълумоти воқеии Health Connect · ", "Реальные данные Health Connect · ", "Recorded in Health Connect · ") +
                                data.latestTime.atZone(VITALS_ZONE).format(DateTimeFormatter.ofPattern("dd.MM · HH:mm")),
                            color = palette.muted, fontSize = 9.sp
                        )
                    } else {
                        Text(
                            vitalsTr(language, "Ҳоло ченкунии пайвастшуда нест. Рақами сохта нишон дода намешавад.", "Подключённых измерений пока нет. Вымышленные значения не показываются.", "No connected measurements yet. No synthetic values are shown."),
                            color = palette.muted, fontSize = 9.sp
                        )
                    }
                }
                else -> Text(error.ifBlank { vitalsTr(language, "Маълумот ҳоло дастрас нест.", "Данные пока недоступны.", "Data is not available yet.") }, color = palette.muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun VitalMetric(
    modifier: Modifier,
    value: String,
    unit: String,
    label: String,
    palette: ShahrvandPalette
) {
    Surface(modifier, color = palette.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(value, color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(unit, color = palette.muted, fontSize = 7.sp)
            }
            Text(label, color = palette.muted, fontSize = 8.sp, maxLines = 2)
        }
    }
}
