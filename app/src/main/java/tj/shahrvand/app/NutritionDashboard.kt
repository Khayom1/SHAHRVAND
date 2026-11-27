package tj.shahrvand.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

private fun nutritionDashboardTr(language: String, tg: String, ru: String, en: String): String =
    when (language) {
        "ru" -> ru
        "en" -> en
        else -> tg
    }

@Composable
internal fun NutritionDashboardCard(
    language: String,
    getAccessToken: suspend () -> String,
    onOpenNutrition: () -> Unit
) {
    val palette = LocalShahrvandPalette.current
    val scope = rememberCoroutineScope()
    var meals by remember { mutableStateOf<List<MealLogRecord>>(emptyList()) }
    var water by remember { mutableStateOf<List<WaterLogRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        error = ""
        try {
            val token = getAccessToken()
            meals = NutritionRepository.loadTodayMeals(token)
            water = NutritionRepository.loadTodayWater(token)
        } catch (e: Exception) {
            error = e.message ?: nutritionDashboardTr(language, "Маълумоти ғизо дастрас нест.", "Данные питания недоступны.", "Nutrition data is unavailable.")
        } finally {
            loading = false
        }
    }

    val calories = meals.sumOf { it.nutrients.energyKcal }
    val protein = meals.sumOf { it.nutrients.proteinG }
    val waterMl = water.sumOf { it.volumeMl }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        nutritionDashboardTr(language, "ҒИЗО ВА ОБ", "ПИТАНИЕ И ВОДА", "NUTRITION & WATER"),
                        color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        nutritionDashboardTr(language, "Имрӯз чӣ истеъмол кардӣ?", "Что ты употребил сегодня?", "Today's nutrition"),
                        color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
                Surface(color = palette.surfaceVariant, shape = RoundedCornerShape(11.dp)) {
                    Text(
                        nutritionDashboardTr(language, "${meals.size} сабт", "${meals.size} записей", "${meals.size} entries"),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        color = palette.navy, fontSize = 9.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else if (error.isNotBlank()) {
                Text(error, color = palette.muted, fontSize = 10.sp)
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NutritionSummaryMetric(
                        Modifier.weight(1f),
                        String.format(Locale.US, "%.0f", calories),
                        "kcal",
                        nutritionDashboardTr(language, "Калория", "Калории", "Calories"),
                        palette
                    )
                    NutritionSummaryMetric(
                        Modifier.weight(1f),
                        String.format(Locale.US, "%.1f", protein),
                        "г",
                        nutritionDashboardTr(language, "Сафеда", "Белок", "Protein"),
                        palette
                    )
                    NutritionSummaryMetric(
                        Modifier.weight(1f),
                        waterMl.toString(),
                        "мл",
                        nutritionDashboardTr(language, "Об", "Вода", "Water"),
                        palette
                    )
                }
                Text(
                    nutritionDashboardTr(
                        language,
                        "Инҳо танҳо сабтҳои воридшудаанд; калорияҳои сабтнашуда ҳисоб намешаванд.",
                        "Это только внесённые записи; незаписанная еда не учитывается.",
                        "These are logged entries only; unrecorded food is not estimated."
                    ),
                    color = palette.muted, fontSize = 9.sp, lineHeight = 13.sp
                )
            }
            Button(
                onClick = onOpenNutrition,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text(nutritionDashboardTr(language, "Кушодани рӯзномаи ғизо", "Открыть дневник питания", "Open nutrition diary"))
            }
        }
    }
}

@Composable
private fun NutritionSummaryMetric(
    modifier: Modifier,
    value: String,
    unit: String,
    label: String,
    palette: ShahrvandPalette
) {
    Surface(
        modifier = modifier,
        color = palette.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 9.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(value, color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(unit, color = palette.muted, fontSize = 8.sp)
            }
            Text(label, color = palette.muted, fontSize = 9.sp, maxLines = 1)
        }
    }
}
