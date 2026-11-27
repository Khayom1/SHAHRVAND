package tj.shahrvand.app

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class CleanupPickedFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String
)

private fun cleanupFileInfo(context: Context, uri: Uri, isVideo: Boolean): CleanupPickedFile {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri)?.lowercase().orEmpty()
    val allowed = if (isVideo) {
        setOf("video/mp4", "video/quicktime", "video/webm")
    } else {
        setOf("image/jpeg", "image/png", "image/webp")
    }
    require(mime in allowed) {
        if (isVideo) "Интихоб кун: MP4, MOV ё WebM." else "Интихоб кун: JPEG, PNG ё WebP."
    }

    var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "evidence"
    var size = -1L
    var cursor: Cursor? = null
    try {
        cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) displayName = it.getString(nameIndex) ?: displayName
                if (sizeIndex >= 0 && !it.isNull(sizeIndex)) size = it.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {
        // Size is measured while copying to the app cache before upload.
    } finally {
        cursor?.close()
    }
    require(size <= 50L * 1024L * 1024L || size < 0L) {
        "Ҳаҷми файл набояд аз 50 МБ зиёд бошад."
    }
    return CleanupPickedFile(uri, displayName, size, mime)
}

private fun cleanupFormatBytes(bytes: Long): String =
    if (bytes < 0L) "ҳаҷм ҳангоми боргузорӣ санҷида мешавад"
    else String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))

private fun cleanupStatusLabel(language: String, status: String): String = when (status) {
    "pending_review" -> nutritionTr(language, "Дар навбати баррасӣ", "Ожидает проверки", "Pending review")
    "approved" -> nutritionTr(language, "Тасдиқ шуд", "Подтверждено", "Approved")
    "rejected" -> nutritionTr(language, "Рад шуд", "Отклонено", "Rejected")
    "resubmission_required" -> nutritionTr(language, "Далелҳоро аз нав фирист", "Нужно отправить заново", "Resubmission required")
    else -> nutritionTr(language, "Ҳолати номаълум", "Неизвестный статус", "Unknown status")
}

private fun cleanupTimeLabel(value: String): String = runCatching {
    Instant.parse(value).atZone(ZoneId.of("Asia/Dushanbe"))
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm"))
}.getOrDefault(value)

