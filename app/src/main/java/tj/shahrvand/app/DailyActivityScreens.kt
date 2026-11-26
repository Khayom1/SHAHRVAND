package tj.shahrvand.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Executors
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val ACTIVITY_ZONE = "Asia/Dushanbe"
private val A_Navy = androidx.compose.ui.graphics.Color(0xFF183247)
private val A_Gold = androidx.compose.ui.graphics.Color(0xFFC8954A)
private val A_Green = androidx.compose.ui.graphics.Color(0xFF2F7969)
private val A_Muted = androidx.compose.ui.graphics.Color(0xFF687987)
private val A_Border = androidx.compose.ui.graphics.Color(0xFFE0E7EC)

private fun aTr(l: String, tg: String, ru: String, en: String) = when (l) { "ru" -> ru; "en" -> en; else -> tg }

data class ActivityTask(
    val id: String, val type: String,
    val titleTg: String, val titleRu: String, val titleEn: String,
    val descriptionTg: String, val descriptionRu: String, val descriptionEn: String,
    val distanceM: Int, val reps: Int, val limitSec: Int, val minDurationSec: Int, val xp: Int
) {
    fun title(l: String) = aTr(l, titleTg, titleRu, titleEn)
    fun description(l: String) = aTr(l, descriptionTg, descriptionRu, descriptionEn)
}
data class ActivitySession(val id: String, val nonce: String, val startedAt: String, val expiresAt: String)
data class ActivityOverview(val tasks: List<ActivityTask>, val completed: Map<String, Int>, val interests: Set<String>)
data class PoseMetrics(val reps: Int = 0, val frames: Int = 0, val visibleRatio: Double = 0.0, val confidence: Double = 0.0)

object DailyActivityRepository {
    private val base get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val key get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private suspend fun request(path: String, method: String, token: String, body: JSONObject? = null): String = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Сессия ба охир расид. Дубора ворид шавед." }
        var c: HttpURLConnection? = null
        try {
            c = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("apikey", key)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
            }
            if (body != null) c.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(raw).optString("message") }.getOrNull().orEmpty()
                throw IllegalStateException(message.ifBlank { "HTTP $status" })
            }
            raw
        } finally { c?.disconnect() }
    }

    suspend fun load(token: String): ActivityOverview {
        val day = LocalDate.now(ZoneId.of(ACTIVITY_ZONE))
        val tasksJson = JSONArray(request(
            "/rest/v1/shahrvand_activity_task_catalog?select=id,activity_type,title_tg,title_ru,title_en,description_tg,description_ru,description_en,target_distance_m,target_reps,time_limit_seconds,min_duration_seconds,xp&is_active=eq.true&order=sort_order.asc",
            "GET", token
        ))
        val tasks = buildList {
            for (i in 0 until tasksJson.length()) {
                val o = tasksJson.optJSONObject(i) ?: continue
                add(ActivityTask(
                    o.optString("id"), o.optString("activity_type"),
                    o.optString("title_tg"), o.optString("title_ru"), o.optString("title_en"),
                    o.optString("description_tg"), o.optString("description_ru"), o.optString("description_en"),
                    o.optInt("target_distance_m"), o.optInt("target_reps"), o.optInt("time_limit_seconds"),
                    o.optInt("min_duration_seconds"), o.optInt("xp")
                ))
            }
        }
        val completedJson = JSONArray(request(
            "/rest/v1/shahrvand_activity_sessions?select=task_id,xp_awarded&challenge_date=eq.$day&status=eq.verified",
            "GET", token
        ))
        val completed = buildMap { for (i in 0 until completedJson.length()) {
            val o = completedJson.optJSONObject(i) ?: continue
            put(o.optString("task_id"), o.optInt("xp_awarded"))
        } }
        val interestsJson = JSONArray(request(
            "/rest/v1/shahrvand_user_interests?select=interest_code", "GET", token
        ))
        val interests = buildSet { for (i in 0 until interestsJson.length()) add(interestsJson.optJSONObject(i)?.optString("interest_code").orEmpty()) }
            .filter { it.isNotBlank() }.toSet()
        return ActivityOverview(tasks, completed, interests)
    }

    suspend fun start(token: String, taskId: String): ActivitySession {
        val result = JSONObject(request("/rest/v1/rpc/shahrvand_start_activity_session", "POST", token,
            JSONObject().put("p_task_id", taskId)))
        if (!result.optBoolean("ok")) throw IllegalStateException("Сессия оғоз нашуд.")
        return ActivitySession(result.getString("session_id"), result.getString("nonce"),
            result.getString("started_at"), result.getString("expires_at"))
    }

    suspend fun cancel(token: String, session: ActivitySession) {
        val result = JSONObject(request("/rest/v1/rpc/shahrvand_cancel_activity_session", "POST", token,
            JSONObject().put("p_session_id", session.id).put("p_nonce", session.nonce)))
        if (!result.optBoolean("cancelled")) throw IllegalStateException("Сессия бекор нашуд.")
    }

    suspend fun finish(token: String, session: ActivitySession, evidence: JSONObject): JSONObject {
        return JSONObject(request("/rest/v1/rpc/shahrvand_finish_activity_session", "POST", token,
            JSONObject().put("p_session_id", session.id).put("p_nonce", session.nonce).put("p_evidence", evidence)))
    }

    suspend fun saveInterests(token: String, codes: Set<String>) {
        require(codes.size == 5) { "Маҳз 5 шавқро интихоб кун." }
        val arr = JSONArray().apply { codes.sorted().forEach { put(it) } }
        val result = JSONObject(request("/rest/v1/rpc/shahrvand_set_my_interests", "POST", token, JSONObject().put("p_codes", arr)))
        if (!result.optBoolean("ok")) throw IllegalStateException("Шавқҳо нигоҳ дошта нашуданд.")
    }
}

