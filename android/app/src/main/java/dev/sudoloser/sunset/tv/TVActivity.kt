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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                Box(Modifier.fillMaxSize().background(Color(0xFF0a0a0a))) {
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

    AnimatedContent(
        targetState = when {
            selectedItem != null -> "details"
            else -> currentScreen
        },
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                animationSpec = tween(300),
                initialOffsetX = { it / 4 }
            ) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "nav"
    ) { screen ->
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
    var allMovies by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allShows by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var featuredIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val recent = apiClient.getRecentlyAdded(userId)
            val cw = if (userId != null) apiClient.getContinueWatching(userId) else emptyList()
            recentlyAdded = recent

            val movies = mutableListOf<MediaItem>()
            val shows = mutableMapOf<String, MediaItem>()
            recent.forEach { item ->
                when {
                    item.mediaType == MediaType.EPISODE && item.showTitle != null -> {
                        if (!shows.containsKey(item.showTitle)) {
                            shows[item.showTitle] = item.copy(title = item.showTitle!!)
                        }
                    }
                    item.mediaType.name.lowercase() == "movie" -> movies.add(item)
                    else -> movies.add(item)
                }
            }
            allMovies = movies
            allShows = shows.values.toList()
            continueWatching = cw.distinctBy { it.showTitle ?: it.title }
        } catch (e: Exception) { e.printStackTrace() }
        loading = false
    }

    // Auto-rotate featured item
    LaunchedEffect(recentlyAdded.size) {
        if (recentlyAdded.size > 1) {
            while (true) {
                delay(8000)
                featuredIndex = (featuredIndex + 1) % recentlyAdded.size.coerceAtLeast(1)
            }
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFE50914), strokeWidth = 3.dp)
        }
        return
    }

    val featured = recentlyAdded.getOrNull(featuredIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0a0a0a))
    ) {
        // Featured Hero Banner
        TVFeaturedHero(
            item = featured,
            baseUrl = baseUrl,
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
            },
            onMoreInfo = { onSelectItem(it) }
        )

        // Content Rows
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp)
        ) {
            if (continueWatching.isNotEmpty()) {
                TVContentRow(
                    title = "Continue Watching",
                    items = continueWatching,
                    baseUrl = baseUrl,
                    onSelect = onSelectItem
                )
            }

            if (recentlyAdded.isNotEmpty()) {
                TVContentRow(
                    title = "Recently Added",
                    items = recentlyAdded,
                    baseUrl = baseUrl,
                    onSelect = onSelectItem
                )
            }

            if (allShows.isNotEmpty()) {
                TVContentRow(
                    title = "TV Shows",
                    items = allShows,
                    baseUrl = baseUrl,
                    onSelect = onSelectItem
                )
            }

            if (allMovies.isNotEmpty()) {
                TVContentRow(
                    title = "Movies",
                    items = allMovies,
                    baseUrl = baseUrl,
                    onSelect = onSelectItem
                )
            }

            Spacer(Modifier.height(80.dp))

            // Bottom Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TVBottomNavItem("Settings", isSelected = false, onClick = onSettings)
            }
        }
    }
}

@Composable
fun TVFeaturedHero(
    item: MediaItem?,
    baseUrl: String,
    onPlay: (MediaItem) -> Unit,
    onMoreInfo: (MediaItem) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isPlayFocused by remember { mutableStateOf(false) }
    var isInfoFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        if (item != null) {
            // Background Image
            AsyncImage(
                model = "$baseUrl/api/media/${item.id}/asset/backdrop.jpg",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient Overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0a0a0a).copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xFF0a0a0a).copy(alpha = 0.95f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0a0a0a).copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 64.dp, bottom = 64.dp, end = 64.dp)
                    .widthIn(max = 500.dp)
            ) {
                // Title
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 48.sp
                )

                Spacer(Modifier.height(12.dp))

                // Meta info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.year?.let {
                        Text("$it", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    item.genres?.split(',')?.take(2)?.joinToString(" · ")?.let {
                        Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TVHeroButton(
                        text = "Play",
                        isFocused = isPlayFocused,
                        onFocusChanged = { isPlayFocused = it },
                        onClick = { onPlay(item) },
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    TVHeroButton(
                        text = "More Info",
                        isFocused = isInfoFocused,
                        onFocusChanged = { isInfoFocused = it },
                        onClick = { onMoreInfo(item) },
                        variant = "secondary"
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0a0a0a)),
                contentAlignment = Alignment.Center
            ) {
                Text("No content available", color = Color.White.copy(alpha = 0.5f), fontSize = 20.sp)
            }
        }

        // Dots indicator
        if (item != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 64.dp, bottom = 72.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // We don't have access to recentlyAdded here, so skip dots
            }
        }
    }
}