@Composable
internal fun CleanupSubmissionScreen(
    language: String,
    userId: String,
    getAccessToken: suspend () -> String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalShahrvandPalette.current
    val scope = rememberCoroutineScope()

    var beforePhoto by remember { mutableStateOf<CleanupPickedFile?>(null) }
    var beforeVideo by remember { mutableStateOf<CleanupPickedFile?>(null) }
    var afterPhoto by remember { mutableStateOf<CleanupPickedFile?>(null) }
    var afterVideo by remember { mutableStateOf<CleanupPickedFile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(emptyList<CleanupSubmissionRecord>()) }

    suspend fun refreshHistory() {
        history = CleanupSubmissionRepository.loadMySubmissions(getAccessToken())
    }

    LaunchedEffect(userId) {
        loading = true
        error = ""
        try {
            if (userId.isNotBlank()) refreshHistory()
        } catch (e: Exception) {
            error = e.message ?: nutritionTr(language, "Таърихи дархостҳо бор нашуд.", "Не удалось загрузить историю заявок.", "Could not load submission history.")
        } finally {
            loading = false
        }
    }

    val hasPending = history.any { it.status == "pending_review" }
    val beforePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { cleanupFileInfo(context, uri, false) }
                .onSuccess { beforePhoto = it; error = "" }
                .onFailure { error = it.message ?: "Файл қабул нашуд." }
        }
    }
    val beforeVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { cleanupFileInfo(context, uri, true) }
                .onSuccess { beforeVideo = it; error = "" }
                .onFailure { error = it.message ?: "Файл қабул нашуд." }
        }
    }
    val afterPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { cleanupFileInfo(context, uri, false) }
                .onSuccess { afterPhoto = it; error = "" }
                .onFailure { error = it.message ?: "Файл қабул нашуд." }
        }
    }
    val afterVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { cleanupFileInfo(context, uri, true) }
                .onSuccess { afterVideo = it; error = "" }
                .onFailure { error = it.message ?: "Файл қабул нашуд." }
        }
    }

    Column(Modifier.fillMaxSize().shahrvandCanvas()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !uploading) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = nutritionTr(language, "Бозгашт", "Назад", "Back"), tint = palette.ink)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        nutritionTr(language, "ҶОМЕАИ САБЗ", "ЗЕЛЁНОЕ СООБЩЕСТВО", "GREEN COMMUNITY"),
                        color = palette.green, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        nutritionTr(language, "Тозакунии кӯча", "Уборка улицы", "Street clean-up"),
                        color = palette.ink, fontSize = 21.sp, fontWeight = FontWeight.Bold
                    )
                }
                Surface(shape = CircleShape, color = palette.surface, border = BorderStroke(1.dp, palette.border)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = palette.green, modifier = Modifier.padding(10.dp).size(21.dp))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(1.dp, palette.border),
                shape = RoundedCornerShape(21.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        nutritionTr(language, "ЧӢ БОЯД КАРД", "ЧТО НУЖНО СДЕЛАТЬ", "WHAT TO DO"),
                        color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        nutritionTr(
                            language,
                            "Пеш аз оғози кор аз ҷой акс ва видео гир. Партовҳоро ҷамъ карда ба қуттии партов парто. Баъд аз анҷом боз акс ва видео гир.",
                            "До начала работы сними место на фото и видео. Собери мусор и выбрось его в контейнер. После завершения снова сними фото и видео.",
                            "Before starting, take a photo and video of the area. Collect litter and place it in a bin. After finishing, take another photo and video."
                        ),
                        color = palette.ink, fontSize = 11.sp, lineHeight = 16.sp
                    )
                    Text(
                        nutritionTr(language, "Барои баррасӣ ҳамаи 4 файл лозиманд.", "Для проверки нужны все 4 файла.", "All four files are required for review."),
                        color = palette.navyLight, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (hasPending) {
                Surface(
                    color = palette.goldSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text(
                        nutritionTr(
                            language,
                            "Дархости пешина ҳанӯз баррасӣ нашудааст. Баъди қарор гирифтани администратор метавонед дархости нав фиристед.",
                            "Предыдущая заявка ещё не проверена. Новую можно отправить после решения администратора.",
                            "Your previous submission is still awaiting review. You can submit another after the reviewer decides."
                        ),
                        modifier = Modifier.fillMaxWidth().padding(11.dp),
                        color = palette.ink, fontSize = 10.sp, lineHeight = 15.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    nutritionTr(language, "ДАЛЕЛҲОИ ИҶРО", "ДОКАЗАТЕЛЬСТВА ВЫПОЛНЕНИЯ", "COMPLETION EVIDENCE"),
                    color = palette.muted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                )
            }
            item {
                CleanupEvidenceCard(
                    title = nutritionTr(language, "Пеш аз тозагӣ · акс", "До уборки · фото", "Before clean-up · photo"),
                    file = beforePhoto,
                    icon = "image",
                    language = language,
                    enabled = !uploading && !hasPending,
                    onPick = {
                        beforePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemove = { beforePhoto = null }
                )
            }
            item {
                CleanupEvidenceCard(
                    title = nutritionTr(language, "Пеш аз тозагӣ · видео", "До уборки · видео", "Before clean-up · video"),
                    file = beforeVideo,
                    icon = "video",
                    language = language,
                    enabled = !uploading && !hasPending,
                    onPick = {
                        beforeVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    onRemove = { beforeVideo = null }
                )
            }
            item {
                CleanupEvidenceCard(
                    title = nutritionTr(language, "Баъди тозагӣ · акс", "После уборки · фото", "After clean-up · photo"),
                    file = afterPhoto,
                    icon = "image",
                    language = language,
                    enabled = !uploading && !hasPending,
                    onPick = {
                        afterPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemove = { afterPhoto = null }
                )
            }
            item {
                CleanupEvidenceCard(
                    title = nutritionTr(language, "Баъди тозагӣ · видео", "После уборки · видео", "After clean-up · video"),
                    file = afterVideo,
                    icon = "video",
                    language = language,
                    enabled = !uploading && !hasPending,
                    onPick = {
                        afterVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    onRemove = { afterVideo = null }
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = palette.navyLight, modifier = Modifier.size(17.dp))
                            Text(
                                nutritionTr(language, "МАХФИЯТ ВА БАРРАСӢ", "КОНФИДЕНЦИАЛЬНОСТЬ И ПРОВЕРКА", "PRIVACY AND REVIEW"),
                                color = palette.ink, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            nutritionTr(
                                language,
                                "Файлҳо дар анбори хусусӣ нигоҳ дошта мешаванд ва барои баррасӣ ба масъули SHAHRVAND дастрас мешаванд. Барнома GPS-и телефонро намепурсад, аммо худи акс ё видео метавонад метамаълумоти ҷойгиршавӣ дошта бошад; пеш аз фиристодан файлро санҷ.",
                                "Файлы хранятся в приватном хранилище и доступны ответственному SHAHRVAND для проверки. Приложение не запрашивает GPS телефона, но выбранные фото и видео могут содержать геометаданные местоположения; проверь их перед отправкой.",
                                "Files are stored privately for review by SHAHRVAND staff. The app does not request phone GPS, but selected photos or videos may contain embedded location metadata; check them before sending."
                            ),
                            color = palette.muted, fontSize = 10.sp, lineHeight = 15.sp
                        )
                    }
                }
            }
            if (error.isNotBlank()) {
                item { Text(error, color = palette.danger, fontSize = 11.sp, lineHeight = 16.sp) }
            }
            if (status.isNotBlank()) {
                item {
                    Surface(color = palette.successSurface, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, palette.successBorder)) {
                        Text(status, modifier = Modifier.fillMaxWidth().padding(11.dp), color = palette.ink, fontSize = 11.sp)
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            uploading = true
                            error = ""
                            status = ""
                            try {
                                progress = nutritionTr(language, "Боркунии 4 далел…", "Загрузка 4 доказательств…", "Uploading four evidence files…")
                                val submitted = CleanupSubmissionRepository.submit(
                                    context = context.applicationContext,
                                    userId = userId,
                                    accessToken = getAccessToken(),
                                    beforePhoto = beforePhoto!!.uri,
                                    beforeVideo = beforeVideo!!.uri,
                                    afterPhoto = afterPhoto!!.uri,
                                    afterVideo = afterVideo!!.uri
                                )
                                history = listOf(submitted) + history
                                beforePhoto = null
                                beforeVideo = null
                                afterPhoto = null
                                afterVideo = null
                                status = nutritionTr(
                                    language,
                                    "Дархост фиристода шуд. Пас аз баррасии администратор ҳолат нав мешавад.",
                                    "Заявка отправлена. Статус обновится после проверки администратором.",
                                    "Submission sent. Its status will update after administrator review."
                                )
                                try {
                                    refreshHistory()
                                } catch (_: Exception) {
                                    error = nutritionTr(language, "Дархост фиристода шуд, аммо таърих нав нашуд. Барои санҷиш навсозиро пахш кун.", "Заявка отправлена, но история не обновилась. Нажми обновить для проверки.", "Submission was sent, but history did not refresh. Refresh to verify its status.")
                                }
                            } catch (e: Exception) {
                                error = e.message ?: nutritionTr(language, "Дархост фиристода нашуд.", "Не удалось отправить заявку.", "Could not submit the evidence.")
                            } finally {
                                uploading = false
                                progress = ""
                            }
                        }
                    },
                    enabled = !uploading && !loading && !hasPending &&
                        userId.isNotBlank() && beforePhoto != null && beforeVideo != null &&
                        afterPhoto != null && afterVideo != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    if (uploading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White)
                        Spacer(Modifier.size(8.dp))
                        Text(progress.ifBlank { nutritionTr(language, "Боркунӣ…", "Загрузка…", "Uploading…") })
                    } else {
                        Text(nutritionTr(language, "Фиристодан барои баррасӣ", "Отправить на проверку", "Submit for review"), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(nutritionTr(language, "Таърихи дархостҳо", "История заявок", "Submission history"), color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(nutritionTr(language, "Ҳолати баррасии масъул", "Статус проверки", "Reviewer status"), color = palette.muted, fontSize = 10.sp)
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                loading = true
                                try { refreshHistory() }
                                catch (e: Exception) { error = e.message ?: "Refresh failed" }
                                finally { loading = false }
                            }
                        },
                        enabled = !loading && !uploading
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = nutritionTr(language, "Навсозӣ", "Обновить", "Refresh"), tint = palette.navyLight)
                    }
                }
            }
            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (history.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(15.dp), color = palette.surface, border = BorderStroke(1.dp, palette.border)) {
                        Text(
                            nutritionTr(language, "Ҳоло дархост надорӣ.", "Пока нет заявок.", "You have no submissions yet."),
                            modifier = Modifier.fillMaxWidth().padding(12.dp), color = palette.muted, fontSize = 10.sp
                        )
                    }
                }
            } else {
                items(history, key = { it.id }) { record ->
                    CleanupHistoryCard(language, record)
                }
            }
            item { Spacer(Modifier.size(12.dp)) }
        }
    }
}

