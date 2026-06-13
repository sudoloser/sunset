package dev.sudoloser.sunset.ui.mediadetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import coil.compose.AsyncImage
import dev.sudoloser.sunset.api.ApiClient
import android.util.Log
import dev.sudoloser.sunset.data.PrefKeys
import dev.sudoloser.sunset.data.dataStore
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MediaDetailsScreen(
    item: MediaItem,
    baseUrl: String,
    apiClient: ApiClient,
    userId: String?,
    onPlay: (MediaItem) -> Unit,
    onClose: () -> Unit
) {
    var episodes by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf(1) }
    var inMyList by remember { mutableStateOf(false) }
    var userRating by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var downloadPath by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        downloadPath = ctx.dataStore.data.first()[PrefKeys.DOWNLOAD_PATH] ?: ""
    }

    fun downloadItem(itemId: String, title: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = apiClient.generateMediaToken(itemId)
                val url = apiClient.getDownloadUrl(itemId, token)
                val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle(title)
                    .setDescription("Downloading $title")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                if (downloadPath.isNotBlank()) {
                    val dir = java.io.File(downloadPath)
                    if (dir.exists() || dir.mkdirs()) {
                        request.setDestinationUri(Uri.fromFile(java.io.File(dir, "$title.mp4")))
                    }
                }
                downloadManager.enqueue(request)
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Downloading $title", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(item.id) {
        try {
            val isShow = item.mediaType.name == "EPISODE" || item.showTitle != null
            if (isShow) {
                val title = item.showTitle ?: item.title
                episodes = apiClient.getShowEpisodes(title, userId)
                if (episodes.isNotEmpty()) {
                    selectedSeason = episodes.first().season ?: 1
                }
            }
            if (userId != null) {
                val userItems = apiClient.getUserItems(userId)
                inMyList = userItems.any { it.id == item.id }
            }
        } catch (e: Exception) { Log.e("SunSet", "Failed to load media details", e) }
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Heavy blurred backdrop
        AsyncImage(
            model = "$baseUrl/api/media/${item.id}/asset/backdrop.jpg",
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                AsyncImage(
                    model = "$baseUrl/api/media/${item.id}/asset/backdrop.jpg",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                                startY = 400f
                            )
                        )
                )

                SunsetIconButton(
                    icon = SunsetIcons.Back,
                    onClick = onClose,
                    modifier = Modifier.padding(16.dp),
                    backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    AsyncImage(
                        model = "$baseUrl/api/media/${item.id}/asset/logo.png",
                        contentDescription = item.title,
                        modifier = Modifier
                            .width(280.dp)
                            .padding(bottom = 28.dp)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SunsetButton(
                            text = "Play",
                            onClick = {
                                val epToPlay = if (episodes.isNotEmpty()) {
                                    episodes.find { (it.progress ?: 0.0) < 0.9 } ?: episodes.first()
                                } else {
                                    item
                                }
                                onPlay(epToPlay)
                            },
                            variant = ButtonVariant.Primary,
                            modifier = Modifier.width(160.dp)
                        )

                        SunsetButton(
                            text = if (inMyList) "✓ My List" else "+ My List",
                            onClick = {
                                scope.launch {
                                    try {
                                        if (userId != null) {
                                            if (inMyList) apiClient.removeUserItem(userId, item.id)
                                            else apiClient.addUserItem(userId, item.id)
                                            inMyList = !inMyList
                                        }
                                    } catch (e: Exception) { Log.e("SunSet", "Failed to update My List", e) }
                                }
                            },
                            variant = ButtonVariant.Secondary,
                            modifier = Modifier.width(160.dp)
                        )

                        if (item.mediaType.name == "MOVIE") {
                            SunsetIconButton(
                                icon = SunsetIcons.Download,
                                onClick = { downloadItem(item.id, item.title) },
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Content Area
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(24.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item.year?.let {
                        Text(it.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                    if (episodes.isNotEmpty()) {
                        val seasonCount = episodes.map { it.season ?: 1 }.distinct().size
                        Text("$seasonCount Seasons", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                    item.rating?.let {
                        Text("TMDB ${"%.1f".format(it)}/10", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }

                    if (item.versionTag != null) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(item.versionTag, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = if (star <= userRating) SunsetIcons.Star else SunsetIcons.StarOutline,
                                contentDescription = null,
                                tint = if (star <= userRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp).clickable { userRating = star }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = item.description ?: "No description available for this title.",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                if (episodes.isNotEmpty()) {
                    Spacer(Modifier.height(48.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Episodes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        
                        val seasons = episodes.map { it.season ?: 1 }.distinct().sorted()
                        var seasonExpanded by remember { mutableStateOf(false) }
                        
                        Box {
                            TextButton(
                                onClick = { seasonExpanded = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Text("Season $selectedSeason ▾", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(
                                expanded = seasonExpanded,
                                onDismissRequest = { seasonExpanded = false },
                                modifier = Modifier.background(Color(0xFF1A1A1A))
                            ) {
                                seasons.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("Season $s", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                                        onClick = { selectedSeason = s; seasonExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    val seasonEps = episodes.filter { (it.season ?: 1) == selectedSeason }
                        .sortedBy { it.episode }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        seasonEps.forEach { ep ->
                            // Blurred Pill Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                    .clickable { onPlay(ep) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = ep.episode.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.width(28.dp),
                                    textAlign = TextAlign.Center
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            ep.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if ((ep.progress ?: 0.0) > 0.7) {
                                            Icon(
                                                SunsetIcons.Check,
                                                contentDescription = "Watched",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Icon(SunsetIcons.Play, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    SunsetIcons.Download,
                                    contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { downloadItem(ep.id, ep.title) }
                                )
                            }
                        }
                    }
                }

                // Cast Section
                val castList = item.cast?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                if (castList.isNotEmpty()) {
                    Spacer(Modifier.height(48.dp))
                    Text("Cast", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        castList.take(6).forEach { actor ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val initials = actor.split(" ").take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .joinToString("")
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(initials, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Text(actor, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(120.dp))
            }
        }
    }
}
