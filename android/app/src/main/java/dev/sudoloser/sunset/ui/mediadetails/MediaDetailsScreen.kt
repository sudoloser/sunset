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
import dev.sudoloser.sunset.ui.theme.NetflixRed
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
            if (item.mediaType.name == "EPISODE") {
                val showTitle = item.showTitle ?: item.title
                episodes = apiClient.getShowEpisodes(showTitle)
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
            .background(Color.Black.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
                                startY = 200f
                            )
                        )
                )

                SunsetIconButton(
                    icon = SunsetIcons.Back,
                    onClick = onClose,
                    modifier = Modifier.padding(8.dp),
                    backgroundColor = Color(0x66000000)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SunsetButton(
                        text = "Play",
                        onClick = onPlay,
                        variant = ButtonVariant.Primary
                    )

                    SunsetButton(
                        text = if (inMyList) "In My List" else "My List",
                        onClick = {
                            scope.launch {
                                try {
                                    if (userId != null) {
                                        if (inMyList) {
                                            apiClient.removeUserItem(userId, item.id)
                                        } else {
                                            apiClient.addUserItem(userId, item.id)
                                        }
                                        inMyList = !inMyList
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        variant = ButtonVariant.Secondary
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.year != null) {
                        Text(item.year.toString(), color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                    if (item.rating != null) {
                        Text("TMDB: ${item.rating}/10", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your rating: ", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    (1..5).forEach { star ->
                        Text(
                            text = if (star <= userRating) "★" else "☆",
                            fontSize = 20.sp,
                            color = if (star <= userRating) NetflixRed else Color.Gray,
                            modifier = Modifier.clickable { userRating = star }
                        )
                    }
                }

                if (item.description != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = item.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val castList = item.cast?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                if (castList.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Cast", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        castList.take(5).forEach { actor ->
                            val initials = actor.split(" ").take(2)
                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                .joinToString("")
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF333333)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initials, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (episodes.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))

                    val seasons = episodes.map { it.season ?: 1 }.distinct().sorted()
                    var seasonExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = seasonExpanded, onExpandedChange = { seasonExpanded = it }) {
                        OutlinedTextField(
                            value = "Season $selectedSeason",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seasonExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = seasonExpanded, onDismissRequest = { seasonExpanded = false }) {
                            seasons.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("Season $s") },
                                    onClick = { selectedSeason = s; seasonExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val seasonEps = episodes.filter { (it.season ?: 1) == selectedSeason }
                        .sortedBy { it.episode }
                    seasonEps.forEach { ep ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay() }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${ep.episode}. ${ep.title}",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Text("▶", color = NetflixRed, fontSize = 14.sp)
                        }
                        HorizontalDivider(color = Color(0xFF333333))
                    }
                }
            }
        }
    }
}
