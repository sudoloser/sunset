package dev.sudoloser.sunset.ui.mediadetails

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MediaDetailsScreen(
    item: MediaItem,
    baseUrl: String,
    apiClient: ApiClient,
    userId: String?,
    onPlay: () -> Unit,
    onClose: () -> Unit
) {
    var episodes by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf(1) }
    var inMyList by remember { mutableStateOf(false) }
    var userRating by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(item.id) {
        try {
            val isShow = item.mediaType.name == "EPISODE" || item.showTitle != null
            if (isShow) {
                val title = item.showTitle ?: item.title
                episodes = apiClient.getShowEpisodes(title)
                if (episodes.isNotEmpty()) {
                    selectedSeason = episodes.first().season ?: 1
                }
            }
            if (userId != null) {
                val userItems = apiClient.getUserItems(userId)
                inMyList = userItems.any { it.id == item.id }
            }
        } catch (_: Exception) {}
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background Image (Blurred)
        AsyncImage(
            model = "$baseUrl/api/media/${item.id}/asset/backdrop.jpg",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
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
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 300f
                            )
                        )
                )

                SunsetIconButton(
                    icon = SunsetIcons.Back,
                    onClick = onClose,
                    modifier = Modifier.padding(16.dp),
                    backgroundColor = Color.Black.copy(alpha = 0.5f)
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
                            .width(260.dp)
                            .padding(bottom = 24.dp),
                        onError = { /* Fallback handled below */ }
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SunsetButton(
                            text = "Play",
                            onClick = onPlay,
                            variant = ButtonVariant.Primary,
                            modifier = Modifier.width(140.dp)
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
                                    } catch (_: Exception) {}
                                }
                            },
                            variant = ButtonVariant.Secondary,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }

            // Content Area
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item.year?.let {
                        Text(it.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    if (episodes.isNotEmpty()) {
                        val seasonCount = episodes.map { it.season ?: 1 }.distinct().size
                        Text("$seasonCount Seasons", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    item.rating?.let {
                        Text("TMDB ${"%.1f".format(it)}/10", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            Text(
                                text = if (star <= userRating) "★" else "☆",
                                fontSize = 18.sp,
                                color = if (star <= userRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable { userRating = star }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = item.description ?: "No description available for this title.",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )

                if (episodes.isNotEmpty()) {
                    Spacer(Modifier.height(40.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Episodes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        
                        val seasons = episodes.map { it.season ?: 1 }.distinct().sorted()
                        var seasonExpanded by remember { mutableStateOf(false) }
                        
                        Box {
                            TextButton(
                                onClick = { seasonExpanded = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                            ) {
                                Text("Season $selectedSeason ▾", fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = seasonExpanded,
                                onDismissRequest = { seasonExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                seasons.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("Season $s") },
                                        onClick = { selectedSeason = s; seasonExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val seasonEps = episodes.filter { (it.season ?: 1) == selectedSeason }
                        .sortedBy { it.episode }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        seasonEps.forEach { ep ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPlay() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = ep.episode.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ep.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(SunsetIcons.Play, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Cast Section
                val castList = item.cast?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                if (castList.isNotEmpty()) {
                    Spacer(Modifier.height(40.dp))
                    Text("Cast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        castList.take(5).forEach { actor ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val initials = actor.split(" ").take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .joinToString("")
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(initials, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(actor, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}