@Composable
fun TVHeroButton(
    text: String,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    variant: String = "primary",
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusable()
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .clickable { onClick() }
            .background(
                if (variant == "primary") {
                    if (isFocused) Color(0xFFE50914) else Color(0xFFE50914).copy(alpha = 0.85f)
                } else {
                    if (isFocused) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                },
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isFocused) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 32.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TVContentRow(
    title: String,
    items: List<MediaItem>,
    baseUrl: String,
    onSelect: (MediaItem) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 64.dp, bottom = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                TVCard(
                    item = item,
                    baseUrl = baseUrl,
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
fun TVCard(
    item: MediaItem,
    baseUrl: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isFocused) 12f else 0f,
        animationSpec = tween(200),
        label = "card_elevation"
    )

    Column(
        modifier = Modifier
            .width(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = -elevation
            }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        // Poster
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(240.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1a1a1a))
        ) {
            AsyncImage(
                model = "$baseUrl/api/media/${item.id}/asset/folder.jpg",
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Focus overlay with gradient
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE50914).copy(alpha = 0.15f),
                                    Color(0xFFE50914).copy(alpha = 0.3f)
                                )
                            )
                        )
                )
                // Play icon hint
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color.White, fontSize = 20.sp)
                }
            }

            // Progress bar for continue watching
            if (item.progress != null && item.progress!! > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (item.progress!! / 100.0).toFloat().coerceIn(0f, 1f))
                            .fillMaxSize()
                            .background(Color(0xFFE50914))
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Title
        Text(
            text = item.title,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TVBottomNavItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .background(
                if (isFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isFocused) Color(0xFFE50914) else Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
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
    val context = LocalContext.current

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
            .background(Color(0xFF0a0a0a))
            .verticalScroll(rememberScrollState())
    ) {
        // Backdrop
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            AsyncImage(
                model = "$baseUrl/api/media/${item.id}/asset/backdrop.jpg",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0a0a0a).copy(alpha = 0.4f),
                                Color.Transparent,
                                Color(0xFF0a0a0a)
                            )
                        )
                    )
            )

            // Back button
            TVBackButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.TopStart)
            )
        }

        // Details Content
        Column(
            modifier = Modifier
                .padding(horizontal = 64.dp)
                .offset(y = (-40).dp)
        ) {
            // Title + Play Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        item.year?.let {
                            Text("$it", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                        }
                        item.genres?.let {
                            Text(it, color = Color(0xFFE50914), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                TVPlayButton(
                    text = "Play",
                    onClick = { onPlay(item) }
                )
            }

            // Description
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = item.description,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }

            // Episodes
            if (episodes.isNotEmpty()) {
                Spacer(Modifier.height(40.dp))
                Text(
                    "Episodes",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                val grouped = episodes.groupBy { it.season ?: 1 }
                grouped.keys.sorted().forEach { season ->
                    Text(
                        "Season $season",
                        color = Color(0xFFE50914),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    grouped[season]?.forEach { ep ->
                        TVEpisodeCard(
                            episode = ep,
                            baseUrl = baseUrl,
                            isCurrent = false,
                            onClick = { onPlay(ep) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun TVBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "back_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .background(
                if (isFocused) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.5f),
                CircleShape
            )
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("←", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TVPlayButton(text: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "play_scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .background(
                if (isFocused) Color(0xFFE50914) else Color(0xFFE50914).copy(alpha = 0.85f),
                RoundedCornerShape(8.dp)
            )
            .border(1.dp, if (isFocused) Color.White.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(horizontal = 36.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TVEpisodeCard(
    episode: MediaItem,
    baseUrl: String,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ep_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .background(
                if (isFocused) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f),
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                if (isFocused) Color(0xFFE50914).copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Episode number
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isFocused) Color(0xFFE50914) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${episode.episode ?: "?"}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    episode.title,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isFocused) {
                Text(
                    "▶ Play",
                    color = Color(0xFFE50914),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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
            .background(Color(0xFF0a0a0a))
            .verticalScroll(rememberScrollState())
    ) {
        TVBackButton(onClick = onBack, modifier = Modifier.padding(32.dp))

        Column(modifier = Modifier.padding(horizontal = 64.dp)) {
            Text(
                "Settings",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(40.dp))

            // Libraries
            TVSettingsCard(title = "Libraries") {
                if (loading) {
                    CircularProgressIndicator(color = Color(0xFFE50914), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else if (libraries.isEmpty()) {
                    Text("No libraries configured", color = Color.White.copy(alpha = 0.5f))
                } else {
                    libraries.forEach { lib ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lib.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (lib.libType == LibraryType.MOVIES) "Movies" else "Shows",
                                color = Color(0xFFE50914),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Server Info
            TVSettingsCard(title = "Server") {
                Text("URL: $baseUrl", color = Color.White.copy(alpha = 0.6f))
                Text("User: ${userId ?: "Not logged in"}", color = Color.White.copy(alpha = 0.6f))
            }

            Spacer(Modifier.height(32.dp))

            // Exit Button
            TVSettingsCard(title = "") {
                var exitFocused by remember { mutableStateOf(false) }
                val exitScale by animateFloatAsState(
                    targetValue = if (exitFocused) 1.03f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "exit_scale"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = exitScale; scaleY = exitScale }
                        .focusable()
                        .onFocusChanged { exitFocused = it.isFocused }
                        .clickable { onExitTvMode() }
                        .background(
                            if (exitFocused) Color(0xFFE50914) else Color(0xFFE50914).copy(alpha = 0.8f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Switch to Mobile Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun TVSettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                if (isFocused) Color(0xFFE50914).copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(24.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        content()
    }
}
