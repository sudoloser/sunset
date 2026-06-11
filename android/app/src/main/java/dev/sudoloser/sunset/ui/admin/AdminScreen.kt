package dev.sudoloser.sunset.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    apiClient: ApiClient,
    baseUrl: String,
    onBack: () -> Unit
) {
    var libraries by remember { mutableStateOf<List<Library>>(emptyList()) }
    var storage by remember { mutableStateOf<StorageInfo?>(null) }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var uptime by remember { mutableLongStateOf(0L) }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var inviteCopied by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showNewUser by remember { mutableStateOf(false) }
    var newLibName by remember { mutableStateOf("") }
    var newLibPath by remember { mutableStateOf("") }
    var newLibType by remember { mutableStateOf(LibraryType.MOVIES) }
    var newUserName by remember { mutableStateOf("") }
    var newUserPass by remember { mutableStateOf("") }
    var newUserAdmin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            try { uptime = apiClient.getUptime(); delay(1000) } catch (_: Exception) { delay(5000) }
        }
    }

    LaunchedEffect(Unit) {
        try {
            libraries = apiClient.getLibraries()
            storage = apiClient.getStorage()
            users = apiClient.getUsers()
        } catch (_: Exception) {}
    }

    val formatUptime: (Long) -> String = { seconds ->
        val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
        "${h}h ${m}m ${s}s"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SunsetIconButton(
                icon = SunsetIcons.Back,
                onClick = onBack,
                backgroundColor = Color.White.copy(alpha = 0.1f)
            )
            Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }

        SunsetButton(
            text = "Scan All Libraries",
            onClick = { scope.launch { try { apiClient.triggerScan() } catch (_: Exception) {} } },
            fullWidth = true
        )

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Libraries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                libraries.forEach { lib ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lib.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(lib.path, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        TextButton(onClick = {
                            scope.launch { try { apiClient.deleteLibrary(lib.id); libraries = libraries - lib } catch (_: Exception) {} }
                        }) { Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                    }
                }

                if (showAdd) {
                    SunsetInput(value = newLibName, onValueChange = { newLibName = it }, label = "Name", placeholder = "Movies")
                    SunsetInput(value = newLibPath, onValueChange = { newLibPath = it }, label = "Path", placeholder = "/mnt/media/movies")
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        SunsetButton(
                            text = if (newLibType == LibraryType.MOVIES) "MOVIES" else "SHOWS",
                            onClick = { expanded = true },
                            variant = ButtonVariant.Secondary,
                            fullWidth = true
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Movies") }, onClick = { newLibType = LibraryType.MOVIES; expanded = false })
                            DropdownMenuItem(text = { Text("Shows") }, onClick = { newLibType = LibraryType.SHOWS; expanded = false })
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SunsetButton(text = "Cancel", onClick = { showAdd = false }, variant = ButtonVariant.Ghost, modifier = Modifier.weight(1f))
                        SunsetButton(text = "Add", onClick = {
                            scope.launch {
                                try {
                                    apiClient.addLibrary(LibraryInput(newLibName, newLibPath, newLibType))
                                    libraries = apiClient.getLibraries()
                                    newLibName = ""; newLibPath = ""
                                    showAdd = false
                                } catch (_: Exception) {}
                            }
                        }, modifier = Modifier.weight(1f))
                    }
                } else {
                    SunsetButton(text = "+ Add Library", onClick = { showAdd = true }, variant = ButtonVariant.Secondary, fullWidth = true)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SunsetCard(modifier = Modifier.weight(1f)) {
                Column {
                    Text("Uptime", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Text(formatUptime(uptime), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            storage?.let { s ->
                SunsetCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text("Users", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Text(s.userCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Invite Codes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (inviteCode != null) {
                    Text(inviteCode!!, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SunsetButton(text = "Generate", onClick = {
                        scope.launch { try { inviteCode = apiClient.createInvite(); inviteCopied = false } catch (_: Exception) {} }
                    }, modifier = Modifier.weight(1f))
                    if (inviteCode != null) {
                        SunsetButton(text = if (inviteCopied) "Copied!" else "Copy", onClick = { inviteCopied = true }, variant = ButtonVariant.Secondary, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Users", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                users.forEach { u ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(u.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(if (u.isAdmin) "ADMIN" else "USER", style = MaterialTheme.typography.labelSmall, color = if (u.isAdmin) NetflixRed else Color.Gray)
                    }
                }

                if (showNewUser) {
                    SunsetInput(value = newUserName, onValueChange = { newUserName = it }, label = "Username")
                    SunsetInput(value = newUserPass, onValueChange = { newUserPass = it }, label = "Password", password = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newUserAdmin, onCheckedChange = { newUserAdmin = it })
                        Text("Admin Privileges", color = Color.White)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SunsetButton(text = "Cancel", onClick = { showNewUser = false }, variant = ButtonVariant.Ghost, modifier = Modifier.weight(1f))
                        SunsetButton(text = "Create", onClick = {
                            scope.launch {
                                try {
                                    apiClient.createUser(CreateUserRequest(newUserName, newUserPass, newUserAdmin))
                                    users = apiClient.getUsers()
                                    newUserName = ""; newUserPass = ""; newUserAdmin = false
                                    showNewUser = false
                                } catch (_: Exception) {}
                            }
                        }, modifier = Modifier.weight(1f))
                    }
                } else {
                    SunsetButton(text = "+ Create User", onClick = { showNewUser = true }, variant = ButtonVariant.Secondary, fullWidth = true)
                }
            }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var b = bytes.toDouble()
    for (unit in units) {
        b /= 1024
        if (b < 1024) return "${"%.1f".format(b)} $unit"
    }
    return "${"%.1f".format(b)} PB"
}
