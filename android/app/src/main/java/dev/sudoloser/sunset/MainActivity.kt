package dev.sudoloser.sunset

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import dev.sudoloser.sunset.ui.search.SearchScreen
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppContent(this)
        }
    }
}

@Composable
fun AppContent(activity: ComponentActivity) {
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf("loading") }
    var apiClient by remember { mutableStateOf<ApiClient?>(null) }
    var user by remember { mutableStateOf<User?>(null) }
    var status by remember { mutableStateOf<SetupStatus?>(null) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("home") }
    var darkTheme by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Back button handling
    BackHandler(enabled = step == "main") {
        when {
            showSearch -> showSearch = false
            selectedItem != null -> selectedItem = null
            showAdmin -> showAdmin = false
            activeTab != "home" -> activeTab = "home"
            else -> activity.finish()
        }
    }

    // Load saved state
    LaunchedEffect(Unit) {
        darkTheme = activity.dataStore.data.first()[PrefKeys.DARK_MODE] ?: true
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
                        } catch (_: Exception) {}
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
            SunsetTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NetflixRed)
                }
            }
        }

        "server_selection" -> {
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var connecting by remember { mutableStateOf(false) }

            SunsetTheme {
                ServerSelectionScreen(
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
            }
        }

        "onboarding" -> {
            apiClient?.let { client ->
                SunsetTheme {
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
                SunsetTheme {
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

                if (showSearch) {
                    SunsetTheme {
                        SearchScreen(
                            apiClient = client,
                            baseUrl = baseUrl,
                            onSelect = { item ->
                                selectedItem = item
                                showSearch = false
                            },
                            onClose = { showSearch = false }
                        )
                    }
                    return
                }

                if (selectedItem != null) {
                    SunsetTheme {
                        MediaDetailsScreen(
                            item = selectedItem!!,
                            baseUrl = baseUrl,
                            apiClient = client,
                            userId = userId,
                            onPlay = { startPlayer(activity, client, selectedItem!!, baseUrl, userId) },
                            onClose = { selectedItem = null }
                        )
                    }
                    return
                }

                if (showAdmin) {
                    SunsetTheme {
                        AdminScreen(
                            apiClient = client,
                            baseUrl = baseUrl,
                            onBack = { showAdmin = false }
                        )
                    }
                    return
                }

                SunsetTheme {
                    val iconHome: @Composable () -> Unit = { Icon(SunsetIcons.Home, contentDescription = "Home") }
                    val iconLibrary: @Composable () -> Unit = { Icon(SunsetIcons.Library, contentDescription = "Library") }
                    val iconSettings: @Composable () -> Unit = { Icon(SunsetIcons.Settings, contentDescription = "Settings") }
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
                            when (activeTab) {
                                "home" -> DashboardScreen(
                                    apiClient = client,
                                    baseUrl = baseUrl,
                                    onPlayItem = { item ->
                                    startPlayer(activity, client, item, baseUrl, userId)
                                    },
                                    onSearch = { showSearch = true },
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
                                    darkTheme = darkTheme,
                                    onDarkThemeChange = { newVal ->
                                        darkTheme = newVal
                                        scope.launch {
                                            activity.dataStore.edit { it[PrefKeys.DARK_MODE] = newVal }
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
                                    onGoToAdmin = { showAdmin = true }
                                )
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
    onServerSelected: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SunSet", style = MaterialTheme.typography.displayLarge, color = NetflixRed)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.trim() },
            label = { Text("Server URL") },
            placeholder = { Text("http://192.168.1.100:7867") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri
            )
        )
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
