package dev.sudoloser.sunset.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.*
import dev.sudoloser.sunset.ui.components.*
import dev.sudoloser.sunset.ui.theme.NetflixRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    apiClient: ApiClient,
    onComplete: (User) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var serverName by remember { mutableStateOf("") }
    var adminUsername by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var libraries by remember { mutableStateOf(listOf<LibraryInput>()) }
    var newLibName by remember { mutableStateOf("") }
    var newLibPath by remember { mutableStateOf("") }
    var newLibType by remember { mutableStateOf(LibraryType.MOVIES) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        Text(
            text = "SunSet",
            style = MaterialTheme.typography.displayLarge,
            color = NetflixRed,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Server Setup",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(32.dp))

        when (step) {
            0 -> {
                SunsetInput(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = "Server Name",
                    placeholder = "My Server",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                SunsetButton(
                    text = "Next",
                    onClick = { step = 1 },
                    fullWidth = true,
                    enabled = serverName.isNotBlank()
                )
            }
            1 -> {
                SunsetInput(
                    value = adminUsername,
                    onValueChange = { adminUsername = it },
                    label = "Admin Username",
                    placeholder = "admin",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                SunsetInput(
                    value = adminPassword,
                    onValueChange = { adminPassword = it },
                    label = "Admin Password",
                    password = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it }
                    )
                    Text("Remember me", color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SunsetButton(
                        text = "Back",
                        onClick = { step = 0 },
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    SunsetButton(
                        text = "Next",
                        onClick = { step = 2 },
                        modifier = Modifier.weight(1f),
                        enabled = adminUsername.isNotBlank() && adminPassword.isNotBlank()
                    )
                }
            }
            2 -> {
                Text(
                    text = "Add Media Libraries",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                SunsetInput(
                    value = newLibName,
                    onValueChange = { newLibName = it },
                    label = "Library Name",
                    placeholder = "Movies",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                SunsetInput(
                    value = newLibPath,
                    onValueChange = { newLibPath = it },
                    label = "Library Path",
                    placeholder = "/path/to/media",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = if (newLibType == LibraryType.MOVIES) "Movies" else "TV Shows",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Movies") },
                            onClick = { newLibType = LibraryType.MOVIES; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("TV Shows") },
                            onClick = { newLibType = LibraryType.SHOWS; expanded = false }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                SunsetButton(
                    text = "Add Library",
                    onClick = {
                        if (newLibName.isNotBlank() && newLibPath.isNotBlank()) {
                            libraries = libraries + LibraryInput(newLibName, newLibPath, newLibType)
                            newLibName = ""
                            newLibPath = ""
                        }
                    },
                    variant = ButtonVariant.Secondary,
                    fullWidth = true
                )

                if (libraries.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Added Libraries:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    libraries.forEach { lib ->
                        Text(
                            text = "${lib.name} (${lib.path}) - ${if (lib.libType == LibraryType.MOVIES) "Movies" else "Shows"}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SunsetButton(
                        text = "Back",
                        onClick = { step = 1 },
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    SunsetButton(
                        text = "Finish Setup",
                        onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                try {
                                    val onboardRequest = OnboardRequest(
                                        serverName = serverName,
                                        adminUser = UserConfig(adminUsername, adminPassword),
                                        libraries = libraries
                                    )
                                    apiClient.onboard(onboardRequest)
                                    val user = apiClient.login(LoginRequest(adminUsername, adminPassword))
                                    if (user != null) {
                                        onComplete(user)
                                    } else {
                                        error = "Login failed after setup"
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Setup failed"
                                }
                                loading = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading
                    )
                }
            }
        }
    }
}
