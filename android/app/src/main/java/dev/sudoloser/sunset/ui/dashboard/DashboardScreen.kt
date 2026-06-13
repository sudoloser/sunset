package dev.sudoloser.sunset.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sudoloser.sunset.api.ApiClient
import android.util.Log
import dev.sudoloser.sunset.data.models.Library
import dev.sudoloser.sunset.data.models.LibraryType
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    apiClient: ApiClient,
    baseUrl: String,
    userId: String? = null,
    onPlayItem: (MediaItem) -> Unit,
    onSearch: () -> Unit,
    onSelectItem: ((MediaItem) -> Unit)? = null
) {
    var recentlyAdded by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var continueWatching by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var libraries by remember { mutableStateOf<List<Library>>(emptyList()) }
    var libraryItems by remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var genreItems by remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    var collections by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var featured by remember { mutableStateOf<MediaItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()

    suspend fun loadData() {
        try {
            val recentData = apiClient.getRecentlyAdded(userId)
            val libs = apiClient.getLibraries()
            val continueData = if (userId != null) apiClient.getContinueWatching(userId) else emptyList()
            
            fun dedupe(items: List<MediaItem>): List<MediaItem> {
                val seen = mutableSetOf<String>()
                return items.map { item ->
                    if (item.mediaType.name == "EPISODE" && item.showTitle != null) {
                        item.copy(title = item.showTitle)
                    } else item
                }.filter { item ->
                    val title = item.showTitle ?: item.title
                    if (seen.contains(title)) return@filter false
                    seen.add(title)
                    true
                }
            }

            val dedupedRecent = dedupe(recentData)
            val dedupedCW = dedupe(continueData)

            val itemsMap = mutableMapOf<String, List<MediaItem>>()
            libs.forEach { lib ->
                try { 
                    val items = apiClient.getLibraryItems(lib.id, userId)
                    itemsMap[lib.id] = dedupe(items).take(15)
                } catch (e: Exception) { Log.e("SunSet", "Failed to load library items", e) }
            }

            val gens = apiClient.getGenres()
            val genreMap = mutableMapOf<String, List<MediaItem>>()
            gens.take(5).forEach { g ->
                try { 
                    val items = apiClient.getGenreItems(g, userId)
                    genreMap[g] = dedupe(items).take(15)
                } catch (e: Exception) { Log.e("SunSet", "Failed to load library items", e) }
            }

            recentlyAdded = dedupedRecent
            continueWatching = dedupedCW
            libraries = libs
            libraryItems = itemsMap
            genres = gens.take(5)
            genreItems = genreMap
            featured = dedupedRecent.firstOrNull()
        } catch (e: Exception) { Log.e("SunSet", "Failed to load dashboard data", e) }
        loading = false
    }

    LaunchedEffect(Unit) { loadData() }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                loadData()
                isRefreshing = false
            }
        },
        state = refreshState,
        modifier = Modifier.fillMaxSize()
    ) {
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

            if (continueWatching.isNotEmpty()) {
                item {
                    MediaRow(
                        title = "Continue Watching",
                        items = continueWatching,
                        baseUrl = baseUrl,
                        onClick = { item -> onSelectItem?.invoke(item) ?: onPlayItem(item) }
                    )
                }
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
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                size = 28.dp
            )
        }
    }
}
}
