package tj.shahrvand.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.ZoneId

data class MealLogRecord(
    val id: String,
    val itemName: String,
    val portionGrams: Double,
    val nutrients: NutritionTotals,
    val sourceSnapshot: String,
    val loggedAt: String
)

data class WaterLogRecord(
    val id: String,
    val volumeMl: Int,
    val loggedAt: String
)

object NutritionRepository {
    private const val ZONE_ID = "Asia/Dushanbe"
    private val baseUrl get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publicKey get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    suspend fun searchFoods(query: String, accessToken: String): List<FoodCatalogItem> =
        withContext(Dispatchers.IO) {
            requireValidSession(accessToken)
            val term = query.trim().take(60)
            val filter = if (term.isBlank()) "" else {
                val encoded = URLEncoder.encode("*$term*", Charsets.UTF_8.name())
                "&or=(name_tg.ilike.$encoded,name_ru.ilike.$encoded,name_en.ilike.$encoded)"
            }
            val rows = JSONArray(
                request(
                    "/rest/v1/shahrvand_food_catalog?select=id,name_tg,name_ru,name_en,category,preparation_state,energy_kcal_per_100g,protein_g_per_100g,carbohydrate_g_per_100g,fat_g_per_100g,fiber_g_per_100g,sodium_mg_per_100g,data_source,source_reference,verified_at,is_locally_verified&is_active=eq.true$filter&order=name_tg.asc&limit=60",
                    "GET",
                    accessToken
                )
            )
            buildList {
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    val food = row.toFoodCatalogItem()
                    if (food.isUsableForCalculation()) add(food)
                }
            }
        }

    suspend fun loadTodayMeals(accessToken: String): List<MealLogRecord> =
        withContext(Dispatchers.IO) {
            requireValidSession(accessToken)
            val zone = ZoneId.of(ZONE_ID)
            val today = LocalDate.now(zone)
            val start = today.atStartOfDay(zone).toInstant().toString()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toString()
            val rows = JSONArray(
                request(
                    "/rest/v1/shahrvand_meal_logs?select=id,item_name_snapshot,portion_g,energy_kcal,protein_g,carbohydrate_g,fat_g,fiber_g,sodium_mg,source_snapshot,logged_at&logged_at=gte.${encode(start)}&logged_at=lt.${encode(end)}&order=logged_at.desc&limit=200",
                    "GET",
                    accessToken
                )
            )
            buildList {
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    add(
                        MealLogRecord(
                            id = row.optString("id"),
                            itemName = row.optString("item_name_snapshot"),
                            portionGrams = row.optDouble("portion_g"),
                            nutrients = NutritionTotals(
                                energyKcal = row.optDouble("energy_kcal"),
                                proteinG = row.optDouble("protein_g"),
                                carbohydrateG = row.optDouble("carbohydrate_g"),
                                fatG = row.optDouble("fat_g"),
                                fiberG = row.optDouble("fiber_g"),
                                sodiumMg = row.optDouble("sodium_mg")
                            ),
                            sourceSnapshot = row.optString("source_snapshot"),
                            loggedAt = row.optString("logged_at")
                        )
                    )
                }
            }
        }

    suspend fun addFoodMeal(
        food: FoodCatalogItem,
        portionGrams: Double,
        accessToken: String
    ): MealLogRecord = withContext(Dispatchers.IO) {
        requireValidSession(accessToken)
        val nutrients = NutritionCalculator.forFoodPortion(food, portionGrams)
            ?: throw IllegalArgumentException("Миқдор ё маълумоти ғизоӣ нодуруст аст.")
        val body = JSONObject()
            .put("item_name_snapshot", food.nameTg)
            .put("catalog_item_id", food.id)
            .put("recipe_slug_snapshot", JSONObject.NULL)
            .put("portion_g", portionGrams)
            .put("energy_kcal", nutrients.energyKcal)
            .put("protein_g", nutrients.proteinG)
            .put("carbohydrate_g", nutrients.carbohydrateG)
            .put("fat_g", nutrients.fatG)
            .put("fiber_g", nutrients.fiberG)
            .put("sodium_mg", nutrients.sodiumMg)
            .put("source_snapshot", "${food.source.name}:${food.sourceReference}")
        val returned = JSONArray(
            request(
                "/rest/v1/shahrvand_meal_logs?select=id,item_name_snapshot,portion_g,energy_kcal,protein_g,carbohydrate_g,fat_g,fiber_g,sodium_mg,source_snapshot,logged_at",
                "POST",
                accessToken,
                body,
                "return=representation"
            )
        )
        val row = returned.optJSONObject(0)
            ?: throw IllegalStateException("Сервер сабти хӯрокро барнагардонд.")
        MealLogRecord(
            id = row.optString("id"),
            itemName = row.optString("item_name_snapshot"),
            portionGrams = row.optDouble("portion_g"),
            nutrients = NutritionTotals(
                row.optDouble("energy_kcal"),
                row.optDouble("protein_g"),
                row.optDouble("carbohydrate_g"),
                row.optDouble("fat_g"),
                row.optDouble("fiber_g"),
                row.optDouble("sodium_mg")
            ),
            sourceSnapshot = row.optString("source_snapshot"),
            loggedAt = row.optString("logged_at")
        )
    }

    suspend fun deleteMeal(mealId: String, accessToken: String) =
        withContext(Dispatchers.IO) {
            requireValidSession(accessToken)
            require(mealId.matches(Regex("^[0-9a-fA-F-]{36}$"))) { "ID-и сабти хӯрок нодуруст аст." }
            request(
                "/rest/v1/shahrvand_meal_logs?id=eq.${encode(mealId)}",
                "DELETE",
                accessToken
            )
        }

    suspend fun loadTodayWater(accessToken: String): List<WaterLogRecord> =
        withContext(Dispatchers.IO) {
            requireValidSession(accessToken)
            val zone = ZoneId.of(ZONE_ID)
            val today = LocalDate.now(zone)
            val start = today.atStartOfDay(zone).toInstant().toString()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toString()
            val rows = JSONArray(
                request(
                    "/rest/v1/shahrvand_water_logs?select=id,volume_ml,logged_at&logged_at=gte.${encode(start)}&logged_at=lt.${encode(end)}&order=logged_at.desc&limit=200",
                    "GET",
                    accessToken
                )
            )
            buildList {
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    add(WaterLogRecord(row.optString("id"), row.optInt("volume_ml"), row.optString("logged_at")))
                }
            }
        }

    suspend fun addWater(volumeMl: Int, accessToken: String): WaterLogRecord =
        withContext(Dispatchers.IO) {
            requireValidSession(accessToken)
            require(volumeMl in 1..5000) { "Миқдори об бояд аз 1 то 5000 мл бошад." }
            val rows = JSONArray(
                request(
                    "/rest/v1/shahrvand_water_logs?select=id,volume_ml,logged_at",
                    "POST",
                    accessToken,
                    JSONObject().put("volume_ml", volumeMl),
                    "return=representation"
                )
            )
            val row = rows.optJSONObject(0)
                ?: throw IllegalStateException("Сервер сабти обро барнагардонд.")
            WaterLogRecord(row.optString("id"), row.optInt("volume_ml"), row.optString("logged_at"))
        }

    suspend fun deleteWater(logId: String, accessToken: String) =
        withContext(Dispatchers.IO) {
            requireValidSession(accessToken)
            require(logId.matches(Regex("^[0-9a-fA-F-]{36}$"))) { "ID-и сабти об нодуруст аст." }
            request(
                "/rest/v1/shahrvand_water_logs?id=eq.${encode(logId)}",
                "DELETE",
                accessToken
            )
        }

    private fun JSONObject.toFoodCatalogItem() = FoodCatalogItem(
        id = optString("id"),
        nameTg = optString("name_tg"),
        nameRu = optString("name_ru"),
        nameEn = optString("name_en"),
        category = optString("category"),
        preparationState = when (optString("preparation_state")) {
            "raw" -> FoodPreparationState.RAW
            "cooked" -> FoodPreparationState.COOKED
            "dried" -> FoodPreparationState.DRIED
            "baked" -> FoodPreparationState.BAKED
            else -> FoodPreparationState.UNKNOWN
        },
        nutrientsPer100g = FoodNutrients(
            optDouble("energy_kcal_per_100g"),
            optDouble("protein_g_per_100g"),
            optDouble("carbohydrate_g_per_100g"),
            optDouble("fat_g_per_100g"),
            optDouble("fiber_g_per_100g"),
            optDouble("sodium_mg_per_100g")
        ),
        source = when (optString("data_source")) {
            "usda_fdc" -> NutritionDataSource.USDA_FOOD_DATA_CENTRAL
            "tajik_lab" -> NutritionDataSource.TAJIK_LAB_ANALYSIS
            "manufacturer_label" -> NutritionDataSource.MANUFACTURER_LABEL
            else -> NutritionDataSource.RECIPE_ESTIMATE
        },
        sourceReference = optString("source_reference"),
        verifiedAt = optString("verified_at").takeIf { it.isNotBlank() && it != "null" },
        isLocallyVerified = optBoolean("is_locally_verified")
    )

    private fun requireValidSession(accessToken: String) {
        require(accessToken.isNotBlank()) { "Сессия ба охир расид. Дубора ворид шавед." }
        require(baseUrl.startsWith("https://") && publicKey.isNotBlank()) {
            "Танзимоти Supabase нодуруст аст."
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun request(
        path: String,
        method: String,
        accessToken: String,
        body: JSONObject? = null,
        prefer: String? = null
    ): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 12_000
                readTimeout = 15_000
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
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val error = runCatching { JSONObject(raw) }.getOrNull()
                val message = error?.optString("message").orEmpty()
                val code = error?.optString("code").orEmpty()
                throw IllegalStateException(
                    message.takeIf { it.isNotBlank() }
                        ?: code.takeIf { it.isNotBlank() }
                        ?: "HTTP $status"
                )
            }
            return raw
        } finally {
            connection?.disconnect()
        }
    }
}
