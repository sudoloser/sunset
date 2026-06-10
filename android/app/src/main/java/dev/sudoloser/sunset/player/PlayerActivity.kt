package dev.sudoloser.sunset.player

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import dev.sudoloser.sunset.R
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var videoUrl: String? = null
    private var videoTitle: String? = null
    private var itemId: String? = null
    private var baseUrl: String? = null
    private var userId: String? = null
    private var saveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        videoUrl = intent.getStringExtra("video_url")
        videoTitle = intent.getStringExtra("video_title")
        itemId = intent.getStringExtra("item_id")
        baseUrl = intent.getStringExtra("base_url")
        userId = intent.getStringExtra("user_id")

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        playerView = findViewById(R.id.player_view)
        initializePlayer()
    }

    private fun initializePlayer() {
        val url = videoUrl ?: return
        val id = itemId ?: return

        val trackSelector = DefaultTrackSelector(this).also { selector ->
            selector.setParameters(selector.buildUponParameters().setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE))
        }
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build().also { exoPlayer ->
            playerView.player = exoPlayer

            val dataSourceFactory = DefaultHttpDataSource.Factory()

            val mainMediaItem = MediaItem.Builder().setUri(url).build()
            val mainSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mainMediaItem)

            val sources = mutableListOf<MediaSource>(mainSource)

            // Fetch and add subtitles
            if (baseUrl != null) {
                scope.launch {
                    try {
                        val subtitleNames = fetchSubtitles(id)
                        subtitleNames.forEach { name ->
                            val subUrl = "$baseUrl/api/media/$id/subtitle/${java.net.URLEncoder.encode(name, "UTF-8")}"
                            val mimeType = if (name.endsWith(".vtt")) "text/vtt" else "application/x-subrip"
                            val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUrl))
                                .setMimeType(mimeType)
                                .build()
                            val subSource = SingleSampleMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(subConfig, 0L)
                            sources.add(subSource)
                        }
                    } catch (_: Exception) {}
                    withContext(Dispatchers.Main) {
                        val mergedSource = MergingMediaSource(*sources.toTypedArray())
                        exoPlayer.setMediaSource(mergedSource)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                }
            } else {
                exoPlayer.setMediaSource(mainSource)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }

            // Start periodic save
            startSaveJob(exoPlayer, id)
        }
    }

    private suspend fun fetchSubtitles(id: String): List<String> {
        val request = Request.Builder().url("$baseUrl/api/media/$id/subtitles").get().build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()
        val arr = json.decodeFromString<JsonArray>(body)
        return arr.map { it.jsonPrimitive.content }
    }

    private fun startSaveJob(exoPlayer: ExoPlayer, id: String) {
        saveJob?.cancel()
        saveJob = scope.launch {
            while (isActive) {
                delay(15000)
                val pos = exoPlayer.currentPosition.toDouble()
                val dur = exoPlayer.duration.toDouble()
                val playing = exoPlayer.isPlaying
                savePlayback(id, pos, dur, isPlaying = playing)
            }
        }
    }

    private fun savePlayback(itemId: String, timestamp: Double, duration: Double, isPlaying: Boolean) {
        if (baseUrl == null) return
        try {
            val payload = buildString {
                append("{\"item_id\":\"$itemId\",\"timestamp\":$timestamp,\"duration\":$duration")
                if (userId != null) append(",\"user_id\":\"$userId\"")
                append(",\"is_playing\":$isPlaying}")
            }
            val request = Request.Builder()
                .url("$baseUrl/api/playback")
                .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            httpClient.newCall(request).execute()
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        saveJob?.cancel()
        player?.let { p ->
            if (itemId != null) {
                savePlayback(itemId!!, p.currentPosition.toDouble(), p.duration.toDouble(), isPlaying = false)
            }
            p.stop()
        }
    }

    override fun onDestroy() {
        val pos = player?.currentPosition?.toDouble()
        val dur = player?.duration?.toDouble()
        player?.release()
        player = null
        saveJob?.cancel()
        if (itemId != null && pos != null) {
            savePlayback(itemId!!, pos, dur ?: 0.0, isPlaying = false)
        }
        scope.cancel()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onDestroy()
    }
}