private data class Interest(val code: String, val tg: String, val ru: String, val en: String) {
    fun label(language: String) = aTr(language, tg, ru, en)
}
private val activityInterests = listOf(
    Interest("science","Илм","Наука","Science"),
    Interest("technology","Технология","Технологии","Technology"),
    Interest("history","Таърих","История","History"),
    Interest("geography","Ҷуғрофия","География","Geography"),
    Interest("mathematics","Математика","Математика","Mathematics"),
    Interest("languages","Забонҳо","Языки","Languages"),
    Interest("health","Саломатӣ","Здоровье","Health"),
    Interest("nature","Табиат","Природа","Nature"),
    Interest("culture","Фарҳанг","Культура","Culture"),
    Interest("arts","Санъат","Искусство","Arts"),
    Interest("sports","Варзиш","Спорт","Sports"),
    Interest("finance","Молия","Финансы","Finance")
)

@Composable
fun DailyActivitiesScreen(language: String, getAccessToken: suspend () -> String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overview by remember { mutableStateOf<ActivityOverview?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var activeTask by remember { mutableStateOf<ActivityTask?>(null) }
    var activeSession by remember { mutableStateOf<ActivitySession?>(null) }
    var pendingTask by remember { mutableStateOf<ActivityTask?>(null) }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var savingInterests by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    suspend fun begin(task: ActivityTask) {
        try {
            busy = true
            val session = DailyActivityRepository.start(getAccessToken(), task.id)
            activeTask = task
            activeSession = session
            error = ""
        } catch (e: Exception) { error = e.message ?: "Session error" }
        finally { busy = false }
    }

    val healthPermissions = remember { setOf(HealthPermission.getReadPermission(StepsRecord::class)) }
    var healthConnected by remember { mutableStateOf(false) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        healthConnected = healthPermissions.all { it in granted }
    }
    LaunchedEffect(Unit) {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            runCatching {
                val client = HealthConnectClient.getOrCreate(context)
                healthConnected = healthPermissions.all { it in client.permissionController.getGrantedPermissions() }
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val task = pendingTask
        pendingTask = null
        if (task != null) {
            val required = if (task.type.startsWith("gps_")) listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION)
                else listOf(Manifest.permission.CAMERA)
            if (required.all { grants[it] == true || ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                scope.launch { begin(task) }
            } else error = aTr(language, "Иҷозати зарурӣ дода нашуд.", "Не предоставлено необходимое разрешение.", "Required permission was not granted.")
        }
    }

    LaunchedEffect(refresh) {
        loading = true
        error = ""
        try {
            overview = DailyActivityRepository.load(getAccessToken())
            selectedInterests = overview?.interests.orEmpty()
        } catch (e: Exception) { error = e.message ?: "Network error" }
        finally { loading = false }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                aTr(language, "ФАЪОЛИЯТИ ҲАРРӮЗА", "ЕЖЕДНЕВНАЯ АКТИВНОСТЬ", "DAILY ACTIVITY"),
                color=A_Gold, fontSize=9.sp, fontWeight=FontWeight.ExtraBold
            )
            Text(
                aTr(language, "Қадамҳои воқеӣ. Натиҷаи санҷидашуда.", "Реальные действия. Проверяемый результат.", "Real actions. Measurable results."),
                color=A_Navy, fontSize=22.sp, lineHeight=27.sp, fontWeight=FontWeight.Black
            )
            Text(
                aTr(language,
                    "Ҳаракат кун, омӯз ва машқро анҷом деҳ. Система пешрафтро аз рӯи меъёрҳои муайян месанҷад.",
                    "Двигайся, учись и тренируйся. Система проверяет результат по установленным критериям.",
                    "Move, learn, and train. The system checks progress against defined criteria."),
                color=A_Muted, fontSize=12.sp, lineHeight=17.sp
            )
        }
        Card(
            modifier=Modifier.fillMaxWidth(),
            colors=CardDefaults.cardColors(containerColor=A_Navy),
            shape=RoundedCornerShape(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal=15.dp, vertical=13.dp),
                verticalAlignment=Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(
                        aTr(language, "МЕЪЁРИ ЯГОНА БАРОИ ҲАМА", "ЕДИНЫЕ КРИТЕРИИ ДЛЯ ВСЕХ", "ONE STANDARD FOR EVERYONE"),
                        color=androidx.compose.ui.graphics.Color(0xFFE5C995),
                        fontSize=8.sp, fontWeight=FontWeight.Bold
                    )
                    Text(
                        aTr(language, "Натиҷа баъди санҷиши серверӣ ба ҳисоб гирифта мешавад.", "Результат засчитывается после серверной проверки.", "Results count after server verification."),
                        color=androidx.compose.ui.graphics.Color.White, fontSize=11.sp, lineHeight=16.sp
                    )
                }
                Text("✓", color=androidx.compose.ui.graphics.Color(0xFFE5C995), fontSize=28.sp, fontWeight=FontWeight.Black)
            }
        }
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE && !healthConnected) {
            TextButton(onClick={healthPermissionLauncher.launch(healthPermissions)}) {
                Text(aTr(language,"Пайваст кардани Samsung Health / Health Connect","Подключить Samsung Health / Health Connect","Connect Samsung Health / Health Connect"))
            }
        } else if (healthConnected) {
            Text(aTr(language,"Қадамҳо аз Health Connect низ санҷида мешаванд.","Шаги также сверяются с Health Connect.","Steps are also cross-checked with Health Connect."),color=A_Green,fontSize=10.sp)
        }
        Spacer(Modifier.height(10.dp))
        if (loading && overview == null) Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center) { CircularProgressIndicator() }
        else if (overview == null) {
            Text(error, color=MaterialTheme.colorScheme.error)
            OutlinedButton(onClick={refresh++}) { Text(aTr(language,"Аз нав кӯшиш кун","Повторить","Retry")) }
        } else if (overview!!.interests.size != 5) {
            InterestPicker(language, selectedInterests, onToggle={ code ->
                selectedInterests = if (code in selectedInterests) selectedInterests - code else if (selectedInterests.size < 5) selectedInterests + code else selectedInterests
            }, saving=savingInterests, error=error, onSave={
                scope.launch {
                    savingInterests=true
                    try { DailyActivityRepository.saveInterests(getAccessToken(), selectedInterests); refresh++ }
                    catch(e:Exception){error=e.message ?: "Save failed"}
                    finally{savingInterests=false}
                }
            })
        } else {
            val taskCount = overview!!.tasks.size
            val done = overview!!.tasks.count { overview!!.completed.containsKey(it.id) }
            val progress = if (taskCount > 0) done.toFloat() / taskCount else 0f
            Card(colors=CardDefaults.cardColors(containerColor=androidx.compose.ui.graphics.Color.White), border=BorderStroke(1.dp,A_Border), shape=RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(aTr(language,"ПЕШРАФТИ ИМРӮЗ","ПРОГРЕСС ЗА СЕГОДНЯ","TODAY'S PROGRESS"), fontWeight=FontWeight.ExtraBold, color=A_Navy, fontSize=10.sp)
                            Text("$done/$taskCount " + aTr(language,"вазифа иҷро шуд","заданий выполнено","challenges completed"), color=A_Muted, fontSize=12.sp)
                        }
                        Text(overview!!.completed.values.sum().toString()+" XP", color=A_Gold, fontWeight=FontWeight.Black, fontSize=18.sp)
                    }
                    LinearProgressIndicator(progress={progress}, modifier=Modifier.fillMaxWidth().padding(top=8.dp), color=A_Green)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                aTr(language, "ИНТИХОБИ ВАЗИФА", "ВЫБЕРИ ЗАДАНИЕ", "CHOOSE A CHALLENGE"),
                color=A_Navy, fontSize=11.sp, fontWeight=FontWeight.ExtraBold,
                modifier=Modifier.padding(top=5.dp, bottom=1.dp)
            )
            LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp), contentPadding=PaddingValues(bottom=24.dp)) {
                items(overview!!.tasks, key={it.id}) { task ->
                    val completed = overview!!.completed[task.id]
                    Card(colors=CardDefaults.cardColors(containerColor=if(completed!=null) androidx.compose.ui.graphics.Color(0xFFF0F7F3) else androidx.compose.ui.graphics.Color.White),
                        border=BorderStroke(1.dp, if(completed!=null) androidx.compose.ui.graphics.Color(0xFFC9DED1) else A_Border), shape=RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement=Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(45.dp).clip(RoundedCornerShape(14.dp))
                                        .background(if(completed!=null) androidx.compose.ui.graphics.Color(0xFFE2F0E7) else androidx.compose.ui.graphics.Color(0xFFF0F4F6)),
                                    contentAlignment=Alignment.Center
                                ) { Text(taskIcon(task.type), fontSize=23.sp) }
                                Spacer(Modifier.width(11.dp))
                                Text(task.title(language), color=A_Navy, fontWeight=FontWeight.Bold, fontSize=13.sp, modifier=Modifier.weight(1f))
                                Text("+"+task.xp+" XP", color=A_Gold, fontWeight=FontWeight.Bold, fontSize=11.sp)
                            }
                            Text(task.description(language), color=A_Muted, fontSize=12.sp, lineHeight=17.sp, modifier=Modifier.padding(top=8.dp))
                            if (completed != null) Text("✓ "+aTr(language,"Иҷро шуд","Выполнено","Completed")+" · +$completed XP", color=A_Green, fontWeight=FontWeight.Bold, fontSize=12.sp, modifier=Modifier.padding(top=8.dp))
                            else Button(onClick={
                                val permissions = if(task.type.startsWith("gps_")) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACTIVITY_RECOGNITION)
                                    else if(task.type.startsWith("pose_")) arrayOf(Manifest.permission.CAMERA) else emptyArray()
                                val missing = permissions.filter { ContextCompat.checkSelfPermission(context,it) != PackageManager.PERMISSION_GRANTED }
                                if(missing.isEmpty()) scope.launch { begin(task) }
                                else { pendingTask=task; permissionLauncher.launch(missing.toTypedArray()) }
                            }, enabled=!busy && activeTask==null, modifier=Modifier.padding(top=9.dp).fillMaxWidth().height(44.dp),
                                shape=RoundedCornerShape(13.dp)) {
                                Text(aTr(language,"Оғози вазифа","Начать задание","Start challenge"), fontSize=12.sp, fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        if (error.isNotBlank() && overview != null) Text(error, color=MaterialTheme.colorScheme.error, fontSize=11.sp)
    }

    val task = activeTask
    val session = activeSession
    if (task != null && session != null) {
        ActivitySessionDialog(language, task, session, selectedInterests, getAccessToken, onFinish={ refresh++; activeTask=null; activeSession=null },
            onCancel={ activeTask=null; activeSession=null; refresh++ })
    }
}

