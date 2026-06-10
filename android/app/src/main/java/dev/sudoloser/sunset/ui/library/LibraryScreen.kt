package dev.sudoloser.sunset.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.sudoloser.sunset.data.models.PlaybackState
import dev.sudoloser.sunset.ui.components.*

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

    LaunchedEffect(Unit) {
        try {
            libraries = apiClient.getLibraries()
            if (userId != null) {
                continueWatching = apiClient.getContinueWatching(userId)
                myListItems = apiClient.getUserItems(userId)
                val progress = mutableMapOf<String, Float>()
                for (item in continueWatching) {
                    try {
                        val state = apiClient.getPlayback(item.id)
                        if (state.duration != null && state.duration > 0) {
                            progress[item.id] = (state.timestamp.toFloat() / state.duration).coerceIn(0f, 1f)
                        }
                    } catch (_: Exception) {}
                }
                playbackProgress = progress
            }
        } catch (_: Exception) {}
        loading = false
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
            onBack = { selectedLibrary = null }
        )
        return
    }

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

        if (continueWatching.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            MediaRow(
                title = "Continue Watching",
                items = continueWatching,
                baseUrl = baseUrl,
                onPlay = onPlayItem,
                getProgress = { playbackProgress[it.id] ?: 0f }
            )
        }

        if (myListItems.isNotEmpty()) {
            MediaRow(
                title = "My List",
                items = myListItems,
                baseUrl = baseUrl,
                onPlay = onPlayItem
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

        Spacer(Modifier.height(80.dp))
    }
}
