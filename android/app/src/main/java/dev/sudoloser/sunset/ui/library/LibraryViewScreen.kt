package dev.sudoloser.sunset.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.Library
import dev.sudoloser.sunset.data.models.LibraryType
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.ui.components.*

@Composable
fun LibraryViewScreen(
    library: Library,
    apiClient: ApiClient,
    baseUrl: String,
    onPlayItem: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedShow by remember { mutableStateOf<String?>(null) }
    var showEpisodes by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    LaunchedEffect(library.id) {
        try {
            items = apiClient.getLibraryItems(library.id)
        } catch (_: Exception) {}
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (selectedShow != null) {
        LaunchedEffect(selectedShow) {
            try {
                showEpisodes = apiClient.getShowEpisodes(selectedShow!!)
            } catch (_: Exception) {}
        }
        ShowEpisodesView(
            showTitle = selectedShow!!,
            episodes = showEpisodes,
            onPlayItem = onPlayItem,
            onBack = { selectedShow = null; showEpisodes = emptyList() }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("< Back") }
            Spacer(Modifier.width(8.dp))
            Text(
                text = library.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        val columnCount = adaptiveGridColumns()
        if (library.libType == LibraryType.MOVIES) {
            LazyVerticalGrid(
                columns = columnCount,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Poster(item = item, baseUrl = baseUrl, onClick = { onPlayItem(item) })
                }
            }
        } else {
            val grouped = items.groupBy { it.showTitle ?: it.title }
            LazyVerticalGrid(
                columns = columnCount,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (showTitle, eps) ->
                    item {
                        Poster(
                            item = eps.first(),
                            baseUrl = baseUrl,
                            onClick = { selectedShow = showTitle },
                            subtitle = "${eps.size} episodes"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun adaptiveGridColumns(): GridCells {
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    return when {
        width > 1000.dp -> GridCells.Fixed(6)
        width > 700.dp -> GridCells.Fixed(5)
        width > 500.dp -> GridCells.Fixed(4)
        else -> GridCells.Fixed(3)
    }
}

@Composable
fun ShowEpisodesView(
    showTitle: String,
    episodes: List<MediaItem>,
    onPlayItem: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("< Back to Shows") }
            Spacer(Modifier.width(8.dp))
            Text(
                text = showTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        val seasons = episodes.groupBy { it.season ?: 1 }
        seasons.toSortedMap().forEach { (season, eps) ->
            Text(
                "Season $season",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                eps.sortedBy { it.episode }.forEach { ep ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onPlayItem(ep) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${ep.episode}. ${ep.title}",
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!ep.overview.isNullOrBlank()) {
                                    Text(
                                        ep.overview,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { onPlayItem(ep) }) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Play ${ep.title}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
