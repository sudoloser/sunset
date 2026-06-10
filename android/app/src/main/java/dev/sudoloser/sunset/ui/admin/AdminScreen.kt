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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Back") }
            Spacer(Modifier.width(8.dp))
            Text("Admin", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        Button(onClick = { scope.launch { try { apiClient.triggerScan() } catch (_: Exception) {} } }) {
            Text("Scan All Libraries")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Libraries", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

                libraries.forEach { lib ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lib.name, fontSize = 14.sp)
                            Text(lib.path, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            scope.launch { try { apiClient.deleteLibrary(lib.id); libraries = libraries - lib } catch (_: Exception) {} }
                        }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                    }
                }

                if (showAdd) {
                    OutlinedTextField(value = newLibName, onValueChange = { newLibName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newLibPath, onValueChange = { newLibPath = it }, label = { Text("Path") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAdd = false }) { Text("Cancel") }
                        Button(onClick = {
                            scope.launch {
                                try {
                                    apiClient.addLibrary(LibraryInput(newLibName, newLibPath, newLibType))
                                    libraries = apiClient.getLibraries()
                                    newLibName = ""; newLibPath = ""
                                    showAdd = false
                                } catch (_: Exception) {}
                            }
                        }) { Text("Add") }
                    }
                } else {
                    OutlinedButton(onClick = { showAdd = true }) { Text("+ Add Library") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Server Status", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Uptime: ${formatUptime(uptime)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        storage?.let { s ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Storage", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Items: ${s.itemCount}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Libraries: ${s.libraryCount}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Users: ${s.userCount}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Total Size: ${formatBytes(s.totalSize)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Invite Codes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (inviteCode != null) {
                    Text(inviteCode!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch { try { inviteCode = apiClient.createInvite(); inviteCopied = false } catch (_: Exception) {} }
                    }) { Text("Generate Code") }
                    if (inviteCode != null) {
                        Button(onClick = { inviteCopied = true }) {
                            Text(if (inviteCopied) "Copied!" else "Copy")
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Users", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

                users.forEach { u ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(u.username, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(if (u.isAdmin) "Admin" else "User", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (showNewUser) {
                    OutlinedTextField(value = newUserName, onValueChange = { newUserName = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newUserPass, onValueChange = { newUserPass = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newUserAdmin, onCheckedChange = { newUserAdmin = it })
                        Text("Admin", fontSize = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showNewUser = false }) { Text("Cancel") }
                        Button(onClick = {
                            scope.launch {
                                try {
                                    apiClient.createUser(CreateUserRequest(newUserName, newUserPass, newUserAdmin))
                                    users = apiClient.getUsers()
                                    newUserName = ""; newUserPass = ""; newUserAdmin = false
                                    showNewUser = false
                                } catch (_: Exception) {}
                            }
                        }) { Text("Create User") }
                    }
                } else {
                    OutlinedButton(onClick = { showNewUser = true }) { Text("+ New User") }
                }
            }
        }

        OutlinedButton(onClick = {
            scope.launch { try { apiClient.redeemInvite("") } catch (_: Exception) {} }
        }) { Text("Invite Code (Redeem)") }
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