@Composable
private fun CleanupEvidenceCard(
    title: String,
    file: CleanupPickedFile?,
    icon: String,
    language: String,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = LocalShahrvandPalette.current
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border),
        shape = RoundedCornerShape(17.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(palette.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (icon == "video") Icons.Filled.VideoLibrary else Icons.Filled.Image,
                    contentDescription = null,
                    tint = palette.navyLight,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = palette.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                if (file == null) {
                    Text(nutritionTr(language, "Файл интихоб нашудааст", "Файл не выбран", "No file selected"), color = palette.muted, fontSize = 9.sp)
                } else {
                    Text(file.name, color = palette.ink, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(cleanupFormatBytes(file.sizeBytes) + " · " + file.mimeType, color = palette.muted, fontSize = 8.sp)
                }
            }
            if (file != null) {
                IconButton(onClick = onRemove, enabled = enabled, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = nutritionTr(language, "Нест кардан", "Удалить", "Remove"), tint = palette.muted, modifier = Modifier.size(17.dp))
                }
            } else {
                OutlinedButton(
                    onClick = onPick,
                    enabled = enabled,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 38.dp)
                ) {
                    Text(nutritionTr(language, "Интихоб", "Выбрать", "Choose"), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun CleanupHistoryCard(language: String, record: CleanupSubmissionRecord) {
    val palette = LocalShahrvandPalette.current
    val (bg, textColor) = when (record.status) {
        "approved" -> palette.successSurface to palette.green
        "rejected", "resubmission_required" -> palette.dangerSurface to palette.danger
        else -> palette.goldSurface to palette.navy
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border),
        shape = RoundedCornerShape(17.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cleanupTimeLabel(record.createdAt), modifier = Modifier.weight(1f), color = palette.muted, fontSize = 9.sp)
                Surface(color = bg, shape = RoundedCornerShape(10.dp)) {
                    Text(cleanupStatusLabel(language, record.status), modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!record.rejectionReason.isNullOrBlank()) {
                Text(
                    nutritionTr(language, "Сабаби рад: ", "Причина отказа: ", "Review note: ") + record.rejectionReason,
                    color = palette.danger, fontSize = 10.sp, lineHeight = 15.sp
                )
            }
            if (record.xpAwarded > 0) {
                Text("${record.xpAwarded} XP", color = palette.green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else if (record.status == "approved") {
                Text(
                    nutritionTr(language, "Тасдиқ шуд · XP ҳоло ба ин миссия пайваст нашудааст.", "Подтверждено · XP пока не подключён к этой миссии.", "Approved · XP is not connected to this mission yet."),
                    color = palette.muted, fontSize = 9.sp
                )
            }
        }
    }
}
