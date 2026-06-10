package dev.sudoloser.sunset.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.sudoloser.sunset.ui.components.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.*
import dev.sudoloser.sunset.ui.components.*
import dev.sudoloser.sunset.ui.theme.NetflixRed
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
            try {
                uptime = apiClient.getUptime()
                delay(1000)
            } catch (_: Exception) { delay(5000) }
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
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        "${h}h ${m}m ${s}s"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SunsetButton(text = "< Back", onClick = onBack, variant = ButtonVariant.Ghost)

        Spacer(Modifier.height(12.dp))

        Text("Admin", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)

        Spacer(Modifier.height(8.dp))

        SunsetButton(text = "Scan All Libraries", onClick = {
            scope.launch { try { apiClient.triggerScan() } catch (_: Exception) {} }
        })

        Spacer(Modifier.height(16.dp))

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text("Libraries", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            libraries.forEach { lib ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(lib.name, fontSize = 14.sp)
                        Text(lib.path, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SunsetButton(text = "Remove", onClick = {
                        scope.launch { try { apiClient.deleteLibrary(lib.id); libraries = libraries - lib } catch (_: Exception) {} }
                    }, variant = ButtonVariant.Danger)
                }
            }

            if (showAdd) {
                Spacer(Modifier.height(8.dp))
                SunsetInput(value = newLibName, onValueChange = { newLibName = it }, label = "Name", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                SunsetInput(value = newLibPath, onValueChange = { newLibPath = it }, label = "Path", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = if (newLibType == LibraryType.MOVIES) "Movies" else "Shows",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Movies") }, onClick = { newLibType = LibraryType.MOVIES; expanded = false })
                        DropdownMenuItem(text = { Text("Shows") }, onClick = { newLibType = LibraryType.SHOWS; expanded = false })
                    }
                }
                Spacer(Modifier.height(4.dp))
                SunsetButton(text = "Add", onClick = {
                    scope.launch {
                        try {
                            apiClient.addLibrary(LibraryInput(newLibName, newLibPath, newLibType))
                            libraries = apiClient.getLibraries()
                            newLibName = ""; newLibPath = ""
                            showAdd = false
                        } catch (_: Exception) {}
                    }
                })
            } else {
                SunsetButton(text = "+ Add Library", onClick = { showAdd = true }, variant = ButtonVariant.Secondary)
            }
        }

        Spacer(Modifier.height(16.dp))

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text("Server Status", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Uptime: ${formatUptime(uptime)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))

        storage?.let { s ->
            SunsetCard(modifier = Modifier.fillMaxWidth()) {
                Text("Storage", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Items: ${s.itemCount}", fontSize = 14.sp)
                Text("Libraries: ${s.libraryCount}", fontSize = 14.sp)
                Text("Users: ${s.userCount}", fontSize = 14.sp)
                Text("Total Size: ${formatBytes(s.totalSize)}", fontSize = 14.sp)
            }
            Spacer(Modifier.height(16.dp))
        }

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text("Invite Codes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (inviteCode != null) {
                Text(inviteCode!!, fontSize = 14.sp, color = NetflixRed)
                SunsetButton(
                    text = if (inviteCopied) "Copied!" else "Copy",
                    onClick = { inviteCopied = true },
                    variant = ButtonVariant.Secondary
                )
            }
            SunsetButton(text = "Generate Code", onClick = {
                scope.launch { try { inviteCode = apiClient.createInvite(); inviteCopied = false } catch (_: Exception) {} }
            })
        }

        Spacer(Modifier.height(16.dp))

        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Text("Users", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            users.forEach { u ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(u.username, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(if (u.isAdmin) "Admin" else "User", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (showNewUser) {
                Spacer(Modifier.height(8.dp))
                SunsetInput(value = newUserName, onValueChange = { newUserName = it }, label = "Username", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                SunsetInput(value = newUserPass, onValueChange = { newUserPass = it }, label = "Password", password = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = newUserAdmin, onCheckedChange = { newUserAdmin = it })
                    Text("Admin", fontSize = 14.sp)
                }
                SunsetButton(text = "Create User", onClick = {
                    scope.launch {
                        try {
                            apiClient.createUser(CreateUserRequest(newUserName, newUserPass, newUserAdmin))
                            users = apiClient.getUsers()
                            newUserName = ""; newUserPass = ""; newUserAdmin = false
                            showNewUser = false
                        } catch (_: Exception) {}
                    }
                })
            } else {
                SunsetButton(text = "+ New User", onClick = { showNewUser = true }, variant = ButtonVariant.Secondary)
            }
        }

        Spacer(Modifier.height(16.dp))

        SunsetButton(text = "Invite Code (Redeem)", onClick = {
            scope.launch { try {
                apiClient.redeemInvite("")
            } catch (_: Exception) {} }
        }, variant = ButtonVariant.Secondary)
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
