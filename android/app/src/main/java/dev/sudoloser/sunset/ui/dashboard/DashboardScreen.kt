package dev.sudoloser.sunset.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.Library
import dev.sudoloser.sunset.data.models.LibraryType
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    apiClient: ApiClient,
    baseUrl: String,
    onPlayItem: (MediaItem) -> Unit,
    onSearch: () -> Unit,
    onShowClicked: ((String, List<MediaItem>) -> Unit)? = null
) {
    var recentlyAdded by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var libraries by remember { mutableStateOf<List<Library>>(emptyList()) }
    var libraryItems by remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var genreItems by remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    var collections by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var featured by remember { mutableStateOf<MediaItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val recent = apiClient.getRecentlyAdded()
            val libs = apiClient.getLibraries()
            val itemsMap = mutableMapOf<String, List<MediaItem>>()
            libs.forEach { lib ->
                try { itemsMap[lib.id] = apiClient.getLibraryItems(lib.id) } catch (_: Exception) {}
            }
            val gens = apiClient.getGenres()
            val genreMap = mutableMapOf<String, List<MediaItem>>()
            gens.take(5).forEach { g ->
                try { genreMap[g] = apiClient.getGenreItems(g) } catch (_: Exception) {}
            }
            val cols = recent.filter { it.collectionName != null }
                .groupBy { it.collectionName }
                .values
                .filter { it.size >= 2 }
                .flatten()

            recentlyAdded = recent
            libraries = libs
            libraryItems = itemsMap
            genres = gens.take(5)
            genreItems = genreMap
            collections = cols
            featured = recent.firstOrNull()
        } catch (_: Exception) {}
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
        return
    }

    if (recentlyAdded.isEmpty() && libraries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No content yet.\nAdd libraries in Settings > Admin and run a scan.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        if (featured != null) {
            item {
                Hero(
                    item = featured,
                    baseUrl = baseUrl,
                    onPlay = { featured?.let { onPlayItem(it) } }
                )
            }
        }

        if (collections.isNotEmpty()) {
            val collectionItems = recentlyAdded.filter { it.collectionName != null }
            item {
                MediaRow(
                    title = "Collections",
                    items = collectionItems,
                    baseUrl = baseUrl,
                    onPlay = onPlayItem
                )
            }
        }

        item {
            MediaRow(
                title = "Recently Added",
                items = recentlyAdded,
                baseUrl = baseUrl,
                onPlay = onPlayItem
            )
        }

        genres.forEach { genre ->
            val items = genreItems[genre].orEmpty()
            if (items.isNotEmpty()) {
                item {
                    MediaRow(
                        title = genre,
                        items = items,
                        baseUrl = baseUrl,
                        onPlay = onPlayItem
                    )
                }
            }
        }

        libraries.forEach { lib ->
            val allItems = libraryItems[lib.id].orEmpty()
            if (allItems.isNotEmpty()) {
                if (lib.libType == LibraryType.SHOWS) {
                    val grouped = allItems.groupBy { it.showTitle ?: it.title }
                    val showCards = grouped.map { (_, eps) -> eps.first() }
                    item {
                        MediaRow(
                            title = lib.name,
                            items = showCards,
                            baseUrl = baseUrl,
                            onPlay = { clicked ->
                                val showTitle = clicked.showTitle
                                if (showTitle != null && onShowClicked != null) {
                                    onShowClicked(showTitle, grouped[showTitle] ?: emptyList())
                                } else {
                                    onPlayItem(clicked)
                                }
                            },
                            getSubtitle = { item -> "${grouped[item.showTitle ?: item.title]?.size ?: 0} episodes" }
                        )
                    }
                } else {
                    item {
                        MediaRow(
                            title = lib.name,
                            items = allItems,
                            baseUrl = baseUrl,
                            onPlay = onPlayItem
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}
