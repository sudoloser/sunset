package dev.sudoloser.sunset.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.ui.components.*
import dev.sudoloser.sunset.ui.theme.NetflixRed
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    apiClient: ApiClient,
    baseUrl: String,
    userId: String?,
    currentUsername: String?,
    isAdmin: Boolean,
    onLogout: () -> Unit,
    onGoToAdmin: () -> Unit
) {
    var tab by remember { mutableStateOf("account") }
    val tabs = buildList {
        add("account")
        add("appearance")
        add("subtitles")
        if (isAdmin) add("admin")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        TabRow(selectedTabIndex = tabs.indexOf(tab)) {
            tabs.forEach { t ->
                Tab(
                    selected = tab == t,
                    onClick = { tab = t },
                    text = { Text(t.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        when (tab) {
            "account" -> AccountSettings(apiClient, baseUrl, userId, currentUsername, isAdmin, onLogout)
            "appearance" -> AppearanceSettings()
            "subtitles" -> SubtitleSettings()
            "admin" -> onGoToAdmin()
        }
    }
}

@Composable
fun AccountSettings(
    apiClient: ApiClient,
    baseUrl: String,
    userId: String?,
    currentUsername: String?,
    isAdmin: Boolean,
    onLogout: () -> Unit
) {
    var username by remember { mutableStateOf(currentUsername ?: "") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordMsg by remember { mutableStateOf<String?>(null) }
    var usernameMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (userId != null) {
                    AsyncImage(
                        model = apiClient.getProfilePictureUrl(userId),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (currentUsername?.take(2)?.uppercase() ?: "U"),
                            fontSize = 28.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                SunsetButton(
                    text = "Upload Photo",
                    onClick = {
                        scope.launch {
                            try {
                                if (userId != null) {
                                    apiClient.uploadProfilePicture(userId, "")
                                }
                            } catch (_: Exception) {}
                        }
                    },
                    variant = ButtonVariant.Secondary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            SunsetInput(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (usernameMsg != null) {
                Text(usernameMsg!!, fontSize = 12.sp, color = NetflixRed)
                Spacer(Modifier.height(8.dp))
            }
            SunsetButton(
                text = "Save",
                onClick = {
                    scope.launch {
                        try {
                            if (userId != null) {
                                apiClient.changeUsername(userId, username)
                                usernameMsg = "Username updated!"
                            }
                        } catch (e: Exception) {
                            usernameMsg = e.message
                        }
                    }
                },
                variant = ButtonVariant.Secondary
            )
        }

        Spacer(Modifier.height(16.dp))

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            SunsetInput(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = "Current Password",
                password = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            SunsetInput(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New Password",
                password = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (passwordMsg != null) {
                Text(passwordMsg!!, fontSize = 12.sp, color = NetflixRed)
                Spacer(Modifier.height(8.dp))
            }
            SunsetButton(
                text = "Change Password",
                onClick = {
                    scope.launch {
                        try {
                            if (userId != null) {
                                apiClient.changePassword(userId, currentPassword, newPassword)
                                passwordMsg = "Password updated!"
                                currentPassword = ""
                                newPassword = ""
                            }
                        } catch (e: Exception) {
                            passwordMsg = e.message
                        }
                    }
                },
                variant = ButtonVariant.Secondary
            )
        }

        Spacer(Modifier.height(16.dp))

        SunsetButton(
            text = "Log Out",
            onClick = onLogout,
            variant = ButtonVariant.Danger,
            fullWidth = true
        )
    }
}

@Composable
fun AppearanceSettings() {
    var theme by remember { mutableStateOf("dark") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text("Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SunsetButton(
                    text = "Dark",
                    onClick = { theme = "dark" },
                    variant = if (theme == "dark") ButtonVariant.Primary else ButtonVariant.Secondary
                )
                SunsetButton(
                    text = "Light",
                    onClick = { theme = "light" },
                    variant = if (theme == "light") ButtonVariant.Primary else ButtonVariant.Secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettings() {
    var color by remember { mutableStateOf("#FFFFFF") }
    var bgOpacity by remember { mutableStateOf(0) }
    var size by remember { mutableIntStateOf(100) }
    var font by remember { mutableStateOf("Inter") }
    var bold by remember { mutableStateOf(false) }

    val colors = listOf("#FFFFFF", "#FFFF00", "#00FF00", "#00FFFF", "#FF0000", "#FF00FF", "#0000FF", "#000000")
    val fonts = listOf("Inter", "Arial", "Helvetica", "Times New Roman", "Courier New", "Georgia", "Verdana")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text("Color", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(c)))
                            .clickable { color = c }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text("Background", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SunsetButton(
                    text = "None",
                    onClick = { bgOpacity = 0 },
                    variant = if (bgOpacity == 0) ButtonVariant.Primary else ButtonVariant.Secondary
                )
                SunsetButton(
                    text = "Black",
                    onClick = { bgOpacity = 85 },
                    variant = if (bgOpacity == 85) ButtonVariant.Primary else ButtonVariant.Secondary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text("Size: $size%", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = size.toFloat(),
                onValueChange = { size = it.toInt() },
                valueRange = 50f..200f,
                steps = 29
            )
        }

        Spacer(Modifier.height(16.dp))

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text("Font", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = font,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    fonts.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f) },
                            onClick = { font = f; expanded = false }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = bold, onCheckedChange = { bold = it })
            Text("Bold", color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Preview text",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}
