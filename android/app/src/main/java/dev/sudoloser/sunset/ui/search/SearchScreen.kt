package dev.sudoloser.sunset.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.models.MediaItem
import dev.sudoloser.sunset.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    apiClient: ApiClient,
    baseUrl: String,
    onSelect: (MediaItem) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var libraryItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        try {
            val libs = apiClient.getLibraries()
            val allItems = mutableListOf<MediaItem>()
            libs.forEach { lib ->
                try { allItems.addAll(apiClient.getLibraryItems(lib.id)) } catch (_: Exception) {}
            }
            libraryItems = allItems
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.95f))
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
                            } catch (_: Exception) {}
                        }                    } else {
                        results = emptyList()
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = "Search movies, shows..."
            )
            Spacer(Modifier.width(8.dp))
            SunsetButton(text = "Cancel", onClick = onClose, variant = ButtonVariant.Ghost)
        }

        Spacer(Modifier.height(16.dp))

        if (query.isBlank()) {
            if (libraryItems.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(libraryItems.take(24)) { item ->
                        Poster(
                            item = item,
                            baseUrl = baseUrl,
                            onClick = { onSelect(item) }
                        )
                    }
                }
            }
        } else {
            if (results.isEmpty()) {
                Text("No results found", color = androidx.compose.ui.graphics.Color.Gray)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { item ->
                        Poster(
                            item = item,
                            baseUrl = baseUrl,
                            onClick = { onSelect(item) }
                        )
                    }
                }
            }
        }
    }
}
