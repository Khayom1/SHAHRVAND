package tj.shahrvand.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun nutritionTr(language: String, tg: String, ru: String, en: String): String =
    when (language) {
        "ru" -> ru
        "en" -> en
        else -> tg
    }

private fun FoodCatalogItem.localizedName(language: String): String = when (language) {
    "ru" -> nameRu
    "en" -> nameEn
    else -> nameTg
}

private fun foodPreparationLabel(language: String, state: FoodPreparationState): String = when (state) {
    FoodPreparationState.RAW -> nutritionTr(language, "хом", "сырой", "raw")
    FoodPreparationState.COOKED -> nutritionTr(language, "пухта", "приготовленный", "cooked")
    FoodPreparationState.DRIED -> nutritionTr(language, "хушк", "сушёный", "dried")
    FoodPreparationState.BAKED -> nutritionTr(language, "дар танӯр пухта", "запечённый", "baked")
    FoodPreparationState.FERMENTED -> nutritionTr(language, "ферментшуда", "ферментированный", "fermented")
    FoodPreparationState.CANNED -> nutritionTr(language, "консервшуда", "консервированный", "canned")
    FoodPreparationState.UNKNOWN -> nutritionTr(language, "номаълум", "неизвестно", "unknown")
}

private fun macroSummary(language: String, protein: Double, carbs: Double, fat: Double): String {
    val p = nutritionTr(language, "сафеда", "белка", "protein")
    val c = nutritionTr(language, "карбогидрат", "углеводов", "carbs")
    val f = nutritionTr(language, "равған", "жиров", "fat")
    return String.format(Locale.US, "%.1f г %s · %.1f г %s · %.1f г %s", protein, p, carbs, c, fat, f)
}

private fun nutritionSourceLabel(language: String, source: NutritionDataSource): String = when (source) {
    NutritionDataSource.USDA_FOOD_DATA_CENTRAL -> "USDA FoodData Central"
    NutritionDataSource.TAJIK_LAB_ANALYSIS ->
        nutritionTr(language, "Таҳлили лаборатории Тоҷикистон", "Лабораторный анализ Таджикистана", "Tajikistan laboratory analysis")
    NutritionDataSource.MANUFACTURER_LABEL ->
        nutritionTr(language, "Нишони истеҳсолкунанда", "Этикетка производителя", "Manufacturer label")
    NutritionDataSource.RECIPE_ESTIMATE ->
        nutritionTr(language, "Ҳисоби дорухат", "Расчёт по рецепту", "Recipe calculation")
    NutritionDataSource.USER_ENTERED ->
        nutritionTr(language, "Маълумоти воридкардаи корбар", "Данные пользователя", "User-entered data")
}

