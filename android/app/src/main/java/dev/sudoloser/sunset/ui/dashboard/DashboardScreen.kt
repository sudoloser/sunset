package dev.sudoloser.sunset.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.Library
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    apiClient: ApiClient,
    baseUrl: String,
    onPlayItem: (MediaItem) -> Unit,
    onSearch: () -> Unit
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
            val items = libraryItems[lib.id].orEmpty()
            if (items.isNotEmpty()) {
                item {
                    MediaRow(
                        title = lib.name,
                        items = items,
                        baseUrl = baseUrl,
                        onPlay = onPlayItem
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}
