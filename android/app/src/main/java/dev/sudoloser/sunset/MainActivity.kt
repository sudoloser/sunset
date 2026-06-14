package dev.sudoloser.sunset

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.data.models.SetupStatus
import dev.sudoloser.sunset.data.models.User
import dev.sudoloser.sunset.player.PlayerActivity
import dev.sudoloser.sunset.ui.admin.AdminScreen
import dev.sudoloser.sunset.ui.dashboard.DashboardScreen
import dev.sudoloser.sunset.ui.library.LibrariesScreen
import dev.sudoloser.sunset.ui.login.LoginScreen
import dev.sudoloser.sunset.ui.mediadetails.MediaDetailsScreen
import dev.sudoloser.sunset.ui.onboarding.OnboardingScreen
import dev.sudoloser.sunset.ui.settings.SettingsScreen
import dev.sudoloser.sunset.ui.theme.NetflixRed
import dev.sudoloser.sunset.ui.theme.SunsetTheme
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import android.content.Context

import androidx.activity.compose.BackHandler
import dev.sudoloser.sunset.data.PrefKeys
import dev.sudoloser.sunset.data.dataStore

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import dev.sudoloser.sunset.tv.EmergencyMenu
import dev.sudoloser.sunset.tv.copyLogcat
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var lastShakeTime = 0L
    private var onShakeCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        setContent {
            var showEmergencyMenu by remember { mutableStateOf(false) }
            onShakeCallback = { showEmergencyMenu = true }

            AppContent(this)

            if (showEmergencyMenu) {
                EmergencyMenu(
                    showExitTv = false,
                    onCopyLog = { copyLogcat(this) },
                    onDismiss = { showEmergencyMenu = false }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(
            this,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        if (acceleration > 12f) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > 1000) {
                lastShakeTime = now
                onShakeCallback?.invoke()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun AppContent(activity: ComponentActivity) {
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf("loading") }
    var apiClient by remember { mutableStateOf<ApiClient?>(null) }
    var user by remember { mutableStateOf<User?>(null) }
    var status by remember { mutableStateOf<SetupStatus?>(null) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var showAdmin by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("home") }
    var themeMode by remember { mutableStateOf("system") }
    var useMaterial3 by remember { mutableStateOf(true) }
    var tvMode by remember { mutableStateOf(false) }
    var showServerSwitcher by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val resolvedDarkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    // Back button handling
    BackHandler(enabled = step == "main") {
        when {
            selectedItem != null -> selectedItem = null
            showAdmin -> showAdmin = false
            activeTab != "home" -> activeTab = "home"
            else -> activity.finish()
        }
    }

    // Load saved state
    LaunchedEffect(Unit) {
        themeMode = activity.dataStore.data.first()[PrefKeys.THEME_MODE] ?: "system"
        useMaterial3 = activity.dataStore.data.first()[PrefKeys.USE_MATERIAL3] ?: true
        tvMode = activity.dataStore.data.first()[PrefKeys.TV_MODE] ?: false
        val url = activity.dataStore.data.first()[PrefKeys.SERVER_URL]
        if (url != null) {
            serverUrl = url
            apiClient = ApiClient(url)
            try {
                val s = apiClient!!.getStatus()
                status = s
                if (s.setupComplete) {
                    val uid = activity.dataStore.data.first()[PrefKeys.USER_ID]
                    if (uid != null) {
                        try {
                            val u = apiClient!!.getUserProfile(uid)
                            if (u != null) {
                                user = u
                                step = "main"
                                return@LaunchedEffect
                            }
                        } catch (e: Exception) { Log.e("SunSet", "Failed to load user profile", e) }
                    }
                    step = "login"
                } else {
                    step = "onboarding"
                }
            } catch (_: Exception) {
                step = "server_selection"
            }
        } else {
            step = "server_selection"
        }
    }

        when (step) {
        "loading" -> {
            SunsetTheme(darkTheme = resolvedDarkTheme, useMaterial3 = useMaterial3) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NetflixRed)
                }
            }
        }

        "server_selection" -> {
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var connecting by remember { mutableStateOf(false) }

            SunsetTheme(darkTheme = resolvedDarkTheme, useMaterial3 = useMaterial3) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) + slideInVertically(
                            animationSpec = tween(400, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 3 }
                        ) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "step"
                ) { currentStep ->
                    when (currentStep) {
                        "server_selection" -> ServerSelectionScreen(
                            errorMessage = errorMessage,
                            loading = connecting,
                            onServerSelected = { url ->
                                scope.launch {
                                    connecting = true
                                    errorMessage = null
                                    apiClient = ApiClient(url)
                                    try {
                                        val s = apiClient!!.getStatus()
                                        activity.dataStore.edit { it[PrefKeys.SERVER_URL] = url }
                                        serverUrl = url
                                        status = s
                                        step = if (s.setupComplete) "login" else "onboarding"
                                    } catch (e: Exception) {
                                        val type = e::class.simpleName ?: "Exception"
                                        val detail = when {
                                            e.message?.contains("timeout", ignoreCase = true) == true -> "Connection timed out"
                                            e.message?.contains("refused", ignoreCase = true) == true -> "Connection refused (server not running on this port)"
                                            e.message?.contains("resolve", ignoreCase = true) == true -> "Could not resolve hostname"
                                            e.message?.contains("not permitted", ignoreCase = true) == true -> "Cleartext HTTP blocked by system"
                                            else -> "${e.message?.take(80) ?: "no message"} ($type)"
                                        }
                                        errorMessage = "Can't connect: $detail"
                                    } finally {
                                        connecting = false
                                    }
                                }
                            }
                        )
                        else -> {}
                    }
                }
            }
        }

        "onboarding" -> {
            apiClient?.let { client ->
                SunsetTheme(darkTheme = resolvedDarkTheme, useMaterial3 = useMaterial3) {
                    OnboardingScreen(
                        apiClient = client,
                        onComplete = { u ->
                            user = u
                            scope.launch {
                                activity.dataStore.edit {
                                    it[PrefKeys.USER_ID] = u.userId
                                    it[PrefKeys.USERNAME] = u.username
                                    it[PrefKeys.IS_ADMIN] = u.isAdmin.toString()
                                }
                            }
                            step = "main"
                        }
                    )
                }
            }
        }

        "login" -> {
            apiClient?.let { client ->
                SunsetTheme(darkTheme = resolvedDarkTheme, useMaterial3 = useMaterial3) {
                    LoginScreen(
                        apiClient = client,
                        onLogin = { u ->
                            user = u
                            scope.launch {
                                activity.dataStore.edit {
                                    it[PrefKeys.USER_ID] = u.userId
                                    it[PrefKeys.USERNAME] = u.username
                                    it[PrefKeys.IS_ADMIN] = u.isAdmin.toString()
                                }
                            }
                            step = "main"
                        }
                    )
                }
            }
        }

        "main" -> {
            apiClient?.let { client ->
                val baseUrl = serverUrl ?: ""
                val userId = user?.userId

                SunsetTheme(darkTheme = resolvedDarkTheme, useMaterial3 = useMaterial3) {
                    if (showServerSwitcher) {
                        var swError by remember { mutableStateOf<String?>(null) }
                        var swLoading by remember { mutableStateOf(false) }
                        ServerSelectionScreen(
                            errorMessage = swError,
                            loading = swLoading,
                            onCancel = { showServerSwitcher = false },
                            onServerSelected = { url ->
                                scope.launch {
                                    swLoading = true
                                    swError = null
                                    val newClient = ApiClient(url)
                                    try {
                                        newClient.getStatus()
                                        activity.dataStore.edit { it[PrefKeys.SERVER_URL] = url }
                                        serverUrl = url
                                        apiClient = newClient
                                        showServerSwitcher = false
                                    } catch (e: Exception) {
                                        swError = "Can't connect: ${e.message?.take(80) ?: "no message"}"
                                    } finally {
                                        swLoading = false
                                    }
                                }
                            }
                        )
                    } else {
                    AnimatedContent(
                        targetState = when {
                            selectedItem != null -> "details"
                            showAdmin -> "admin"
                            else -> "tabs"
                        },
                        transitionSpec = {
                            if (targetState == "tabs") {
                                slideInVertically(
                                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it / 4 }
                                ) + fadeIn(animationSpec = tween(300)) togetherWith
                                slideOutVertically(
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                    targetOffsetY = { it }
                                ) + fadeOut(animationSpec = tween(200))
                            } else {
                                slideInVertically(
                                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it }
                                ) + fadeIn(animationSpec = tween(250)) togetherWith
                                slideOutVertically(
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                    targetOffsetY = { it / 3 }
                                ) + fadeOut(animationSpec = tween(150))
                            }
                        },
                        label = "main_content"
                    ) { target ->
                        when (target) {
                            "details" -> {
                                val item = selectedItem
                                if (item != null) {
                                    MediaDetailsScreen(
                                        item = item,
                                        baseUrl = baseUrl,
                                        apiClient = client,
                                        userId = userId,
                                        onPlay = { itemToPlay -> startPlayer(activity, client, itemToPlay, baseUrl, userId) },
                                        onClose = { selectedItem = null }
                                    )
                                }
                            }
                            "admin" -> AdminScreen(
                                apiClient = client,
                                baseUrl = baseUrl,
                                onBack = { showAdmin = false }
                            )
                            else -> {
                                if (tvMode) {
                                    dev.sudoloser.sunset.tv.TVNavHost(
                                        baseUrl = baseUrl,
                                        userId = userId,
                                        onExitTvMode = {
                                            tvMode = false
                                            scope.launch {
                                                activity.dataStore.edit { it[PrefKeys.TV_MODE] = false }
                                            }
                                        }
                                    )
                                } else {
                                    val iconHome: @Composable () -> Unit = { Icon(SunsetIcons.Home, contentDescription = "Home") }
                                    val iconLibrary: @Composable () -> Unit = { Icon(SunsetIcons.Library, contentDescription = "Library") }
                                    val iconSettings: @Composable () -> Unit = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                                    
                                    val tabs = listOf(
                                        "home" to iconHome,
                                        "library" to iconLibrary,
                                        "settings" to iconSettings
                                    )

                                    NavigationSuite(
                                        tabs = tabs,
                                        activeTab = activeTab,
                                        onTabSelected = { activeTab = it }
                                    ) { padding ->
                                        Box(modifier = Modifier.padding(padding)) {
                                            AnimatedContent(
                                                targetState = activeTab,
                                                transitionSpec = {
                                                    fadeIn(animationSpec = tween(250)) + slideInVertically(
                                                        animationSpec = tween(250),
                                                        initialOffsetY = { it / 20 }
                                                    ) togetherWith fadeOut(animationSpec = tween(150))
                                                },
                                                label = "tab"
                                            ) { tab ->
                                                when (tab) {
                                                    "home" -> DashboardScreen(
                                                        apiClient = client,
                                                        baseUrl = baseUrl,
                                                        userId = userId,
                                                        onPlayItem = { item ->
                                                            startPlayer(activity, client, item, baseUrl, userId)
                                                        },
                                                        onSelectItem = { item -> selectedItem = item }
                                                    )
                                                    "library" -> LibrariesScreen(
                                                        apiClient = client,
                                                        baseUrl = baseUrl,
                                                        userId = userId,
                                                        isAdmin = user?.isAdmin == true,
                                                        onPlayItem = { item ->
                                                            startPlayer(activity, client, item, baseUrl, userId)
                                                        },
                                                        onSelectItem = { selectedItem = it },
                                                        onGoToSettings = { activeTab = "settings" }
                                                    )
                                                    "settings" -> SettingsScreen(
                                                        apiClient = client,
                                                        baseUrl = baseUrl,
                                                        userId = userId,
                                                        currentUsername = user?.username,
                                                        isAdmin = user?.isAdmin == true,
                                                        themeMode = themeMode,
                                                        onThemeModeChange = { newVal ->
                                                            themeMode = newVal
                                                            scope.launch {
                                                                activity.dataStore.edit { it[PrefKeys.THEME_MODE] = newVal }
                                                            }
                                                        },
                                                        useMaterial3 = useMaterial3,
                                                        onMaterial3Change = { newVal ->
                                                            useMaterial3 = newVal
                                                            scope.launch {
                                                                activity.dataStore.edit { it[PrefKeys.USE_MATERIAL3] = newVal }
                                                            }
                                                        },
                                                        tvMode = tvMode,
                                                        onTvModeChange = { newVal ->
                                                            tvMode = newVal
                                                            scope.launch {
                                                                activity.dataStore.edit { it[PrefKeys.TV_MODE] = newVal }
                                                            }
                                                        },
                                                        onLogout = {
                                                            scope.launch {
                                                                activity.dataStore.edit {
                                                                    it.remove(PrefKeys.USER_ID)
                                                                    it.remove(PrefKeys.USERNAME)
                                                                    it.remove(PrefKeys.IS_ADMIN)
                                                                }
                                                                user = null
                                                                step = "login"
                                                            }
                                                        },
                                                        onGoToAdmin = { /* Handled internally in SettingsScreen */ },
                                                        onChangeServer = { showServerSwitcher = true }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}
}

private fun startPlayer(activity: ComponentActivity, client: ApiClient, item: MediaItem, baseUrl: String, userId: String?) {
    val intent = Intent(activity, PlayerActivity::class.java).apply {
        putExtra("video_url", client.getStreamUrl(item.id))
        putExtra("video_title", item.title)
        putExtra("item_id", item.id)
        putExtra("base_url", baseUrl)
        putExtra("user_id", userId)
        putExtra("show_title", item.showTitle)
        putExtra("media_type", item.mediaType.name)
    }
    activity.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectionScreen(
    errorMessage: String? = null,
    loading: Boolean = false,
    onCancel: (() -> Unit)? = null,
    onServerSelected: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (onCancel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    Spacer(Modifier.width(4.dp))
                    Text("Back")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Text("SunSet", style = MaterialTheme.typography.displayLarge, color = NetflixRed)
        Spacer(Modifier.height(32.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.trim() },
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.100:7867") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                enabled = !loading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NetflixRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    cursorColor = NetflixRed,
                    focusedLabelColor = NetflixRed,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri
                )
            )
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = NetflixRed,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (input.isNotBlank()) onServerSelected(input.trimEnd('/')) },
            modifier = Modifier.fillMaxWidth(),
            enabled = input.isNotBlank() && !loading
        ) {
            Text(if (loading) "Connecting..." else "Connect")
        }
    }
}
