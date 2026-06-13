package dev.sudoloser.sunset.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sudoloser.sunset.api.ApiClient
import android.util.Log
import dev.sudoloser.sunset.data.models.Library
import dev.sudoloser.sunset.data.models.LibraryType
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.data.models.MediaType
import dev.sudoloser.sunset.data.models.PlaybackState
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrariesScreen(
    apiClient: ApiClient,
    baseUrl: String,
    userId: String?,
    isAdmin: Boolean,
    onPlayItem: (MediaItem) -> Unit,
    onSelectItem: (MediaItem) -> Unit,
    onGoToSettings: () -> Unit
) {
    var libraries by remember { mutableStateOf<List<Library>>(emptyList()) }
    var continueWatching by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var myListItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var playbackProgress by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var selectedLibrary by remember { mutableStateOf<Library?>(null) }
    var loading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var filterType by remember { mutableStateOf("all") }
    var filterGenre by remember { mutableStateOf<String?>(null) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }

    suspend fun loadData() {
        try {
            libraries = apiClient.getLibraries()
            if (userId != null) {
                val rawContinueWatching = apiClient.getContinueWatching(userId)
                
                fun dedupe(items: List<MediaItem>): List<MediaItem> {
                    val seen = mutableSetOf<String>()
                    return items.map { item ->
                        if (item.mediaType == MediaType.EPISODE && item.showTitle != null) {
                            item.copy(title = item.showTitle)
                        } else item
                    }.filter { item ->
                        val title = item.showTitle ?: item.title
                        if (seen.contains(title)) return@filter false
                        seen.add(title)
                        true
                    }
                }
                
                continueWatching = dedupe(rawContinueWatching)
                myListItems = apiClient.getUserItems(userId)
                val progress = mutableMapOf<String, Float>()
                for (item in continueWatching) {
                    try {
                        val state = apiClient.getPlayback(item.id)
                        if (state.duration != null && state.duration > 0) {
                            progress[item.id] = (state.timestamp.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
                        }
                    } catch (e: Exception) { Log.e("SunSet", "Failed to load playback", e) }
                }
                playbackProgress = progress
            }

            // Load all items and genres for search
            val libs = apiClient.getLibraries()
            val items = mutableListOf<MediaItem>()
            libs.forEach { lib ->
                try { items.addAll(apiClient.getLibraryItems(lib.id, userId)) } catch (_: Exception) {}
            }
            allItems = items
            genres = try { apiClient.getGenres() } catch (_: Exception) { emptyList() }
        } catch (e: Exception) { Log.e("SunSet", "Failed to load library data", e) }
        loading = false
    }

    LaunchedEffect(Unit) { loadData() }

    fun applyFilters(items: List<MediaItem>): List<MediaItem> {
        return items.filter { item ->
            val typeMatch = filterType == "all" || item.mediaType.name == filterType
            val genreMatch = filterGenre == null || item.genres?.contains(filterGenre!!, ignoreCase = true) == true
            typeMatch && genreMatch
        }
    }

    fun groupEpisodes(items: List<MediaItem>): List<Pair<MediaItem, Int>> {
        val grouped = mutableMapOf<String, MutableList<MediaItem>>()
        items.forEach { item ->
            if (item.mediaType == MediaType.EPISODE && item.showTitle != null) {
                grouped.getOrPut(item.showTitle!!) { mutableListOf() }.add(item)
            } else {
                grouped[item.title] = mutableListOf(item)
            }
        }
        return grouped.map { (key, eps) ->
            val rep = eps.minByOrNull { it.season ?: 0 }?.copy(title = key) ?: eps.first().copy(title = key)
            rep to eps.size
        }.sortedByDescending { it.second }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (selectedLibrary != null) {
        LibraryViewScreen(
            library = selectedLibrary!!,
            apiClient = apiClient,
            baseUrl = baseUrl,
            onPlayItem = onPlayItem,
            onBack = { selectedLibrary = null },
            onSelectItem = onSelectItem
        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "My Library",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))

            // Search Bar
            SunsetInput(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    isSearchActive = q.isNotBlank()
                    searchJob?.cancel()
                    if (q.isNotBlank()) {
                        searchJob = scope.launch {
                            delay(300)
                            try {
                                val resultsData = apiClient.search(q)
                                val seen = mutableSetOf<String>()
                                searchResults = resultsData.filter { item ->
                                    val title = item.showTitle ?: item.title
                                    if (seen.contains(title)) return@filter false
                                    seen.add(title)
                                    true
                                }
                            } catch (e: Exception) { Log.e("SunSet", "Search failed", e) }
                        }
                    } else {
                        searchResults = emptyList()
                        isSearchActive = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = "Search movies, shows..."
            )

            // Filter Chips - only show when search is active
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(300))
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        FilterChip(selected = filterType == "all", onClick = { filterType = "all" }, label = { Text("All") })
                        FilterChip(selected = filterType == "MOVIE", onClick = { filterType = "MOVIE" }, label = { Text("Movies") })
                        FilterChip(selected = filterType == "EPISODE", onClick = { filterType = "EPISODE" }, label = { Text("Shows") })
                    }
                    if (genres.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            FilterChip(selected = filterGenre == null, onClick = { filterGenre = null }, label = { Text("All Genres") })
                            genres.take(10).forEach { genre ->
                                FilterChip(
                                    selected = filterGenre == genre,
                                    onClick = { filterGenre = if (filterGenre == genre) null else genre },
                                    label = { Text(genre) }
                                )
                            }
                        }
                    }
                }
            }

            // Search Results - slide down when active
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically(tween(400)) + fadeIn(tween(400)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(300))
            ) {
                val filtered = applyFilters(searchResults)
                if (searchQuery.isNotBlank() && filtered.isNotEmpty()) {
                    val grouped = if (filterType == "EPISODE") groupEpisodes(filtered) else filtered.map { it to 0 }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false
                    ) {
                        items(grouped, key = { it.first.id }) { (item, count) ->
                            Poster(
                                item = item,
                                baseUrl = baseUrl,
                                onClick = { onSelectItem(item) },
                                subtitle = if (count > 1) "$count episodes" else null
                            )
                        }
                    }
                } else if (searchQuery.isNotBlank() && filtered.isEmpty()) {
                    Text(
                        "No results found",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSearchActive) {
                // Normal library content
                if (continueWatching.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    MediaRow(
                        title = "Continue Watching",
                        items = continueWatching,
                        baseUrl = baseUrl,
                        onClick = { item -> onSelectItem(item) },
                        getProgress = { playbackProgress[it.id] ?: 0f }
                    )
                }

                if (myListItems.isNotEmpty()) {
                    MediaRow(
                        title = "My List",
                        items = myListItems,
                        baseUrl = baseUrl,
                        onClick = { item -> onSelectItem(item) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Libraries",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (libraries.isEmpty()) {
                    if (isAdmin) {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No libraries yet. Add some in Settings!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = onGoToSettings) { Text("Go to Settings") }
                            }
                        }
                    } else {
                        Text(
                            text = "No libraries available",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    libraries.forEach { lib ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            onClick = { selectedLibrary = lib }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lib.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        lib.path,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (lib.libType == LibraryType.MOVIES) "Movies" else "Shows",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
