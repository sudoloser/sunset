package dev.sudoloser.sunset.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import coil.compose.AsyncImage
import dev.sudoloser.sunset.api.ApiClient
import android.util.Log
import dev.sudoloser.sunset.data.PrefKeys
import dev.sudoloser.sunset.data.dataStore
import dev.sudoloser.sunset.ui.admin.AdminScreen
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiClient: ApiClient,
    baseUrl: String,
    userId: String?,
    currentUsername: String?,
    isAdmin: Boolean,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    useMaterial3: Boolean,
    onMaterial3Change: (Boolean) -> Unit,
    tvMode: Boolean,
    onTvModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onGoToAdmin: () -> Unit
) {
    var tab by remember { mutableStateOf("account") }
    val tabs = buildList {
        add("media"); add("account"); add("appearance"); add("discord")
        if (isAdmin) add("admin")
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(tab),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            divider = {},
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[tabs.indexOf(tab)])
                        .height(3.dp)
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                )
            },
            edgePadding = 24.dp
        ) {
            tabs.forEach { t ->
                val selected = tab == t
                Tab(
                    selected = selected,
                    onClick = { tab = t },
                    text = {
                        Text(
                            t.replaceFirstChar { it.uppercase() },
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when (tab) {
                "media" -> MediaSettings()
                "appearance" -> AppearanceSettings(darkTheme, onDarkThemeChange, useMaterial3, onMaterial3Change, tvMode, onTvModeChange)
                "account" -> AccountSettings(apiClient, baseUrl, userId, currentUsername, onLogout)
                "discord" -> DiscordSettings(apiClient, userId)
                "admin" -> AdminScreen(apiClient, baseUrl, onBack = { tab = "account" })
            }
        }
    }
}

@Composable
fun AppearanceSettings(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    useMaterial3: Boolean,
    onMaterial3Change: (Boolean) -> Unit,
    tvMode: Boolean,
    onTvModeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDarkThemeChange(!darkTheme) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Mode", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (darkTheme) "Dark theme active" else "Light theme active",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = onDarkThemeChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onMaterial3Change(!useMaterial3) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Material 3", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (useMaterial3) "Modern Material 3 design" else "Classic Material 2 design",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = useMaterial3,
                        onCheckedChange = onMaterial3Change,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTvModeChange(!tvMode) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TV / Leanback Mode", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (tvMode) "Large UI optimized for TV & D-pad" else "Standard mobile interface",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = tvMode,
                        onCheckedChange = onTvModeChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SunsetButton(
                        text = "Primary",
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    SunsetButton(
                        text = "Secondary",
                        onClick = {},
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                SunsetCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "This is how content cards will look with the current theme.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun MediaSettings() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Clip Recording
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            RecordingResolutionSetting()
        }

        // Subtitles
        SubtitleSettingsContent()

        // Downloads
        DownloadSettingsContent()

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun RecordingResolutionSetting() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var selectedResolution by remember { mutableIntStateOf(1080) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        selectedResolution = ctx.dataStore.data.first()[PrefKeys.RECORD_RESOLUTION] ?: 1080
    }

    Column {
        Text("Clip Recording Resolution", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Resolution for screen clips saved to DCIM",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(480, 720, 1080, 1440, 2160).forEach { res ->
                val label = if (res == 2160) "4K" else "${res}p"
                val isSelected = selectedResolution == res
                Surface(
                    onClick = {
                        selectedResolution = res
                        scope.launch { ctx.dataStore.edit { it[PrefKeys.RECORD_RESOLUTION] = res } }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSettings(
    apiClient: ApiClient,
    baseUrl: String,
    userId: String?,
    currentUsername: String?,
    onLogout: () -> Unit
) {
    var username by remember { mutableStateOf(currentUsername ?: "") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordMsg by remember { mutableStateOf<String?>(null) }
    var usernameMsg by remember { mutableStateOf<String?>(null) }
    var photoMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && userId != null) {
            scope.launch {
                try {
                    val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        apiClient.uploadProfilePicture(userId, b64)
                        photoMsg = "Photo updated!"
                    }
                } catch (e: Exception) {
                    photoMsg = "Failed: ${e.message}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (userId != null) {
                    AsyncImage(
                        model = apiClient.getProfilePictureUrl(userId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (currentUsername?.take(2)?.uppercase() ?: "U"),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                SunsetButton(
                    text = "Change Photo",
                    onClick = { photoPicker.launch("image/*") },
                    variant = ButtonVariant.Secondary
                )
                if (photoMsg != null) {
                    Text(photoMsg!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SunsetInput(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username"
                )
                if (usernameMsg != null) {
                    Text(usernameMsg!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                SunsetButton(
                    text = "Save Username",
                    onClick = {
                        scope.launch {
                            try {
                                if (userId != null) {
                                    apiClient.changeUsername(userId, username)
                                    usernameMsg = "Username updated!"
                                }
                            } catch (e: Exception) { usernameMsg = e.message }
                        }
                    },
                    fullWidth = true
                )
            }
        }

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SunsetInput(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = "Current Password",
                    password = true
                )
                SunsetInput(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New Password",
                    password = true
                )
                if (passwordMsg != null) {
                    Text(passwordMsg!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                SunsetButton(
                    text = "Update Password",
                    onClick = {
                        scope.launch {
                            try {
                                if (userId != null) {
                                    apiClient.changePassword(userId, currentPassword, newPassword)
                                    passwordMsg = "Password updated!"
                                    currentPassword = ""; newPassword = ""
                                }
                            } catch (e: Exception) { passwordMsg = e.message }
                        }
                    },
                    fullWidth = true
                )
            }
        }

        SunsetButton(
            text = "Log Out",
            onClick = onLogout,
            variant = ButtonVariant.Danger,
            fullWidth = true
        )
        
        Spacer(Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettings() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        DownloadSettingsContent()
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun DownloadSettingsContent() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var downloadPath by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        downloadPath = ctx.dataStore.data.first()[PrefKeys.DOWNLOAD_PATH] ?: ""
    }

    SunsetCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Download Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Files will be saved to this directory on your device.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            SunsetInput(
                value = downloadPath,
                onValueChange = { downloadPath = it; saved = false },
                label = "Download Path",
                placeholder = "e.g. /storage/emulated/0/Download/SunSet"
            )
            SunsetButton(
                text = if (saved) "✓ Saved" else "Save Path",
                onClick = {
                    scope.launch {
                        ctx.dataStore.edit { it[PrefKeys.DOWNLOAD_PATH] = downloadPath }
                        saved = true
                    }
                },
                fullWidth = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettings(
    apiClient: ApiClient,
    userId: String?
) {
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("online") }
    var loading by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }
    var stopLoading by remember { mutableStateOf(false) }
    var stopStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val profile = apiClient.getUserProfile(userId)
                if (profile?.discordToken != null) token = profile.discordToken
                if (profile?.discordStatus != null) status = profile.discordStatus
            } catch (e: Exception) { Log.e("SunSet", "Failed to load Discord config", e) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Discord Rich Presence", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Show what you're watching on your Discord profile. This requires your User Token.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )

                SunsetInput(
                    value = token,
                    onValueChange = { token = it },
                    label = "User Token",
                    placeholder = "Paste your token here",
                    password = true
                )

                Text("Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("online", "idle", "dnd").forEach { s ->
                        val selected = status == s
                        SunsetButton(
                            text = s.uppercase(),
                            onClick = { status = s },
                            variant = if (selected) ButtonVariant.Primary else ButtonVariant.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SunsetButton(
                    text = if (loading) "Saving..." else if (saveStatus == "success") "✓ Saved" else "Sync Discord",
                    onClick = {
                        if (userId == null) return@SunsetButton
                        loading = true; saveStatus = null
                        scope.launch {
                            try {
                                apiClient.updateDiscordConfig(userId, token, status)
                                saveStatus = "success"
                                delay(3000); saveStatus = null
                            } catch (_: Exception) { saveStatus = "error" }
                            loading = false
                        }
                    },
                    enabled = token.isNotBlank() && !loading,
                    fullWidth = true
                )
                
                if (saveStatus == "error") {
                    Text("Connection failed. Check your token.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                SunsetButton(
                    text = if (stopLoading) "Stopping..." else if (stopStatus == "success") "✓ Stopped" else "Stop RPC",
                    onClick = {
                        if (userId == null) return@SunsetButton
                        stopLoading = true; stopStatus = null
                        scope.launch {
                            try {
                                apiClient.stopDiscordRpc(userId)
                                stopStatus = "success"
                                delay(3000); stopStatus = null
                            } catch (_: Exception) { stopStatus = "error" }
                            stopLoading = false
                        }
                    },
                    variant = ButtonVariant.Danger,
                    enabled = !stopLoading,
                    fullWidth = true
                )

                if (stopStatus == "error") {
                    Text("Failed to stop RPC.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How to find your token?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "1. Open Discord in browser\n2. Ctrl+Shift+I > Network tab\n3. Filter by '/api' and refresh\n4. Look for 'authorization' header in any request",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettings() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SubtitleSettingsContent()
        Spacer(Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsContent() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val FONT_OPTIONS = listOf(
        "Inter, sans-serif" to "Inter",
        "\"Google Sans\", sans-serif" to "Google Sans",
        "Arial, sans-serif" to "Arial",
        "Helvetica, sans-serif" to "Helvetica",
        "Verdana, sans-serif" to "Verdana",
        "\"Times New Roman\", serif" to "Times New Roman",
        "\"Courier New\", monospace" to "Courier New",
    )

    val COLORS = listOf(
        "#ffffff", "#ffff00", "#00ff00", "#00ffff",
        "#ff9900", "#ff66cc", "#ff0000", "#cccccc"
    )

    var color by remember { mutableStateOf("#ffffff") }
    var backgroundOpacity by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableIntStateOf(100) }
    var font by remember { mutableStateOf("Inter, sans-serif") }
    var bold by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = ctx.dataStore.data.first()
        color = prefs[PrefKeys.SUBTITLE_COLOR] ?: "#ffffff"
        backgroundOpacity = prefs[PrefKeys.SUBTITLE_BACKGROUND_OPACITY] ?: 0f
        size = prefs[PrefKeys.SUBTITLE_SIZE] ?: 100
        font = prefs[PrefKeys.SUBTITLE_FONT] ?: "Inter, sans-serif"
        bold = prefs[PrefKeys.SUBTITLE_BOLD] ?: false
    }

    fun save() {
        scope.launch {
            ctx.dataStore.edit {
                it[PrefKeys.SUBTITLE_COLOR] = color
                it[PrefKeys.SUBTITLE_BACKGROUND_OPACITY] = backgroundOpacity
                it[PrefKeys.SUBTITLE_SIZE] = size
                it[PrefKeys.SUBTITLE_FONT] = font
                it[PrefKeys.SUBTITLE_BOLD] = bold
            }
        }
    }

    SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Style", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Column {
                    Text("Text Color", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        COLORS.forEach { c ->
                            val isSelected = color == c
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(c)))
                                    .border(if (isSelected) 3.dp else 1.5.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
                                    .clickable { color = c; save() }
                            )
                        }
                    }
                }

                Column {
                    Text("Background", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0f to "None", 0.85f to "Black Rectangle (CC)").forEach { (value, label) ->
                            val selected = backgroundOpacity == value
                            SunsetButton(
                                text = label,
                                onClick = { backgroundOpacity = value; save() },
                                variant = if (selected) ButtonVariant.Primary else ButtonVariant.Secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column {
                    Text("Size: $size%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    Slider(
                        value = size.toFloat(),
                        onValueChange = { size = it.toInt(); save() },
                        valueRange = 50f..200f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Text("Font", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    FONT_OPTIONS.forEach { (value, label) ->
                        val selected = font == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { font = value; save() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { bold = !bold; save() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bold text", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = bold,
                        onCheckedChange = { bold = it; save() },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color(0xFF111111), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "This is a preview of your subtitle style.",
                        color = Color(android.graphics.Color.parseColor(color)),
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        fontSize = (14 * size / 100).sp,
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                            .background(
                                if (backgroundOpacity > 0) MaterialTheme.colorScheme.background.copy(alpha = backgroundOpacity)
                                else Color.Transparent
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
