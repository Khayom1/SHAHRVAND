package tj.shahrvand.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

data class HealthProfileRecord(
    val heightCm: Double,
    val sexAtBirth: String,
    val activityLevel: String,
    val healthGoal: String
)

data class BodyMeasurementRecord(
    val weightKg: Double,
    val measuredAt: String,
    val source: String
)

data class HealthProfileSnapshot(
    val profile: HealthProfileRecord?,
    val latestMeasurement: BodyMeasurementRecord?
)

object HealthProfileRepository {
    private val baseUrl get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publicKey get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    suspend fun load(userId: String, accessToken: String): HealthProfileSnapshot =
        withContext(Dispatchers.IO) {
            requireValidSession(userId, accessToken)
            val encodedUserId = URLEncoder.encode(userId, Charsets.UTF_8.name())
            val profileRows = requestArray(
                "/rest/v1/shahrvand_health_profiles" +
                    "?select=height_cm,sex_at_birth,activity_level,health_goal" +
                    "&user_id=eq." + encodedUserId + "&limit=1",
                "GET",
                accessToken
            )
            val measurementRows = requestArray(
                "/rest/v1/shahrvand_body_measurements" +
                    "?select=weight_kg,measured_at,source&user_id=eq." + encodedUserId +
                    "&order=measured_at.desc,id.desc&limit=1",
                "GET",
                accessToken
            )

            val profile = profileRows.optJSONObject(0)?.let { row ->
                HealthProfileRecord(
                    heightCm = row.optDouble("height_cm"),
                    sexAtBirth = row.optString("sex_at_birth", "not_specified"),
                    activityLevel = row.optString("activity_level", "light"),
                    healthGoal = row.optString("health_goal", "build_habit")
                )
            }
            val measurement = measurementRows.optJSONObject(0)?.let { row ->
                BodyMeasurementRecord(
                    weightKg = row.optDouble("weight_kg"),
                    measuredAt = row.optString("measured_at"),
                    source = row.optString("source", "manual")
                )
            }
            HealthProfileSnapshot(profile, measurement)
        }

    suspend fun save(
        userId: String,
        accessToken: String,
        heightCm: Double,
        weightKg: Double,
        sexAtBirth: String,
        activityLevel: String,
        healthGoal: String,
        previousWeightKg: Double?
    ): Boolean = withContext(Dispatchers.IO) {
        requireValidSession(userId, accessToken)
        require(heightCm.isFinite() && heightCm in 80.0..250.0) {
            "Қад бояд аз 80 то 250 сантиметр бошад."
        }
        require(weightKg.isFinite() && weightKg in 5.0..500.0) {
            "Вазн бояд аз 5 то 500 килограмм бошад."
        }
        require(sexAtBirth in setOf("female", "male", "not_specified")) {
            "Интихоби ҷинси биологӣ нодуруст аст."
        }
        require(activityLevel in setOf("sedentary", "light", "moderate", "high")) {
            "Сатҳи фаъолият нодуруст аст."
        }
        require(healthGoal in setOf(
            "maintain", "build_strength", "improve_endurance",
            "build_habit", "gain_weight", "weight_management"
        )) { "Ҳадафи саломатӣ нодуруст аст." }

        val profileBody = JSONObject()
            .put("user_id", userId)
            .put("height_cm", heightCm)
            .put("sex_at_birth", sexAtBirth)
            .put("activity_level", activityLevel)
            .put("health_goal", healthGoal)
            .put("updated_at", Instant.now().toString())

        request(
            "/rest/v1/shahrvand_health_profiles?on_conflict=user_id",
            "POST",
            accessToken,
            profileBody,
            "resolution=merge-duplicates,return=minimal"
        )

        val isNewWeight = previousWeightKg == null ||
            kotlin.math.abs(previousWeightKg - weightKg) >= 0.05
        if (isNewWeight) {
            val measurementBody = JSONObject()
                .put("user_id", userId)
                .put("weight_kg", weightKg)
                .put("source", "manual")
            request(
                "/rest/v1/shahrvand_body_measurements",
                "POST",
                accessToken,
                measurementBody,
                "return=minimal"
            )
        }
        isNewWeight
    }

    private fun requireValidSession(userId: String, accessToken: String) {
        require(userId.matches(Regex("^[0-9a-fA-F-]{36}$"))) {
            "Идентификатори корбар нодуруст аст."
        }
        require(accessToken.isNotBlank()) {
            "Сессия ба охир расид. Дубора ворид шавед."
        }
        require(baseUrl.startsWith("https://") && publicKey.isNotBlank()) {
            "Танзимоти Supabase нодуруст аст."
        }
    }

    private fun requestArray(
        path: String,
        method: String,
        accessToken: String
    ): JSONArray {
        val raw = request(path, method, accessToken, null, null)
        return JSONArray(raw)
    }

    private fun request(
        path: String,
        method: String,
        accessToken: String,
        body: JSONObject?,
        prefer: String?
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
                        ?: "HTTP " + status
                )
            }
            return raw
        } finally {
            connection?.disconnect()
        }
    }
}
