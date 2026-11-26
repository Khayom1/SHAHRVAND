package tj.shahrvand.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

private val SpaceNavy: Color @Composable get() = LocalShahrvandPalette.current.navy
private val SpaceNavyText: Color @Composable get() = LocalShahrvandPalette.current.navyText
private val SpaceGold: Color @Composable get() = LocalShahrvandPalette.current.gold
private val SpaceGreen: Color @Composable get() = LocalShahrvandPalette.current.green
private val SpaceRed: Color @Composable get() = LocalShahrvandPalette.current.danger
private val SpaceMuted: Color @Composable get() = LocalShahrvandPalette.current.muted
private val SpaceBorder: Color @Composable get() = LocalShahrvandPalette.current.border
private val SpacePage: Color @Composable get() = LocalShahrvandPalette.current.background

private fun spaceTr(lang: String, tg: String, ru: String, en: String): String = when (lang) {
    "ru" -> ru
    "en" -> en
    else -> tg
}

data class PersonalNote(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val plannedOn: String?,
    val createdAt: String
)

data class MoneyEntry(
    val id: String,
    val entryType: String,
    val category: String,
    val customCategory: String?,
    val amount: Double,
    val comment: String?,
    val occurredOn: String
)

private data class MoneyCategory(
    val code: String,
    val titleTg: String,
    val titleRu: String,
    val titleEn: String,
    val icon: String
) {
    fun title(lang: String) = spaceTr(lang, titleTg, titleRu, titleEn)
}

private val expenseCategories = listOf(
    MoneyCategory("transport", "Роҳкиро ва нақлиёт", "Транспорт и проезд", "Transport", "🚕"),
    MoneyCategory("groceries", "Хӯрокворӣ", "Продукты", "Groceries", "🛒"),
    MoneyCategory("household", "Молҳои хона", "Товары для дома", "Household", "🏠"),
    MoneyCategory("utilities", "Барқ, об ва коммуналӣ", "Коммунальные услуги", "Utilities", "💡"),
    MoneyCategory("rent", "Иҷораи хона", "Аренда жилья", "Rent", "🏡"),
    MoneyCategory("communication", "Телефон ва интернет", "Телефон и интернет", "Phone and internet", "📱"),
    MoneyCategory("education", "Таҳсил ва китоб", "Учёба и книги", "Education", "📚"),
    MoneyCategory("health", "Саломатӣ", "Здоровье", "Healthcare", "🩺"),
    MoneyCategory("medicine", "Дору", "Лекарства", "Medicine", "💊"),
    MoneyCategory("clothing", "Либос ва пойафзол", "Одежда и обувь", "Clothing", "👕"),
    MoneyCategory("personal", "Хароҷоти шахсӣ", "Личные расходы", "Personal", "🧴"),
    MoneyCategory("entertainment", "Истироҳат ва фароғат", "Отдых и развлечения", "Leisure", "🎬"),
    MoneyCategory("family", "Оила ва фарзандон", "Семья и дети", "Family", "👨‍👩‍👧"),
    MoneyCategory("loan", "Қарз ва пардохт", "Долги и платежи", "Debt and payments", "💳"),
    MoneyCategory("other", "Дигар", "Другое", "Other", "📦")
)

private val incomeCategories = listOf(
    MoneyCategory("salary", "Маош", "Зарплата", "Salary", "💼"),
    MoneyCategory("business", "Даромади тиҷорат", "Доход от бизнеса", "Business income", "🏪"),
    MoneyCategory("freelance", "Кори иловагӣ", "Подработка / фриланс", "Freelance", "💻"),
    MoneyCategory("scholarship", "Стипендия", "Стипендия", "Scholarship", "🎓"),
    MoneyCategory("gift", "Тӯҳфа", "Подарок", "Gift", "🎁"),
    MoneyCategory("refund", "Баргардонидани пул", "Возврат денег", "Refund", "↩️"),
    MoneyCategory("other", "Дигар", "Другое", "Other", "📦")
)

