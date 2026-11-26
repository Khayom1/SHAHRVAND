package tj.shahrvand.app

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

private val ProfileNavy = Color(0xFF20384B)
private val ProfileGreen = Color(0xFF36766D)
private val ProfileGold = Color(0xFFB98A50)
private val ProfilePage = Color(0xFFF1F6FA)
private val ProfileInk = Color(0xFF263640)
private val ProfileMuted = Color(0xFF777F7E)
private val ProfileBorder = Color(0xFFDDE7EF)

private fun profileTr(language: String, tg: String, ru: String, en: String): String =
    when (language) {
        "ru" -> ru
        "en" -> en
        else -> tg
    }

private fun profileAgeLabel(language: String, age: Int): String = when {
    age <= 12 -> profileTr(language, "7–12 сола", "7–12 лет", "Ages 7–12")
    age <= 17 -> profileTr(language, "13–17 сола", "13–17 лет", "Ages 13–17")
    else -> profileTr(language, "18+ сола", "18+ лет", "Ages 18+")
}

@Composable
internal fun ShahrvandProfileScreen(
    language: String,
    firstName: String,
    lastName: String,
    age: Int,
    login: String,
    totalXp: Int,
    totalCompleted: Int,
    getAccessToken: suspend () -> String,
    onServerXp: (Int) -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    var achievementSummary by remember(login) { mutableStateOf<AchievementSummary?>(null) }
    var achievementLoading by remember(login) { mutableStateOf(true) }
    LaunchedEffect(login) {
        achievementLoading = true
        try {
            val summary = SupabaseCompetitionRepository.myAchievements(getAccessToken())
            achievementSummary = summary
            onServerXp(summary.totalXp)
        } catch (_: Exception) {
            achievementSummary = null
        } finally {
            achievementLoading = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(ProfilePage).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = profileTr(language, "Бозгашт", "Назад", "Back"), tint = ProfileNavy)
            }
            Text(
                profileTr(language, "Профили ман", "Мой профиль", "My profile"),
                modifier = Modifier.weight(1f),
                color = ProfileNavy,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = profileTr(language, "Танзимот", "Настройки", "Settings"), tint = ProfileNavy)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(27.dp),
            colors = CardDefaults.cardColors(containerColor = ProfileNavy),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 11.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val initials = (firstName.trim().firstOrNull()?.uppercase() ?: "Ш") +
                    (lastName.trim().firstOrNull()?.uppercase() ?: "")
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ")
                        .ifBlank { profileTr(language, "Шаҳрванд", "Гражданин", "Citizen") },
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text("@$login", color = Color(0xFFE7D4A8), fontSize = 13.sp)
                Text(profileAgeLabel(language, age), color = Color(0xFFDCE6ED), fontSize = 12.sp)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ProfileStatCard(
                profileTr(language, "Холҳо", "Очки", "XP"),
                totalXp.toString(),
                Modifier.weight(1f)
            )
            ProfileStatCard(
                profileTr(language, "Миссияҳо", "Задания", "Missions"),
                totalCompleted.toString(),
                Modifier.weight(1f)
            )
        }

        ProfileAchievementsCard(language, achievementSummary, achievementLoading)

        ProfileSectionTitle(profileTr(language, "Маълумоти ҳисоб", "Данные аккаунта", "Account details"))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(21.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.84f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileInfoRow(profileTr(language, "Ном", "Имя", "First name"), firstName.ifBlank { "—" })
                HorizontalDivider(color = ProfileBorder)
                ProfileInfoRow(profileTr(language, "Насаб", "Фамилия", "Last name"), lastName.ifBlank { "—" })
                HorizontalDivider(color = ProfileBorder)
                ProfileInfoRow(profileTr(language, "Синну сол", "Возраст", "Age"), "$age")
                HorizontalDivider(color = ProfileBorder)
                ProfileInfoRow(profileTr(language, "Логин", "Логин", "Username"), login)
            }
        }

        Button(
            onClick = onSettings,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(profileTr(language, "Кушодани танзимот", "Открыть настройки", "Open settings"), fontWeight = FontWeight.Bold)
        }
        Text(
            profileTr(language, "Маълумоти ҳисобро дар танзимот тағйир дода метавонӣ.",
                "Данные аккаунта можно изменить в настройках.", "You can edit your account details in Settings."),
            color = ProfileMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 18.dp)
        )
    }
}

