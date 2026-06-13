package dev.sudoloser.sunset.player

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.media3.ui.CaptionStyleCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import dev.sudoloser.sunset.data.PrefKeys
import dev.sudoloser.sunset.data.dataStore
import dev.sudoloser.sunset.ui.components.*
import dev.sudoloser.sunset.ui.theme.SunsetTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

import android.widget.Toast
import androidx.media3.common.PlaybackException
import androidx.media3.common.MimeTypes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@UnstableApi
class PlayerActivity : ComponentActivity() {
    private var player by mutableStateOf<ExoPlayer?>(null)
    private var videoUrl: String? = null
    private var videoTitle: String? = null
    private var itemId: String? = null
    private var baseUrl: String? = null
    private var userId: String? = null
    private var showTitle: String? = null
    private var mediaType: String? = null
    private var activeItemId: String? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val lenientJson = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        videoUrl = intent.getStringExtra("video_url")
        videoTitle = intent.getStringExtra("video_title")
        itemId = intent.getStringExtra("item_id")
        baseUrl = intent.getStringExtra("base_url")?.trimEnd('/')
        userId = intent.getStringExtra("user_id")
        showTitle = intent.getStringExtra("show_title")
        mediaType = intent.getStringExtra("media_type")

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        setContent {
            SunsetTheme {
                CustomPlayerScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @Composable
    fun CustomPlayerScreen() {
        val context = LocalContext.current
        var isPlaying by remember { mutableStateOf(false) }
        var currentTime by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var showControls by remember { mutableStateOf(true) }
        var playbackSpeed by remember { mutableFloatStateOf(1f) }
        var activeSubtitle by remember { mutableStateOf<String?>(null) }
        var subtitleTracks by remember { mutableStateOf<List<String>>(emptyList()) }
        
        var showSubtitlePicker by remember { mutableStateOf(false) }
        var showSpeedPicker by remember { mutableStateOf(false) }
        var shouldRestorePosition by remember { mutableStateOf(true) }
        var skipDirection by remember { mutableStateOf<String?>(null) }
        var resizeMode by remember { mutableIntStateOf(0) } // 0: Fit, 3: Fill, 4: Zoom

        var subtitleColor by remember { mutableStateOf("#ffffff") }
        var subtitleBgOpacity by remember { mutableFloatStateOf(0f) }
        var subtitleSize by remember { mutableIntStateOf(100) }
        var subtitleBold by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val prefs = context.dataStore.data.first()
            subtitleColor = prefs[PrefKeys.SUBTITLE_COLOR] ?: "#ffffff"
            subtitleBgOpacity = prefs[PrefKeys.SUBTITLE_BACKGROUND_OPACITY] ?: 0f
            subtitleSize = prefs[PrefKeys.SUBTITLE_SIZE] ?: 100
            subtitleBold = prefs[PrefKeys.SUBTITLE_BOLD] ?: false
        }

        var episodes by remember { mutableStateOf<List<dev.sudoloser.sunset.data.models.MediaItem>>(emptyList()) }
        var showEpisodePicker by remember { mutableStateOf(false) }
        var currentItemId by remember { mutableStateOf(itemId ?: "") }
        var currentEpisodeTitle by remember { mutableStateOf(videoTitle ?: "") }
        var currentVideoUrl by remember { mutableStateOf(videoUrl ?: "") }
        
        val isSeries = mediaType?.uppercase() == "EPISODE" || !showTitle.isNullOrEmpty()
        
        val episodeShowTitle = remember(showTitle, videoTitle, mediaType) {
            val rawTitle = showTitle ?: if (mediaType?.uppercase() == "EPISODE") videoTitle else null
            rawTitle?.replace(Regex("(?i)\\[SUB\\]|\\[DUB\\]|\\(SUB\\)|\\(DUB\\)"), "")?.trim()
        }

        // Fetch episodes
        LaunchedEffect(episodeShowTitle) {
            if (episodeShowTitle != null && baseUrl != null) {
                try {
                    val list = withContext(Dispatchers.IO) { fetchEpisodes(episodeShowTitle) }
                    episodes = list
                    if (list.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "No episodes found for: $episodeShowTitle", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error fetching episodes: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fun switchEpisode(ep: dev.sudoloser.sunset.data.models.MediaItem) {
            player?.let { saveCurrentPlayback(it.currentPosition, it.duration, currentId = currentItemId) }
            currentVideoUrl = "$baseUrl/api/stream/${ep.id}"
            currentEpisodeTitle = ep.title
            currentItemId = ep.id
            shouldRestorePosition = true
            showEpisodePicker = false
        }

        // Initialize Player
        LaunchedEffect(currentItemId) {
            activeItemId = currentItemId
            player?.release()

            val trackSelector = DefaultTrackSelector(context)
            val exoPlayer = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build()
            
            player = exoPlayer
                    
            exoPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (!playing) {
                        exoPlayer.let { saveCurrentPlayback(it.currentPosition, it.duration, false, currentItemId) }
                    }
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        duration = exoPlayer.duration
                        if (shouldRestorePosition) {
                            shouldRestorePosition = false
                            exoPlayer.playWhenReady = false
                            scope.launch {
                                val savedState = withContext(Dispatchers.IO) { currentItemId.let { fetchPlaybackState(it) } }
                                savedState?.let { exoPlayer.seekTo((it.timestamp * 1000).toLong()) }
                                exoPlayer.playWhenReady = true
                            }
                        }
                    } else if (state == Player.STATE_ENDED) {
                        val currentIdx = episodes.indexOfFirst { it.id == currentItemId }
                        if (currentIdx >= 0 && currentIdx < episodes.size - 1) {
                            val nextEp = episodes[currentIdx + 1]
                            saveCurrentPlayback(exoPlayer.currentPosition, exoPlayer.duration, false, currentItemId)
                            currentVideoUrl = "$baseUrl/api/stream/${nextEp.id}"
                            currentEpisodeTitle = nextEp.title
                            currentItemId = nextEp.id
                            shouldRestorePosition = false
                        }
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    val msg = if (error.message?.contains("404") == true) {
                        "Video not found (404). It may have been moved or deleted."
                    } else {
                        "Playback Error: ${error.message}"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            })

            // Prepare Media Item
            val subtitleConfigs = mutableListOf<MediaItem.SubtitleConfiguration>()
            try {
                val subs = withContext(Dispatchers.IO) { fetchSubtitleList(currentItemId) }
                subtitleTracks = subs
                subs.forEach { name ->
                    val subUrl = "$baseUrl/api/media/$currentItemId/subtitle/${Uri.encode(name)}"
                    val mimeType = if (name.endsWith(".vtt")) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
                    val label = name.substringBeforeLast(".").split(".").last().uppercase()
                    
                    val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUrl))
                        .setMimeType(mimeType)
                        .setLabel(label)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    subtitleConfigs.add(subConfig)
                }
            } catch (_: Exception) {}

            val mediaItem = MediaItem.Builder()
                .setUri(currentVideoUrl)
                .setSubtitleConfigurations(subtitleConfigs)
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            
            // Progress update loop - frequent updates for smooth seek bar
            var lastSave = 0L
            while (true) {
                delay(200)
                exoPlayer.let {
                    currentTime = it.currentPosition
                    duration = it.duration
                    if (it.isPlaying) {
                        val now = System.currentTimeMillis()
                        if (now - lastSave >= 2000) {
                            saveCurrentPlayback(it.currentPosition, it.duration, currentId = currentItemId)
                            lastSave = now
                        }
                    }
                }
            }
        }

        // Hide controls timer
        LaunchedEffect(showControls, isPlaying) {
            if (showControls && isPlaying) {
                delay(3500)
                showControls = false
                showSubtitlePicker = false
                showSpeedPicker = false
                showEpisodePicker = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { 
                            showControls = !showControls
                            if (!showControls) {
                                hideSystemUI()
                            }
                        },
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) {
                                player?.seekTo((player!!.currentPosition - 10000).coerceAtLeast(0))
                                skipDirection = "-10s"
                            } else {
                                player?.seekTo((player!!.currentPosition + 10000).coerceAtMost(player!!.duration))
                                skipDirection = "+10s"
                            }
                        }
                    )
                }
        ) {
            
            var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setBackgroundColor(android.graphics.Color.BLACK)
                        this.resizeMode = resizeMode
                        playerViewRef = this
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = resizeMode
                    view.subtitleView?.let { sv ->
                        sv.setApplyEmbeddedStyles(false)
                        sv.setStyle(
                            CaptionStyleCompat(
                                android.graphics.Color.parseColor(subtitleColor),
                                if (subtitleBgOpacity > 0) android.graphics.Color.argb((subtitleBgOpacity * 255).toInt(), 0, 0, 0)
                                else android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT, // windowColor
                                CaptionStyleCompat.EDGE_TYPE_NONE, // edgeType
                                android.graphics.Color.WHITE, // edgeColor
                                if (subtitleBold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Skip direction indicator
            if (skipDirection != null) {
                LaunchedEffect(skipDirection) {
                    delay(600)
                    skipDirection = null
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = skipDirection ?: "",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // UI Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PlayerControls(
                    title = currentEpisodeTitle,
                    isPlaying = isPlaying,
                    currentTime = currentTime,
                    duration = duration,
                    onTogglePlay = { player?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onSeek = { player?.seekTo(it) },
                    onSkip = { player?.seekTo(player!!.currentPosition + it) },
                    onBack = { finish() },
                    playbackSpeed = playbackSpeed,
                    onSpeedClick = { showSpeedPicker = !showSpeedPicker; showSubtitlePicker = false; showEpisodePicker = false },
                    onSubtitlesClick = { showSubtitlePicker = !showSubtitlePicker; showSpeedPicker = false; showEpisodePicker = false },
                    onEpisodesClick = { showEpisodePicker = !showEpisodePicker; showSubtitlePicker = false; showSpeedPicker = false },
                    onAspectRatioClick = {
                        resizeMode = when (resizeMode) {
                            0 -> 3 // Fill
                            3 -> 4 // Zoom
                            else -> 0 // Fit
                        }
                    },
                    hasSubtitles = subtitleTracks.isNotEmpty(),
                    hasEpisodes = isSeries
                )
            }

            // Speed Picker
            if (showSpeedPicker) {
                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 64.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp).width(120.dp)) {
                            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                Text(
                                    text = "${speed}x",
                                    color = if (playbackSpeed == speed) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            playbackSpeed = speed
                                            player?.setPlaybackSpeed(speed)
                                            showSpeedPicker = false
                                        }
                                        .padding(12.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Subtitle Picker
            if (showSubtitlePicker) {
                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 64.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp).width(200.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        activeSubtitle = null
                                        player?.trackSelectionParameters = player!!.trackSelectionParameters.buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                                        showSubtitlePicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Off", color = if (activeSubtitle == null) MaterialTheme.colorScheme.primary else Color.White, fontWeight = FontWeight.Bold)
                                if (activeSubtitle == null) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            subtitleTracks.forEach { sub ->
                                val label = sub.substringBeforeLast(".").split(".").last().uppercase()
                                val isSelected = activeSubtitle == sub
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            activeSubtitle = sub
                                            player?.trackSelectionParameters = player!!.trackSelectionParameters.buildUpon()
                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                .setPreferredTextLanguage(label.lowercase())
                                                .build()
                                            showSubtitlePicker = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White, fontWeight = FontWeight.Bold)
                                    if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Episode Picker
            if (showEpisodePicker) {
                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 64.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp).width(300.dp).heightIn(max = 420.dp)) {
                            Text(
                                text = episodeShowTitle ?: "Episodes",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                            )

                            val grouped = episodes.groupBy { it.season ?: 1 }
                            val sortedSeasons = grouped.keys.sorted()

                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                sortedSeasons.forEach { season ->
                                    val seasonEpisodes = grouped[season]!!
                                    item {
                                        Text(
                                            text = "Season $season",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                        )
                                    }
                                    items(seasonEpisodes, key = { it.id }) { ep ->
                                        val isCurrent = ep.id == currentItemId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    if (!isCurrent) switchEpisode(ep)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${ep.episode ?: "?"}",
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(28.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ep.title,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (isCurrent) {
                                                Icon(
                                                    SunsetIcons.Play,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
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

    @Composable
    fun PlayerControls(
        title: String,
        isPlaying: Boolean,
        currentTime: Long,
        duration: Long,
        onTogglePlay: () -> Unit,
        onSeek: (Long) -> Unit,
        onSkip: (Long) -> Unit,
        onBack: () -> Unit,
        playbackSpeed: Float,
        onSpeedClick: () -> Unit,
        onSubtitlesClick: () -> Unit,
        onEpisodesClick: () -> Unit,
        onAspectRatioClick: () -> Unit,
        hasSubtitles: Boolean,
        hasEpisodes: Boolean
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.Black.copy(alpha = 0.7f))
        )).padding(32.dp)) {
            
            // Top Bar
            Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(SunsetIcons.Back, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                
                IconButton(onClick = onAspectRatioClick) {
                    Icon(SunsetIcons.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                if (hasSubtitles) {
                    IconButton(onClick = onSubtitlesClick) {
                        Icon(SunsetIcons.Subtitles, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                if (hasEpisodes) {
                    IconButton(onClick = onEpisodesClick) {
                        Icon(SunsetIcons.Episodes, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                
                TextButton(onClick = onSpeedClick) {
                    Text("${playbackSpeed}x", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }

            // Center Controls
            Row(modifier = Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                IconButton(onClick = { onSkip(-10000) }) {
                    Icon(SunsetIcons.SkipBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
                
                Box(modifier = Modifier.size(84.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { onTogglePlay() }, contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPlaying) SunsetIcons.Pause else SunsetIcons.Play,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }

                IconButton(onClick = { onSkip(10000) }) {
                    Icon(SunsetIcons.SkipForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }

            // Bottom Bar
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                val progress = if (duration > 0) currentTime.toFloat() / duration.toFloat() else 0f
                Slider(
                    value = progress,
                    onValueChange = { onSeek((it * duration).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(currentTime), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(duration), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private suspend fun fetchSubtitleList(id: String): List<String> {
        val request = Request.Builder().url("$baseUrl/api/media/$id/subtitles").build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()
        return Json.decodeFromString<JsonArray>(body).map { it.jsonPrimitive.content }
    }

    private suspend fun fetchEpisodes(showTitle: String): List<dev.sudoloser.sunset.data.models.MediaItem> {
        val request = Request.Builder().url("$baseUrl/api/shows/${Uri.encode(showTitle)}/episodes").build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()
        return lenientJson.decodeFromString<List<dev.sudoloser.sunset.data.models.MediaItem>>(body)
    }

    private suspend fun fetchPlaybackState(id: String): dev.sudoloser.sunset.data.models.PlaybackState? {
        val url = if (userId != null) "$baseUrl/api/playback/$id?user_id=$userId" else "$baseUrl/api/playback/$id"
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: return null
        return try { lenientJson.decodeFromString<dev.sudoloser.sunset.data.models.PlaybackState>(body) } catch (_: Exception) { null }
    }

    private fun saveCurrentPlayback(pos: Long, dur: Long, playing: Boolean = true, currentId: String? = null) {
        val id = currentId ?: itemId
        if (baseUrl == null || id == null) return
        scope.launch(Dispatchers.IO) {
            try {
                val payload = """{"item_id":"$id","timestamp":${pos / 1000.0},"duration":${dur / 1000.0},"user_id":"$userId","is_playing":$playing}"""
                val request = Request.Builder()
                    .url("$baseUrl/api/playback")
                    .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
                httpClient.newCall(request).execute()
            } catch (_: Exception) {}
        }
    }

    override fun onStop() {
        super.onStop()
        player?.let { saveCurrentPlayback(it.currentPosition, it.duration, false, activeItemId) }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        scope.cancel()
        super.onDestroy()
    }
}
