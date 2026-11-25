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
    val login: String,
    val refreshToken: String,
    val expiresAt: Long
) {
    val ageBand: String
        get() = when {
            age <= 12 -> "7_12"
            age <= 17 -> "13_17"
            else -> "18_plus"
        }
}

data class ShahrvandTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long
)

object SupabaseAuthRepository {
    private const val TAG = "ShahrvandAuth"
    private val baseUrl get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publicKey get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private const val AUTH_FUNCTION = "/functions/v1/shahrvand-auth"

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
            require(password.length in 8..128) { "Парол бояд аз 8 то 128 аломат бошад." }

            val normalizedLogin = login.trim().lowercase()
            val response = request(
                AUTH_FUNCTION,
                "POST",
                JSONObject()
                    .put("action", "register")
                    .put("first_name", firstName.trim())
                    .put("last_name", if (lastName.isBlank()) JSONObject.NULL else lastName.trim())
                    .put("age", age)
                    .put("login", normalizedLogin)
                    .put("password", password),
                authenticated = false
            )

            if (response.optBoolean("needs_login")) {
                signInInternal(normalizedLogin, password)
            } else {
                accountFromResponse(response, normalizedLogin)
            }
        }.onFailure { Log.w(TAG, "Registration failed", it) }
    }

    suspend fun signIn(login: String, password: String): Result<ShahrvandAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalizedLogin = login.trim().lowercase()
                require(normalizedLogin.matches(Regex("^[a-z0-9][a-z0-9._-]{3,23}$"))) {
                    "Логин нодуруст аст."
                }
                require(password.isNotBlank()) { "Паролро ворид кунед." }
                signInInternal(normalizedLogin, password)
            }.onFailure { Log.w(TAG, "Sign in failed", it) }
        }

    suspend fun refreshSession(refreshToken: String): Result<ShahrvandTokens> = withContext(Dispatchers.IO) {
        runCatching {
            require(refreshToken.isNotBlank()) { "Сессия ба охир расид. Дубора ворид шавед." }
            val response = request(
                "/auth/v1/token?grant_type=refresh_token",
                "POST",
                JSONObject().put("refresh_token", refreshToken),
                authenticated = false
            )
            val access = response.optString("access_token")
            val refresh = response.optString("refresh_token")
            val expires = response.optLong(
                "expires_at",
                System.currentTimeMillis() / 1000L + response.optLong("expires_in", 3600L)
            )
            if (access.isBlank() || refresh.isBlank()) {
                throw IllegalStateException("Supabase сессияи навро барнагардонд.")
            }
            ShahrvandTokens(access, refresh, expires)
        }.onFailure { Log.w(TAG, "Session refresh failed", it) }
    }

    private fun signInInternal(login: String, password: String): ShahrvandAccount {
        val response = request(
            AUTH_FUNCTION,
            "POST",
            JSONObject()
                .put("action", "login")
                .put("login", login)
                .put("password", password),
            authenticated = false
        )
        return accountFromResponse(response, login)
    }

    private fun accountFromResponse(response: JSONObject, fallbackLogin: String): ShahrvandAccount {
        if (!response.optBoolean("ok")) {
            throw IllegalStateException(humanError(response.optString("error")))
        }
        val profile = response.optJSONObject("profile")
            ?: throw IllegalStateException("Профили корбар аз сервер гирифта нашуд.")
        val session = response.optJSONObject("session")
            ?: throw IllegalStateException("Сессияи воридшавӣ сохта нашуд.")
        val accessToken = session.optString("access_token")
        val refreshToken = session.optString("refresh_token")
        val expiresAt = session.optLong(
            "expires_at",
            System.currentTimeMillis() / 1000L + session.optLong("expires_in", 3600L)
        )
        val userId = profile.optString("id")
            .ifBlank { session.optJSONObject("user")?.optString("id").orEmpty() }
        if (accessToken.isBlank() || refreshToken.isBlank() || userId.isBlank()) {
            throw IllegalStateException("Supabase сессияи дурустро барнагардонд.")
        }
        return ShahrvandAccount(
            userId = userId,
            accessToken = accessToken,
            firstName = profile.optString("first_name"),
            lastName = profile.optString("last_name").takeIf { it.isNotBlank() && it != "null" },
            age = profile.optInt("age"),
            login = profile.optString("login", fallbackLogin),
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
    }

    private fun humanError(code: String): String = when (code) {
        "INVALID_NAME" -> "Номро дуруст ворид кунед. Насаб ихтиёрӣ аст."
        "INVALID_AGE" -> "Синну сол бояд аз 7 то 120 бошад."
        "INVALID_LOGIN" -> "Логин бояд 4–24 аломат дошта бошад."
        "INVALID_PASSWORD" -> "Парол бояд аз 8 то 128 аломат бошад."
        "LOGIN_ALREADY_USED" -> "Ин логин аллакай истифода шудааст."
        "INVALID_CREDENTIALS" -> "Логин ё парол нодуруст аст."
        "RATE_LIMITED" -> "Кӯшишҳо зиёд шуданд. Баъдтар такрор кунед."
        "SERVICE_TEMPORARILY_UNAVAILABLE" -> "Сервер муваққатан дастнорас аст. Баъдтар кӯшиш кунед."
        "ACCOUNT_CREATION_FAILED" -> "Сохтани ҳисоб иҷро нашуд. Эҳтимол логин аллакай истифода шудааст."
        "PROFILE_CREATION_FAILED" -> "Сохтани профил иҷро нашуд."
        else -> code.ifBlank { "Хатогӣ рух дод. Баъдтар кӯшиш кунед." }
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
                connectTimeout = 15_000
                readTimeout = 20_000
                doInput = true
                setRequestProperty("apikey", publicKey)
                setRequestProperty("Accept", "application/json")
                if (accessToken != null) {
                    setRequestProperty("Authorization", "Bearer $accessToken")
                } else if (path.startsWith("/auth/") || path.startsWith("/functions/")) {
                    setRequestProperty("Authorization", "Bearer $publicKey")
                }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                if (prefer != null) setRequestProperty("Prefer", prefer)
            }
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val errorBody = runCatching { JSONObject(response) }.getOrNull()
                val errorCode = errorBody?.optString("error").orEmpty()
                val message = errorBody?.optString("msg")?.takeIf { it.isNotBlank() }
                    ?: errorBody?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: errorBody?.optString("error_description")?.takeIf { it.isNotBlank() }
                    ?: humanError(errorCode.ifBlank { errorBody?.optString("error").orEmpty() })
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