private suspend fun readHealthSteps(context: Context, startedAt: String): Long {
    if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return 0L
    return runCatching {
        val client = HealthConnectClient.getOrCreate(context)
        val permission = HealthPermission.getReadPermission(StepsRecord::class)
        if (permission !in client.permissionController.getGrantedPermissions()) return@runCatching 0L
        val start = Instant.parse(startedAt)
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, Instant.now())
            )
        )
        response.records.filter { it.metadata.dataOrigin.packageName == "com.sec.android.app.shealth" }.sumOf { it.count }
    }.getOrDefault(0L)
}

private fun taskIcon(type: String) = when(type) {
    "gps_walk" -> "🚶"; "gps_run" -> "🏃"; "pose_squat" -> "🏋️"; "pose_pushup" -> "💪"; else -> "📚"
}

@Composable
private fun InterestPicker(language: String, selected: Set<String>, onToggle: (String)->Unit,
                           saving: Boolean, error: String, onSave: ()->Unit) {
    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(aTr(language,"5 шавқи худро интихоб кун","Выбери 5 интересов","Choose your 5 interests"), color=A_Navy, fontSize=18.sp, fontWeight=FontWeight.Bold)
        Text(aTr(language,"Дарсҳои ҳаррӯза аз ҳамин мавзӯъҳо интихоб мешаванд.","Ежедневные уроки будут подбираться по этим темам.","Daily lessons will be selected from these interests."), color=A_Muted, fontSize=12.sp)
        Text("${selected.size}/5", color=A_Gold, fontWeight=FontWeight.Bold)
        LazyColumn(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(4.dp)) {
            items(activityInterests, key={it.code}) { item ->
                FilterChip(selected=item.code in selected, onClick={onToggle(item.code)}, enabled=!saving,
                    label={Text(aTr(language,item.tg,item.ru,item.en))})
            }
        }
        if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error,fontSize=11.sp)
        Button(onClick=onSave, enabled=selected.size==5 && !saving, modifier=Modifier.fillMaxWidth()) {
            Text(if(saving) "…" else aTr(language,"Нигоҳ доштан","Сохранить","Save"))
        }
    }
}

