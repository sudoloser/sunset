package dev.sudoloser.sunset.player

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import dev.sudoloser.sunset.data.PrefKeys
import dev.sudoloser.sunset.data.dataStore
import dev.sudoloser.sunset.ui.components.*
import dev.sudoloser.sunset.ui.theme.SunsetTheme
import androidx.datastore.preferences.core.edit
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

@UnstableApi
class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var videoUrl: String? = null
    private var videoTitle: String? = null
    private var itemId: String? = null
    private var baseUrl: String? = null
    private var userId: String? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        videoUrl = intent.getStringExtra("video_url")
        videoTitle = intent.getStringExtra("video_title")
        itemId = intent.getStringExtra("item_id")
        baseUrl = intent.getStringExtra("base_url")
        userId = intent.getStringExtra("user_id")

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        setContent {
            SunsetTheme {
                CustomPlayerScreen()
            }
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

        // Initialize Player
        LaunchedEffect(Unit) {
            val trackSelector = DefaultTrackSelector(context)
            player = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build().also { exoPlayer ->
                    
                    exoPlayer.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                        }
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                duration = exoPlayer.duration
                            }
                        }
                    })

                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                    val mainMediaItem = MediaItem.Builder().setUri(videoUrl).build()
                    val mainSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mainMediaItem)
                    val sources = mutableListOf<MediaSource>(mainSource)

                    // Fetch Subtitles
                    itemId?.let { id ->
                        try {
                            val subs = withContext(Dispatchers.IO) { fetchSubtitleList(id) }
                            subtitleTracks = subs
                            subs.forEach { name ->
                                val subUrl = "$baseUrl/api/media/$id/subtitle/${java.net.URLEncoder.encode(name, "UTF-8")}"
                                val mimeType = if (name.endsWith(".vtt")) "text/vtt" else "application/x-subrip"
                                val label = name.substringBeforeLast(".").split(".").last().uppercase()
                                
                                val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUrl))
                                    .setMimeType(mimeType)
                                    .setLabel(label)
                                    .build()
                                sources.add(SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(subConfig, 0L))
                            }
                        } catch (_: Exception) {}
                    }

                    val mergedSource = MergingMediaSource(*sources.toTypedArray())
                    exoPlayer.setMediaSource(mergedSource)
                    exoPlayer.prepare()

                    // Load saved progress
                    itemId?.let { id ->
                        try {
                            val state = withContext(Dispatchers.IO) { fetchPlaybackState(id) }
                            state?.let { exoPlayer.seekTo((it.timestamp * 1000).toLong()) }
                        } catch (_: Exception) {}
                    }

                    exoPlayer.playWhenReady = true
                }
            
            // Progress update loop
            while (true) {
                delay(500)
                player?.let { 
                    currentTime = it.currentPosition
                    if (it.isPlaying) {
                        saveCurrentPlayback(it.currentPosition, it.duration)
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
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { showControls = !showControls }) {
            
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { view ->
                    view.player = player
                },
                modifier = Modifier.fillMaxSize()
            )

            // UI Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PlayerControls(
                    title = videoTitle ?: "Video",
                    isPlaying = isPlaying,
                    currentTime = currentTime,
                    duration = duration,
                    onTogglePlay = { player?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onSeek = { player?.seekTo(it) },
                    onSkip = { player?.seekTo(player!!.currentPosition + it) },
                    onBack = { finish() },
                    playbackSpeed = playbackSpeed,
                    onSpeedClick = { showSpeedPicker = !showSpeedPicker; showSubtitlePicker = false },
                    onSubtitlesClick = { showSubtitlePicker = !showSubtitlePicker; showSpeedPicker = false },
                    hasSubtitles = subtitleTracks.isNotEmpty()
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
        hasSubtitles: Boolean
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
                
                if (hasSubtitles) {
                    IconButton(onClick = onSubtitlesClick) {
                        Icon(SunsetIcons.Subtitles, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
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

    private suspend fun fetchPlaybackState(id: String): dev.sudoloser.sunset.data.models.PlaybackState? {
        val request = Request.Builder().url("$baseUrl/api/playback/$id").build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: return null
        return try { Json.decodeFromString<dev.sudoloser.sunset.data.models.PlaybackState>(body) } catch (_: Exception) { null }
    }

    private fun saveCurrentPlayback(pos: Long, dur: Long) {
        if (baseUrl == null || itemId == null) return
        scope.launch(Dispatchers.IO) {
            try {
                val payload = """{"item_id":"$itemId","timestamp":${pos / 1000.0},"duration":${dur / 1000.0},"user_id":"$userId","is_playing":true}"""
                val request = Request.Builder()
                    .url("$baseUrl/api/playback")
                    .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
                httpClient.newCall(request).execute()
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        scope.cancel()
        super.onDestroy()
    }
}
