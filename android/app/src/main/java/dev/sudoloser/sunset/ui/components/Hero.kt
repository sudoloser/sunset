package dev.sudoloser.sunset.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import dev.sudoloser.sunset.ui.theme.NetflixRed

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
            .height(320.dp)
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
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black
                        ),
                        startY = 0f,
                        endY = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            AsyncImage(
                model = logoUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(200.dp)
                    .padding(bottom = 12.dp)
            )

            if (item.year != null) {
                Text(
                    text = item.year.toString(),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SunsetButton(
                    text = "Play",
                    onClick = onPlay,
                    variant = ButtonVariant.Primary
                )
            }
        }
    }
}
