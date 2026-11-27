package tj.shahrvand.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class CleanupSubmissionRecord(
    val id: String,
    val status: String,
    val createdAt: String,
    val rejectionReason: String?,
    val xpAwarded: Int
)

object CleanupSubmissionRepository {
    private const val BUCKET = "shahrvand-cleanup-evidence"
    private const val MAX_FILE_BYTES = 50L * 1024L * 1024L
    private val baseUrl get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publicKey get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    private data class UploadSpec(
        val slot: String,
        val expectedPrefix: String
    )

    suspend fun submit(
        context: Context,
        userId: String,
        accessToken: String,
        beforePhoto: Uri,
        beforeVideo: Uri,
        afterPhoto: Uri,
        afterVideo: Uri
    ): CleanupSubmissionRecord = withContext(Dispatchers.IO) {
        requireValidSession(userId, accessToken)
        val specs = listOf(
            UploadSpec("before_photo", "image/"),
            UploadSpec("before_video", "video/"),
            UploadSpec("after_photo", "image/"),
            UploadSpec("after_video", "video/")
        )
        val uris = listOf(beforePhoto, beforeVideo, afterPhoto, afterVideo)
        val uploadedPaths = mutableListOf<String>()

        try {
            for (index in uris.indices) {
                val path = uploadEvidence(
                    context = context,
                    uri = uris[index],
                    userId = userId,
                    accessToken = accessToken,
                    spec = specs[index]
                )
                uploadedPaths += path
            }

            val body = JSONObject()
                .put("p_before_photo_path", uploadedPaths[0])
                .put("p_before_video_path", uploadedPaths[1])
                .put("p_after_photo_path", uploadedPaths[2])
                .put("p_after_video_path", uploadedPaths[3])

            val result = JSONObject(
                request(
                    path = "/rest/v1/rpc/shahrvand_create_cleanup_submission",
                    method = "POST",
                    accessToken = accessToken,
                    body = body
                )
            )
            if (!result.optBoolean("ok")) {
                throw IllegalStateException("Сервер дархости тозакуниро қабул накард.")
            }

            CleanupSubmissionRecord(
                id = result.optString("submission_id"),
                status = result.optString("status", "pending_review"),
                createdAt = result.optString("submitted_at"),
                rejectionReason = null,
                xpAwarded = result.optInt("xp_awarded")
            )
        } catch (error: Exception) {
            uploadedPaths.forEach { path ->
                runCatching { deleteEvidence(path, accessToken) }
            }
            throw error
        }
    }

    suspend fun loadMySubmissions(accessToken: String): List<CleanupSubmissionRecord> =
        withContext(Dispatchers.IO) {
            requireValidSession("read-only", accessToken, requireUserId = false)
            val response = org.json.JSONArray(
                request(
                    path = "/rest/v1/shahrvand_cleanup_submissions" +
                        "?select=id,status,created_at,rejection_reason,xp_awarded" +
                        "&order=created_at.desc&limit=20",
                    method = "GET",
                    accessToken = accessToken
                )
            )
            buildList {
                for (index in 0 until response.length()) {
                    val row = response.optJSONObject(index) ?: continue
                    add(
                        CleanupSubmissionRecord(
                            id = row.optString("id"),
                            status = row.optString("status"),
                            createdAt = row.optString("created_at"),
                            rejectionReason = row.optString("rejection_reason")
                                .takeIf { it.isNotBlank() && it != "null" },
                            xpAwarded = row.optInt("xp_awarded")
                        )
                    )
                }
            }
        }

    private fun uploadEvidence(
        context: Context,
        uri: Uri,
        userId: String,
        accessToken: String,
        spec: UploadSpec
    ): String {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)?.lowercase()
            ?: throw IllegalArgumentException("Формати файл муайян нашуд.")
        val allowed = when (spec.slot) {
            "before_photo", "after_photo" -> setOf("image/jpeg", "image/png", "image/webp")
            else -> setOf("video/mp4", "video/quicktime", "video/webm")
        }
        require(mimeType in allowed && mimeType.startsWith(spec.expectedPrefix)) {
            "Файл бояд дар формати тасвир ё видеои иҷозатдодашуда бошад."
        }

        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "video/mp4" -> "mp4"
            "video/quicktime" -> "mov"
            "video/webm" -> "webm"
            else -> throw IllegalArgumentException("Формати файл дастгирӣ намешавад.")
        }

        val tempFile = File.createTempFile("shahrvand_cleanup_", ".$extension", context.cacheDir)
        try {
            val input = resolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Файл кушода нашуд.")
            input.use { source ->
                FileOutputStream(tempFile).use { destination ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total in 1..MAX_FILE_BYTES) {
                            "Ҳаҷми ҳар файл набояд аз 50 МБ зиёд бошад."
                        }
                        destination.write(buffer, 0, read)
                    }
                }
            }
            require(tempFile.length() > 0L) { "Файли интихобшуда холӣ аст." }

            val path = "$userId/cleanup_${UUID.randomUUID()}_${spec.slot}.$extension"
            val encodedPath = path.split('/').joinToString("/") {
                URLEncoder.encode(it, Charsets.UTF_8.name()).replace("+", "%20")
            }
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$baseUrl/storage/v1/object/$BUCKET/$encodedPath")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    doOutput = true
                    setFixedLengthStreamingMode(tempFile.length())
                    setRequestProperty("apikey", publicKey)
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", mimeType)
                    setRequestProperty("Cache-Control", "3600")
                    setRequestProperty("x-upsert", "false")
                }
                FileInputStream(tempFile).use { source ->
                    connection.outputStream.use { destination ->
                        source.copyTo(destination, DEFAULT_BUFFER_SIZE)
                    }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val message = runCatching { JSONObject(raw).optString("message") }
                        .getOrNull()?.takeIf { it.isNotBlank() }
                    throw IllegalStateException(
                        message ?: "Боргузории файл иҷро нашуд (HTTP $status)."
                    )
                }
                return path
            } finally {
                connection?.disconnect()
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun deleteEvidence(path: String, accessToken: String) {
        val encodedPath = path.split('/').joinToString("/") {
            URLEncoder.encode(it, Charsets.UTF_8.name()).replace("+", "%20")
        }
        request(
            path = "/storage/v1/object/$BUCKET/$encodedPath",
            method = "DELETE",
            accessToken = accessToken
        )
    }

    private fun requireValidSession(
        userId: String,
        accessToken: String,
        requireUserId: Boolean = true
    ) {
        if (requireUserId) {
            require(userId.matches(Regex("^[0-9a-fA-F-]{36}$"))) {
                "Сессияи корбар нодуруст аст."
            }
        }
        require(accessToken.isNotBlank()) {
            "Сессия ба охир расид. Дубора ворид шавед."
        }
        require(baseUrl.startsWith("https://") && publicKey.isNotBlank()) {
            "Танзимоти Supabase нодуруст аст."
        }
    }

    private fun request(
        path: String,
        method: String,
        accessToken: String,
        body: JSONObject? = null
    ): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 12_000
                readTimeout = 20_000
                doInput = true
                setRequestProperty("apikey", publicKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                    it.write(body.toString())
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val parsed = runCatching { JSONObject(raw) }.getOrNull()
                val message = parsed?.optString("message").orEmpty()
                val code = parsed?.optString("code").orEmpty()
                throw IllegalStateException(
                    message.takeIf { it.isNotBlank() }
                        ?: code.takeIf { it.isNotBlank() }
                        ?: "Дархост иҷро нашуд (HTTP $status)."
                )
            }
            return raw
        } finally {
            connection?.disconnect()
        }
    }
}
