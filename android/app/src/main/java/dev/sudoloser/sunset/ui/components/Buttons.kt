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
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    fullWidth: Boolean = false
) {
    val (bg, fg) = when (variant) {
        ButtonVariant.Primary -> NetflixRed to Color.White
        ButtonVariant.Secondary -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
        ButtonVariant.Outline -> Color.Transparent to MaterialTheme.colorScheme.onSurface
        ButtonVariant.Danger -> Color(0xFFC62828) to Color.White
        ButtonVariant.Ghost -> Color.Transparent to MaterialTheme.colorScheme.onSurface
    }
    val border = when (variant) {
        ButtonVariant.Outline -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        else -> null
    }
    val widthModifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier
    val shape = if (variant == ButtonVariant.Primary) RoundedCornerShape(8.dp) else RoundedCornerShape(12.dp)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(widthModifier).height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        border = border,
        shape = shape,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            letterSpacing = 0.sp
        )
    }
}

@Composable
fun SunsetIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
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
