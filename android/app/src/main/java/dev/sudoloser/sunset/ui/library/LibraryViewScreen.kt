package dev.sudoloser.sunset.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SunsetButton(
            text = "< Back",
            onClick = onBack,
            variant = ButtonVariant.Ghost
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = library.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(12.dp))

        if (library.libType == LibraryType.MOVIES) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Poster(
                        item = item,
                        baseUrl = baseUrl,
                        onClick = { onPlayItem(item) }
                    )
                }
            }
        } else {
            val shows = items.groupBy { it.showTitle ?: it.title }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shows.forEach { (showTitle, episodeItems) ->
                    val first = episodeItems.first()
                    item {
                        Poster(
                            item = first,
                            baseUrl = baseUrl,
                            onClick = {
                                selectedShow = showTitle
                            },
                            subtitle = "${episodeItems.size} episodes"
                        )
                    }
                }
            }
        }
    }

    if (selectedShow != null) {
        LaunchedEffect(selectedShow) {
            try {
                showEpisodes = apiClient.getShowEpisodes(selectedShow!!)
            } catch (_: Exception) {}
        }

        AlertDialog(
            onDismissRequest = { selectedShow = null; showEpisodes = emptyList() },
            title = { Text(selectedShow!!) },
            text = {
                Column {
                    val seasons = showEpisodes.groupBy { it.season ?: 1 }
                    seasons.toSortedMap().forEach { (season, eps) ->
                        Text(
                            "Season $season",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        eps.sortedBy { it.episode }.forEach { ep ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlayItem(ep) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${ep.episode}. ${ep.title}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp
                                )
                                Text(">", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedShow = null; showEpisodes = emptyList() }) {
                    Text("Close")
                }
            }
        )
    }
}
