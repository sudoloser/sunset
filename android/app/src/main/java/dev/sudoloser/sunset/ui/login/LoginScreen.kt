package dev.sudoloser.sunset.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

@Composable
fun LoginScreen(
    apiClient: ApiClient,
    onLogin: (User) -> Unit
) {
    var mode by remember { mutableStateOf("login") } // "login" or "signup"
    var step by remember { mutableIntStateOf(0) } // 0: invite code, 1: account details
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "SunSet",
                style = MaterialTheme.typography.displayLarge,
                color = NetflixRed,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-2).sp
            )

            Spacer(Modifier.height(48.dp))

            if (mode == "login") {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(Modifier.height(24.dp))

                SunsetInput(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    placeholder = "Enter your username"
                )

                Spacer(Modifier.height(16.dp))

                SunsetInput(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    password = true
                )

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                Spacer(Modifier.height(32.dp))

                SunsetButton(
                    text = if (loading) "Signing in..." else "Sign In",
                    onClick = {
                        loading = true; error = null
                        scope.launch {
                            try {
                                val user = apiClient.login(LoginRequest(username, password))
                                if (user != null) onLogin(user)
                                else error = "Invalid credentials"
                            } catch (e: Exception) { error = e.message ?: "Login failed" }
                            loading = false
                        }
                    },
                    fullWidth = true,
                    enabled = !loading && username.isNotBlank() && password.isNotBlank()
                )

                Spacer(Modifier.height(24.dp))

                TextButton(onClick = { mode = "signup"; step = 0; error = null }) {
                    Text("Don't have an account? Sign Up", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            } else {
                // SIGN UP FLOW
                Text(
                    text = if (step == 0) "Enter Invite Code" else "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(Modifier.height(24.dp))

                if (step == 0) {
                    SunsetInput(
                        value = inviteCode,
                        onValueChange = { inviteCode = it },
                        label = "Invite Code",
                        placeholder = "PASTE_CODE_HERE"
                    )
                    
                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(32.dp))

                    SunsetButton(
                        text = if (loading) "Validating..." else "Continue",
                        onClick = {
                            loading = true; error = null
                            scope.launch {
                                try {
                                    val ok = apiClient.redeemInvite(inviteCode)
                                    if (ok) step = 1
                                    else error = "Invalid or expired invite code"
                                } catch (e: Exception) { error = "Validation failed" }
                                loading = false
                            }
                        },
                        fullWidth = true,
                        enabled = !loading && inviteCode.isNotBlank()
                    )
                } else {
                    SunsetInput(
                        value = username,
                        onValueChange = { username = it },
                        label = "Choose Username",
                        placeholder = "Username"
                    )
                    Spacer(Modifier.height(16.dp))
                    SunsetInput(
                        value = password,
                        onValueChange = { password = it },
                        label = "Choose Password",
                        password = true
                    )
                    
                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(32.dp))

                    SunsetButton(
                        text = if (loading) "Creating Account..." else "Sign Up",
                        onClick = {
                            loading = true; error = null
                            scope.launch {
                                try {
                                    // Create standard user
                                    val ok = apiClient.createUser(CreateUserRequest(username, password, isAdmin = false))
                                    if (ok) {
                                        val user = apiClient.login(LoginRequest(username, password))
                                        if (user != null) onLogin(user)
                                    } else error = "Could not create account"
                                } catch (e: Exception) { error = e.message }
                                loading = false
                            }
                        },
                        fullWidth = true,
                        enabled = !loading && username.isNotBlank() && password.length >= 4
                    )
                }

                Spacer(Modifier.height(24.dp))

                TextButton(onClick = { mode = "login"; error = null }) {
                    Text("Already have an account? Sign In", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
