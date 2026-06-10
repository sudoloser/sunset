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
    onBack: () -> Unit,
    onSelectItem: ((MediaItem) -> Unit)? = null
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SunsetIconButton(
                icon = SunsetIcons.Back,
                onClick = onBack,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            Text(
                text = library.name,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        val columnCount = adaptiveGridColumns()
        if (library.libType == LibraryType.MOVIES) {
            LazyVerticalGrid(
                columns = columnCount,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(items) { item ->
                    Poster(item = item, baseUrl = baseUrl, onClick = { onSelectItem?.invoke(item) ?: onPlayItem(item) })
                }
            }
        } else {
            val grouped = items.groupBy { it.showTitle ?: it.title }
            LazyVerticalGrid(
                columns = columnCount,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                grouped.forEach { (showTitle, eps) ->
                    item {
                        Poster(
                            item = eps.first(),
                            baseUrl = baseUrl,
                            onClick = { onSelectItem?.invoke(eps.first()) ?: onPlayItem(eps.first()) },
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
