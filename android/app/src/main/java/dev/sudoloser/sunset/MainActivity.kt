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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context

val Context.dataStore by preferencesDataStore(name = "settings")

private val SERVER_URL_KEY = stringPreferencesKey("server_url")
private val USER_ID_KEY = stringPreferencesKey("user_id")
private val USERNAME_KEY = stringPreferencesKey("username")
private val IS_ADMIN_KEY = stringPreferencesKey("is_admin")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SunsetTheme(darkTheme = true) {
                AppContent(this)
            }
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
    val scope = rememberCoroutineScope()

    // Load saved state
    LaunchedEffect(Unit) {
        val url = activity.dataStore.data.first()[SERVER_URL_KEY]
        if (url != null) {
            serverUrl = url
            apiClient = ApiClient(url)
            try {
                val s = apiClient!!.getStatus()
                status = s
                if (s.setupComplete) {
                    val uid = activity.dataStore.data.first()[USER_ID_KEY]
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NetflixRed)
            }
        }

        "server_selection" -> {
            ServerSelectionScreen { url ->
                scope.launch {
                    activity.dataStore.edit { it[SERVER_URL_KEY] = url }
                    serverUrl = url
                    apiClient = ApiClient(url)
                    try {
                        val s = apiClient!!.getStatus()
                        status = s
                        step = if (s.setupComplete) "login" else "onboarding"
                    } catch (_: Exception) {
                        step = "server_selection"
                    }
                }
            }
        }

        "onboarding" -> {
            apiClient?.let { client ->
                OnboardingScreen(
                    apiClient = client,
                    onComplete = { u ->
                        user = u
                        scope.launch {
                            activity.dataStore.edit {
                                it[USER_ID_KEY] = u.userId
                                it[USERNAME_KEY] = u.username
                                it[IS_ADMIN_KEY] = u.isAdmin.toString()
                            }
                        }
                        step = "main"
                    }
                )
            }
        }

        "login" -> {
            apiClient?.let { client ->
                LoginScreen(
                    serverName = status?.serverName ?: "SunSet",
                    apiClient = client,
                    onLogin = { u ->
                        user = u
                        scope.launch {
                            activity.dataStore.edit {
                                it[USER_ID_KEY] = u.userId
                                it[USERNAME_KEY] = u.username
                                it[IS_ADMIN_KEY] = u.isAdmin.toString()
                            }
                        }
                        step = "main"
                    }
                )
            }
        }

        "main" -> {
            apiClient?.let { client ->
                val baseUrl = serverUrl ?: ""
                val userId = user?.userId

                if (showSearch) {
                    SearchScreen(
                        apiClient = client,
                        baseUrl = baseUrl,
                        onSelect = { item ->
                            selectedItem = item
                            showSearch = false
                        },
                        onClose = { showSearch = false }
                    )
                    return
                }

                if (selectedItem != null) {
                    MediaDetailsScreen(
                        item = selectedItem!!,
                        baseUrl = baseUrl,
                        apiClient = client,
                        userId = userId,
                        onPlay = {
                            val intent = Intent(activity, PlayerActivity::class.java).apply {
                                putExtra("video_url", client.getStreamUrl(selectedItem!!.id))
                                putExtra("video_title", selectedItem!!.title)
                                putExtra("item_id", selectedItem!!.id)
                            }
                            activity.startActivity(intent)
                        },
                        onClose = { selectedItem = null }
                    )
                    return
                }

                if (showAdmin) {
                    AdminScreen(
                        apiClient = client,
                        baseUrl = baseUrl,
                        onBack = { showAdmin = false }
                    )
                    return
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = activeTab == "home",
                                onClick = { activeTab = "home" },
                                icon = { Icon(SunsetIcons.Home, contentDescription = null) },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = activeTab == "library",
                                onClick = { activeTab = "library" },
                                icon = { Icon(SunsetIcons.Library, contentDescription = null) },
                                label = { Text("Library") }
                            )
                            NavigationBarItem(
                                selected = activeTab == "settings",
                                onClick = { activeTab = "settings" },
                                icon = { Icon(SunsetIcons.Settings, contentDescription = null) },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        when (activeTab) {
                            "home" -> DashboardScreen(
                                apiClient = client,
                                baseUrl = baseUrl,
                                onPlayItem = { item ->
                                    val intent = Intent(activity, PlayerActivity::class.java).apply {
                                        putExtra("video_url", client.getStreamUrl(item.id))
                                        putExtra("video_title", item.title)
                                        putExtra("item_id", item.id)
                                    }
                                    activity.startActivity(intent)
                                },
                                onSearch = { showSearch = true }
                            )
                            "library" -> LibrariesScreen(
                                apiClient = client,
                                baseUrl = baseUrl,
                                userId = userId,
                                isAdmin = user?.isAdmin == true,
                                onPlayItem = { item ->
                                    val intent = Intent(activity, PlayerActivity::class.java).apply {
                                        putExtra("video_url", client.getStreamUrl(item.id))
                                        putExtra("video_title", item.title)
                                        putExtra("item_id", item.id)
                                    }
                                    activity.startActivity(intent)
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
                                onLogout = {
                                    scope.launch {
                                        activity.dataStore.edit {
                                            it.remove(USER_ID_KEY)
                                            it.remove(USERNAME_KEY)
                                            it.remove(IS_ADMIN_KEY)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectionScreen(onServerSelected: (String) -> Unit) {
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
            onValueChange = { input = it },
            label = { Text("Server URL") },
            placeholder = { Text("http://your-server:7867") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (input.isNotBlank()) onServerSelected(input) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }
    }
}