@Composable
internal fun NutritionScreen(
    language: String,
    getAccessToken: suspend () -> String,
    onBack: () -> Unit
) {
    val palette = LocalShahrvandPalette.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var foods by remember { mutableStateOf(emptyList<FoodCatalogItem>()) }
    var meals by remember { mutableStateOf(emptyList<MealLogRecord>()) }
    var waterLogs by remember { mutableStateOf(emptyList<WaterLogRecord>()) }
    var loading by remember { mutableStateOf(true) }
    var searching by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<FoodCatalogItem?>(null) }
    var showRecipeCalculator by remember { mutableStateOf(false) }
    var portionText by remember { mutableStateOf("100") }

    suspend fun reloadToday() {
        val token = getAccessToken()
        meals = NutritionRepository.loadTodayMeals(token)
        waterLogs = NutritionRepository.loadTodayWater(token)
    }

    LaunchedEffect(Unit) {
        loading = true
        error = ""
        try {
            val token = getAccessToken()
            foods = NutritionRepository.searchFoods("", token)
            meals = NutritionRepository.loadTodayMeals(token)
            waterLogs = NutritionRepository.loadTodayWater(token)
        } catch (e: Exception) {
            error = e.message ?: nutritionTr(language, "Маълумот гирифта нашуд.", "Не удалось загрузить данные.", "Could not load nutrition data.")
        } finally {
            loading = false
        }
    }

    val totals = NutritionTotals(
        energyKcal = meals.sumOf { it.nutrients.energyKcal },
        proteinG = meals.sumOf { it.nutrients.proteinG },
        carbohydrateG = meals.sumOf { it.nutrients.carbohydrateG },
        fatG = meals.sumOf { it.nutrients.fatG },
        fiberG = meals.sumOf { it.nutrients.fiberG },
        sodiumMg = meals.sumOf { it.nutrients.sodiumMg }
    )
    val waterTotal = waterLogs.sumOf { it.volumeMl }

    if (showRecipeCalculator) {
        RecipeCalculatorScreen(
            language = language,
            foods = foods,
            onBack = { showRecipeCalculator = false },
            onSaveMeal = { name, recipeIngredients, cookedWeight, portion ->
                val added = NutritionRepository.addRecipeMeal(
                    displayName = name,
                    ingredients = recipeIngredients,
                    finalCookedWeightGrams = cookedWeight,
                    portionGrams = portion,
                    accessToken = getAccessToken()
                )
                meals = listOf(added) + meals
                added
            }
        )
        return
    }

    Column(
        Modifier.fillMaxSize().shahrvandCanvas(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = nutritionTr(language, "Бозгашт", "Назад", "Back"), tint = palette.ink)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        nutritionTr(language, "ҒИЗО ВА ҒИЗОГИРӢ", "ПИТАНИЕ", "NUTRITION"),
                        color = palette.navyLight, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        nutritionTr(language, "Рӯзномаи ғизо", "Дневник питания", "Food diary"),
                        color = palette.ink, fontSize = 21.sp, fontWeight = FontWeight.Bold
                    )
                }
                Surface(shape = CircleShape, color = palette.surface, border = BorderStroke(1.dp, palette.border)) {
                    Icon(Icons.Filled.LocalDining, contentDescription = null, tint = palette.navyLight, modifier = Modifier.padding(10.dp).size(21.dp))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(1.dp, palette.border),
                shape = RoundedCornerShape(23.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(nutritionTr(language, "ЭНЕРГИЯИ ИМРӮЗА", "ЭНЕРГИЯ ЗА СЕГОДНЯ", "TODAY'S ENERGY"), color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(String.format(Locale.US, "%.0f", totals.energyKcal), color = palette.ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
                                Text("kcal", color = palette.muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
                            }
                            Text(nutritionTr(language, "Аз сабтҳои ғизоии имрӯз", "По записям за сегодня", "From today's logged meals"), color = palette.muted, fontSize = 10.sp)
                        }
                        Box(
                            Modifier.size(76.dp).clip(CircleShape).background(palette.goldSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LocalDining, contentDescription = null, tint = palette.gold, modifier = Modifier.size(34.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroPill(language, nutritionTr(language, "Сафеда", "Белки", "Protein"), totals.proteinG, Color(0xFF4E8DE8), Modifier.weight(1f))
                        MacroPill(language, nutritionTr(language, "Карбогидрат", "Углеводы", "Carbs"), totals.carbohydrateG, Color(0xFF37AE91), Modifier.weight(1f))
                        MacroPill(language, nutritionTr(language, "Равған", "Жиры", "Fat"), totals.fatG, Color(0xFF9B77D8), Modifier.weight(1f))
                    }
                    Text(
                        nutritionTr(
                            language,
                            "Ҳадафи калориявӣ ҳоло таъин нашудааст. Ин танҳо ҷамъбасти сабтҳои воридкардаи туст.",
                            "Цель по калориям пока не задана. Это только сумма внесённых тобой записей.",
                            "No calorie target is set yet. This is only the total from meals you have logged."
                        ),
                        color = palette.muted, fontSize = 10.sp, lineHeight = 15.sp
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(1.dp, palette.border),
                shape = RoundedCornerShape(21.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = palette.navyLight)
                        Text(nutritionTr(language, "Маҳсулотро интихоб кун", "Выбери продукт", "Choose a food"), color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it.take(60) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(nutritionTr(language, "Ном ё маҳсулот", "Название продукта", "Food name")) },
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    searching = true
                                    error = ""
                                    try {
                                        foods = NutritionRepository.searchFoods(query, getAccessToken())
                                    } catch (e: Exception) {
                                        error = e.message ?: nutritionTr(language, "Ҷустуҷӯ иҷро нашуд.", "Поиск не выполнен.", "Search failed.")
                                    } finally {
                                        searching = false
                                    }
                                }
                            },
                            enabled = !searching && !loading,
                            modifier = Modifier.heightIn(min = 52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            else Icon(Icons.Filled.Search, contentDescription = nutritionTr(language, "Ҷустуҷӯ", "Поиск", "Search"))
                        }
                    }
                    when {
                        searching || loading -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                        foods.isEmpty() -> Text(
                            nutritionTr(
                                language,
                                "Дар феҳрист ҳоло маҳсулоти фаъол нест. Баъд аз тасдиқи маълумоти ғизоӣ маҳсулот илова мешавад.",
                                "В каталоге пока нет активных продуктов. Продукты появятся после проверки пищевых данных.",
                                "There are no active foods in the catalogue yet. Foods will appear after their nutrition data is verified."
                            ),
                            color = palette.muted, fontSize = 11.sp, lineHeight = 16.sp
                        )
                        else -> Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            foods.take(8).forEach { food ->
                                FoodSearchRow(
                                    language = language,
                                    food = food,
                                    onClick = {
                                        selectedFood = food
                                        portionText = "100"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showRecipeCalculator = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.RestaurantMenu, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(nutritionTr(language, "Ҳисоб кардани дорухат", "Рассчитать рецепт", "Calculate a recipe"))
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(nutritionTr(language, "Ғизои сабтшуда", "Записанные блюда", "Logged meals"), color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(nutritionTr(language, "Имрӯз", "Сегодня", "Today"), color = palette.muted, fontSize = 10.sp)
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = palette.surface, border = BorderStroke(1.dp, palette.border)) {
                        Text("${meals.size}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = palette.navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            if (meals.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(17.dp), color = palette.surface.copy(alpha = 0.9f), border = BorderStroke(1.dp, palette.border)) {
                        Text(
                            nutritionTr(language, "Ҳанӯз хӯрок сабт нашудааст. Аз рӯйхати боло маҳсулотро интихоб кун.", "Пока нет записей. Выбери продукт из списка выше.", "No meals logged yet. Choose a food from the list above."),
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            color = palette.muted, fontSize = 11.sp, lineHeight = 16.sp
                        )
                    }
                }
            } else {
                items(meals, key = { it.id }) { meal ->
                    MealLogCard(
                        language = language,
                        meal = meal,
                        onDelete = {
                            scope.launch {
                                try {
                                    NutritionRepository.deleteMeal(meal.id, getAccessToken())
                                    meals = meals.filterNot { it.id == meal.id }
                                } catch (e: Exception) {
                                    error = e.message ?: nutritionTr(language, "Сабт нест карда нашуд.", "Не удалось удалить запись.", "Could not delete the entry.")
                                }
                            }
                        }
                    )
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Color(0xFF438FDD))
                            Text(nutritionTr(language, "Обнӯшӣ", "Вода", "Water"), color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${waterTotal} мл", color = palette.navyLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(200, 250, 500).forEach { amount ->
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            saving = true
                                            error = ""
                                            try {
                                                val added = NutritionRepository.addWater(amount, getAccessToken())
                                                waterLogs = listOf(added) + waterLogs
                                            } catch (e: Exception) {
                                                error = e.message ?: nutritionTr(language, "Об сабт нашуд.", "Не удалось записать воду.", "Could not log water.")
                                            } finally {
                                                saving = false
                                            }
                                        }
                                    },
                                    enabled = !saving,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("+${amount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        waterLogs.take(3).forEach { log ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${log.volumeMl} мл", color = palette.muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                NutritionRepository.deleteWater(log.id, getAccessToken())
                                                waterLogs = waterLogs.filterNot { it.id == log.id }
                                            } catch (e: Exception) {
                                                error = e.message ?: nutritionTr(language, "Сабт нест карда нашуд.", "Не удалось удалить запись.", "Could not delete the entry.")
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = nutritionTr(language, "Нест кардан", "Удалить", "Delete"), tint = palette.muted, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                        Text(
                            nutritionTr(language, "Меъёри об барои ҳама яксон нест; ҳаҷми умумиро танҳо ҳамчун сабти нӯшидан нишон медиҳем.", "Потребность в воде индивидуальна; здесь показан только записанный объём.", "Water needs vary; this shows only the volume you logged."),
                            color = palette.muted, fontSize = 10.sp, lineHeight = 14.sp
                        )
                    }
                }
            }
            if (error.isNotBlank()) {
                item {
                    Text(error, color = palette.danger, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            if (saving) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }

    val foodForDialog = selectedFood
    if (foodForDialog != null) {
        val parsedPortion = portionText.replace(',', '.').toDoubleOrNull()
        val preview = parsedPortion?.let { NutritionCalculator.forFoodPortion(foodForDialog, it) }
        AlertDialog(
            onDismissRequest = { if (!saving) selectedFood = null },
            title = { Text(foodForDialog.localizedName(language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        nutritionTr(language, "Ҳолати маҳсулот: ", "Состояние продукта: ", "Food state: ") +
                            foodPreparationLabel(language, foodForDialog.preparationState),
                        color = palette.muted, fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = portionText,
                        onValueChange = { value -> portionText = value.filter { it.isDigit() || it == '.' || it == ',' }.take(7) },
                        label = { Text(nutritionTr(language, "Миқдор", "Количество", "Portion")) },
                        suffix = { Text("г") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    if (preview != null) {
                        Text(
                            String.format(Locale.US, "%.0f kcal", preview.energyKcal),
                            color = palette.navy, fontWeight = FontWeight.Bold, fontSize = 22.sp
                        )
                        Text(
                            macroSummary(language, preview.proteinG, preview.carbohydrateG, preview.fatG),
                            color = palette.muted, fontSize = 11.sp
                        )
                    } else {
                        Text(nutritionTr(language, "Миқдори дурустро ворид кун.", "Введи корректное количество.", "Enter a valid portion."), color = palette.danger, fontSize = 11.sp)
                    }
                    Text(nutritionTr(language, "Манбаъ: ", "Источник: ", "Source: ") + nutritionSourceLabel(language, foodForDialog.source), color = palette.muted, fontSize = 10.sp)
                    if (!foodForDialog.isLocallyVerified) {
                        Text(
                            nutritionTr(language, "Маълумот ҳанӯз дар Тоҷикистон лабораторӣ тасдиқ нашудааст.", "Данные ещё не подтверждены лабораторно в Таджикистане.", "This data has not been locally laboratory-verified in Tajikistan."),
                            color = palette.muted, fontSize = 10.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !saving && preview != null,
                    onClick = {
                        scope.launch {
                            saving = true
                            error = ""
                            try {
                                val meal = NutritionRepository.addFoodMeal(foodForDialog, parsedPortion!!, foodForDialog.localizedName(language), getAccessToken())
                                meals = listOf(meal) + meals
                                selectedFood = null
                            } catch (e: Exception) {
                                error = e.message ?: nutritionTr(language, "Хӯрок сабт нашуд.", "Не удалось сохранить еду.", "Could not save the meal.")
                            } finally {
                                saving = false
                            }
                        }
                    }
                ) {
                    Text(nutritionTr(language, "Илова кардан", "Добавить", "Add meal"))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFood = null }, enabled = !saving) {
                    Text(nutritionTr(language, "Бекор кардан", "Отмена", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun MacroPill(
    language: String,
    label: String,
    value: Double,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalShahrvandPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = tint.copy(alpha = if (palette.isDark) 0.16f else 0.10f)
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = palette.muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(String.format(Locale.US, "%.1fg", value), color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FoodSearchRow(
    language: String,
    food: FoodCatalogItem,
    onClick: () -> Unit
) {
    val palette = LocalShahrvandPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceVariant.copy(alpha = if (palette.isDark) 0.8f else 0.55f),
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(palette.surface), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LocalDining, contentDescription = null, tint = palette.navyLight, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(food.localizedName(language), color = palette.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    nutritionTr(language, "Ба 100 г · ", "На 100 г · ", "Per 100 g · ") +
                        foodPreparationLabel(language, food.preparationState),
                    color = palette.muted, fontSize = 9.sp
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(String.format(Locale.US, "%.0f", food.nutrientsPer100g.energyKcal), color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("kcal", color = palette.muted, fontSize = 9.sp)
            }
            Icon(Icons.Filled.Add, contentDescription = nutritionTr(language, "Илова", "Добавить", "Add"), tint = palette.navyLight, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MealLogCard(
    language: String,
    meal: MealLogRecord,
    onDelete: () -> Unit
) {
    val palette = LocalShahrvandPalette.current
    val time = runCatching {
        Instant.parse(meal.loggedAt).atZone(ZoneId.of("Asia/Dushanbe"))
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("")
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border),
        shape = RoundedCornerShape(17.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 7.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(palette.goldSurface), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LocalDining, contentDescription = null, tint = palette.gold, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(meal.itemName, color = palette.ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${meal.portionGrams.toInt()} г · $time", color = palette.muted, fontSize = 9.sp)
                Text(
                    macroSummary(language, meal.nutrients.proteinG, meal.nutrients.carbohydrateG, meal.nutrients.fatG),
                    color = palette.muted, fontSize = 9.sp
                )
                Text(
                    meal.sourceSnapshot.substringBefore(':').replace('_', ' '),
                    color = palette.muted, fontSize = 8.sp, maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format(Locale.US, "%.0f", meal.nutrients.energyKcal), color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("kcal", color = palette.muted, fontSize = 9.sp)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = nutritionTr(language, "Нест кардан", "Удалить", "Delete"), tint = palette.muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
