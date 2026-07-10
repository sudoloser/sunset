package dev.sudoloser.sunset.ui.downloads

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.PrefKeys
import dev.sudoloser.sunset.data.dataStore
import dev.sudoloser.sunset.ui.components.SunsetIcons
import dev.sudoloser.sunset.ui.components.SunsetIconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DownloadInfo(
    val id: Long,
    val itemId: String,
    val showTitle: String,
    val seasonNum: Int,
    val episodeNum: Int,
    val title: String,
    val status: Int,
    val progress: Float,
    val localUri: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    apiClient: ApiClient,
    baseUrl: String,
    onClose: () -> Unit,
    onPlayLocal: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloads by remember { mutableStateOf<List<DownloadInfo>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            refreshTrigger++
        }
    }

    LaunchedEffect(Unit, refreshTrigger) {
        records = ctx.dataStore.data.first()[PrefKeys.DOWNLOAD_RECORDS] ?: emptySet()
        downloads = withContext(Dispatchers.IO) {
            records.mapNotNull { entry ->
                val parts = entry.split("|")
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(id)
                var info: DownloadInfo? = null
                dm.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val progress = if (total > 0) downloaded.toFloat() / total else 0f
                        val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val localUri = if (uriIdx >= 0) cursor.getString(uriIdx) else null

                        if (parts.size >= 6) {
                            info = DownloadInfo(id, parts[1], parts[2], parts[3].toIntOrNull() ?: 0, parts[4].toIntOrNull() ?: 0, parts[5], status, progress, localUri)
                        } else if (parts.size >= 5) {
                            info = DownloadInfo(id, parts[1], parts[2], 0, parts[3].toIntOrNull() ?: 0, parts[4], status, progress, localUri)
                        } else {
                            info = DownloadInfo(id, parts.getOrElse(2) { "" }, "", 0, 0, parts[1], status, progress, localUri)
                        }
                    }
                }
                info
            }.sortedByDescending { it.id }
        }
    }

    val filteredDownloads = remember(downloads, searchQuery) {
        if (searchQuery.isBlank()) downloads
        else downloads.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.showTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedDownloads = remember(filteredDownloads) {
        filteredDownloads.groupBy { it.showTitle.ifBlank { it.title } }.toSortedMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(SunsetIcons.Back, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search downloads...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                ),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(SunsetIcons.Close, contentDescription = "Clear")
                        }
                    }
                }
            )

            Spacer(Modifier.height(4.dp))

            if (filteredDownloads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isNotBlank()) "No matching downloads" else "No downloads yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedDownloads.forEach { (groupName, items) ->
                        item(key = "header_$groupName") {
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                        items(items, key = { it.id }) { dl ->
                            DownloadItem(
                                info = dl,
                                onRemove = {
                                    scope.launch {
                                        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                        dm.remove(dl.id)
                                        ctx.dataStore.edit { prefs ->
                                            val currentRecords = prefs[PrefKeys.DOWNLOAD_RECORDS] ?: emptySet()
                                            prefs[PrefKeys.DOWNLOAD_RECORDS] = currentRecords.filterNot { it.startsWith("${dl.id}|") }.toSet()
                                        }
                                        refreshTrigger++
                                    }
                                },
                                onRetry = {
                                    scope.launch {
                                        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                        dm.remove(dl.id)
                                        val token = apiClient.generateMediaToken(dl.itemId)
                                        val url = apiClient.getDownloadUrl(dl.itemId, token)
                                        val request = DownloadManager.Request(Uri.parse(url))
                                            .setTitle(dl.title)
                                            .setDescription("Downloading ${dl.title}")
                                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            .setAllowedOverMetered(true)
                                            .setAllowedOverRoaming(true)
                                        val newId = dm.enqueue(request)
                                        ctx.dataStore.edit { prefs ->
                                            val currentRecords = prefs[PrefKeys.DOWNLOAD_RECORDS] ?: emptySet()
                                            val updated = currentRecords.filterNot { it.startsWith("${dl.id}|") }.toMutableSet()
                                            updated.add("$newId|${dl.itemId}|${dl.showTitle}|${dl.seasonNum}|${dl.episodeNum}|${dl.title}")
                                            prefs[PrefKeys.DOWNLOAD_RECORDS] = updated
                                        }
                                        refreshTrigger++
                                    }
                                },
                                onPlay = {
                                    dl.localUri?.let { uri ->
                                        val displayTitle = if (dl.showTitle.isNotBlank()) "${dl.showTitle} - S%02dE%02d".format(dl.seasonNum, dl.episodeNum) else dl.title
                                        onPlayLocal(uri, displayTitle, dl.itemId)
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    info: DownloadInfo,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
    onPlay: () -> Unit
) {
    val statusText = when (info.status) {
        DownloadManager.STATUS_PENDING -> "Pending"
        DownloadManager.STATUS_RUNNING -> "Downloading"
        DownloadManager.STATUS_PAUSED -> "Paused"
        DownloadManager.STATUS_SUCCESSFUL -> "Completed"
        DownloadManager.STATUS_FAILED -> "Failed"
        else -> "Unknown"
    }

    val displayTitle = if (info.showTitle.isNotBlank()) "${info.showTitle} - S%02dE%02d".format(info.seasonNum, info.episodeNum) else info.title

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayTitle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    statusText,
                    fontSize = 13.sp,
                    color = when (info.status) {
                        DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.error
                        DownloadManager.STATUS_SUCCESSFUL -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (info.status == DownloadManager.STATUS_RUNNING || info.status == DownloadManager.STATUS_PENDING || info.status == DownloadManager.STATUS_PAUSED) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { info.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${(info.progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (info.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    IconButton(onClick = onPlay) {
                        Icon(SunsetIcons.Play, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onRemove) {
                        Icon(SunsetIcons.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DownloadManager.STATUS_FAILED -> {
                    IconButton(onClick = onRetry) {
                        Icon(SunsetIcons.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                else -> {
                    IconButton(onClick = onRemove) {
                        Icon(SunsetIcons.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
