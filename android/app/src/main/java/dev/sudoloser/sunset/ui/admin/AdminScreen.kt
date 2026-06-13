package dev.sudoloser.sunset.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sudoloser.sunset.api.ApiClient
import android.util.Log
import dev.sudoloser.sunset.data.models.*
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(
    apiClient: ApiClient,
    baseUrl: String,
    onBack: () -> Unit
) {
    var libraries by remember { mutableStateOf<List<Library>>(emptyList()) }
    var storage by remember { mutableStateOf<StorageInfo?>(null) }
    var uptime by remember { mutableLongStateOf(0L) }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    
    var showAddLib by remember { mutableStateOf(false) }
    var newLibName by remember { mutableStateOf("") }
    var newLibPath by remember { mutableStateOf("") }
    var newLibType by remember { mutableStateOf(LibraryType.MOVIES) }

    var showNewUser by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserPass by remember { mutableStateOf("") }
    var newUserAdmin by remember { mutableStateOf(false) }

    var inviteCode by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            libraries = apiClient.getLibraries()
            storage = apiClient.getStorage()
            users = apiClient.getUsers()
            while(true) {
                uptime = apiClient.getUptime()
                delay(1000)
            }
        } catch (e: Exception) { Log.e("SunSet", "Failed to load admin data", e) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Admin Panel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            SunsetButton(text = "Scan All", onClick = { scope.launch { try { apiClient.triggerScan() } catch (e: Exception) { Log.e("SunSet", "Scan failed", e) } } })
        }

        // Server Status
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Server Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("UPTIME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(formatUptime(uptime), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("VERSION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("v0.2.0", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }

                storage?.let { s ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("ITEMS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(s.itemCount.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("USERS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(s.userCount.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOTAL SIZE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatBytes(s.totalSize), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Libraries
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Libraries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { showAddLib = !showAddLib }) {
                        Icon(SunsetIcons.Plus, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (showAddLib) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SunsetInput(value = newLibName, onValueChange = { newLibName = it }, label = "Library Name")
                        SunsetInput(value = newLibPath, onValueChange = { newLibPath = it }, label = "Path")
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LibraryType.entries.forEach { type ->
                                SunsetButton(
                                    text = type.name,
                                    onClick = { newLibType = type },
                                    variant = if (newLibType == type) ButtonVariant.Primary else ButtonVariant.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        SunsetButton(text = "Add Library", onClick = {
                            scope.launch {
                                try {
                                    apiClient.addLibrary(LibraryInput(newLibName, newLibPath, newLibType))
                                    libraries = apiClient.getLibraries()
                                    showAddLib = false
                                } catch (e: Exception) { Log.e("SunSet", "Failed to add library", e) }
                            }
                        }, fullWidth = true)
                    }
                }

                libraries.forEach { lib ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lib.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(lib.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            scope.launch { try { apiClient.deleteLibrary(lib.id); libraries = apiClient.getLibraries() } catch (e: Exception) { Log.e("SunSet", "Failed to delete library", e) } }
                        }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Users
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("User Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { showNewUser = !showNewUser }) {
                        Icon(SunsetIcons.Plus, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (showNewUser) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SunsetInput(value = newUserName, onValueChange = { newUserName = it }, label = "Username")
                        SunsetInput(value = newUserPass, onValueChange = { newUserPass = it }, label = "Password", password = true)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = newUserAdmin, onCheckedChange = { newUserAdmin = it })
                            Text("Administrator", color = MaterialTheme.colorScheme.onSurface)
                        }
                        SunsetButton(text = "Create User", onClick = {
                            scope.launch {
                                try {
                                    apiClient.createUser(CreateUserRequest(newUserName, newUserPass, newUserAdmin))
                                    users = apiClient.getUsers()
                                    showNewUser = false
                                } catch (e: Exception) { Log.e("SunSet", "Failed to create user", e) }
                            }
                        }, fullWidth = true)
                    }
                }

                users.forEach { u ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(u.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            val isAdminUser = u.isAdmin
                            Text(if (isAdminUser) "ADMIN" else "USER", style = MaterialTheme.typography.labelSmall, color = if (isAdminUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = {
                            scope.launch { try { apiClient.deleteUser(u.userId); users = apiClient.getUsers() } catch (e: Exception) { Log.e("SunSet", "Failed to delete user", e) } }
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // Invites
        SunsetCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Invite Codes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Generate single-use registration codes.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                
                SunsetButton(text = "Generate Code", onClick = {
                    scope.launch { try { inviteCode = apiClient.createInvite() } catch (e: Exception) { Log.e("SunSet", "Failed to create invite", e) } }
                }, variant = ButtonVariant.Secondary, fullWidth = true)

                if (inviteCode.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(inviteCode, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

private fun formatUptime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatBytes(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var b = bytes.toDouble()
    var i = 0
    while (b >= 1024 && i < units.size - 1) {
        b /= 1024
        i++
    }
    return "%.1f %s".format(b, units[i])
}