private fun noteKindTitle(lang: String, kind: String): String = when (kind) {
    "plan" -> spaceTr(lang, "Нақшаи фардо", "План на завтра", "Tomorrow's plan")
    "report" -> spaceTr(lang, "Ҳисоботи рӯз", "Отчёт за день", "Daily report")
    "event" -> spaceTr(lang, "Рӯйдод", "Событие", "Event")
    "spiritual" -> spaceTr(lang, "Нақшаи рӯҳонӣ", "Духовный план", "Spiritual plan")
    else -> spaceTr(lang, "Ёддошт", "Заметка", "Note")
}

private fun moneyCategoryTitle(lang: String, category: String, custom: String?): String {
    val source = (expenseCategories + incomeCategories).firstOrNull { it.code == category }
    return if (category == "other" && !custom.isNullOrBlank()) custom
    else source?.title(lang) ?: spaceTr(lang, "Дигар", "Другое", "Other")
}

private fun formatSomoni(value: Double): String = String.format(Locale.US, "%,.2f", value)

object PersonalSpaceRepository {
    private val baseUrl get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publicKey get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    private suspend fun request(
        path: String,
        method: String,
        accessToken: String,
        body: JSONObject? = null,
        prefer: String? = null
    ): String = withContext(Dispatchers.IO) {
        require(accessToken.isNotBlank()) { "Сессияи корбар ёфт нашуд. Дубора ворид шавед." }
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 20_000
                doInput = true
                setRequestProperty("apikey", publicKey)
                setRequestProperty("Authorization", "Bearer " + accessToken)
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                if (prefer != null) setRequestProperty("Prefer", prefer)
            }
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val parsed = runCatching { JSONObject(response) }.getOrNull()
                val message = parsed?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: parsed?.optString("details")?.takeIf { it.isNotBlank() }
                    ?: parsed?.optString("hint")?.takeIf { it.isNotBlank() }
                    ?: "Хатои сервер: HTTP $code"
                throw IllegalStateException(message)
            }
            response
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchNotes(accessToken: String): List<PersonalNote> {
        val response = request(
            "/rest/v1/shahrvand_notes?select=id,kind,title,body,planned_on,created_at&order=created_at.desc&limit=100",
            "GET", accessToken
        )
        val array = JSONArray(response)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    PersonalNote(
                        id = item.optString("id"),
                        kind = item.optString("kind", "note"),
                        title = item.optString("title"),
                        body = item.optString("body"),
                        plannedOn = item.optString("planned_on").takeIf { it.isNotBlank() && it != "null" },
                        createdAt = item.optString("created_at")
                    )
                )
            }
        }
    }

    suspend fun saveNote(
        accessToken: String,
        userId: String,
        existingId: String?,
        kind: String,
        title: String,
        body: String,
        plannedOn: String?
    ) {
        require(title.trim().length in 1..120) { "Сарлавҳа бояд аз 1 то 120 аломат бошад." }
        require(body.trim().length in 1..10000) { "Матнро ворид кунед (то 10 000 аломат)." }
        if (!plannedOn.isNullOrBlank()) LocalDate.parse(plannedOn)
        val payload = JSONObject()
            .put("kind", kind)
            .put("title", title.trim())
            .put("body", body.trim())
            .put("planned_on", plannedOn?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("updated_at", Instant.now().toString())
        val path = if (existingId.isNullOrBlank()) {
            payload.put("user_id", userId)
            "/rest/v1/shahrvand_notes"
        } else {
            "/rest/v1/shahrvand_notes?id=eq.$existingId"
        }
        request(path, if (existingId.isNullOrBlank()) "POST" else "PATCH", accessToken, payload, "return=minimal")
    }

    suspend fun deleteNote(accessToken: String, id: String) {
        request("/rest/v1/shahrvand_notes?id=eq.$id", "DELETE", accessToken)
    }

    suspend fun fetchMoneyEntries(accessToken: String): List<MoneyEntry> {
        val response = request(
            "/rest/v1/shahrvand_financial_entries?select=id,entry_type,category,custom_category,amount,comment,occurred_on&order=occurred_on.desc,created_at.desc&limit=500",
            "GET", accessToken
        )
        val array = JSONArray(response)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    MoneyEntry(
                        id = item.optString("id"),
                        entryType = item.optString("entry_type"),
                        category = item.optString("category"),
                        customCategory = item.optString("custom_category").takeIf { it.isNotBlank() && it != "null" },
                        amount = item.optDouble("amount", 0.0),
                        comment = item.optString("comment").takeIf { it.isNotBlank() && it != "null" },
                        occurredOn = item.optString("occurred_on")
                    )
                )
            }
        }
    }

    suspend fun saveMoneyEntry(
        accessToken: String,
        userId: String,
        existingId: String?,
        entryType: String,
        category: String,
        customCategory: String?,
        amount: Double,
        comment: String?,
        occurredOn: String
    ) {
        require(entryType == "income" || entryType == "expense") { "Навъи амалиёт нодуруст аст." }
        require(amount.isFinite() && amount > 0.0 && amount <= 999999999.99) { "Маблағро дуруст ворид кунед." }
        LocalDate.parse(occurredOn)
        val allowedCategories = if (entryType == "income") incomeCategories else expenseCategories
        require(category in allowedCategories.map { it.code }) { "Ин категория ба навъи интихобшуда мувофиқ нест." }
        if (category == "other") require(!customCategory.isNullOrBlank() && customCategory.trim().length <= 60) {
            "Номи категорияи дигарро нависед (то 60 аломат)."
        }
        val payload = JSONObject()
            .put("entry_type", entryType)
            .put("category", category)
            .put("custom_category", if (category == "other") customCategory?.trim() else JSONObject.NULL)
            .put("amount", amount)
            .put("currency", "TJS")
            .put("comment", comment?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("occurred_on", occurredOn)
            .put("updated_at", Instant.now().toString())
        val path = if (existingId.isNullOrBlank()) {
            payload.put("user_id", userId)
            "/rest/v1/shahrvand_financial_entries"
        } else {
            "/rest/v1/shahrvand_financial_entries?id=eq.$existingId"
        }
        request(path, if (existingId.isNullOrBlank()) "POST" else "PATCH", accessToken, payload, "return=minimal")
    }

    suspend fun deleteMoneyEntry(accessToken: String, id: String) {
        request("/rest/v1/shahrvand_financial_entries?id=eq.$id", "DELETE", accessToken)
    }
}

