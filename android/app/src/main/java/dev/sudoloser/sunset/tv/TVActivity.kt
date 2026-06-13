package dev.sudoloser.sunset.tv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.PrefKeys
import dev.sudoloser.sunset.data.dataStore
import androidx.datastore.preferences.core.edit
import dev.sudoloser.sunset.data.models.Library
import dev.sudoloser.sunset.data.models.LibraryType
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.data.models.MediaType
import dev.sudoloser.sunset.player.PlayerActivity
import dev.sudoloser.sunset.ui.theme.NetflixRed
import dev.sudoloser.sunset.ui.theme.SunsetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class TVActivity : ComponentActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var lastShakeTime = 0L
    private var onShakeCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val baseUrl = intent.getStringExtra("base_url") ?: ""
        val userId = intent.getStringExtra("user_id")

        setContent {
            val scope = rememberCoroutineScope()
            var showEmergencyMenu by remember { mutableStateOf(false) }

            onShakeCallback = { showEmergencyMenu = true }

            SunsetTheme {
                Box(Modifier.fillMaxSize()) {
                    TVNavHost(
                        baseUrl = baseUrl,
                        userId = userId,
                        onExitTvMode = {
                            scope.launch {
                                dataStore.edit { it[PrefKeys.TV_MODE] = false }
                            }
                            finish()
                        }
                    )

                    if (showEmergencyMenu) {
                        EmergencyMenu(
                            showExitTv = true,
                            onCopyLog = { copyLogcat(this@TVActivity) },
                            onExitTvMode = {
                                showEmergencyMenu = false
                                scope.launch {
                                    dataStore.edit { it[PrefKeys.TV_MODE] = false }
                                }
                                finish()
                            },
                            onDismiss = { showEmergencyMenu = false }
                        )
                    }
                }
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

fun copyLogcat(context: Context) {
    try {
        val process = Runtime.getRuntime().exec("logcat -d -t 500 *:V")
        val log = process.inputStream.bufferedReader().readText()
        val clip = ClipData.newPlainText("SunSet Logs", log)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to copy logs: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun EmergencyMenu(
    showExitTv: Boolean,
    onCopyLog: () -> Unit,
    onExitTvMode: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Emergency Menu", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Shake detected!", color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = { onCopyLog(); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Copy Log", fontWeight = FontWeight.Bold)
                }

                if (showExitTv && onExitTvMode != null) {
                    Button(
                        onClick = { onExitTvMode() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Switch to Mobile Layout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TVNavHost(baseUrl: String, userId: String?, onExitTvMode: () -> Unit = {}) {
    var currentScreen by remember { mutableStateOf("home") }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiClient = remember { ApiClient(baseUrl) }

    when {
        selectedItem != null -> {
            TVMediaDetails(
                item = selectedItem!!,
                baseUrl = baseUrl,
                apiClient = apiClient,
                userId = userId,
                onBack = { selectedItem = null },
                onPlay = { item ->
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        putExtra("video_url", apiClient.getStreamUrl(item.id))
                        putExtra("video_title", item.title)
                        putExtra("item_id", item.id)
                        putExtra("base_url", baseUrl)
                        putExtra("user_id", userId)
                        putExtra("show_title", item.showTitle)
                        putExtra("media_type", item.mediaType.name)
                    }
                    context.startActivity(intent)
                }
            )
        }
        currentScreen == "settings" -> {
            TVSettings(
                apiClient = apiClient,
                baseUrl = baseUrl,
                userId = userId,
                onBack = { currentScreen = "home" },
                onExitTvMode = onExitTvMode
            )
        }
        else -> {
            TVHome(
                baseUrl = baseUrl,
                userId = userId,
                apiClient = apiClient,
                onSelectItem = { selectedItem = it },
                onSettings = { currentScreen = "settings" }
            )
        }
    }
}

@Composable
fun TVHome(
    baseUrl: String,
    userId: String?,
    apiClient: ApiClient,
    onSelectItem: (MediaItem) -> Unit,
    onSettings: () -> Unit
) {
    var continueWatching by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var recentlyAdded by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var groupedShows by remember { mutableStateOf<List<Pair<MediaItem, Int>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val recent = apiClient.getRecentlyAdded(userId)
            val cw = if (userId != null) apiClient.getContinueWatching(userId) else emptyList()
            recentlyAdded = recent

            val shows = mutableMapOf<String, MutableList<MediaItem>>()
            recent.forEach { item ->
                if (item.mediaType == MediaType.EPISODE && item.showTitle != null) {
                    shows.getOrPut(item.showTitle!!) { mutableListOf() }.add(item)
                } else {
                    shows[item.title] = mutableListOf(item)
                }
            }
            groupedShows = shows.map { (key, eps) ->
                val rep = eps.minByOrNull { it.season ?: 0 }?.copy(title = key) ?: eps.first().copy(title = key)
                rep to eps.size
            }.sortedByDescending { it.second }

            continueWatching = cw.distinctBy { it.showTitle ?: it.title }
        } catch (e: Exception) { e.printStackTrace() }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SunSet",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            TVFocusableButton("Settings", onClick = onSettings)
        }

        Spacer(Modifier.height(32.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            if (continueWatching.isNotEmpty()) {
                item {
                    TVSection("Continue Watching") {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.height(340.dp)
                        ) {
                            items(continueWatching) { item ->
                                TVPosterCard(item, baseUrl) { onSelectItem(item) }
                            }
                        }
                    }
                }
            }

            item {
                TVSection("Recently Added") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(340.dp)
                    ) {
                        items(recentlyAdded.take(20)) { item ->
                            TVPosterCard(item, baseUrl) { onSelectItem(item) }
                        }
                    }
                }
            }

            if (groupedShows.isNotEmpty()) {
                item {
                    TVSection("All Shows") {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.height(340.dp)
                        ) {
                            items(groupedShows.take(20)) { (item, count) ->
                                TVPosterCard(item, baseUrl, subtitle = "$count episodes") { onSelectItem(item) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TVSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun TVMediaDetails(
    item: MediaItem,
    baseUrl: String,
    apiClient: ApiClient,
    userId: String?,
    onBack: () -> Unit,
    onPlay: (MediaItem) -> Unit
) {
    var episodes by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(item) {
        try {
            if (item.mediaType == MediaType.EPISODE || item.showTitle != null) {
                val showTitle = item.showTitle ?: item.title
                episodes = apiClient.getShowEpisodes(showTitle, userId)
            }
        } catch (e: Exception) { e.printStackTrace() }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TVFocusableButton("← Back", onClick = onBack)
        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            AsyncImage(
                model = "$baseUrl/api/media/${item.id}/asset/folder.jpg",
                contentDescription = item.title,
                modifier = Modifier.width(240.dp).height(360.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(item.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                item.year?.let { Text("Year: $it", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item.genres?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                item.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp) }

                Spacer(Modifier.height(8.dp))
                TVFocusableButton("Play", onClick = { onPlay(item) })
            }
        }

        if (episodes.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            Text("Episodes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            val grouped = episodes.groupBy { it.season ?: 1 }
            grouped.keys.sorted().forEach { season ->
                Spacer(Modifier.height(16.dp))
                Text("Season $season", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                grouped[season]?.forEach { ep ->
                    TVFocusableCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("${ep.episode ?: "?"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(40.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ep.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            TVFocusableButton("Play") { onPlay(ep) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TVSettings(
    apiClient: ApiClient,
    baseUrl: String,
    userId: String?,
    onBack: () -> Unit,
    onExitTvMode: () -> Unit = {}
) {
    var libraries by remember { mutableStateOf<List<Library>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { libraries = apiClient.getLibraries() } catch (e: Exception) { e.printStackTrace() }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TVFocusableButton("← Back", onClick = onBack)
        Spacer(Modifier.height(24.dp))

        Text("Settings", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(32.dp))

        // Libraries
        TVFocusableCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Libraries", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (loading) {
                    CircularProgressIndicator()
                } else if (libraries.isEmpty()) {
                    Text("No libraries configured", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    libraries.forEach { lib ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lib.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (lib.libType == LibraryType.MOVIES) "Movies" else "Shows",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Server Info
        TVFocusableCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Server", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("URL: $baseUrl", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("User: ${userId ?: "Not logged in"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Exit TV Mode
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onExitTvMode,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Switch to Mobile Mode", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TVPosterCard(
    item: MediaItem,
    baseUrl: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(180.dp)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = if (isFocused) 1.08f else 1f
                scaleY = if (isFocused) 1.08f else 1f
            }
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(270.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = "$baseUrl/api/media/${item.id}/asset/folder.jpg",
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isFocused) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            fontSize = 14.sp
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun TVFocusableButton(text: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        modifier = Modifier
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = if (isFocused) 1.05f else 1f
                scaleY = if (isFocused) 1.05f else 1f
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text,
            color = if (isFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TVFocusableCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = if (isFocused) 1.02f else 1f
                scaleY = if (isFocused) 1.02f else 1f
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        content()
    }
}
