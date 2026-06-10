package dev.sudoloser.sunset.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sudoloser.sunset.ui.theme.NetflixRed

enum class ButtonVariant {
    Primary, Secondary, Outline, Danger, Ghost
}

@Composable
fun SunsetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary
) {
    val (bg, fg) = when (variant) {
        ButtonVariant.Primary -> NetflixRed to Color.White
        ButtonVariant.Secondary -> Color(0xFF333333) to Color.White
        ButtonVariant.Outline -> Color.Transparent to Color.White
        ButtonVariant.Danger -> Color(0xFFC62828) to Color.White
        ButtonVariant.Ghost -> Color.Transparent to Color.White
    }
    val border = when (variant) {
        ButtonVariant.Outline -> BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        else -> null
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        border = border,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SunsetIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = Color.White,
    backgroundColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}
