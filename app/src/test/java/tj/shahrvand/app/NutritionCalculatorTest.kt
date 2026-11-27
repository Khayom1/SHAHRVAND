package tj.shahrvand.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {
    private fun food(
        id: String,
        state: FoodPreparationState,
        kcal: Double,
        protein: Double = 0.0,
        carbs: Double = 0.0,
        fat: Double = 0.0,
        fiber: Double = 0.0,
        sodium: Double = 0.0
    ) = FoodCatalogItem(
        id = id,
        nameTg = id,
        nameRu = id,
        nameEn = id,
        category = "test",
        preparationState = state,
        nutrientsPer100g = FoodNutrients(kcal, protein, carbs, fat, fiber, sodium),
        source = NutritionDataSource.USDA_FOOD_DATA_CENTRAL,
        sourceReference = "https://fdc.nal.usda.gov/food-details/$id/nutrients",
        verifiedAt = "2026-11-27",
        isLocallyVerified = false
    )

    @Test
    fun portionScalesPer100GramValues() {
        val apple = food(
            id = "apple-raw",
            state = FoodPreparationState.RAW,
            kcal = 52.0,
            protein = 0.3,
            carbs = 13.8,
            fat = 0.2,
            fiber = 2.4,
            sodium = 1.0
        )

        val result = NutritionCalculator.forFoodPortion(apple, 150.0)

        assertEquals(78.0, result?.energyKcal ?: 0.0, 0.0001)
        assertEquals(0.45, result?.proteinG ?: 0.0, 0.0001)
        assertEquals(20.7, result?.carbohydrateG ?: 0.0, 0.0001)
        assertEquals(0.3, result?.fatG ?: 0.0, 0.0001)
        assertEquals(3.6, result?.fiberG ?: 0.0, 0.0001)
        assertEquals(1.5, result?.sodiumMg ?: 0.0, 0.0001)
    }

    @Test
    fun recipeUsesMeasuredFinalCookedWeight() {
        val cookedRice = food(
            id = "rice-cooked",
            state = FoodPreparationState.COOKED,
            kcal = 130.0,
            protein = 2.7,
            carbs = 28.2,
            fat = 0.3,
            fiber = 0.4,
            sodium = 1.0
        )
        val cookedChicken = food(
            id = "chicken-cooked",
            state = FoodPreparationState.COOKED,
            kcal = 165.0,
            protein = 31.0,
            carbs = 0.0,
            fat = 3.6,
            fiber = 0.0,
            sodium = 74.0
        )

        val result = NutritionCalculator.forRecipe(
            ingredients = listOf(
                RecipeIngredient(cookedRice, 150.0),
                RecipeIngredient(cookedChicken, 100.0)
            ),
            finalCookedWeightGrams = 300.0
        )

        assertEquals(360.0, result?.ingredientTotals?.energyKcal ?: 0.0, 0.0001)
        assertEquals(120.0, result?.per100g?.energyKcal ?: 0.0, 0.0001)
        assertEquals(300.0, result?.finalCookedWeightGrams ?: 0.0, 0.0001)
    }

    @Test
    fun rejectsUnknownPreparationAndMissingSource() {
        val unknown = food("unknown", FoodPreparationState.UNKNOWN, 100.0)
        val noSource = food("no-source", FoodPreparationState.RAW, 100.0).copy(sourceReference = "")

        assertNull(NutritionCalculator.forFoodPortion(unknown, 100.0))
        assertNull(NutritionCalculator.forFoodPortion(noSource, 100.0))
    }

    @Test
    fun rejectsInvalidPortionsAndRecipeWeights() {
        val apple = food("apple", FoodPreparationState.RAW, 52.0)

        assertNull(NutritionCalculator.forFoodPortion(apple, 0.0))
        assertNull(NutritionCalculator.forFoodPortion(apple, Double.NaN))
        assertNull(NutritionCalculator.forRecipe(emptyList(), 200.0))
        assertNull(NutritionCalculator.forRecipe(listOf(RecipeIngredient(apple, -10.0)), 200.0))
        assertNull(NutritionCalculator.forRecipe(listOf(RecipeIngredient(apple, 100.0)), 0.0))
        assertNull(NutritionCalculator.forRecipe(listOf(RecipeIngredient(apple, 100000.1)), 200000.0))
        assertNull(NutritionCalculator.forRecipe(listOf(RecipeIngredient(apple, 100.0)), 100000.1))
    }

    @Test
    fun rejectsUnknownNutritionSourceAndOversizedPortions() {
        val valid = food("valid", FoodPreparationState.RAW, 52.0)
        val unknownSource = valid.copy(source = NutritionDataSource.UNKNOWN)

        assertNull(NutritionCalculator.forFoodPortion(unknownSource, 100.0))
        assertNull(NutritionCalculator.forFoodPortion(valid, 10000.01))
        assertNull(NutritionCalculator.forFoodPortion(valid, Double.POSITIVE_INFINITY))
    }

    @Test
    fun nutrientValuesMustBeFiniteAndNonNegative() {
        assertTrue(FoodNutrients(100.0, 2.0, 3.0, 4.0, 1.0, 5.0).isValid())
        assertTrue(!FoodNutrients(-1.0, 2.0, 3.0, 4.0, 1.0, 5.0).isValid())
        assertTrue(!FoodNutrients(Double.NaN, 2.0, 3.0, 4.0, 1.0, 5.0).isValid())
    }
}