@Composable
private fun ActivitySessionDialog(language: String, task: ActivityTask, session: ActivitySession,
                                  interestCodes: Set<String>, getAccessToken: suspend ()->String, onFinish: ()->Unit, onCancel: ()->Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var elapsed by remember(session.id) { mutableLongStateOf(0L) }
    var foregroundMs by remember(session.id) { mutableLongStateOf(0L) }
    var isForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isForeground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var poseMetrics by remember(session.id) { mutableStateOf(PoseMetrics()) }
    var quizStarted by remember(session.id) { mutableStateOf(false) }
    var quizAnswers by remember(session.id) { mutableStateOf(mapOf<Int,Int>()) }
    var submitting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val started = remember(session.id) {
        val serverStart = runCatching { Instant.parse(session.startedAt).toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
        SystemClock.elapsedRealtime() - (System.currentTimeMillis() - serverStart).coerceAtLeast(0L)
    }
    LaunchedEffect(session.id) {
        var lastTick = SystemClock.elapsedRealtime()
        while (true) {
            delay(500)
            val now = SystemClock.elapsedRealtime()
            val delta = (now - lastTick).coerceAtLeast(0L)
            elapsed = (now - started) / 1000L
            if (isForeground) foregroundMs += delta
            lastTick = now
        }
    }
    val gps = task.type.startsWith("gps_")
    val pose = task.type.startsWith("pose_")

    val gpsMetrics by GpsTrackingStore.metrics.collectAsState()
    val distance = gpsMetrics.distanceM
    val gpsSamples = gpsMetrics.samples
    val avgAccuracy = gpsMetrics.averageAccuracyM
    val maxSpeed = gpsMetrics.maxSpeedKmh
    val steps = gpsMetrics.stepCount
    DisposableEffect(session.id, gps) {
        if (gps) {
            GpsTrackingStore.reset()
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, ActivityLocationService::class.java))
            }.onFailure { status = it.message ?: "GPS service failed to start" }
        }
        onDispose {
            if (gps) {
                context.stopService(Intent(context, ActivityLocationService::class.java))
            }
        }
    }

    val limit = task.limitSec
    val distanceReached = distance >= task.distanceM
    val repsReached = poseMetrics.reps >= task.reps
    val timeReady = elapsed >= task.minDurationSec
    val timedOut = limit > 0 && elapsed > limit
    val canSubmit = when {
        gps -> distanceReached && timeReady && !timedOut && gpsSamples >= 12
        pose -> repsReached && timeReady && !timedOut && poseMetrics.frames >= 30
        else -> elapsed >= 1200 && foregroundMs >= 1200000L && quizStarted && quizAnswers.size == 5
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(task.title(language), color=A_Navy, fontWeight=FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max=540.dp), verticalArrangement=Arrangement.spacedBy(9.dp)) {
                if (gps) {
                    Text("%02d:%02d".format(elapsed/60,elapsed%60),fontSize=28.sp,color=A_Navy,fontWeight=FontWeight.Black)
                    Text("%.2f / %.2f km".format(java.util.Locale.US,distance/1000.0,task.distanceM/1000.0),fontSize=20.sp,color=A_Green,fontWeight=FontWeight.Bold)
                    Text(aTr(language,"GPS намунаҳо: $gpsSamples · Қадамҳо: $steps · дақиқии миёна: %.1f м".format(avgAccuracy),
                        "GPS-точки: $gpsSamples · шаги: $steps · средняя точность: %.1f м".format(avgAccuracy),
                        "GPS samples: $gpsSamples · steps: $steps · average accuracy: %.1f m".format(avgAccuracy)),fontSize=11.sp,color=A_Muted)
                    if(task.type.startsWith("gps_") && limit > 0) {
                        Text(aTr(language,"Мӯҳлат: ${limit/60} дақиқа","Лимит: ${limit/60} минут","Time limit: ${limit/60} min"),color=A_Muted,fontSize=11.sp)
                    } else if (task.type.startsWith("gps_")) {
                        Text(aTr(language,"Вақти иҷро маҳдуд нест.","Ограничения по времени нет.","No time limit."),color=A_Muted,fontSize=11.sp)
                    }
                    Text(aTr(language,"Роҳро бехатар интихоб кун. Телефон GPS-ро танҳо ҳангоми фаъол будани сессия истифода мебарад.","Выбирай безопасный маршрут. GPS работает только во время активной сессии.","Choose a safe route. GPS is used only during this active session."),fontSize=11.sp,color=A_Muted)
                } else if (pose) {
                    PoseCameraCounter(task.type,onMetrics={poseMetrics=it},modifier=Modifier.fillMaxWidth().height(230.dp))
                    Text("${poseMetrics.reps}/${task.reps}",fontSize=26.sp,fontWeight=FontWeight.Black,color=A_Navy)
                    Text(aTr(language,"Танҳо ҳаракати пурра ва дуруст ҳисоб мешавад. Камера танҳо дар телефон кор мекунад; видео сабт ё фиристода намешавад.",
                        "Считаются только полные правильные движения. Камера работает локально; видео не записывается и не отправляется.",
                        "Only full, correct repetitions count. Camera processing stays on-device; video is not saved or uploaded."),fontSize=11.sp,color=A_Muted)
                    Text(aTr(language,"Вақт: $elapsed / ${task.limitSec} сония","Время: $elapsed / ${task.limitSec} секунд","Time: $elapsed / ${task.limitSec} seconds"),fontWeight=FontWeight.Bold,color=A_Green)
                } else {
                    LearningPanel(language, interestCodes, foregroundMs/1000L, quizStarted, {quizStarted=true}, quizAnswers, {i,v->quizAnswers=quizAnswers+(i to v)})
                }
                if(status.isNotBlank()) Text(status,color=MaterialTheme.colorScheme.error,fontSize=11.sp)
            }
        },
        confirmButton = {
            Button(onClick={
                scope.launch {
                    submitting=true; status=""
                    try {
                        val evidence=JSONObject()
                        if(gps) {
                            val healthSteps = if (task.type=="gps_walk" || task.type=="gps_run") readHealthSteps(context,session.startedAt) else 0L
                            evidence.put("distance_m",distance).put("gps_samples",gpsSamples).put("avg_accuracy_m",avgAccuracy)
                                .put("max_speed_kmh",maxSpeed).put("step_count",max(steps,healthSteps.toInt())).put("route_hash",gpsMetrics.routeHash)
                        } else if(pose) {
                            evidence.put("valid_reps",poseMetrics.reps).put("camera_frames",poseMetrics.frames)
                                .put("pose_visible_ratio",poseMetrics.visibleRatio).put("pose_confidence",poseMetrics.confidence)
                        } else {
                            evidence.put("foreground_ms",foregroundMs).put("quiz_total",5)
                                .put("quiz_correct",quizAnswers.count{(index,answer)->answer==quizCorrectAnswers[index]})
                                .put("interest_codes",JSONArray().apply { interestCodes.sorted().forEach { put(it) } })
                        }
                        val result=DailyActivityRepository.finish(getAccessToken(),session,evidence)
                        if(result.optBoolean("ok")) { status=aTr(language,"Сервер тасдиқ кард!","Сервер подтвердил!","Verified by server!"); delay(650); onFinish() }
                        else status=aTr(language,"Натиҷа қабул нашуд: ","Результат не принят: ","Result rejected: ")+result.optString("reason")
                    } catch(e:Exception) { status=e.message ?: "Verification error" }
                    finally{submitting=false}
                }
            },enabled=canSubmit && !submitting) {
                Text(if(submitting) "…" else aTr(language,"Санҷидан","Проверить","Submit for verification"))
            }
        },
        dismissButton = {
            TextButton(onClick={
                scope.launch {
                    submitting=true
                    try { DailyActivityRepository.cancel(getAccessToken(),session) }
                    catch(e:Exception) { status=e.message ?: "Cancel failed" }
                    onCancel()
                }
            },enabled=!submitting) { Text(aTr(language,"Бекор кардан","Отмена","Cancel")) }
        }
    )
}

