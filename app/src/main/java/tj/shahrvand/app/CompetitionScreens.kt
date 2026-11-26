package tj.shahrvand.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val C_Navy = Color(0xFF20384B)
private val C_Gold = Color(0xFFB98A50)
private val C_Green = Color(0xFF36766D)
private val C_Muted = Color(0xFF777F7E)
private val C_Border = Color(0xFFDDE7EF)

data class WeeklyTask(
    val id: String, val weekday: Int, val titleTg: String, val titleRu: String, val titleEn: String,
    val descriptionTg: String, val descriptionRu: String, val descriptionEn: String,
    val category: String, val icon: String, val xp: Int
) {
    fun title(l: String) = when(l) { "ru" -> titleRu; "en" -> titleEn; else -> titleTg }
    fun description(l: String) = when(l) { "ru" -> descriptionRu; "en" -> descriptionEn; else -> descriptionTg }
}
data class WeeklyData(val tasks: List<WeeklyTask>, val completed: Map<String, Int>, val today: LocalDate, val weekStart: LocalDate)
data class LeaderEntry(
    val rank: Int,
    val login: String,
    val xp: Int,
    val completed: Int,
    val isMe: Boolean,
    val achievementCodes: List<String> = emptyList()
)

data class AchievementSummary(
    val verifiedTasks: Int,
    val progressFrom: Int,
    val nextThreshold: Int,
    val progress: Float,
    val unlockedCodes: List<String>
)

object SupabaseCompetitionRepository {
    private val base get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val key get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val zone = ZoneId.of("Asia/Dushanbe")
    private suspend fun call(path: String, method: String, token: String, body: JSONObject? = null): String = withContext(Dispatchers.IO) {
        var c: HttpURLConnection? = null
        try {
            c = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method; connectTimeout = 12000; readTimeout = 15000
                setRequestProperty("apikey", key); setRequestProperty("Authorization", "Bearer " + token)
                setRequestProperty("Accept", "application/json")
                if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
            }
            if (body != null) c.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(raw).optString("message") }.getOrNull().orEmpty()
                val friendly = when {
                    message.contains("TASK_ALREADY_COMPLETED") -> "Ин вазифа аллакай иҷро шудааст."
                    message.contains("TASK_NOT_AVAILABLE_TODAY") -> "Ин вазифа имрӯз дастрас нест."
                    message.contains("AUTH_REQUIRED") -> "Сессия ба охир расид. Дубора ворид шавед."
                    message.contains("PROFILE_REQUIRED") -> "Профили корбар ёфт нашуд."
                    else -> message.takeIf { it.isNotBlank() } ?: "HTTP " + code
                }
                throw IllegalStateException(friendly)
            }
            raw
        } finally { c?.disconnect() }
    }
    suspend fun fetchWeek(token: String): WeeklyData {
        val today = LocalDate.now(zone)
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val a = JSONArray(call("/rest/v1/shahrvand_weekly_tasks?select=id,weekday,title_tg,title_ru,title_en,description_tg,description_ru,description_en,category,icon,xp&is_active=eq.true&order=weekday.asc,sort_order.asc", "GET", token))
        val tasks = buildList {
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                add(WeeklyTask(o.optString("id"),o.optInt("weekday"),o.optString("title_tg"),o.optString("title_ru"),o.optString("title_en"),
                    o.optString("description_tg"),o.optString("description_ru"),o.optString("description_en"),o.optString("category"),o.optString("icon"),o.optInt("xp")))
            }
        }
        val b = JSONArray(call("/rest/v1/shahrvand_weekly_completions?select=task_id,xp_awarded&week_start=eq." + monday, "GET", token))
        val completed = buildMap { for (i in 0 until b.length()) { val o=b.optJSONObject(i) ?: continue; put(o.optString("task_id"),o.optInt("xp_awarded")) } }
        return WeeklyData(tasks, completed, today, monday)
    }
    suspend fun complete(token: String, taskId: String): Int {
        val o = JSONObject(call("/rest/v1/rpc/shahrvand_complete_weekly_task","POST",token,JSONObject().put("p_task_id",taskId)))
        if (!o.optBoolean("ok")) throw IllegalStateException("Вазифа тасдиқ нашуд.")
        return o.optInt("xp_awarded")
    }
    suspend fun leaderboard(token: String, period: String, band: String): List<LeaderEntry> {
        val body = JSONObject().put("p_period", period).put("p_age_band", band)
        val a = JSONArray(call("/rest/v1/rpc/shahrvand_get_leaderboard", "POST", token, body))
        return buildList {
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val codes = o.optJSONArray("achievement_codes")
                val badges = buildList {
                    if (codes != null) for (j in 0 until codes.length()) {
                        codes.optString(j).takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
                add(LeaderEntry(
                    o.optInt("rank"), o.optString("login"), o.optInt("weekly_xp"),
                    o.optInt("tasks_completed"), o.optBoolean("is_me"), badges
                ))
            }
        }
    }

    suspend fun myAchievements(token: String): AchievementSummary {
        val raw = call("/rest/v1/rpc/shahrvand_get_my_achievements", "POST", token, JSONObject())
        val result = JSONObject(raw)
        val array = result.optJSONArray("achievements")
        val codes = buildList {
            if (array != null) for (i in 0 until array.length()) {
                val code = array.optJSONObject(i)?.optString("code").orEmpty()
                if (code.isNotBlank()) add(code)
            }
        }
        return AchievementSummary(
            verifiedTasks = result.optInt("verified_tasks"),
            progressFrom = result.optInt("progress_from"),
            nextThreshold = result.optInt("next_threshold"),
            progress = result.optDouble("progress").toFloat().coerceIn(0f, 1f),
            unlockedCodes = codes
        )
    }
}

