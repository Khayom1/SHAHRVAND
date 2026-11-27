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
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import java.util.Locale

private data class RecipeLineItem(
    val key: Int,
    val food: FoodCatalogItem,
    val amountGrams: Double
)

@Composable
internal fun RecipeCalculatorScreen(
    language: String,
    foods: List<FoodCatalogItem>,
    onBack: () -> Unit,
    onSaveMeal: suspend (
        displayName: String,
        ingredients: List<RecipeIngredient>,
        finalCookedWeightGrams: Double,
        portionGrams: Double
    ) -> MealLogRecord
) {
    val palette = LocalShahrvandPalette.current
    val scope = rememberCoroutineScope()
    var recipeName by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(emptyList<RecipeLineItem>()) }
    var finalWeightText by remember { mutableStateOf("") }
    var portionText by remember { mutableStateOf("250") }
    var selectedFood by remember { mutableStateOf<FoodCatalogItem?>(null) }
    var ingredientAmountText by remember { mutableStateOf("100") }
    var nextKey by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val ingredients = lines.map { RecipeIngredient(it.food, it.amountGrams) }
    val finalWeight = finalWeightText.replace(',', '.').toDoubleOrNull()
    val portion = portionText.replace(',', '.').toDoubleOrNull()
    val recipeResult = if (finalWeight != null) {
        NutritionCalculator.forRecipe(ingredients, finalWeight)
    } else null
    val portionTotals = if (recipeResult != null && portion != null && portion > 0.0) {
        NutritionTotals(
            recipeResult.per100g.energyKcal * portion / 100.0,
            recipeResult.per100g.proteinG * portion / 100.0,
            recipeResult.per100g.carbohydrateG * portion / 100.0,
            recipeResult.per100g.fatG * portion / 100.0,
            recipeResult.per100g.fiberG * portion / 100.0,
            recipeResult.per100g.sodiumMg * portion / 100.0
        )
    } else null
    val filteredFoods = foods.filter { food ->
        val q = query.trim()
        q.isBlank() || food.nameTg.contains(q, true) ||
            food.nameRu.contains(q, true) || food.nameEn.contains(q, true)
    }

    Column(
        Modifier.fillMaxSize().shahrvandCanvas(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !saving) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = nutritionTr(language, "Бозгашт", "Назад", "Back"), tint = palette.ink)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        nutritionTr(language, "ҲИСОБИ ДОРОХАТ", "РАСЧЁТ РЕЦЕПТА", "RECIPE CALCULATOR"),
                        color = palette.navyLight, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        nutritionTr(language, "Таоми худро ҳисоб кун", "Рассчитай своё блюдо", "Calculate your recipe"),
                        color = palette.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
                Surface(shape = CircleShape, color = palette.surface, border = BorderStroke(1.dp, palette.border)) {
                    Icon(Icons.Filled.RestaurantMenu, contentDescription = null, tint = palette.navyLight, modifier = Modifier.padding(10.dp).size(22.dp))
                }
            }

            OutlinedTextField(
                value = recipeName,
                onValueChange = { recipeName = it.take(160) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(nutritionTr(language, "Номи таом (масалан, оши палав)", "Название блюда (например, плов)", "Recipe name (e.g. pilaf)")) },
                shape = RoundedCornerShape(14.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(1.dp, palette.border),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(nutritionTr(language, "КОМПОНЕНТҲО", "ИНГРЕДИЕНТЫ", "INGREDIENTS"), color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = palette.navyLight)
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it.take(60) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(nutritionTr(language, "Маҳсулотро ҷустуҷӯ кун", "Найти продукт", "Search ingredients")) },
                            shape = RoundedCornerShape(13.dp)
                        )
                    }
                    Text(
                        nutritionTr(language, "Аз рӯйхати маҳсулоти дорои манбаи ғизоӣ интихоб кун.", "Выбирай продукты с указанным источником данных.", "Choose foods with a documented nutrition source."),
                        color = palette.muted, fontSize = 10.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (filteredFoods.isEmpty()) {
                item {
                    Text(
                        nutritionTr(language, "Маҳсулоти мувофиқ ёфт нашуд.", "Подходящие продукты не найдены.", "No matching ingredients found."),
                        color = palette.muted, fontSize = 11.sp, modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(filteredFoods.take(40), key = { "food-${it.id}" }) { food ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !saving) {
                            selectedFood = food
                            ingredientAmountText = "100"
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(palette.surfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.RestaurantMenu, contentDescription = null, tint = palette.navyLight, modifier = Modifier.size(17.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    when (language) { "ru" -> food.nameRu; "en" -> food.nameEn; else -> food.nameTg },
                                    color = palette.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    nutritionTr(language, "100 г · ", "100 г · ", "100 g · ") +
                                        foodPreparationLabel(language, food.preparationState),
                                    color = palette.muted, fontSize = 9.sp
                                )
                            }
                            Text(
                                String.format(Locale.US, "%.0f kcal", food.nutrientsPer100g.energyKcal),
                                color = palette.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Filled.Add, contentDescription = null, tint = palette.navyLight)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(nutritionTr(language, "Компонентҳои интихобшуда", "Выбранные ингредиенты", "Selected ingredients"), modifier = Modifier.weight(1f), color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(shape = RoundedCornerShape(10.dp), color = palette.surfaceVariant) {
                                Text("${lines.size}/30", color = palette.navyLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                            }
                        }
                        if (lines.isEmpty()) {
                            Text(
                                nutritionTr(language, "Ҳанӯз компонент илова нашудааст.", "Ингредиенты ещё не добавлены.", "No ingredients added yet."),
                                color = palette.muted, fontSize = 10.sp
                            )
                        } else {
                            lines.forEach { line ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            when (language) { "ru" -> line.food.nameRu; "en" -> line.food.nameEn; else -> line.food.nameTg },
                                            color = palette.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                                        )
                                        Text(String.format(Locale.US, "%.1f г · %s · %s", line.amountGrams, foodPreparationLabel(language, line.food.preparationState), nutritionSourceLabel(language, line.food.source)), color = palette.muted, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(
                                        String.format(Locale.US, "%.0f kcal", line.food.nutrientsPer100g.energyKcal * line.amountGrams / 100.0),
                                        color = palette.ink, fontSize = 10.sp, fontWeight = FontWeight.Medium
                                    )
                                    IconButton(onClick = { lines = lines.filterNot { it.key == line.key } }, enabled = !saving, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = nutritionTr(language, "Нест кардан", "Удалить", "Remove"), tint = palette.muted, modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(nutritionTr(language, "ВАЗНИ ТАОМИ ПУХТА", "ВЕС ГОТОВОГО БЛЮДА", "FINAL COOKED WEIGHT"), color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        OutlinedTextField(
                            value = finalWeightText,
                            onValueChange = { finalWeightText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(8) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(nutritionTr(language, "Вазни тамоми таом пас аз пухтан", "Вес всего блюда после приготовления", "Total cooked weight")) },
                            suffix = { Text("г") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(13.dp)
                        )
                        OutlinedTextField(
                            value = portionText,
                            onValueChange = { portionText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(8) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(nutritionTr(language, "Порсияи хӯрдашуда", "Съеденная порция", "Your portion")) },
                            suffix = { Text("г") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(13.dp)
                        )
                        if (recipeResult != null) {
                            Text(
                                nutritionTr(language, String.format(Locale.US, "%.1f ккал / 100 г", recipeResult.per100g.energyKcal), String.format(Locale.US, "%.1f ккал / 100 г", recipeResult.per100g.energyKcal), String.format(Locale.US, "%.1f kcal / 100 g", recipeResult.per100g.energyKcal)),
                                color = palette.navy, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                nutritionTr(language, "Ҳисоби тахминӣ аз рӯйи компонентҳои воридшуда; талафи моддаҳо ҳангоми пухтан ҳисоб нашудааст.", "Оценка по введённым ингредиентам; потери питательных веществ при приготовлении не учтены.", "Estimate from entered ingredients; nutrient losses during cooking are not modelled."),
                                color = palette.muted, fontSize = 10.sp, lineHeight = 15.sp
                            )
                            if (portionTotals != null) {
                                Text(
                                    nutritionTr(language, String.format(Locale.US, "%.0f ккал · ", portionTotals.energyKcal), String.format(Locale.US, "%.0f ккал · ", portionTotals.energyKcal), String.format(Locale.US, "%.0f kcal · ", portionTotals.energyKcal)) + macroSummary(language, portionTotals.proteinG, portionTotals.carbohydrateG, portionTotals.fatG),
                                    color = palette.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                nutritionTr(language, "Барои ҳисоб компонент илова кун ва вазни ниҳоии таоми пухтаро ворид намо.", "Добавь ингредиенты и укажи конечный вес готового блюда.", "Add ingredients and enter the measured final cooked weight."),
                                color = palette.muted, fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (error.isNotBlank()) {
                item { Text(error, color = palette.danger, fontSize = 11.sp) }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            error = ""
                            try {
                                onSaveMeal(
                                    recipeName.trim(),
                                    ingredients,
                                    finalWeight!!,
                                    portion!!
                                )
                                onBack()
                            } catch (e: Exception) {
                                error = e.message ?: nutritionTr(language, "Таом ба рӯзнома илова нашуд.", "Не удалось добавить блюдо.", "Could not add the recipe to your diary.")
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving && recipeName.trim().isNotEmpty() &&
                        ingredients.isNotEmpty() && recipeResult != null &&
                        finalWeight != null && finalWeight > 0.0 &&
                        portion != null && portion > 0.0 && portion <= 10000.0 &&
                        finalWeight != null && portion <= finalWeight &&
                        finalWeight <= 100000.0 && portionTotals != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(nutritionTr(language, "Илова ба рӯзномаи ғизо", "Добавить в дневник питания", "Add to food diary"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    val foodForDialog = selectedFood
    if (foodForDialog != null) {
        val amount = ingredientAmountText.replace(',', '.').toDoubleOrNull()
        AlertDialog(
            onDismissRequest = { if (!saving) selectedFood = null },
            title = {
                Text(when (language) { "ru" -> foodForDialog.nameRu; "en" -> foodForDialog.nameEn; else -> foodForDialog.nameTg })
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        String.format(Locale.US, "%.0f kcal / 100 g", foodForDialog.nutrientsPer100g.energyKcal),
                        color = palette.navyLight, fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = ingredientAmountText,
                        onValueChange = { ingredientAmountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(8) },
                        singleLine = true,
                        label = { Text(nutritionTr(language, "Миқдор дар дорухат", "Количество в рецепте", "Amount in recipe")) },
                        suffix = { Text("г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Text(
                        nutritionTr(language, "Ҳолат: ", "Состояние: ", "State: ") + foodPreparationLabel(language, foodForDialog.preparationState),
                        color = palette.muted, fontSize = 10.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !saving && amount != null && amount > 0.0 && lines.size < 30,
                    onClick = {
                        lines = lines + RecipeLineItem(nextKey, foodForDialog, amount!!)
                        nextKey += 1
                        selectedFood = null
                    }
                ) { Text(nutritionTr(language, "Илова", "Добавить", "Add")) }
            },
            dismissButton = {
                TextButton(onClick = { selectedFood = null }, enabled = !saving) {
                    Text(nutritionTr(language, "Бекор", "Отмена", "Cancel"))
                }
            }
        )
    }
}
