package dev.sudoloser.sunset.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
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
    onSelectItem: ((MediaItem) -> Unit)? = null
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
            val recentData = apiClient.getRecentlyAdded()
            val libs = apiClient.getLibraries()
            
            // Deduplicate by show_title for episodes
            val seen = mutableSetOf<String>()
            val dedupedRecent = recentData.filter { item ->
                if (item.mediaType.name == "EPISODE" && item.showTitle != null) {
                    if (seen.contains(item.showTitle)) return@filter false
                    seen.add(item.showTitle)
                    true
                } else true
            }

            val itemsMap = mutableMapOf<String, List<MediaItem>>()
            libs.forEach { lib ->
                try { 
                    val items = apiClient.getLibraryItems(lib.id)
                    if (lib.libType == LibraryType.SHOWS) {
                        val libSeen = mutableSetOf<String>()
                        itemsMap[lib.id] = items.filter { item ->
                            val title = item.showTitle ?: item.title
                            if (libSeen.contains(title)) return@filter false
                            libSeen.add(title)
                            true
                        }.take(15)
                    } else {
                        itemsMap[lib.id] = items.take(15)
                    }
                } catch (_: Exception) {}
            }

            val gens = apiClient.getGenres()
            val genreMap = mutableMapOf<String, List<MediaItem>>()
            gens.take(5).forEach { g ->
                try { 
                    val items = apiClient.getGenreItems(g)
                    val gSeen = mutableSetOf<String>()
                    genreMap[g] = items.filter { item ->
                        val title = item.showTitle ?: item.title
                        if (gSeen.contains(title)) return@filter false
                        gSeen.add(title)
                        true
                    }.take(15)
                } catch (_: Exception) {}
            }

            recentlyAdded = dedupedRecent
            libraries = libs
            libraryItems = itemsMap
            genres = gens.take(5)
            genreItems = genreMap
            featured = dedupedRecent.firstOrNull()
        } catch (_: Exception) {}
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Hero(
                    item = featured,
                    baseUrl = baseUrl,
                    onPlay = { featured?.let { onPlayItem(it) } }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                MediaRow(
                    title = "Recently Added",
                    items = recentlyAdded,
                    baseUrl = baseUrl,
                    onClick = { item -> onSelectItem?.invoke(item) ?: onPlayItem(item) }
                )
            }

            // Collections
            val collectionGroups = recentlyAdded
                .filter { it.collectionName != null }
                .groupBy { it.collectionName!! }
                .filter { it.value.size > 1 }

            collectionGroups.forEach { (name, items) ->
                item {
                    MediaRow(
                        title = name,
                        items = items,
                        baseUrl = baseUrl,
                        onClick = { item -> onSelectItem?.invoke(item) ?: onPlayItem(item) }
                    )
                }
            }

            genres.forEach { genre ->
                val items = genreItems[genre].orEmpty()
                if (items.isNotEmpty()) {
                    item {
                        MediaRow(
                            title = genre,
                            items = items,
                            baseUrl = baseUrl,
                            onClick = { item -> onSelectItem?.invoke(item) ?: onPlayItem(item) }
                        )
                    }
                }
            }

            libraries.forEach { lib ->
                val allItems = libraryItems[lib.id].orEmpty()
                if (allItems.isNotEmpty()) {
                    item {
                        MediaRow(
                            title = lib.name,
                            items = allItems,
                            baseUrl = baseUrl,
                            onClick = { item -> onSelectItem?.invoke(item) ?: onPlayItem(item) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }

        // Top Search Icon
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(20f)
        ) {
            SunsetIconButton(
                icon = SunsetIcons.Search,
                onClick = onSearch,
                backgroundColor = Color.Black.copy(alpha = 0.4f),
                size = 28.dp
            )
        }
    }
}