private val quizCorrectAnswers = listOf(0,1,1,1,2)
private data class LessonCopy(val tg:String,val ru:String,val en:String)
private fun lessonFor(code:String,lang:String):String {
    val lesson=when(code) {
        "science" -> LessonCopy("Илм аз мушоҳида, савол ва санҷиши фарзия оғоз мешавад. Натиҷаи таҷриба бояд такроршаванда бошад. Як натиҷа ҳамеша ҳақиқатро исбот намекунад: шароит, хатои ченкунӣ ва манбаъро низ бояд санҷид.","Наука начинается с наблюдения, вопроса и проверки гипотезы. Результат эксперимента должен быть воспроизводимым. Одно наблюдение не доказывает универсальную истину: учитывай условия, погрешность и источник.","Science begins with observation, a question, and testing a hypothesis. Results should be reproducible. A single observation does not prove a universal claim: consider conditions, measurement error, and sources.")
        "technology" -> LessonCopy("Технология воситаи ҳалли масъала аст. Барои ҳифзи ҳисобҳо пароли нодир ва тасдиқи дуқадамӣ истифода мешавад. Маълумоти шахсиро танҳо ба барномаҳои боэътимод диҳед ва пайвандҳои шубҳанокро накушоед.","Технология — инструмент решения задач. Для защиты аккаунтов используй уникальные пароли и двухэтапную проверку. Передавай личные данные только надёжным приложениям и не открывай подозрительные ссылки.","Technology is a tool for solving problems. Protect accounts with unique passwords and two-step verification. Share personal data only with trusted apps and avoid suspicious links.")
        "history" -> LessonCopy("Таърихро бо санҷиши сарчашмаҳо меомӯзанд. Ҳуҷҷат, ашёи бостонӣ ва ёддошт метавонанд маълумот диҳанд, аммо ҳар манбаъ нуқтаи назари худро дорад. Сана, муаллиф ва шароити пайдоиши сарчашмаро муқоиса кун.","Историю изучают через проверку источников. Документы, артефакты и воспоминания дают сведения, но каждый источник имеет свою перспективу. Сравнивай дату, автора и обстоятельства появления источника.","History is studied by examining sources. Documents, artifacts, and memories provide evidence, but each source has a perspective. Compare its date, author, and context.")
        "geography" -> LessonCopy("Харита модели сатҳи Замин аст. Миқёс нишон медиҳад, ки масофаи рӯи харита ба масофаи воқеӣ чӣ қадар мувофиқ аст. Самт, баландӣ ва координатаҳо барои фаҳмидани ҷойгиршавӣ кӯмак мекунанд.","Карта — модель поверхности Земли. Масштаб показывает, как расстояние на карте соотносится с реальным. Направление, высота и координаты помогают определить положение.","A map models Earth's surface. Scale relates map distance to real distance. Direction, elevation, and coordinates help describe location.")
        "mathematics" -> LessonCopy("Математика муносибатҳоро бо рақам ва мантиқ тавсиф мекунад. Барои таносуб аввал воҳидҳоро яксон кун, баъд муқоисаи ду бузургиро ҳисоб намо. Ҳар қадамро навис, то хаторо ёфтан осон шавад.","Математика описывает отношения с помощью чисел и логики. Для пропорции сначала приведи единицы к одному виду, затем сравни величины. Записывай шаги, чтобы легче находить ошибки.","Mathematics describes relationships using numbers and logic. For a ratio, first make units consistent, then compare quantities. Write each step so errors are easier to find.")
        "languages" -> LessonCopy("Забон бо шунидан, хондан ва истифодаи калимаҳо омӯхта мешавад. Калимаро дар ҷумла омӯз, маъно ва тарзи истифодаашро бин. Такрори фосилавӣ хотираро мустаҳкам мекунад.","Язык осваивается через аудирование, чтение и использование слов. Изучай слово в контексте предложения, его значение и употребление. Повторение с интервалами укрепляет память.","Language develops through listening, reading, and using words. Learn vocabulary in sentence context and notice its meaning and usage. Spaced repetition strengthens memory.")
        "health" -> LessonCopy("Саломатӣ аз одатҳои ҳаррӯза вобаста аст: хоби кофӣ, ғизои гуногун, ҳаракат ва гигиена. Ҳангоми машқ баданро гӯш кун, об нӯш ва ҳангоми дард ё сарчархӣ истироҳат намо.","Здоровье зависит от ежедневных привычек: достаточного сна, разнообразного питания, движения и гигиены. Во время нагрузки прислушивайся к телу, пей воду и отдыхай при боли или головокружении.","Health depends on daily habits: sufficient sleep, varied nutrition, movement, and hygiene. During exercise listen to your body, drink water, and rest if you feel pain or dizziness.")
        "nature" -> LessonCopy("Экосистема аз организмҳо ва муҳити онҳо иборат аст. Растаниҳо, ҳайвонот, хок, об ва иқлим бо ҳам таъсир мекунанд. Тағйири як қисми система метавонад ба дигар қисмҳо низ таъсир расонад.","Экосистема состоит из организмов и среды. Растения, животные, почва, вода и климат взаимодействуют. Изменение одной части может повлиять на остальные.","An ecosystem consists of organisms and their environment. Plants, animals, soil, water, and climate interact. A change in one part can affect the others.")
        "culture" -> LessonCopy("Фарҳанг забон, анъана, арзиш ва таҷрибаи муштараки ҷомеаро дар бар мегирад. Барои фаҳмидани фарҳанги дигар аввал гӯш кун, савол деҳ ва аз умумисозии одамон худдорӣ намо.","Культура включает язык, традиции, ценности и общий опыт общества. Чтобы понять другую культуру, сначала слушай, задавай вопросы и избегай обобщений о людях.","Culture includes language, traditions, values, and shared experience. To understand another culture, listen, ask questions, and avoid generalizing about people.")
        "arts" -> LessonCopy("Санъат фикр ва эҳсосро бо шакл, ранг, садо ё ҳаракат ифода мекунад. Композитсия тартиби унсурҳост; контраст диққатро ҷалб мекунад ва тавозун ҳисси ҳамоҳангӣ медиҳад.","Искусство выражает идеи и чувства через форму, цвет, звук или движение. Композиция — порядок элементов; контраст привлекает внимание, а баланс создаёт гармонию.","Art expresses ideas and feelings through form, color, sound, or movement. Composition arranges elements; contrast draws attention and balance creates harmony.")
        "sports" -> LessonCopy("Машқи босамар аз такмил ва истироҳат иборат аст. Гармикунӣ баданро омода мекунад, техникаи дуруст хавфи осебро кам мекунад ва барқароршавӣ ба мушакҳо вақт медиҳад.","Эффективная тренировка сочетает нагрузку и отдых. Разминка готовит тело, правильная техника снижает риск травм, а восстановление даёт мышцам время адаптироваться.","Effective training combines effort and recovery. Warm-ups prepare the body, good technique reduces injury risk, and recovery gives muscles time to adapt.")
        else -> LessonCopy("Буҷет нақшаи даромад ва хароҷот аст. Аввал даромадро ҳисоб кун, хароҷоти заруриро ҷудо намо ва баъд барои пасандоз ва хоҳишҳо маблағ муайян кун. Сабти хароҷот қарорҳои беҳтар медиҳад.","Бюджет — план доходов и расходов. Сначала посчитай доход, выдели обязательные траты, затем определи суммы для сбережений и желаний. Учёт расходов помогает принимать решения.","A budget plans income and spending. Count income, set aside essential costs, then allocate money to savings and wants. Tracking expenses supports better decisions.")
    }
    return aTr(lang,lesson.tg,lesson.ru,lesson.en)
}