@Composable
private fun ProfileAchievementsCard(
    language: String,
    summary: AchievementSummary?,
    loading: Boolean
) {
    val unlocked = summary?.unlockedCodes.orEmpty()
    val target = summary?.progress ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 950),
        label = "achievement_title_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.86f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    profileTr(language, "НИШОНҲО ВА УНВОНҲО", "ЗНАКИ И ТИТУЛЫ", "BADGES & TITLES"),
                    color = ProfileGold, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(
                    profileTr(language, "Пешрафти ту", "Твои достижения", "Your achievements"),
                    color = ProfileNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }

            when {
                loading -> Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ProfileGold, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
                summary == null -> Text(
                    profileTr(language, "Маълумоти нишону унвон бор нашуд. Профилро навсозӣ кун.",
                        "Не удалось загрузить награды. Обнови профиль.",
                        "Could not load awards. Refresh the profile."),
                    color = ProfileMuted, fontSize = 11.sp
                )
                else -> {
                    Text(
                        profileTr(language, "Нишонҳои асосӣ", "Основные значки", "Core badges"),
                        color = ProfileMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AchievementPill(
                            modifier = Modifier.weight(1f),
                            icon = "🌟",
                            title = profileTr(language, "Ворид шуд", "В приложении", "Joined"),
                            unlocked = "app_entry" in unlocked
                        )
                        AchievementPill(
                            modifier = Modifier.weight(1f),
                            icon = "⚡",
                            title = profileTr(language, "Вазифаи аввал", "Первое задание", "First task"),
                            unlocked = "first_task" in unlocked
                        )
                    }

                    Text(
                        profileTr(language, "УНВОНҲОИ МАХСУС", "ОСОБЫЕ ТИТУЛЫ", "SPECIAL TITLES"),
                        color = ProfileMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(
                            listOf("steady_10", "🛡️", "10+", profileTr(language, "Устувор", "Постоянный", "Steady")),
                            listOf("dedicated_25", "💠", "25+", profileTr(language, "Садоқатманд", "Преданный", "Dedicated")),
                            listOf("expert_50", "🏆", "50+", profileTr(language, "Устод", "Эксперт", "Expert")),
                            listOf("legend_100", "👑", "100+", profileTr(language, "Афсона", "Легенда", "Legend"))
                        ).forEach { item ->
                            val code = item[0]
                            val icon = item[1]
                            val count = item[2]
                            val title = item[3]
                            val earned = code in unlocked
                            Surface(
                                modifier = Modifier.weight(1f).animateContentSize(),
                                color = if (earned) Color(0xFFFFF4DE) else Color(0xFFF2F5F8),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (earned) Color(0xFFE1C58D) else ProfileBorder
                                )
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(if (earned) icon else "🔒", fontSize = 17.sp)
                                    Text(count, color = if (earned) ProfileNavy else ProfileMuted,
                                        fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    Text(
                                        title,
                                        color = if (earned) ProfileNavy else ProfileMuted,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    val nextLabel = when (summary.nextThreshold) {
                        10 -> profileTr(language, "Устувор", "Постоянный", "Steady")
                        25 -> profileTr(language, "Садоқатманд", "Преданный", "Dedicated")
                        50 -> profileTr(language, "Устод", "Эксперт", "Expert")
                        else -> profileTr(language, "Афсона", "Легенда", "Legend")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                profileTr(language, "То унвони навбатӣ", "До следующего титула", "Next title"),
                                color = ProfileNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                            Text(nextLabel, color = ProfileGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "${summary.verifiedTasks}/${summary.nextThreshold}",
                            color = ProfileNavy, fontSize = 13.sp, fontWeight = FontWeight.Black
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(10.dp)),
                        color = ProfileGold,
                        trackColor = ProfileBorder
                    )
                    Text(
                        profileTr(
                            language,
                            "Унвонҳо танҳо баъди санҷиши серверӣ кушода мешаванд.",
                            "Титулы открываются только после проверки сервером.",
                            "Titles unlock only after server verification."
                        ),
                        color = ProfileMuted, fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementPill(
    modifier: Modifier,
    icon: String,
    title: String,
    unlocked: Boolean
) {
    Surface(
        modifier = modifier.animateContentSize(),
        color = if (unlocked) Color(0xFFFFF4DE) else Color(0xFFF2F5F8),
        shape = RoundedCornerShape(13.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (unlocked) Color(0xFFE1C58D) else ProfileBorder
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(if (unlocked) icon else "🔒", fontSize = 18.sp)
            Text(
                title,
                color = if (unlocked) ProfileNavy else ProfileMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ProfileStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.84f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, color = ProfileMuted, fontSize = 11.sp)
            Text(value, color = ProfileNavy, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(title, color = ProfileNavy, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ProfileMuted, fontSize = 12.sp, modifier = Modifier.weight(0.9f))
        Text(
            value,
            color = ProfileInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
internal fun ShahrvandSettingsDialog(
    language: String,
    firstName: String,
    lastName: String,
    age: Int,
    login: String,
    getAccessToken: suspend () -> String,
    fontScale: Float,
    fontFamilyChoice: String,
    onClose: () -> Unit,
    onLanguage: (String) -> Unit,
    onFontScale: (Float) -> Unit,
    onFontFamily: (String) -> Unit,
    onProfileSaved: (ShahrvandProfile) -> Unit,
    onSignOut: () -> Unit,
    onClearLocalProgress: () -> Unit
) {
    var editedFirstName by remember(firstName) { mutableStateOf(firstName) }
    var editedLastName by remember(lastName) { mutableStateOf(lastName) }
    var editedAge by remember(age) { mutableStateOf(age.toString()) }
    var editedLogin by remember(login) { mutableStateOf(login) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var savingProfile by remember { mutableStateOf(false) }
    var changingPassword by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp).heightIn(min = 300.dp, max = 720.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(profileTr(language, "Танзимоти ШАҲРВАНД", "Настройки SHAHRVAND", "SHAHRVAND settings"),
                            color = ProfileNavy, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(profileTr(language, "Ҳисоб, намуди барнома ва амният", "Аккаунт, оформление и безопасность", "Account, appearance and security"),
                            color = ProfileMuted, fontSize = 10.sp)
                    }
                    TextButton(onClick = onClose) {
                        Text(profileTr(language, "Пӯшидан", "Закрыть", "Close"))
                    }
                }
                HorizontalDivider(color = ProfileBorder, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ProfileSectionTitle(profileTr(language, "Ҳисоб ва профил", "Аккаунт и профиль", "Account and profile"))
                    OutlinedTextField(
                        value = editedFirstName,
                        onValueChange = { editedFirstName = it.take(60); message = "" },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(profileTr(language, "Ном", "Имя", "First name")) }
                    )
                    OutlinedTextField(
                        value = editedLastName,
                        onValueChange = { editedLastName = it.take(60); message = "" },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(profileTr(language, "Насаб (ихтиёрӣ)", "Фамилия (необязательно)", "Last name (optional)")) }
                    )
                    OutlinedTextField(
                        value = age.toString(),
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = false,
                        label = { Text(profileTr(language, "Синну сол", "Возраст", "Age")) },
                        supportingText = {
                            Text(profileTr(language,
                                "Синну сол барои адолати рейтинг баъди сабти ном тағйир намеёбад.",
                                "Возраст нельзя менять после регистрации, чтобы сохранить честность рейтинга.",
                                "Age is locked after registration to protect leaderboard fairness."))
                        }
                    )
                    OutlinedTextField(
                        value = editedLogin,
                        onValueChange = {
                            editedLogin = it.filter { ch -> ch in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-" }.take(24)
                            message = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(profileTr(language, "Логин", "Логин", "Username")) },
                        supportingText = {
                            Text(profileTr(language, "4–24 аломат. Бо логини нав ворид мешавӣ.",
                                "4–24 символа. Вход будет по новому логину.",
                                "4–24 characters. You will sign in with the new username."))
                        }
                    )
                    Button(
                        onClick = {
                            val parsedAge = editedAge.toIntOrNull()
                            if (editedFirstName.trim().isEmpty() || parsedAge == null || parsedAge !in 7..120 ||
                                !editedLogin.trim().matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]{3,23}$"))
                            ) {
                                message = profileTr(language, "Ном, синну сол ё логин нодуруст аст.",
                                    "Проверьте имя, возраст и логин.", "Check the name, age, and username.")
                                messageIsError = true
                            } else {
                                savingProfile = true
                                message = ""
                                scope.launch {
                                    val result = runCatching {
                                        SupabaseAuthRepository.updateProfile(
                                            getAccessToken(), editedFirstName.trim(), editedLastName.trim(),
                                            parsedAge, editedLogin.trim()
                                        ).getOrThrow()
                                    }
                                    result.onSuccess {
                                        onProfileSaved(it)
                                        editedFirstName = it.firstName
                                        editedLastName = it.lastName.orEmpty()
                                        editedAge = it.age.toString()
                                        editedLogin = it.login
                                        message = profileTr(language, "Маълумоти ҳисоб нигоҳ дошта шуд.",
                                            "Данные аккаунта сохранены.", "Account details saved.")
                                        messageIsError = false
                                    }.onFailure {
                                        message = it.message ?: profileTr(language, "Тағйирот нигоҳ дошта нашуд.",
                                            "Не удалось сохранить изменения.", "Could not save changes.")
                                        messageIsError = true
                                    }
                                    savingProfile = false
                                }
                            }
                        },
                        enabled = !savingProfile && !changingPassword,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (savingProfile) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(profileTr(language, "Нигоҳ доштани профил", "Сохранить профиль", "Save profile"))
                    }

                    ProfileSectionTitle(profileTr(language, "Иваз кардани парол", "Изменение пароля", "Change password"))
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(profileTr(language, "Пароли ҷорӣ", "Текущий пароль", "Current password")) }
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(profileTr(language, "Пароли нав", "Новый пароль", "New password")) },
                        supportingText = { Text(profileTr(language, "8–128 аломат", "8–128 символов", "8–128 characters")) }
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(profileTr(language, "Такрори пароли нав", "Повторите новый пароль", "Confirm new password")) }
                    )
                    OutlinedButton(
                        onClick = {
                            if (newPassword.length !in 8..128 || newPassword != confirmPassword || currentPassword.isBlank()) {
                                message = profileTr(language,
                                    "Паролҳоро санҷед: пароли нав бояд 8–128 аломат бошад ва ду майдон мувофиқат кунанд.",
                                    "Проверьте пароли: новый должен содержать 8–128 символов, оба поля должны совпадать.",
                                    "Check the passwords: the new one must be 8–128 characters and both fields must match.")
                                messageIsError = true
                            } else {
                                changingPassword = true
                                message = ""
                                scope.launch {
                                    val result = runCatching {
                                        SupabaseAuthRepository.changePassword(getAccessToken(), currentPassword, newPassword).getOrThrow()
                                    }
                                    result.onSuccess {
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                        message = profileTr(language, "Парол бомуваффақият иваз шуд.",
                                            "Пароль успешно изменён.", "Password changed successfully.")
                                        messageIsError = false
                                    }.onFailure {
                                        message = it.message ?: profileTr(language, "Парол иваз нашуд.",
                                            "Не удалось изменить пароль.", "Could not change password.")
                                        messageIsError = true
                                    }
                                    changingPassword = false
                                }
                            }
                        },
                        enabled = !changingPassword && !savingProfile,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (changingPassword) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(profileTr(language, "Нав кардани парол", "Обновить пароль", "Update password"))
                    }
                    if (message.isNotBlank()) {
                        Text(message, color = if (messageIsError) MaterialTheme.colorScheme.error else ProfileGreen,
                            fontSize = 11.sp, lineHeight = 16.sp)
                    }

                    ProfileSectionTitle(profileTr(language, "Забон", "Язык", "Language"))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("tg", "ru", "en").forEach { code ->
                            FilterChip(selected = language == code, onClick = { onLanguage(code) },
                                label = { Text(code.uppercase()) })
                        }
                    }

                    ProfileSectionTitle(profileTr(language, "Намуди зоҳирӣ", "Оформление", "Appearance"))
                    Text(profileTr(language, "Андозаи ҳуруф", "Размер шрифта", "Font size") +
                        " • \${(fontScale * 100).toInt()}%", color = ProfileInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = fontScale.coerceIn(0.90f, 1.15f),
                        onValueChange = onFontScale,
                        valueRange = 0.90f..1.15f,
                        steps = 4
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("90%", color = ProfileMuted, fontSize = 10.sp)
                        Text("115%", color = ProfileMuted, fontSize = 10.sp)
                    }
                    Text(profileTr(language, "Навъи ҳуруф", "Тип шрифта", "Font family"),
                        color = ProfileInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(
                            "system" to profileTr(language, "Системавӣ (тавсия мешавад)", "Системный (рекомендуется)", "System (recommended)"),
                            "sans" to "Sans Serif",
                            "serif" to "Serif"
                        ).forEach { (code, label) ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .clickable { onFontFamily(code) }.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = fontFamilyChoice == code, onClick = { onFontFamily(code) })
                                Text(label, color = ProfileInk, fontSize = 13.sp)
                            }
                        }
                    }
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ProfilePage)
                    ) {
                        Text(
                            profileTr(language, "Намунаи матн: ин ҳуруф бояд дар кортҳо, тугмаҳо ва рӯйхатҳо ҷой гирад.",
                                "Пример текста: шрифт должен помещаться в карточках, кнопках и списках.",
                                "Text preview: this font should fit cards, buttons, and lists."),
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            color = ProfileInk,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }

                    ProfileSectionTitle(profileTr(language, "Амният ва махфият", "Безопасность и конфиденциальность", "Security and privacy"))
                    ProfileSecurityLine(profileTr(language, "Парол дар танзимоти одии телефон нигоҳ дошта намешавад; санҷиш тавассути Supabase Auth аст.",
                        "Пароль не хранится в обычных настройках телефона; проверка выполняется через Supabase Auth.",
                        "The password is not stored in ordinary app preferences; authentication is handled by Supabase Auth."))
                    ProfileSecurityLine(profileTr(language, "Пайвастшавӣ бо сервер тавассути HTTPS анҷом мешавад.",
                        "Соединение с сервером выполняется через HTTPS.",
                        "Connections to the server use HTTPS."))
                    ProfileSecurityLine(profileTr(language, "Барои маълумоти шахсӣ сессияи фаъол ва иҷозати ҳисоб талаб мешавад.",
                        "Для доступа к личным данным требуется активная сессия и разрешение аккаунта.",
                        "Personal data requires an active session and account authorization."))
                    ProfileSecurityLine(profileTr(language, "Барнома иҷозати GPS, камера ва микрофон талаб намекунад.",
                        "Приложение не запрашивает доступ к GPS, камере и микрофону.",
                        "The app does not request GPS, camera, or microphone access."))

                    ProfileSectionTitle(profileTr(language, "Маълумоти техникӣ", "Техническая информация", "Technical information"))
                    ProfileInfoRow(profileTr(language, "Версияи барнома", "Версия приложения", "App version"), BuildConfig.VERSION_NAME)
                    ProfileInfoRow(profileTr(language, "Android", "Android", "Android"), Build.VERSION.RELEASE ?: "—")
                    ProfileInfoRow(profileTr(language, "Дастгоҳ", "Устройство", "Device"), "\${Build.MANUFACTURER} \${Build.MODEL}".trim())
                    ProfileInfoRow(profileTr(language, "Пайвастшавӣ", "Подключение", "Connection"), "Supabase Auth")

                    ProfileSectionTitle(profileTr(language, "Идоракунии маълумоти маҳаллӣ", "Локальные данные", "Local data management"))
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(profileTr(language, "Пок кардани пешрафти ҳамин телефон",
                            "Сбросить прогресс на этом телефоне", "Clear progress on this device"),
                            textAlign = TextAlign.Center)
                    }
                    Text(profileTr(language, "Ин амал холҳо ва миссияҳои маҳаллиро пок мекунад. Ҳисоб ва маълумоти серверӣ нест намешаванд.",
                        "Будут удалены локальные очки и задания. Аккаунт и серверные данные останутся.",
                        "This clears local points and mission progress only. Your account and server data remain."),
                        color = ProfileMuted, fontSize = 10.sp, lineHeight = 15.sp)

                    OutlinedButton(
                        onClick = { showSignOutConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(profileTr(language, "Баромадан аз ҳисоб", "Выйти из аккаунта", "Sign out"))
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(profileTr(language, "Аз ҳисоб бароем?", "Выйти из аккаунта?", "Sign out?")) },
            text = { Text(profileTr(language, "Барои истифодаи дубора бояд бо логин ва парол ворид шавӣ.",
                "Для следующего входа понадобятся логин и пароль.",
                "You will need your username and password to sign in again.")) },
            confirmButton = {
                TextButton(onClick = onSignOut) {
                    Text(profileTr(language, "Баромадан", "Выйти", "Sign out"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(profileTr(language, "Бекор", "Отмена", "Cancel"))
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(profileTr(language, "Пешрафт пок шавад?", "Сбросить прогресс?", "Clear progress?")) },
            text = { Text(profileTr(language, "Холҳо ва миссияҳои маҳаллӣ нест мешаванд. Ин амалро баргардондан мумкин нест.",
                "Очки и локальные задания будут удалены. Это действие нельзя отменить.",
                "Local points and mission progress will be deleted. This cannot be undone.")) },
            confirmButton = {
                TextButton(onClick = {
                    onClearLocalProgress()
                    showClearConfirm = false
                    message = profileTr(language, "Пешрафти маҳаллӣ пок шуд.",
                        "Локальный прогресс сброшен.", "Local progress cleared.")
                    messageIsError = false
                }) {
                    Text(profileTr(language, "Пок кардан", "Сбросить", "Clear"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(profileTr(language, "Бекор", "Отмена", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun ProfileSecurityLine(text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text("•", color = ProfileGreen, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(8.dp))
        Text(text, color = ProfileMuted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