@Composable
fun FinanceOverviewCard(
    language: String,
    entries: List<MoneyEntry>,
    onOpen: () -> Unit
) {
    val month = LocalDate.now().toString().take(7)
    val income = entries.filter { it.entryType == "income" && it.occurredOn.startsWith(month) }.sumOf { it.amount }
    val expense = entries.filter { it.entryType == "expense" && it.occurredOn.startsWith(month) }.sumOf { it.amount }
    val balance = income - expense
    val maxValue = maxOf(income, expense, 1.0)
    val animatedIncome by animateFloatAsState(
        targetValue = income.toFloat(), animationSpec = tween(850), label = "finance_income_value"
    )
    val animatedExpense by animateFloatAsState(
        targetValue = expense.toFloat(), animationSpec = tween(850), label = "finance_expense_value"
    )
    val animatedBalance by animateFloatAsState(
        targetValue = balance.toFloat(), animationSpec = tween(850), label = "finance_balance_value"
    )
    val animatedIncomeBar by animateFloatAsState(
        targetValue = (income / maxValue).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(950), label = "finance_income_bar"
    )
    val animatedExpenseBar by animateFloatAsState(
        targetValue = (expense / maxValue).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(950), label = "finance_expense_bar"
    )
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = LocalShahrvandPalette.current.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SpaceBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💰", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(spaceTr(language, "Молияи ман", "Мои финансы", "My finances"),
                    color = SpaceNavyText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(spaceTr(language, "Тафсилот →", "Подробнее →", "Details →"),
                    color = SpaceGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            Text(spaceTr(language, "Ҷамъбасти моҳи ҷорӣ", "Итог за текущий месяц", "This month's summary"),
                color = SpaceMuted, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(spaceTr(language, "Даромад", "Доходы", "Income"), color = SpaceGreen, fontSize = 10.sp)
                    Text(formatSomoni(animatedIncome.toDouble()) + " с.", color = SpaceNavyText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(Modifier.weight(1f)) {
                    Text(spaceTr(language, "Хароҷот", "Расходы", "Expenses"), color = SpaceRed, fontSize = 10.sp)
                    Text(formatSomoni(animatedExpense.toDouble()) + " с.", color = SpaceNavyText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(Modifier.weight(1f)) {
                    Text(spaceTr(language, "Бақия", "Баланс", "Balance"), color = SpaceGold, fontSize = 10.sp)
                    Text(formatSomoni(animatedBalance.toDouble()) + " с.", color = if (balance >= 0) SpaceGreen else SpaceRed,
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            val chartIncomeColor = SpaceGreen
            val chartExpenseColor = SpaceRed
            Canvas(Modifier.fillMaxWidth().height(78.dp)) {
                val barWidth = size.width * 0.22f
                val baseY = size.height - 4f
                val maxHeight = size.height - 12f
                val incomeHeight = animatedIncomeBar * maxHeight
                val expenseHeight = animatedExpenseBar * maxHeight
                drawRoundRect(
                    color = chartIncomeColor,
                    topLeft = Offset(size.width * 0.24f, baseY - incomeHeight),
                    size = Size(barWidth, incomeHeight.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = chartExpenseColor,
                    topLeft = Offset(size.width * 0.61f, baseY - expenseHeight),
                    size = Size(barWidth, expenseHeight.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("▮ " + spaceTr(language, "Даромад", "Доходы", "Income"), color = SpaceGreen, fontSize = 10.sp)
                Text("▮ " + spaceTr(language, "Хароҷот", "Расходы", "Expenses"), color = SpaceRed, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun PersonalSpaceHubScreen(
    language: String,
    userId: String,
    initialSection: String,
    entries: List<MoneyEntry>,
    onEntriesChanged: (List<MoneyEntry>) -> Unit,
    getAccessToken: suspend () -> String,
    onBack: () -> Unit
) {
    var section by remember { mutableStateOf(initialSection) }
    LaunchedEffect(initialSection) {
        section = if (initialSection == "budget") "budget" else "notes"
    }

    Column(
        Modifier.fillMaxSize().shahrvandCanvas()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
            ) {
                Text("‹ " + spaceTr(language, "Асосӣ", "Главная", "Home"), color = SpaceGold, fontWeight = FontWeight.Bold)
            }
            Text(
                spaceTr(language, "МАРКАЗИ ШАХСӢ", "ЛИЧНОЕ ПРОСТРАНСТВО", "PERSONAL SPACE"),
                color = SpaceGold, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
            )
            Text(
                spaceTr(language, "Ёддоштҳо ва буҷет", "Заметки и бюджет", "Notes and budget"),
                color = SpaceNavyText, fontSize = 21.sp, fontWeight = FontWeight.Black
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 5.dp),
            shape = RoundedCornerShape(20.dp),
            color = LocalShahrvandPalette.current.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, LocalShahrvandPalette.current.border),
            shadowElevation = 7.dp
        ) {
            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth().padding(5.dp)
            ) {
                SegmentedButton(
                    selected = section == "notes",
                    onClick = { section = "notes" },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) {
                    Text(spaceTr(language, "Ёддоштҳо", "Заметки", "Notes"), fontSize = 11.sp)
                }
                SegmentedButton(
                    selected = section == "budget",
                    onClick = { section = "budget" },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) {
                    Text(spaceTr(language, "Буҷет", "Бюджет", "Budget"), fontSize = 11.sp)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (section == "notes") {
                PersonalNotesScreen(language, userId, getAccessToken)
            } else {
                PersonalFinanceScreen(language, userId, entries, onEntriesChanged, getAccessToken)
            }
        }
    }
}

@Composable
fun PersonalNotesScreen(
    language: String,
    userId: String,
    getAccessToken: suspend () -> String
) {
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf(emptyList<PersonalNote>()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PersonalNote?>(null) }
    var deleting by remember { mutableStateOf<PersonalNote?>(null) }

    suspend fun reload() {
        loading = true
        try {
            notes = PersonalSpaceRepository.fetchNotes(getAccessToken())
            status = ""
        } catch (error: Exception) {
            status = error.message ?: spaceTr(language, "Маълумот бор нашуд.", "Не удалось загрузить данные.", "Could not load data.")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(userId) { reload() }

    Column(Modifier.fillMaxSize().background(SpacePage).padding(horizontal = 16.dp, vertical = 13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(spaceTr(language, "Ёддоштҳо ва рӯйдодҳо", "Заметки и события", "Notes and events"),
                    color = SpaceNavy, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(spaceTr(language, "Нақша, андеша ва ҳисоботи шахсии ту", "Твои планы, мысли и личные отчёты", "Your plans, reflections, and personal reports"),
                    color = SpaceMuted, fontSize = 11.sp)
            }
            Button(onClick = { editing = null; editorOpen = true }, shape = RoundedCornerShape(12.dp)) {
                Text("+ " + spaceTr(language, "Илова", "Добавить", "Add"))
            }
        }
        Spacer(Modifier.height(12.dp))
        if (status.isNotBlank()) {
            Text(status, color = SpaceRed, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SpaceNavyText)
            }
        } else if (notes.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LocalShahrvandPalette.current.surface.copy(alpha = 0.98f)),
                border = BorderStroke(1.dp, LocalShahrvandPalette.current.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📝", fontSize = 38.sp)
                    Text(spaceTr(language, "Ҳоло ёддошт нест", "Пока нет заметок", "No notes yet"),
                        color = SpaceNavyText, fontWeight = FontWeight.Bold)
                    Text(spaceTr(language, "Нақшаи фардо, ҳисоботи рӯз ё рӯйдоди муҳимро илова кун.",
                        "Добавь план на завтра, дневной отчёт или важное событие.",
                        "Add tomorrow's plan, a daily report, or an important event."),
                        color = SpaceMuted, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LocalShahrvandPalette.current.surface.copy(alpha = 0.98f)),
                        border = BorderStroke(1.dp, LocalShahrvandPalette.current.border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(noteKindTitle(language, note.kind), color = SpaceGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                note.plannedOn?.let { Text(it, color = SpaceMuted, fontSize = 10.sp) }
                            }
                            Text(note.title, color = SpaceNavyText, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                modifier = Modifier.padding(top = 5.dp))
                            Text(note.body, color = SpaceMuted, fontSize = 12.sp, lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 5.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editing = note; editorOpen = true }) {
                                    Text(spaceTr(language, "Таҳрир", "Изменить", "Edit"))
                                }
                                TextButton(onClick = { deleting = note }) {
                                    Text(spaceTr(language, "Нест", "Удалить", "Delete"), color = SpaceRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editorOpen) {
        var kind by remember(editing?.id) { mutableStateOf(editing?.kind ?: "note") }
        var title by remember(editing?.id) { mutableStateOf(editing?.title ?: "") }
        var body by remember(editing?.id) { mutableStateOf(editing?.body ?: "") }
        var plannedOn by remember(editing?.id) { mutableStateOf(editing?.plannedOn ?: "") }
        var localError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!saving) editorOpen = false },
            title = { Text(spaceTr(language, "Ёддошт ё нақша", "Заметка или план", "Note or plan"), color = SpaceNavyText) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("note", "plan", "report", "event", "spiritual").forEach { value ->
                            FilterChip(selected = kind == value, onClick = { kind = value },
                                label = { Text(noteKindTitle(language, value), fontSize = 10.sp) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it.take(120) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(spaceTr(language, "Сарлавҳа", "Заголовок", "Title")) })
                    OutlinedTextField(value = body, onValueChange = { body = it.take(10000) },
                        modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 7,
                        label = { Text(spaceTr(language, "Матн / ҳисобот", "Текст / отчёт", "Text / report")) })
                    OutlinedTextField(value = plannedOn, onValueChange = { plannedOn = it.take(10) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(spaceTr(language, "Сана (ихтиёрӣ): YYYY-MM-DD", "Дата (необязательно): YYYY-MM-DD", "Date (optional): YYYY-MM-DD")) })
                    if (localError.isNotBlank()) Text(localError, color = SpaceRed, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    scope.launch {
                        saving = true
                        localError = ""
                        try {
                            PersonalSpaceRepository.saveNote(
                                getAccessToken(), userId, editing?.id, kind, title, body,
                                plannedOn.trim().takeIf { it.isNotBlank() }
                            )
                            reload()
                            editorOpen = false
                        } catch (error: Exception) {
                            localError = error.message ?: spaceTr(language, "Нигоҳ дошта нашуд.", "Не удалось сохранить.", "Could not save.")
                        } finally {
                            saving = false
                        }
                    }
                }) { Text(if (saving) "…" else spaceTr(language, "Нигоҳ доштан", "Сохранить", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { editorOpen = false }, enabled = !saving) {
                    Text(spaceTr(language, "Бекор", "Отмена", "Cancel"))
                }
            }
        )
    }

    deleting?.let { note ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(spaceTr(language, "Ёддошт нест шавад?", "Удалить заметку?", "Delete this note?")) },
            text = { Text(note.title) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            PersonalSpaceRepository.deleteNote(getAccessToken(), note.id)
                            notes = notes.filterNot { it.id == note.id }
                            status = ""
                        } catch (error: Exception) {
                            status = error.message ?: "Delete failed"
                        }
                        deleting = null
                    }
                }) { Text(spaceTr(language, "Нест кардан", "Удалить", "Delete"), color = SpaceRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(spaceTr(language, "Бекор", "Отмена", "Cancel")) }
            }
        )
    }
}

@Composable
fun PersonalFinanceScreen(
    language: String,
    userId: String,
    entries: List<MoneyEntry>,
    onEntriesChanged: (List<MoneyEntry>) -> Unit,
    getAccessToken: suspend () -> String
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MoneyEntry?>(null) }
    var deleting by remember { mutableStateOf<MoneyEntry?>(null) }

    suspend fun reload() {
        loading = true
        try {
            onEntriesChanged(PersonalSpaceRepository.fetchMoneyEntries(getAccessToken()))
            status = ""
        } catch (error: Exception) {
            status = error.message ?: spaceTr(language, "Маълумот бор нашуд.", "Не удалось загрузить данные.", "Could not load data.")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(userId) { reload() }

    val month = LocalDate.now().toString().take(7)
    val monthly = entries.filter { it.occurredOn.startsWith(month) }
    val income = monthly.filter { it.entryType == "income" }.sumOf { it.amount }
    val expense = monthly.filter { it.entryType == "expense" }.sumOf { it.amount }
    val balance = income - expense
    val total = maxOf(income, expense, 1.0)
    val animatedIncome by animateFloatAsState(
        targetValue = income.toFloat(), animationSpec = tween(850), label = "finance_total_income"
    )
    val animatedExpense by animateFloatAsState(
        targetValue = expense.toFloat(), animationSpec = tween(850), label = "finance_total_expense"
    )
    val animatedBalance by animateFloatAsState(
        targetValue = balance.toFloat(), animationSpec = tween(850), label = "finance_total_balance"
    )
    val animatedIncomeShare by animateFloatAsState(
        targetValue = (income / total).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(950), label = "finance_income_share"
    )
    val animatedExpenseShare by animateFloatAsState(
        targetValue = (expense / total).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(950), label = "finance_expense_share"
    )
    val visibleEntries = entries.filter { filter == "all" || it.entryType == filter }

    Column(Modifier.fillMaxSize().background(SpacePage).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(spaceTr(language, "Даромад ва хароҷот", "Доходы и расходы", "Income and expenses"),
                    color = SpaceNavy, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(spaceTr(language, "Ҳамаи маблағҳо бо сомонӣ (TJS)", "Все суммы в сомони (TJS)", "All amounts in Tajik somoni (TJS)"),
                    color = SpaceMuted, fontSize = 10.sp)
            }
            Button(onClick = { editing = null; editorOpen = true }, shape = RoundedCornerShape(12.dp)) {
                Text("+ " + spaceTr(language, "Сабт", "Запись", "Add"))
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceNavy),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(spaceTr(language, "ҶАМЪБАСТИ МОҲ", "ИТОГО ЗА МЕСЯЦ", "MONTHLY TOTAL"),
                    color = Color(0xFFDCE6ED), fontSize = 10.sp)
                Text(spaceTr(language, "Бақия", "Баланс", "Balance") + ": " + formatSomoni(animatedBalance.toDouble()) + " с.",
                    color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(spaceTr(language, "Даромад", "Доходы", "Income"), color = Color(0xFFB7E1D2), fontSize = 10.sp)
                        Text(formatSomoni(animatedIncome.toDouble()) + " с.", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(spaceTr(language, "Хароҷот", "Расходы", "Expenses"), color = Color(0xFFFFC7BC), fontSize = 10.sp)
                        Text(formatSomoni(animatedExpense.toDouble()) + " с.", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(
                    progress = { animatedIncomeShare },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(10.dp)),
                    color = SpaceGreen, trackColor = SpaceRed
                )
                Spacer(Modifier.height(10.dp))
                Canvas(Modifier.fillMaxWidth().height(74.dp)) {
                    val baseY = size.height - 4f
                    val maxBar = size.height - 12f
                    val barWidth = size.width * 0.19f
                    val incomeHeight = animatedIncomeShare * maxBar
                    val expenseHeight = animatedExpenseShare * maxBar
                    val incomeX = size.width * 0.24f
                    val expenseX = size.width * 0.58f
                    drawRoundRect(
                        color = Color(0xFF83CDB2),
                        topLeft = Offset(incomeX, baseY - incomeHeight.coerceAtLeast(4f)),
                        size = Size(barWidth, incomeHeight.coerceAtLeast(4f)),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    drawRoundRect(
                        color = Color(0xFFE48C80),
                        topLeft = Offset(expenseX, baseY - expenseHeight.coerceAtLeast(4f)),
                        size = Size(barWidth, expenseHeight.coerceAtLeast(4f)),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.38f),
                        start = Offset(size.width * 0.12f, baseY),
                        end = Offset(size.width * 0.88f, baseY),
                        strokeWidth = 2f
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all", "income", "expense").forEach { value ->
                FilterChip(selected = filter == value, onClick = { filter = value }, label = {
                    Text(when (value) {
                        "income" -> spaceTr(language, "Даромад", "Доходы", "Income")
                        "expense" -> spaceTr(language, "Хароҷот", "Расходы", "Expenses")
                        else -> spaceTr(language, "Ҳама", "Все", "All")
                    }, fontSize = 11.sp)
                })
            }
        }
        if (status.isNotBlank()) Text(status, color = SpaceRed, fontSize = 11.sp)
        if (loading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SpaceNavy)
            }
        } else if (visibleEntries.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LocalShahrvandPalette.current.surface.copy(alpha = 0.98f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(21.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 35.sp)
                    Text(spaceTr(language, "Ҳоло ягон сабт нест", "Пока нет записей", "No records yet"),
                        color = SpaceNavy, fontWeight = FontWeight.Bold)
                    Text(spaceTr(language, "Даромад ё хароҷоти аввалини худро илова кун.",
                        "Добавь первую запись о доходе или расходе.",
                        "Add your first income or expense record."),
                        color = SpaceMuted, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(visibleEntries, key = { it.id }) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LocalShahrvandPalette.current.surface.copy(alpha = 0.98f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.96f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                        shape = RoundedCornerShape(19.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text((expenseCategories + incomeCategories).firstOrNull { it.code == entry.category }?.icon ?: "📦",
                                fontSize = 22.sp)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(moneyCategoryTitle(language, entry.category, entry.customCategory),
                                    color = SpaceNavy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(entry.occurredOn + (entry.comment?.let { " • $it" } ?: ""),
                                    color = SpaceMuted, fontSize = 10.sp, maxLines = 2)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text((if (entry.entryType == "income") "+" else "−") + formatSomoni(entry.amount) + " с.",
                                    color = if (entry.entryType == "income") SpaceGreen else SpaceRed,
                                    fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Row {
                                    TextButton(onClick = { editing = entry; editorOpen = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                        Text(spaceTr(language, "Таҳрир", "Изменить", "Edit"), fontSize = 10.sp)
                                    }
                                    TextButton(onClick = { deleting = entry }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                        Text(spaceTr(language, "Нест", "Удалить", "Delete"), color = SpaceRed, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editorOpen) {
        var entryType by remember(editing?.id) { mutableStateOf(editing?.entryType ?: "expense") }
        var category by remember(editing?.id) { mutableStateOf(editing?.category ?: "transport") }
        var customCategory by remember(editing?.id) { mutableStateOf(editing?.customCategory ?: "") }
        var amountText by remember(editing?.id) { mutableStateOf(editing?.amount?.let { formatSomoni(it).replace(",", "") } ?: "") }
        var comment by remember(editing?.id) { mutableStateOf(editing?.comment ?: "") }
        var occurredOn by remember(editing?.id) { mutableStateOf(editing?.occurredOn ?: LocalDate.now().toString()) }
        var localError by remember { mutableStateOf("") }
        val categories = if (entryType == "income") incomeCategories else expenseCategories
        AlertDialog(
            onDismissRequest = { if (!saving) editorOpen = false },
            title = { Text(spaceTr(language, "Сабти молиявӣ", "Финансовая запись", "Financial record"), color = SpaceNavy) },
            text = {
                Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = entryType == "expense", onClick = { entryType = "expense"; category = "transport"; customCategory = "" },
                            label = { Text(spaceTr(language, "Хароҷот", "Расход", "Expense")) })
                        FilterChip(selected = entryType == "income", onClick = { entryType = "income"; category = "salary"; customCategory = "" },
                            label = { Text(spaceTr(language, "Даромад", "Доход", "Income")) })
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { value ->
                            if (value.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amountText = value
                        },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(spaceTr(language, "Маблағ (сомонӣ)", "Сумма (сомони)", "Amount (TJS)")) }
                    )
                    Text(spaceTr(language, "Категорияро интихоб кун", "Выбери категорию", "Choose a category"),
                        color = SpaceNavy, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.chunked(2).forEach { rowCategories ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                rowCategories.forEach { item ->
                                    FilterChip(
                                        selected = category == item.code,
                                        onClick = { category = item.code },
                                        label = { Text(item.icon + " " + item.title(language), fontSize = 9.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowCategories.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    if (category == "other") {
                        OutlinedTextField(value = customCategory, onValueChange = { customCategory = it.take(60) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text(spaceTr(language, "Номи категорияи дигар", "Название другой категории", "Custom category name")) })
                    }
                    OutlinedTextField(value = occurredOn, onValueChange = { occurredOn = it.take(10) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(spaceTr(language, "Сана: YYYY-MM-DD", "Дата: YYYY-MM-DD", "Date: YYYY-MM-DD")) })
                    OutlinedTextField(value = comment, onValueChange = { comment = it.take(500) },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3,
                        label = { Text(spaceTr(language, "Шарҳ (ихтиёрӣ)", "Комментарий (необязательно)", "Comment (optional)")) })
                    if (localError.isNotBlank()) Text(localError, color = SpaceRed, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    scope.launch {
                        saving = true
                        localError = ""
                        try {
                            val amount = amountText.replace(",", ".").toDoubleOrNull()
                                ?: throw IllegalArgumentException(spaceTr(language, "Маблағро дуруст навис.", "Укажи корректную сумму.", "Enter a valid amount."))
                            PersonalSpaceRepository.saveMoneyEntry(
                                getAccessToken(), userId, editing?.id, entryType, category,
                                customCategory.takeIf { category == "other" }, amount, comment,
                                occurredOn.trim()
                            )
                            onEntriesChanged(PersonalSpaceRepository.fetchMoneyEntries(getAccessToken()))
                            editorOpen = false
                        } catch (error: Exception) {
                            localError = error.message ?: spaceTr(language, "Сабт нашуд.", "Не удалось сохранить.", "Could not save.")
                        } finally {
                            saving = false
                        }
                    }
                }) { Text(if (saving) "…" else spaceTr(language, "Нигоҳ доштан", "Сохранить", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { editorOpen = false }, enabled = !saving) {
                    Text(spaceTr(language, "Бекор", "Отмена", "Cancel"))
                }
            }
        )
    }

    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(spaceTr(language, "Сабт нест шавад?", "Удалить запись?", "Delete this record?")) },
            text = { Text(moneyCategoryTitle(language, entry.category, entry.customCategory) + " • " + formatSomoni(entry.amount) + " с.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            PersonalSpaceRepository.deleteMoneyEntry(getAccessToken(), entry.id)
                            onEntriesChanged(entries.filterNot { it.id == entry.id })
                            status = ""
                        } catch (error: Exception) {
                            status = error.message ?: "Delete failed"
                        }
                        deleting = null
                    }
                }) { Text(spaceTr(language, "Нест кардан", "Удалить", "Delete"), color = SpaceRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(spaceTr(language, "Бекор", "Отмена", "Cancel")) }
            }
        )
    }
}
