package dev.sudoloser.sunset.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.sudoloser.sunset.data.models.MediaItem

@Composable
fun Hero(
    item: MediaItem?,
    baseUrl: String,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    val backdropUrl = "$baseUrl/api/media/${item.id}/asset/backdrop.jpg"
    val logoUrl = "$baseUrl/api/media/${item.id}/asset/logo.png"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(420.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = 1400f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = logoUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(240.dp)
                    .padding(bottom = 16.dp)
            )

            if (item.genres != null) {
                Text(
                    text = item.genres.split(",").take(3).joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            } else if (item.year != null) {
                Text(
                    text = item.year.toString(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SunsetButton(
                    text = "Play Now",
                    onClick = onPlay,
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