@Composable
private fun LearningPanel(language:String, interestCodes:Set<String>, elapsed:Long, quizStarted:Boolean,
                          onQuiz:()->Unit, answers:Map<Int,Int>, onAnswer:(Int,Int)->Unit) {
    val topics = activityInterests.filter { it.code in interestCodes }
    Column(Modifier.fillMaxWidth().heightIn(max=430.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(aTr(language,"Дарсҳо аз рӯи 5 шавқи интихобшуда","Уроки по 5 выбранным интересам","Lessons based on your 5 selected interests"),color=A_Navy,fontWeight=FontWeight.Bold)
        topics.forEach { topic ->
            Text(topic.label(language),color=A_Gold,fontWeight=FontWeight.Bold,fontSize=12.sp)
            Text(lessonFor(topic.code,language),fontSize=12.sp,lineHeight=18.sp,color=A_Muted)
        }
        Text("%02d:%02d / 20:00".format(elapsed/60,elapsed%60),fontSize=21.sp,color=A_Green,fontWeight=FontWeight.Black)
        if(elapsed>=1200 && !quizStarted) Button(onClick=onQuiz) { Text(aTr(language,"Ба санҷиш гузаштан","Перейти к тесту","Start quiz")) }
        if(quizStarted) {
            val questions=listOf(
                Triple("Пеш аз қабул кардани даъво чӣ лозим аст?","Что нужно перед принятием утверждения?","What should precede accepting a claim?") to listOf("Далелҳо","Овозаи зиёд","Тахмин"),
                Triple("Оё як мушоҳида ҳама вақт қонунро исбот мекунад?","Доказывает ли одно наблюдение универсальный закон?","Does one observation prove a universal law?") to listOf("Ҳа","Не","Ҳамеша"),
                Triple("Агар ду сарчашма ихтилоф кунанд, чӣ бояд кард?","Что делать, если два источника расходятся?","What should you do when sources disagree?") to listOf("Якеро нодида гирифтан","Сарчашмаҳоро муқоиса кардан","Аввалинро қабул кардан"),
                Triple("Далели санҷишшаванда чист?","Что такое проверяемый факт?","What is a verifiable fact?") to listOf("Фикри шахсӣ","Маълумоти санҷиданшаванда","Овоза"),
                Triple("Кадом роҳ барои омӯзиш беҳтар аст?","Какой способ обучения надёжнее?","Which learning approach is stronger?") to listOf("Як бор хондан","Азёд кардан бе фаҳм","Фаҳмидан ва такрор кардан")
            )
            questions.forEachIndexed { index, item ->
                Text(item.first.let { aTr(language,it.first,it.second,it.third) },color=A_Navy,fontWeight=FontWeight.SemiBold,fontSize=11.sp)
                item.second.forEachIndexed { option, _ ->
                    val labels=when(index) {
                        0 -> listOf(aTr(language,"Далелҳо","Факты","Evidence"),aTr(language,"Овозаи зиёд","Много слухов","Many rumours"),aTr(language,"Тахмин","Предположение","Guesswork"))
                        1 -> listOf(aTr(language,"Ҳа","Да","Yes"),aTr(language,"Не","Нет","No"),aTr(language,"Ҳамеша","Всегда","Always"))
                        2 -> listOf(aTr(language,"Якеро нодида гирифтан","Игнорировать один","Ignore one"),aTr(language,"Сарчашмаҳоро муқоиса кардан","Сравнить источники","Compare sources"),aTr(language,"Аввалинро қабул кардан","Принять первый","Accept the first"))
                        3 -> listOf(aTr(language,"Фикри шахсӣ","Личное мнение","Personal opinion"),aTr(language,"Маълумоти санҷиданшаванда","Проверяемые сведения","Testable information"),aTr(language,"Овоза","Слух","Rumour"))
                        else -> listOf(aTr(language,"Як бор хондан","Прочитать один раз","Read once"),aTr(language,"Азёд кардан бе фаҳм","Зубрить без понимания","Memorise without understanding"),aTr(language,"Фаҳмидан ва такрор кардан","Понять и повторить","Understand and review"))
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=answers[index]==option,onClick={onAnswer(index,option)})
                        Text(labels[option],fontSize=10.sp,color=A_Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun PoseCameraCounter(type:String, onMetrics:(PoseMetrics)->Unit, modifier:Modifier=Modifier) {
    val context= LocalContext.current
    val owner= LocalLifecycleOwner.current
    val cleanup = remember { mutableStateOf<(() -> Unit)?>(null) }
    DisposableEffect(Unit) { onDispose { cleanup.value?.invoke() } }
    AndroidView(modifier=modifier, factory={ viewContext ->
        val previewView=PreviewView(viewContext).apply { scaleType=PreviewView.ScaleType.FILL_CENTER }
        val executor=Executors.newSingleThreadExecutor()
        val detector=PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build())
        var provider: ProcessCameraProvider?=null
        var analysisUseCase: ImageAnalysis?=null
        cleanup.value = { provider?.unbindAll(); analysisUseCase?.clearAnalyzer(); detector.close(); executor.shutdown() }
        val future=ProcessCameraProvider.getInstance(viewContext)
        future.addListener({
            try {
                provider=future.get()
                val preview=Preview.Builder().build().also{it.surfaceProvider=previewView.surfaceProvider}
                var reps=0; var frames=0; var bottomSeen=false; var visibleSum=0.0; var confidenceSum=0.0
                val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(480,640)).build()
                analysisUseCase=analysis
                analysis.setAnalyzer(executor) { proxy ->
                    val media=proxy.image
                    if(media==null){proxy.close()} else {
                        val image=InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)
                        detector.process(image).addOnSuccessListener { pose ->
                            frames++
                            val marks=pose.allPoseLandmarks
                            val visible=marks.count{it.inFrameLikelihood>=0.6f}/33.0
                            val confidence=if(marks.isNotEmpty()) marks.map{it.inFrameLikelihood.toDouble()}.average() else 0.0
                            visibleSum+=visible; confidenceSum+=confidence
                            fun point(kind:Int, left:Boolean): Pair<Double,Double>? {
                                val a=pose.getPoseLandmark(if(left) kind else kind+1) ?: return null
                                if(a.inFrameLikelihood<0.55f) return null
                                return a.position.x.toDouble() to a.position.y.toDouble()
                            }
                            fun angle(a:Pair<Double,Double>,b:Pair<Double,Double>,c:Pair<Double,Double>):Double {
                                val abx=a.first-b.first; val aby=a.second-b.second
                                val cbx=c.first-b.first; val cby=c.second-b.second
                                val denom=kotlin.math.sqrt((abx*abx+aby*aby)*(cbx*cbx+cby*cby)).coerceAtLeast(0.0001)
                                return Math.toDegrees(acos(((abx*cbx+aby*cby)/denom).coerceIn(-1.0,1.0)))
                            }
                            var isBottom=false; var isTop=false
                            if(type=="pose_squat") {
                                val knees=listOf(true,false).mapNotNull { left ->
                                    val hip=point(PoseLandmark.LEFT_HIP,left); val knee=point(PoseLandmark.LEFT_KNEE,left); val ankle=point(PoseLandmark.LEFT_ANKLE,left)
                                    if(hip!=null && knee!=null && ankle!=null) angle(hip,knee,ankle) else null
                                }
                                if(knees.isNotEmpty()){isBottom=knees.average()<105.0;isTop=knees.average()>155.0}
                            } else {
                                val elbows=listOf(true,false).mapNotNull { left ->
                                    val shoulder=point(PoseLandmark.LEFT_SHOULDER,left); val elbow=point(PoseLandmark.LEFT_ELBOW,left); val wrist=point(PoseLandmark.LEFT_WRIST,left)
                                    if(shoulder!=null && elbow!=null && wrist!=null) angle(shoulder,elbow,wrist) else null
                                }
                                val alignment=listOf(true,false).mapNotNull { left ->
                                    val shoulder=point(PoseLandmark.LEFT_SHOULDER,left); val hip=point(PoseLandmark.LEFT_HIP,left); val ankle=point(PoseLandmark.LEFT_ANKLE,left)
                                    if(shoulder!=null && hip!=null && ankle!=null) angle(shoulder,hip,ankle) else null
                                }
                                if(elbows.isNotEmpty() && alignment.isNotEmpty() && alignment.average()>145.0){isBottom=elbows.average()<100.0;isTop=elbows.average()>150.0}
                            }
                            if(isBottom) bottomSeen=true
                            if(isTop && bottomSeen){reps++;bottomSeen=false}
                            onMetrics(PoseMetrics(reps,frames,visibleSum/frames,confidenceSum/frames))
                        }.addOnCompleteListener { proxy.close() }
                    }
                }
                provider?.unbindAll()
                provider?.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis)
            } catch (_:Exception) { }
        },ContextCompat.getMainExecutor(viewContext))
        previewView
    }, update={})
}

