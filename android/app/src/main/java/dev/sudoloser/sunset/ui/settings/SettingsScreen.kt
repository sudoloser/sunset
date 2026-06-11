package dev.sudoloser.sunset.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.PrefKeys
import dev.sudoloser.sunset.data.dataStore
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit

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
    onLogout: () -> Unit,
    onGoToAdmin: () -> Unit
) {
    var tab by remember { mutableStateOf("account") }
    val tabs = buildList {
        add("account"); add("discord"); add("downloads")
        if (isAdmin) add("admin")
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            color = Color.White
        )

        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(tab),
            containerColor = Color.Transparent,
            contentColor = Color.White,
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
                            color = if (selected) Color.White else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when (tab) {
                "account" -> AccountSettings(apiClient, baseUrl, userId, currentUsername, onLogout)
                "discord" -> DiscordSettings(apiClient, userId)
                "downloads" -> DownloadSettings()
                "admin" -> onGoToAdmin()
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var downloadPath by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        downloadPath = ctx.dataStore.data.first()[PrefKeys.DOWNLOAD_PATH] ?: ""
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
                Text("Download Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Files will be saved to this directory on your device.",
                    color = Color.Gray,
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
        Spacer(Modifier.height(80.dp))
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
            } catch (_: Exception) {}
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
                    color = Color.Gray,
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
                    color = Color.Gray,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(Modifier.height(80.dp))
    }
}
