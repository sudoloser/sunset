package dev.sudoloser.sunset.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.LoginRequest
import dev.sudoloser.sunset.data.models.User
import dev.sudoloser.sunset.ui.components.*
import dev.sudoloser.sunset.ui.theme.NetflixRed
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    serverName: String,
    apiClient: ApiClient,
    onLogin: (User) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SunSet",
            style = MaterialTheme.typography.displayLarge,
            color = NetflixRed,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = serverName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(48.dp))

        SunsetInput(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            placeholder = "Enter your username",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        SunsetInput(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            password = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Text("Remember me", color = MaterialTheme.colorScheme.onSurface)
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))

        SunsetButton(
            text = if (loading) "Signing in..." else "Sign In",
            onClick = {
                loading = true
                error = null
                scope.launch {
                    try {
                        val user = apiClient.login(LoginRequest(username, password))
                        if (user != null) {
                            onLogin(user)
                        } else {
                            error = "Invalid credentials"
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Login failed"
                    }
                    loading = false
                }
            },
            fullWidth = true,
            enabled = !loading && username.isNotBlank() && password.isNotBlank()
        )
    }
}
