package dev.sudoloser.sunset.ui.downloads

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.sudoloser.sunset.api.ApiClient
import dev.sudoloser.sunset.data.PrefKeys
import dev.sudoloser.sunset.data.dataStore
import dev.sudoloser.sunset.ui.components.SunsetIcons
import dev.sudoloser.sunset.ui.components.SunsetIconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DownloadInfo(
    val id: Long,
    val title: String,
    val itemId: String,
    val status: Int,
    val progress: Float,
    val localUri: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    apiClient: ApiClient,
    baseUrl: String,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloads by remember { mutableStateOf<List<DownloadInfo>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit, refreshTrigger) {
        records = ctx.dataStore.data.first()[PrefKeys.DOWNLOAD_RECORDS] ?: emptySet()
        downloads = withContext(Dispatchers.IO) {
            records.mapNotNull { entry ->
                val parts = entry.split("|", limit = 3)
                if (parts.size != 3) return@mapNotNull null
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
                        info = DownloadInfo(id, parts[1], parts[2], status, progress, localUri)
                    }
                }
                info
            }.sortedByDescending { it.id }
        }
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
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No downloads yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloads, key = { it.id }) { dl ->
                    DownloadItem(
                        info = dl,
                        onRemove = {
                            scope.launch {
                                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                dm.remove(dl.id)
                                ctx.dataStore.edit { prefs ->
                                    val set = prefs[PrefKeys.DOWNLOAD_RECORDS]?.toMutableSet() ?: mutableSetOf()
                                    set.remove("${dl.id}|${dl.title}|${dl.itemId}")
                                    prefs[PrefKeys.DOWNLOAD_RECORDS] = set
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
                                    val set = prefs[PrefKeys.DOWNLOAD_RECORDS]?.toMutableSet() ?: mutableSetOf()
                                    set.remove("${dl.id}|${dl.title}|${dl.itemId}")
                                    set.add("$newId|${dl.title}|${dl.itemId}")
                                    prefs[PrefKeys.DOWNLOAD_RECORDS] = set
                                }
                                refreshTrigger++
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    info: DownloadInfo,
    onRemove: () -> Unit,
    onRetry: () -> Unit
) {
    val statusText = when (info.status) {
        DownloadManager.STATUS_PENDING -> "Pending"
        DownloadManager.STATUS_RUNNING -> "Downloading"
        DownloadManager.STATUS_PAUSED -> "Paused"
        DownloadManager.STATUS_SUCCESSFUL -> "Completed"
        DownloadManager.STATUS_FAILED -> "Failed"
        else -> "Unknown"
    }

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
                    info.title,
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
                DownloadManager.STATUS_FAILED -> {
                    IconButton(onClick = onRetry) {
                        Icon(SunsetIcons.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = SunsetIcons.Check,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
