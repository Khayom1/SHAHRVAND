package tj.shahrvand.app

/**
 * Nutrient values are always tied to a specific food form and a documented source.
 * Values are expressed per 100 g unless a calculated result says otherwise.
 */
enum class NutritionDataSource {
    USDA_FOOD_DATA_CENTRAL,
    TAJIK_LAB_ANALYSIS,
    MANUFACTURER_LABEL,
    RECIPE_ESTIMATE,
    USER_ENTERED
}

enum class FoodPreparationState {
    RAW,
    COOKED,
    DRIED,
    BAKED,
    FERMENTED,
    UNKNOWN
}

data class FoodNutrients(
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sodiumMg: Double
) {
    fun isValid(): Boolean =
        listOf(energyKcal, proteinG, carbohydrateG, fatG, fiberG, sodiumMg)
            .all { it.isFinite() && it >= 0.0 }
}

data class FoodCatalogItem(
    val id: String,
    val nameTg: String,
    val nameRu: String,
    val nameEn: String,
    val category: String,
    val preparationState: FoodPreparationState,
    val nutrientsPer100g: FoodNutrients,
    val source: NutritionDataSource,
    val sourceReference: String,
    val verifiedAt: String?,
    val isLocallyVerified: Boolean
) {
    fun isUsableForCalculation(): Boolean =
        id.isNotBlank() &&
            nameTg.isNotBlank() &&
            nutrientsPer100g.isValid() &&
            sourceReference.isNotBlank() &&
            preparationState != FoodPreparationState.UNKNOWN
}

data class RecipeIngredient(
    val food: FoodCatalogItem,
    val amountGrams: Double
)

data class NutritionTotals(
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sodiumMg: Double
)

data class RecipeNutritionResult(
    val ingredientTotals: NutritionTotals,
    val per100g: NutritionTotals,
    val finalCookedWeightGrams: Double
)

object NutritionCalculator {
    fun forFoodPortion(food: FoodCatalogItem, portionGrams: Double): NutritionTotals? {
        if (!food.isUsableForCalculation() || !portionGrams.isFinite() || portionGrams <= 0.0) {
            return null
        }
        return scale(food.nutrientsPer100g, portionGrams / 100.0)
    }

    /**
     * Calculates a recipe from ingredient weights and the measured final cooked yield.
     * The result is an estimate: nutrient retention and discarded cooking liquid/fat
     * are not modelled unless a future recipe-specific retention model is added.
     */
    fun forRecipe(
        ingredients: List<RecipeIngredient>,
        finalCookedWeightGrams: Double
    ): RecipeNutritionResult? {
        if (ingredients.isEmpty() ||
            !finalCookedWeightGrams.isFinite() ||
            finalCookedWeightGrams <= 0.0
        ) return null

        if (ingredients.any {
                !it.food.isUsableForCalculation() ||
                    !it.amountGrams.isFinite() ||
                    it.amountGrams <= 0.0
            }
        ) return null

        val totals = ingredients.fold(NutritionTotals(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)) { acc, item ->
            val part = scale(item.food.nutrientsPer100g, item.amountGrams / 100.0)
            acc + part
        }
        val per100g = totals * (100.0 / finalCookedWeightGrams)
        return RecipeNutritionResult(
            ingredientTotals = totals,
            per100g = per100g,
            finalCookedWeightGrams = finalCookedWeightGrams
        )
    }

    private fun scale(nutrients: FoodNutrients, factor: Double) = NutritionTotals(
        energyKcal = nutrients.energyKcal * factor,
        proteinG = nutrients.proteinG * factor,
        carbohydrateG = nutrients.carbohydrateG * factor,
        fatG = nutrients.fatG * factor,
        fiberG = nutrients.fiberG * factor,
        sodiumMg = nutrients.sodiumMg * factor
    )

    private operator fun NutritionTotals.plus(other: NutritionTotals) = NutritionTotals(
        energyKcal + other.energyKcal,
        proteinG + other.proteinG,
        carbohydrateG + other.carbohydrateG,
        fatG + other.fatG,
        fiberG + other.fiberG,
        sodiumMg + other.sodiumMg
    )

    private operator fun NutritionTotals.times(factor: Double) = NutritionTotals(
        energyKcal * factor,
        proteinG * factor,
        carbohydrateG * factor,
        fatG * factor,
        fiberG * factor,
        sodiumMg * factor
    )
}
