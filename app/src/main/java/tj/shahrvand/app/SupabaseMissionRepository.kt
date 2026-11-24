package tj.shahrvand.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only public content client.
 * It sends no profile, nickname, age, progress, auth token, or device identifier.
 * The caller invokes it only after the neutral 18+ age selection.
 */
object SupabaseMissionRepository {
    private const val TAG = "ShahrvandCatalog"

    suspend fun fetchMissions(): List<Mission>? = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val publicKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        if (baseUrl.isBlank() || publicKey.isBlank()) return@withContext null

        var connection: HttpURLConnection? = null
        try {
            val query = "/rest/v1/shahrvand_missions" +
                "?select=id,title_tg,title_ru,title_en,description_tg,description_ru,description_en,category,icon,xp,min_age" +
                "&is_active=eq.true&order=sort_order.asc"
            connection = (URL(baseUrl + query).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("apikey", publicKey)
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode !in 200..299) {
                Log.w(TAG, "Public mission catalogue returned HTTP " + connection.responseCode)
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val array = JSONArray(body)
            val result = ArrayList<Mission>(array.length())

            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val minAge = item.optInt("min_age", 7).coerceAtLeast(7)
                if (minAge > 18) continue
                result += Mission(
                    id = item.getString("id"),
                    titleTg = item.getString("title_tg"),
                    titleRu = item.getString("title_ru"),
                    titleEn = item.getString("title_en"),
                    descriptionTg = item.getString("description_tg"),
                    descriptionRu = item.getString("description_ru"),
                    descriptionEn = item.getString("description_en"),
                    category = item.optString("category", "learning"),
                    icon = item.optString("icon", "⭐"),
                    xp = item.optInt("xp", 10).coerceIn(5, 20),
                    minAge = minAge
                )
            }
            result.takeIf { it.isNotEmpty() }
        } catch (error: Exception) {
            Log.w(TAG, "Could not refresh public mission catalogue; using bundled missions.", error)
            null
        } finally {
            connection?.disconnect()
        }
    }
}