private fun cTr(l:String,t:String,r:String,e:String)=when(l){"ru"->r;"en"->e;else->t}
private fun cBand(l:String,b:String)=when(b){"7_12"->cTr(l,"7–12 сола","7–12 лет","Ages 7–12");"13_17"->cTr(l,"13–17 сола","13–17 лет","Ages 13–17");else->cTr(l,"18+ сола","18+ лет","Ages 18+")}

@Composable
fun WeeklyChallengesScreen(language:String,getAccessToken:suspend()->String) {
    var data by remember { mutableStateOf<WeeklyData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val scope= rememberCoroutineScope()
    LaunchedEffect(refresh) {
        loading=true; error=""
        try { data=SupabaseCompetitionRepository.fetchWeek(getAccessToken()) }
        catch(e:Exception) { error=e.message ?: "Network error" }
        finally { loading=false }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF1F6FA)).padding(14.dp)) {
        Text(cTr(language,"Вазифаҳои ҳафтаина","Задания на неделю","Weekly challenges"),color=C_Navy,fontSize=21.sp,fontWeight=FontWeight.Black)
        Text(cTr(language,"Ҳар рӯз 6 вазифа · 42 қадам дар як ҳафта","6 заданий в день · 42 шага за неделю","6 tasks per day · 42 steps per week"),color=C_Muted,fontSize=11.sp)
        Spacer(Modifier.height(10.dp))
        val d=data
        if(loading && d==null) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
        else if(d==null) { Text(error,color=Color.Red); OutlinedButton(onClick={refresh++}){Text(cTr(language,"Аз нав кӯшиш кун","Повторить","Retry"))} }
        else {
            val todays=d.tasks.filter{it.weekday==d.today.dayOfWeek.value}
            val done=todays.count{d.completed.containsKey(it.id)}
            Card(
                colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.84f)),
                border=BorderStroke(1.dp,Color.White.copy(alpha=0.96f)),
                elevation=CardDefaults.cardElevation(defaultElevation=7.dp),
                shape=RoundedCornerShape(21.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(cTr(language,"Пешрафти имрӯз","Прогресс сегодня","Today's progress"),fontWeight=FontWeight.Bold,color=C_Navy)
                            Text(d.today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))+" · "+done+"/6",fontSize=11.sp,color=C_Muted)
                        }
                        Text(d.completed.values.sum().toString()+" XP",fontWeight=FontWeight.Black,color=C_Gold,fontSize=19.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress={done/6f},modifier=Modifier.fillMaxWidth(),color=C_Green,trackColor=C_Border)
                    Text(cTr(language,"Холҳои рейтингро сервер тасдиқ мекунад.","Очки рейтинга подтверждает сервер.","Leaderboard points are confirmed by the server."),fontSize=10.sp,color=C_Muted,modifier=Modifier.padding(top=5.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            if(error.isNotBlank()) Text(error,color=Color.Red,fontSize=11.sp)
            LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=18.dp)) {
                items((1..7).toList()) { day ->
                    val list=d.tasks.filter{it.weekday==day}
                    val isToday=day==d.today.dayOfWeek.value
                    val isPast=day<d.today.dayOfWeek.value
                    val dayDone=list.count{d.completed.containsKey(it.id)}
                    val namesT=listOf("Душанбе","Сешанбе","Чоршанбе","Панҷшанбе","Ҷумъа","Шанбе","Якшанбе")
                    val namesR=listOf("Понедельник","Вторник","Среда","Четверг","Пятница","Суббота","Воскресенье")
                    val namesE=listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
                    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(top=4.dp)) {
                        Text(cTr(language,namesT[day-1],namesR[day-1],namesE[day-1]),fontWeight=FontWeight.Bold,color=C_Navy,fontSize=14.sp)
                        Spacer(Modifier.weight(1f)); Text(dayDone.toString()+"/"+list.size,fontSize=11.sp,color=C_Muted)
                    }
                    list.forEach { task ->
                        val completed=d.completed.containsKey(task.id)
                        Card(
                            colors=CardDefaults.cardColors(containerColor=if(completed) Color(0xFFEAF5EF) else Color.White.copy(alpha=0.84f)),
                            border=BorderStroke(1.dp,if(completed) Color(0xFFC9DED1) else Color.White.copy(alpha=0.96f)),
                            elevation=CardDefaults.cardElevation(defaultElevation=5.dp),
                            shape=RoundedCornerShape(19.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Text(task.icon,fontSize=21.sp); Spacer(Modifier.width(8.dp))
                                    Text(task.title(language),modifier=Modifier.weight(1f),fontSize=12.sp,fontWeight=FontWeight.Bold,color=C_Navy)
                                    Text("+"+task.xp+" XP",fontSize=11.sp,color=C_Gold,fontWeight=FontWeight.Bold)
                                }
                                Text(task.description(language),fontSize=11.sp,lineHeight=16.sp,color=C_Muted,modifier=Modifier.padding(top=4.dp))
                                if(completed) Text("✓ "+cTr(language,"Иҷро шуд","Выполнено","Completed")+" · +"+d.completed[task.id]+" XP",color=C_Green,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=6.dp))
                                else if(isToday) Button(onClick={
                                    scope.launch { busy=task.id; error=""
                                        try { SupabaseCompetitionRepository.complete(getAccessToken(),task.id); refresh++ }
                                        catch(e:Exception){error=e.message ?: "Error"}
                                        finally{busy=null}
                                    }
                                },enabled=busy==null,modifier=Modifier.padding(top=6.dp).height(36.dp),shape=RoundedCornerShape(9.dp),contentPadding=PaddingValues(horizontal=12.dp)) {
                                    if(busy==task.id) CircularProgressIndicator(Modifier.size(15.dp),strokeWidth=2.dp)
                                    else Text(cTr(language,"Иҷро кардам","Выполнено","Mark complete"),fontSize=11.sp)
                                }
                                else Text(if(isPast)cTr(language,"Муҳлат гузашт","День завершён","Day ended") else cTr(language,"Дар рӯзи худ фаъол мешавад","Откроется в свой день","Unlocks on its day"),fontSize=10.sp,color=C_Muted,modifier=Modifier.padding(top=5.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardScreen(language:String,myAgeBand:String,getAccessToken:suspend()->String) {
    var band by remember(myAgeBand) { mutableStateOf(myAgeBand) }
    var period by remember { mutableStateOf("week") }
    var filterExpanded by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf(emptyList<LeaderEntry>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(period, band, refresh) {
        loading = true
        error = ""
        try {
            entries = SupabaseCompetitionRepository.leaderboard(getAccessToken(), period, band)
        } catch (e: Exception) {
            error = e.message ?: "Network error"
            entries = emptyList()
        } finally {
            loading = false
        }
    }

    val periodLabel = when (period) {
        "month" -> cTr(language, "Моҳ", "Месяц", "Month")
        "all_time" -> cTr(language, "Умумӣ", "Всё время", "All-time")
        else -> cTr(language, "Ҳафта", "Неделя", "Week")
    }
    val bandLabel = if (band == "all") {
        cTr(language, "Ҳамаи синну сол", "Все возрасты", "All ages")
    } else cBand(language, band)

    Column(Modifier.fillMaxSize().background(Color(0xFFF1F6FA)).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(cTr(language,"Рейтинг","Рейтинг","Leaderboard"),color=C_Navy,fontSize=22.sp,fontWeight=FontWeight.Black)
        Text(
            cTr(language,"Натиҷаҳои тасдиқшудаи сервер","Подтверждённые сервером результаты","Server-verified results"),
            color=C_Muted,fontSize=11.sp
        )
        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("week","month","all_time").forEachIndexed { i, value ->
                val label = when(value) {
                    "month" -> cTr(language,"Моҳ","Месяц","Month")
                    "all_time" -> cTr(language,"Умумӣ","Всё","All-time")
                    else -> cTr(language,"Ҳафта","Неделя","Week")
                }
                SegmentedButton(
                    selected = period == value,
                    onClick = { period = value },
                    shape = SegmentedButtonDefaults.itemShape(i,3)
                ) { Text(label,fontSize=11.sp,maxLines=1) }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                cTr(language,"ФИЛТРИ СИННУ СОЛ","ФИЛЬТР ВОЗРАСТА","AGE FILTER"),
                color=C_Muted,fontSize=9.sp,fontWeight=FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Box {
                OutlinedButton(
                    onClick = { filterExpanded = true },
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal=12.dp,vertical=7.dp)
                ) {
                    Text("☷  $bandLabel",fontSize=11.sp,color=C_Navy)
                }
                DropdownMenu(
                    expanded = filterExpanded,
                    onDismissRequest = { filterExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(cTr(language,"Ҳамаи синну сол","Все возрасты","All ages")) },
                        onClick = { band="all";filterExpanded=false }
                    )
                    listOf("7_12","13_17","18_plus").forEach { value ->
                        DropdownMenuItem(
                            text = { Text(cBand(language,value)) },
                            onClick = { band=value;filterExpanded=false }
                        )
                    }
                }
            }
        }

        if (entries.isNotEmpty()) {
            val topThree = entries.take(3)
            val maxXp = topThree.maxOf { it.xp }.coerceAtLeast(1)
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                colors = CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.84f)),
                border = BorderStroke(1.dp,Color.White.copy(alpha=0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation=7.dp),
                shape = RoundedCornerShape(21.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(
                            cTr(language,"ПЕШСАФАРОНИ $periodLabel","ЛИДЕРЫ · $periodLabel","TOP SCORES · $periodLabel"),
                            color=C_Gold,fontWeight=FontWeight.ExtraBold,fontSize=9.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text(bandLabel,color=C_Muted,fontSize=9.sp)
                    }
                    Row(
                        Modifier.fillMaxWidth().height(116.dp),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalAlignment=Alignment.Bottom
                    ) {
                        topThree.forEach { entry ->
                            val targetHeight = (entry.xp.toFloat()/maxXp*72f).coerceAtLeast(7f).dp
                            val animatedHeight by animateDpAsState(
                                targetValue=targetHeight,
                                animationSpec=tween(650),
                                label="leaderboard_bar_${entry.rank}"
                            )
                            val barColor = when(entry.rank) {
                                1 -> Color(0xFFD8B56B)
                                2 -> Color(0xFF9EABB7)
                                3 -> Color(0xFFB98772)
                                else -> C_Green
                            }
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.Bottom
                            ) {
                                Box(Modifier.fillMaxWidth().height(76.dp),contentAlignment=Alignment.BottomCenter) {
                                    Box(
                                        Modifier.width(38.dp).height(animatedHeight)
                                            .background(barColor,RoundedCornerShape(topStart=12.dp,topEnd=12.dp))
                                    )
                                }
                                Text("#${entry.rank} ${entry.login}",color=C_Navy,fontWeight=FontWeight.Bold,fontSize=9.sp,maxLines=1)
                                Text("${entry.xp} XP",color=C_Muted,fontSize=9.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(7.dp))
        when {
            loading && entries.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
                CircularProgressIndicator(color=C_Gold)
            }
            error.isNotBlank() && entries.isEmpty() -> Column(Modifier.fillMaxWidth().weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                Text(error,color=Color.Red,fontSize=12.sp)
                OutlinedButton(onClick={refresh++}) { Text(cTr(language,"Навсозӣ","Обновить","Refresh")) }
            }
            entries.isEmpty() -> Column(Modifier.fillMaxWidth().weight(1f).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                Text("🏆",fontSize=36.sp)
                Text(cTr(language,"Ҳоло натиҷа нест","Пока нет результатов","No scores yet"),fontWeight=FontWeight.Bold,color=C_Navy)
                Text(
                    cTr(language,"Дар ин давра ҳоло натиҷаи тасдиқшуда нест.","За этот период пока нет подтверждённых результатов.","No verified scores for this period yet."),
                    color=C_Muted,fontSize=11.sp
                )
            }
            else -> LazyColumn(
                modifier=Modifier.fillMaxWidth().weight(1f),
                verticalArrangement=Arrangement.spacedBy(7.dp),
                contentPadding=PaddingValues(bottom=14.dp)
            ) {
                items(entries,key={it.login}) { e ->
                    val rowColor = if(e.isMe) Color(0xFFFFF4DE) else Color.White.copy(alpha=0.84f)
                    val rowBorder = if(e.isMe) Color(0xFFC69A56) else Color.White.copy(alpha=0.96f)
                    Card(
                        modifier=Modifier.fillMaxWidth().animateContentSize(),
                        colors=CardDefaults.cardColors(containerColor=rowColor),
                        border=BorderStroke(if(e.isMe) 1.6.dp else 1.dp,rowBorder),
                        elevation=CardDefaults.cardElevation(defaultElevation=if(e.isMe) 7.dp else 4.dp),
                        shape=RoundedCornerShape(19.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(
                                when(e.rank){1->"🥇";2->"🥈";3->"🥉";else->"#"+e.rank},
                                modifier=Modifier.width(38.dp),fontWeight=FontWeight.Black,color=C_Navy
                            )
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                Text(
                                    e.login+if(e.isMe) " · "+cTr(language,"ту","вы","you") else "",
                                    fontWeight=FontWeight.Bold,color=C_Navy,fontSize=13.sp
                                )
                                if(e.achievementCodes.isNotEmpty()) {
                                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                        e.achievementCodes.take(4).forEach { code ->
                                            Text(
                                                achievementSymbol(code),
                                                fontSize=13.sp,
                                                modifier=Modifier.background(Color(0xFFF4F1EA),RoundedCornerShape(7.dp)).padding(horizontal=4.dp,vertical=2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(e.completed.toString()+" "+cTr(language,"вазифа","заданий","tasks"),fontSize=10.sp,color=C_Muted)
                            }
                            Text(e.xp.toString()+" XP",fontWeight=FontWeight.Black,color=if(e.isMe) C_Gold else C_Navy,fontSize=15.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun achievementSymbol(code: String): String = when(code) {
    "app_entry" -> "🌟"
    "first_task" -> "⚡"
    "steady_10" -> "🛡️"
    "dedicated_25" -> "💠"
    "expert_50" -> "🏆"
    "legend_100" -> "👑"
    else -> "🏅"
}
