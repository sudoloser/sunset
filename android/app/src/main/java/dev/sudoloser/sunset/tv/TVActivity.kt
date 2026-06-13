package dev.sudoloser.sunset.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.player.PlayerActivity
import dev.sudoloser.sunset.ui.theme.SunsetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TVActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val baseUrl = intent.getStringExtra("base_url") ?: ""
        val userId = intent.getStringExtra("user_id")
        val apiKey = intent.getStringExtra("api_key") ?: ""

        setContent {
            SunsetTheme {
                TVScreen(baseUrl = baseUrl, userId = userId, apiKey = apiKey)
            }
        }
    }
}

@Composable
fun TVScreen(
    baseUrl: String,
    userId: String?,
    apiKey: String,
    apiClient: ApiClient? = null
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val client = apiClient ?: remember { ApiClient(baseUrl) }

    LaunchedEffect(Unit) {
        try {
            val recent = client.getRecentlyAdded(userId)
            val cw = if (userId != null) client.getContinueWatching(userId) else emptyList()
            val all = (cw + recent).distinctBy { it.id }
            items = all
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        Text(
            "SunSet",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            items(items) { item ->
                TVPosterCard(
                    item = item,
                    baseUrl = baseUrl,
                    onClick = {
                        val intent = Intent(context, PlayerActivity::class.java).apply {
                            putExtra("video_url", client.getStreamUrl(item.id))
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
        }
    }
}

@Composable
fun TVPosterCard(
    item: MediaItem,
    baseUrl: String,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(200.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = if (isFocused) 1.1f else 1f
                scaleY = if (isFocused) 1.1f else 1f
            }
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp))
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (item.mediaType.name == "EPISODE" && item.showTitle != null) item.showTitle else item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            fontSize = 16.sp
        )
    }
}
