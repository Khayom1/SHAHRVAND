package tj.shahrvand.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

private fun healthTr(language: String, tg: String, ru: String, en: String): String =
    when (language) {
        "ru" -> ru
        "en" -> en
        else -> tg
    }

@Composable
internal fun HealthProfileScreen(
    language: String,
    userId: String,
    age: Int,
    getAccessToken: suspend () -> String,
    onBack: () -> Unit
) {
    val palette = LocalShahrvandPalette.current
    val scope = rememberCoroutineScope()
    var loading by remember(userId) { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var heightText by remember(userId) { mutableStateOf("") }
    var weightText by remember(userId) { mutableStateOf("") }
    var sexAtBirth by remember(userId) { mutableStateOf("not_specified") }
    var activityLevel by remember(userId) { mutableStateOf("light") }
    var healthGoal by remember(userId) { mutableStateOf("build_habit") }
    var previousWeightKg by remember(userId) { mutableStateOf<Double?>(null) }

    LaunchedEffect(userId) {
        loading = true
        error = ""
        try {
            val snapshot = HealthProfileRepository.load(userId, getAccessToken())
            snapshot.profile?.let { profile ->
                heightText = formatHealthNumber(profile.heightCm)
                sexAtBirth = profile.sexAtBirth
                activityLevel = profile.activityLevel
                healthGoal = profile.healthGoal
            }
            snapshot.latestMeasurement?.let { measurement ->
                weightText = formatHealthNumber(measurement.weightKg)
                previousWeightKg = measurement.weightKg
            }
        } catch (e: Exception) {
            error = e.message ?: healthTr(language, "Маълумот гирифта нашуд.", "Не удалось загрузить данные.", "Could not load the profile.")
        } finally {
            loading = false
        }
    }

    val height = heightText.replace(',', '.').toDoubleOrNull()
    val weight = weightText.replace(',', '.').toDoubleOrNull()
    val bmi = if (height != null && weight != null) {
        HealthAssessment.assessBmi(age, height, weight)
    } else null

    Column(
        Modifier.fillMaxSize().shahrvandCanvas()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = healthTr(language, "Бозгашт", "Назад", "Back"), tint = palette.ink)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    healthTr(language, "САЛОМАТИИ МАН", "МОЁ ЗДОРОВЬЕ", "MY HEALTH"),
                    color = palette.navyLight, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(
                    healthTr(language, "Профили саломатӣ", "Профиль здоровья", "Health profile"),
                    color = palette.ink, fontSize = 21.sp, fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = palette.navyLight, modifier = Modifier.size(24.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = palette.surface),
            border = BorderStroke(1.dp, palette.border),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    healthTr(language, "МАЪЛУМОТИ БАДАН", "ПАРАМЕТРЫ ТЕЛА", "BODY MEASUREMENTS"),
                    color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { value -> heightText = value.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                        modifier = Modifier.weight(1f),
                        label = { Text(healthTr(language, "Қад", "Рост", "Height")) },
                        suffix = { Text("см") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { value -> weightText = value.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                        modifier = Modifier.weight(1f),
                        label = { Text(healthTr(language, "Вазн", "Вес", "Weight")) },
                        suffix = { Text("кг") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Text(
                    healthTr(language, "Ҷинси биологӣ (танҳо барои ҳисобҳои тиббӣ)", "Биологический пол (только для медицинских расчётов)", "Sex at birth (for health estimates only)"),
                    color = palette.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    listOf(
                        "male" to healthTr(language, "Мард", "Мужской", "Male"),
                        "female" to healthTr(language, "Зан", "Женский", "Female"),
                        "not_specified" to healthTr(language, "Намегӯям", "Не указывать", "Skip")
                    ).forEach { option ->
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            RadioButton(
                                selected = sexAtBirth == option.first,
                                onClick = { sexAtBirth = option.first }
                            )
                            Text(option.second, color = palette.ink, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
                HealthProfileDropdown(
                    language = language,
                    label = healthTr(language, "Фаъолияти ҳаррӯза", "Ежедневная активность", "Daily activity"),
                    selected = activityLevel,
                    options = listOf(
                        "sedentary" to healthTr(language, "Камҳаракат", "Малоподвижный", "Sedentary"),
                        "light" to healthTr(language, "Сабук", "Лёгкая", "Light"),
                        "moderate" to healthTr(language, "Миёна", "Средняя", "Moderate"),
                        "high" to healthTr(language, "Баланд", "Высокая", "High")
                    ),
                    onSelected = { activityLevel = it }
                )
                HealthProfileDropdown(
                    language = language,
                    label = healthTr(language, "Ҳадафи ман", "Моя цель", "My goal"),
                    selected = healthGoal,
                    options = listOf(
                        "build_habit" to healthTr(language, "Одатҳои солим", "Здоровые привычки", "Healthy habits"),
                        "maintain" to healthTr(language, "Нигоҳ доштани ҳолати ҷорӣ", "Поддерживать форму", "Maintain current health"),
                        "build_strength" to healthTr(language, "Қувват ва мушакҳо", "Сила и мышцы", "Build strength"),
                        "improve_endurance" to healthTr(language, "Тобоварӣ", "Выносливость", "Improve endurance"),
                        "gain_weight" to healthTr(language, "Зиёд кардани вазн", "Набор веса", "Gain weight"),
                        "weight_management" to healthTr(language, "Идоракунии вазн", "Контроль веса", "Weight management")
                    ),
                    onSelected = { healthGoal = it }
                )
                Text(
                    healthTr(language, "Синну сол аз профили аккаунти SHAHRVAND гирифта мешавад: ", "Возраст берётся из профиля SHAHRVAND: ", "Age is taken from your SHAHRVAND account: ") + age,
                    color = palette.muted, fontSize = 10.sp
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = palette.surface),
            border = BorderStroke(1.dp, palette.border),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    healthTr(language, "ТАҲЛИЛИ ИБТИДОӢ", "ПЕРВИЧНАЯ ОЦЕНКА", "INITIAL SCREENING"),
                    color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                )
                when {
                    bmi == null -> Text(
                        healthTr(language, "Қад ва вазнро дуруст ворид кун, то нишондиҳандаи BMI ҳисоб шавад.", "Введи корректные рост и вес, чтобы рассчитать BMI.", "Enter a valid height and weight to calculate BMI."),
                        color = palette.muted, fontSize = 12.sp, lineHeight = 18.sp
                    )
                    bmi.requiresPediatricReference -> {
                        Text(
                            "BMI " + bmi.bmi.toString(),
                            color = palette.ink, fontSize = 30.sp, fontWeight = FontWeight.Black
                        )
                        Text(
                            healthTr(
                                language,
                                "Барои синни то 20 сол меъёрҳои калонсолон истифода намешаванд. Барои гурӯҳбандии дуруст ҷадвали WHO BMI-for-age бо синну сол ва ҷинс лозим аст.",
                                "До 20 лет нельзя применять взрослые пороги BMI. Для корректной оценки нужны таблицы WHO BMI-for-age с учётом возраста и пола.",
                                "Adult BMI cut-offs are not used under age 20. A WHO BMI-for-age reference that accounts for age and sex is required for classification."
                            ),
                            color = palette.muted, fontSize = 11.sp, lineHeight = 17.sp
                        )
                    }
                    else -> {
                        Text(
                            bmi.bmi.toString(),
                            color = palette.ink, fontSize = 34.sp, fontWeight = FontWeight.Black
                        )
                        val label = when (bmi.adultCategory) {
                            AdultBmiCategory.UNDERWEIGHT -> healthTr(language, "Вазни кам", "Недостаточная масса", "Underweight")
                            AdultBmiCategory.HEALTHY_RANGE -> healthTr(language, "Ҳудуди муқаррарӣ", "Нормальный диапазон", "Healthy range")
                            AdultBmiCategory.OVERWEIGHT -> healthTr(language, "Вазни зиёдатӣ", "Избыточная масса", "Overweight")
                            AdultBmiCategory.OBESITY -> healthTr(language, "Фарбеҳӣ", "Ожирение", "Obesity")
                            null -> ""
                        }
                        Text(label, color = palette.navyLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            healthTr(
                                language,
                                "BMI воситаи скрининг аст, на ташхиси тиббӣ. Барои хулосаи шахсӣ омилҳои дигар низ заруранд.",
                                "BMI — скрининговый показатель, а не медицинский диагноз. Для персонального заключения нужны и другие факторы.",
                                "BMI is a screening measure, not a medical diagnosis. A personal assessment needs other factors too."
                            ),
                            color = palette.muted, fontSize = 10.sp, lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant),
            border = BorderStroke(1.dp, palette.border),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = palette.navyLight)
                Text(
                    healthTr(
                        language,
                        "Ин маълумот хусусӣ аст. Қад, вазн ва интихоби саломатии ту ба рейтинги ҷамъиятӣ намоиш дода намешаванд. Тавсияҳои калория ва машқи тиббӣ дар марҳилаи баъдӣ бо меъёрҳои санҷидашуда илова мешаванд.",
                        "Эти данные приватны. Рост, вес и параметры здоровья не отображаются в публичном рейтинге. Расчёты калорий и медицинские рекомендации будут добавлены позже после проверки методик.",
                        "This information is private. Your height, weight and health choices are not shown on public leaderboards. Calorie targets and clinical exercise recommendations will be added later after method validation."
                    ),
                    color = palette.ink, fontSize = 10.sp, lineHeight = 15.sp
                )
            }
        }

        if (error.isNotBlank()) {
            Text(error, color = palette.danger, fontSize = 11.sp, lineHeight = 16.sp)
        }
        if (status.isNotBlank()) {
            Surface(
                color = palette.successSurface,
                border = BorderStroke(1.dp, palette.successBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(status, modifier = Modifier.fillMaxWidth().padding(11.dp), color = palette.green, fontSize = 11.sp)
            }
        }

        Button(
            onClick = {
                val parsedHeight = heightText.replace(',', '.').toDoubleOrNull()
                val parsedWeight = weightText.replace(',', '.').toDoubleOrNull()
                if (parsedHeight == null || parsedHeight !in 80.0..250.0) {
                    error = healthTr(language, "Қад бояд аз 80 то 250 см бошад.", "Рост должен быть от 80 до 250 см.", "Height must be between 80 and 250 cm.")
                    return@Button
                }
                if (parsedWeight == null || parsedWeight !in 5.0..500.0) {
                    error = healthTr(language, "Вазн бояд аз 5 то 500 кг бошад.", "Вес должен быть от 5 до 500 кг.", "Weight must be between 5 and 500 kg.")
                    return@Button
                }
                scope.launch {
                    saving = true
                    error = ""
                    status = ""
                    try {
                        val weightWasAdded = HealthProfileRepository.save(
                            userId = userId,
                            accessToken = getAccessToken(),
                            heightCm = parsedHeight,
                            weightKg = parsedWeight,
                            sexAtBirth = sexAtBirth,
                            activityLevel = activityLevel,
                            healthGoal = healthGoal,
                            previousWeightKg = previousWeightKg
                        )
                        previousWeightKg = parsedWeight
                        status = if (weightWasAdded) {
                            healthTr(language, "Профили саломатӣ ва ченкунии вазн сабт шуданд.", "Профиль здоровья и измерение веса сохранены.", "Health profile and weight measurement saved.")
                        } else {
                            healthTr(language, "Профили саломатӣ нав шуд. Вазн тағйир наёфтааст; ченкунии такрорӣ илова нашуд.", "Профиль обновлён. Вес не изменился; повторное измерение не добавлено.", "Profile updated. Weight is unchanged, so no duplicate measurement was added.")
                        }
                    } catch (e: Exception) {
                        error = e.message ?: healthTr(language, "Сабт иҷро нашуд.", "Не удалось сохранить данные.", "Could not save the profile.")
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !loading && !saving && userId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(15.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(8.dp))
            } else {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.size(8.dp))
            }
            Text(healthTr(language, "Сабти маълумот", "Сохранить данные", "Save health profile"), fontWeight = FontWeight.Bold)
        }

        if (loading) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun HealthProfileDropdown(
    language: String,
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    val palette = LocalShahrvandPalette.current
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second
        ?: healthTr(language, "Интихоб кун", "Выбрать", "Choose")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = palette.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.layout.Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(selectedLabel, modifier = Modifier.weight(1f), textAlign = TextAlign.Start, fontSize = 12.sp)
                Text("⌄", fontSize = 17.sp, color = palette.muted)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            onSelected(option.first)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatHealthNumber(value: Double): String =
    String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
