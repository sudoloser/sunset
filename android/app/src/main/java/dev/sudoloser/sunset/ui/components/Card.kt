package dev.sudoloser.sunset.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SunsetCard(
    modifier: Modifier = Modifier,
    glass: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val backgroundColor = if (glass) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val mod = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape)
        .padding(16.dp)

    if (onClick != null) {
        Box(modifier = mod.clickable(onClick = onClick)) {
            content()
        }
    } else {
        Box(modifier = mod) {
            content()
        }
    }
}
