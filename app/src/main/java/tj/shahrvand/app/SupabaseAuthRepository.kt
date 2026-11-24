package tj.shahrvand.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ShahrvandAccount(
    val userId: String,
    val accessToken: String,
    val firstName: String,
    val lastName: String?,
    val age: Int,
    val login: String
) {
    val ageBand: String
        get() = when {
            age <= 12 -> "7_12"
            age <= 17 -> "13_17"
            else -> "18_plus"
        }
}

object SupabaseAuthRepository {
    private const val TAG = "ShahrvandAuth"

    private val baseUrl get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publicKey get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    private fun authEmail(login: String) = "${login.lowercase()}@users.shahrvand.invalid"

    suspend fun register(
        firstName: String,
        lastName: String,
        age: Int,
        login: String,
        password: String
    ): Result<ShahrvandAccount> = withContext(Dispatchers.IO) {
        runCatching {
            require(firstName.trim().length in 1..60) { "Номро дуруст ворид кунед." }
            require(lastName.trim().length <= 60) { "Насаб аз 60 аломат зиёд набошад." }
            require(age in 7..120) { "Синну сол бояд аз 7 то 120 бошад." }
            require(login.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]{3,23}$"))) {
                "Логин: 4–24 аломат, танҳо ҳарфҳои англисӣ, рақам, нуқта, _ ё -."
            }
            require(password.length >= 8) { "Парол бояд камаш 8 аломат дошта бошад." }

            val normalizedLogin = login.lowercase()
            val email = authEmail(normalizedLogin)
            val signupBody = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("data", JSONObject().put("login", normalizedLogin))

            val signup = request("/auth/v1/signup", "POST", signupBody, authenticated = false)
            val signupError = signup.optString("msg").ifBlank { signup.optString("message") }
            if ((signup.optJSONObject("user")?.has("id") == true || signup.has("id")) && !signup.has("access_token")) {
                // Email confirmation is enabled; do not claim the account is ready.
                error("Supabase ҳанӯз тасдиқи email талаб мекунад. Дар Auth → Providers → Email тасдиқи email-ро хомӯш кунед ва дубора сабти ном кунед.")
            }

            val session = request(
                "/auth/v1/token?grant_type=password",
                "POST",
                JSONObject().put("email", email).put("password", password),
                authenticated = false
            )
            val accessToken = session.optString("access_token")
            val user = session.optJSONObject("user") ?: error(
                session.optString("msg").ifBlank { session.optString("message", signupError.ifBlank { "Сабти ном иҷро нашуд." }) }
            )
            val userId = user.optString("id")
            if (accessToken.isBlank() || userId.isBlank()) error("Supabase сессияи корбарро барнагардонд.")

            val profile = JSONObject()
                .put("id", userId)
                .put("first_name", firstName.trim())
                .put("last_name", lastName.trim().ifBlank { JSONObject.NULL })
                .put("age", age)
                .put("login", normalizedLogin)
            request("/rest/v1/shahrvand_profiles", "POST", profile, accessToken, prefer = "return=minimal")

            ShahrvandAccount(userId, accessToken, firstName.trim(), lastName.trim().ifBlank { null }, age, normalizedLogin)
        }.onFailure { Log.w(TAG, "Registration failed", it) }
    }

    suspend fun signIn(login: String, password: String): Result<ShahrvandAccount> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedLogin = login.trim().lowercase()
            require(normalizedLogin.matches(Regex("^[a-z0-9][a-z0-9._-]{3,23}$"))) { "Логин нодуруст аст." }
            require(password.isNotBlank()) { "Паролро ворид кунед." }

            val session = request(
                "/auth/v1/token?grant_type=password",
                "POST",
                JSONObject().put("email", authEmail(normalizedLogin)).put("password", password),
                authenticated = false
            )
            val accessToken = session.optString("access_token")
            val user = session.optJSONObject("user") ?: error(
                session.optString("msg").ifBlank { session.optString("message", "Логин ё парол нодуруст аст.") }
            )
            if (accessToken.isBlank()) error("Сессияи воридшавӣ сохта нашуд.")
            val profileResponse = request(
                "/rest/v1/shahrvand_profiles?select=first_name,last_name,age,login&id=eq.${user.optString("id")}",
                "GET",
                null,
                accessToken
            )
            val profile = profileResponse.optJSONArray("data")?.optJSONObject(0)
                ?: profileResponse.optJSONArray("records")?.optJSONObject(0)
                ?: error("Профили корбар ёфт нашуд.")
            ShahrvandAccount(
                user.optString("id"), accessToken,
                profile.optString("first_name"),
                profile.optString("last_name").takeIf { it.isNotBlank() && it != "null" },
                profile.optInt("age"), profile.optString("login", normalizedLogin)
            )
        }.onFailure { Log.w(TAG, "Sign in failed", it) }
    }

    private fun request(
        path: String,
        method: String,
        body: JSONObject?,
        accessToken: String? = null,
        authenticated: Boolean = true,
        prefer: String? = null
    ): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 12_000
                readTimeout = 12_000
                doInput = true
                setRequestProperty("apikey", publicKey)
                setRequestProperty("Accept", "application/json")
                if (authenticated && accessToken != null) setRequestProperty("Authorization", "Bearer $accessToken")
                else if (accessToken != null) setRequestProperty("Authorization", "Bearer $accessToken")
                else if (path.startsWith("/auth/")) setRequestProperty("Authorization", "Bearer $publicKey")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                if (prefer != null) setRequestProperty("Prefer", prefer)
            }
            if (body != null) connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val errorBody = runCatching { JSONObject(response) }.getOrNull()
                val message = errorBody?.optString("msg")?.takeIf { it.isNotBlank() }
                    ?: errorBody?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: errorBody?.optString("error_description")?.takeIf { it.isNotBlank() }
                    ?: errorBody?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Хатои сервер: HTTP $code"
                error(message)
            }
            return when {
                response.isBlank() -> JSONObject()
                response.trimStart().startsWith("[") -> JSONObject().put("data", org.json.JSONArray(response))
                else -> JSONObject(response)
            }
        } finally {
            connection?.disconnect()
        }
    }
}
