package dev.sudoloser.sunset.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sudoloser.sunset.api.ApiClient
import android.util.Log
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.data.models.MediaType
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    apiClient: ApiClient,
    baseUrl: String,
    onSelect: (MediaItem) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    var filterType by remember { mutableStateOf("all") }
    var filterGenre by remember { mutableStateOf<String?>(null) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val libs = apiClient.getLibraries()
            val items = mutableListOf<MediaItem>()
            libs.forEach { lib ->
                try { items.addAll(apiClient.getLibraryItems(lib.id)) } catch (e: Exception) { Log.e("SunSet", "Failed to load library items", e) }
            }
            allItems = items
            genres = try { apiClient.getGenres() } catch (_: Exception) { emptyList() }
        } catch (e: Exception) { Log.e("SunSet", "Search failed", e) }
    }

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
                val key = item.showTitle!!
                grouped.getOrPut(key) { mutableListOf() }.add(item)
            } else {
                grouped[item.title] = mutableListOf(item)
            }
        }
        return grouped.map { (key, eps) ->
            val representative = eps.minByOrNull { it.season ?: 0 }?.copy(title = key) ?: eps.first().copy(title = key)
            representative to eps.size
        }.sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SunsetInput(
                value = query,
                onValueChange = { q ->
                    query = q
                    searchJob?.cancel()
                    if (q.isNotBlank()) {
                        searchJob = scope.launch {
                            delay(300)
                            try {
                                val resultsData = apiClient.search(q)
                                val seen = mutableSetOf<String>()
                                results = resultsData.filter { item ->
                                    val title = item.showTitle ?: item.title
                                    if (seen.contains(title)) return@filter false
                                    seen.add(title)
                                    true
                                }
                            } catch (e: Exception) { Log.e("SunSet", "Search failed", e) }
                        }
                    } else {
                        results = emptyList()
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = "Search movies, shows..."
            )
            Spacer(Modifier.width(8.dp))
            SunsetButton(text = "Cancel", onClick = onClose, variant = ButtonVariant.Ghost)
        }

        Spacer(Modifier.height(12.dp))

        // Filter Chips - Type
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = filterType == "all",
                onClick = { filterType = "all" },
                label = { Text("All") }
            )
            FilterChip(
                selected = filterType == "MOVIE",
                onClick = { filterType = "MOVIE" },
                label = { Text("Movies") }
            )
            FilterChip(
                selected = filterType == "EPISODE",
                onClick = { filterType = "EPISODE" },
                label = { Text("Shows") }
            )
        }

        // Genre Filter
        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = filterGenre == null,
                    onClick = { filterGenre = null },
                    label = { Text("All Genres") }
                )
                genres.take(10).forEach { genre ->
                    FilterChip(
                        selected = filterGenre == genre,
                        onClick = { filterGenre = if (filterGenre == genre) null else genre },
                        label = { Text(genre) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (query.isBlank()) {
            val filtered = applyFilters(allItems)
            if (filtered.isNotEmpty()) {
                val grouped = if (filterType == "EPISODE") groupEpisodes(filtered) else filtered.map { it to 0 }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(grouped.take(24), key = { it.first.id }) { (item, count) ->
                        Poster(
                            item = item,
                            baseUrl = baseUrl,
                            onClick = { onSelect(item) },
                            subtitle = if (count > 1) "$count episodes" else null
                        )
                    }
                }
            }
        } else {
            val filtered = applyFilters(results)
            if (filtered.isEmpty() && results.isNotEmpty()) {
                Text("No results match filters", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (results.isEmpty()) {
                Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val grouped = if (filterType == "EPISODE") groupEpisodes(filtered) else filtered.map { it to 0 }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(grouped, key = { it.first.id }) { (item, count) ->
                        Poster(
                            item = item,
                            baseUrl = baseUrl,
                            onClick = { onSelect(item) },
                            subtitle = if (count > 1) "$count episodes" else null
                        )
                    }
                }
            }
        }
    }
}
