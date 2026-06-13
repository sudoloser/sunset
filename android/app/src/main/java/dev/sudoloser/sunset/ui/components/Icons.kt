package dev.sudoloser.sunset.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object SunsetIcons {
    val Play: ImageVector = ImageVector.Builder("Play", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close()
        }
    }.build()

    val Pause: ImageVector = ImageVector.Builder("Pause", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 4f); horizontalLineToRelative(4f); verticalLineToRelative(16f); horizontalLineToRelative(-4f); close()
            moveTo(14f, 4f); horizontalLineToRelative(4f); verticalLineToRelative(16f); horizontalLineToRelative(-4f); close()
        }
    }.build()

    val Home: ImageVector = ImageVector.Builder("Home", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10f, 20f); verticalLineToRelative(-6f); horizontalLineToRelative(4f); verticalLineToRelative(6f); horizontalLineToRelative(5f); verticalLineToRelative(-8f); horizontalLineToRelative(3f); lineTo(12f, 3f); lineTo(2f, 12f); horizontalLineToRelative(3f); verticalLineToRelative(8f); close()
        }
    }.build()

    val Library: ImageVector = ImageVector.Builder("Library", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 6f); horizontalLineToRelative(2f); verticalLineToRelative(14f); horizontalLineToRelative(-2f); close()
            moveTo(8f, 4f); horizontalLineToRelative(2f); verticalLineToRelative(16f); horizontalLineToRelative(-2f); close()
            moveTo(12f, 7f); horizontalLineToRelative(2f); verticalLineToRelative(13f); horizontalLineToRelative(-2f); close()
            moveTo(16f, 2f); horizontalLineToRelative(2f); verticalLineToRelative(18f); horizontalLineToRelative(-2f); close()
        }
    }.build()

    val Search: ImageVector = ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(11f, 19f); arcTo(8f, 8f, 0f, true, false, 11f, 3f); arcTo(8f, 8f, 0f, false, false, 11f, 19f); close()
            moveTo(21f, 21f); lineTo(16.65f, 16.65f)
        }
    }.build()

    val Settings: ImageVector = ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19.14f, 12.94f); arcTo(1f, 1f, 0f, false, false, 20f, 12f); arcTo(1f, 1f, 0f, false, false, 19.14f, 11.06f); lineTo(21f, 9.54f); arcTo(1f, 1f, 0f, false, false, 21f, 8.13f); lineTo(18.87f, 6.02f); arcTo(1f, 1f, 0f, false, false, 17.46f, 6.02f); lineTo(15.94f, 7.86f); arcTo(1f, 1f, 0f, false, false, 15f, 7f); arcTo(1f, 1f, 0f, false, false, 14.06f, 7.86f); lineTo(12.54f, 6.02f); arcTo(1f, 1f, 0f, false, false, 11.13f, 6.02f); lineTo(9.02f, 8.13f); arcTo(1f, 1f, 0f, false, false, 9.02f, 9.54f); lineTo(10.86f, 11.06f); arcTo(1f, 1f, 0f, false, false, 10f, 12f); arcTo(1f, 1f, 0f, false, false, 10.86f, 12.94f); lineTo(9.02f, 14.46f); arcTo(1f, 1f, 0f, false, false, 9.02f, 15.87f); lineTo(11.13f, 17.98f); arcTo(1f, 1f, 0f, false, false, 12.54f, 17.98f); lineTo(14.06f, 16.14f); arcTo(1f, 1f, 0f, false, false, 15f, 17f); arcTo(1f, 1f, 0f, false, false, 15.94f, 16.14f); lineTo(17.46f, 17.98f); arcTo(1f, 1f, 0f, false, false, 18.87f, 17.98f); lineTo(21f, 15.87f); arcTo(1f, 1f, 0f, false, false, 21f, 14.46f); close()
            moveTo(15f, 15f); arcTo(3f, 3f, 0f, true, true, 15f, 9f); arcTo(3f, 3f, 0f, false, true, 15f, 15f); close()
        }
    }.build()

    val Subtitles: ImageVector = ImageVector.Builder("Subtitles", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 4f); horizontalLineToRelative(-16f); arcTo(2f, 2f, 0f, false, false, 2f, 6f); verticalLineToRelative(12f); arcTo(2f, 2f, 0f, false, false, 4f, 20f); horizontalLineToRelative(16f); arcTo(2f, 2f, 0f, false, false, 22f, 18f); verticalLineToRelative(-12f); arcTo(2f, 2f, 0f, false, false, 20f, 4f); close()
            moveTo(7f, 10f); horizontalLineToRelative(4f); moveTo(7f, 14f); horizontalLineToRelative(2f); moveTo(13f, 14f); horizontalLineToRelative(4f); moveTo(17f, 10f); horizontalLineToRelative(-2f)
        }
    }.build()

    val SkipBack: ImageVector = ImageVector.Builder("SkipBack", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(11f, 18f); verticalLineToRelative(-12f); lineTo(2.5f, 12f); close()
            moveTo(21.5f, 18f); verticalLineToRelative(-12f); lineTo(13f, 12f); close()
        }
    }.build()

    val SkipForward: ImageVector = ImageVector.Builder("SkipForward", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(2.5f, 18f); verticalLineToRelative(-12f); lineTo(11f, 12f); close()
            moveTo(13f, 18f); verticalLineToRelative(-12f); lineTo(21.5f, 12f); close()
        }
    }.build()

    val Back: ImageVector = ImageVector.Builder("Back", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 12f); horizontalLineToRelative(-14f); moveTo(12f, 19f); lineTo(5f, 12f); lineTo(12f, 5f)
        }
    }.build()

    val Plus: ImageVector = ImageVector.Builder("Plus", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 5f); verticalLineToRelative(14f); moveTo(5f, 12f); horizontalLineToRelative(14f)
        }
    }.build()

    val Refresh: ImageVector = ImageVector.Builder("Refresh", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(23f, 4f); verticalLineToRelative(6f); horizontalLineToRelative(-6f); moveTo(1f, 20f); verticalLineToRelative(-6f); horizontalLineToRelative(6f);
            moveTo(3.51f, 9f); arcTo(9f, 9f, 0f, false, true, 21f, 12f); arcTo(9f, 9f, 0f, false, true, 20.49f, 15f);
            moveTo(20.49f, 15f); arcTo(9f, 9f, 0f, false, true, 3f, 12f); arcTo(9f, 9f, 0f, false, true, 3.51f, 9f);
        }
    }.build()

    val Episodes: ImageVector = ImageVector.Builder("Episodes", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 6f); horizontalLineToRelative(6f); verticalLineToRelative(6f); horizontalLineToRelative(-6f); close()
            moveTo(14f, 6f); horizontalLineToRelative(6f); verticalLineToRelative(6f); horizontalLineToRelative(-6f); close()
            moveTo(4f, 14f); horizontalLineToRelative(6f); verticalLineToRelative(6f); horizontalLineToRelative(-6f); close()
            moveTo(14f, 14f); horizontalLineToRelative(6f); verticalLineToRelative(6f); horizontalLineToRelative(-6f); close()
        }
    }.build()

    val MoreVertical: ImageVector = ImageVector.Builder("MoreVertical", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 8f); arcTo(2f, 2f, 0f, true, false, 12f, 4f); arcTo(2f, 2f, 0f, false, false, 12f, 8f); close()
            moveTo(12f, 14f); arcTo(2f, 2f, 0f, true, false, 12f, 10f); arcTo(2f, 2f, 0f, false, false, 12f, 14f); close()
            moveTo(12f, 20f); arcTo(2f, 2f, 0f, true, false, 12f, 16f); arcTo(2f, 2f, 0f, false, false, 12f, 20f); close()
        }
    }.build()

    val Download: ImageVector = ImageVector.Builder("Download", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(21f, 15f); verticalLineToRelative(4f); arcTo(2f, 2f, 0f, false, true, 19f, 21f); horizontalLineToRelative(-14f); arcTo(2f, 2f, 0f, false, true, 3f, 19f); verticalLineToRelative(-4f);
            moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f); moveTo(12f, 15f); verticalLineToRelative(-12f);
        }
    }.build()

    val Star: ImageVector = ImageVector.Builder("Star", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 17.27f); lineTo(18.18f, 21f); lineTo(16.54f, 13.97f); lineTo(22f, 9.24f); lineTo(14.81f, 8.63f); lineTo(12f, 2f); lineTo(9.19f, 8.63f); lineTo(2f, 9.24f); lineTo(7.46f, 13.97f); lineTo(5.82f, 21f); close()
        }
    }.build()

    val StarOutline: ImageVector = ImageVector.Builder("StarOutline", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 17.27f); lineTo(18.18f, 21f); lineTo(16.54f, 13.97f); lineTo(22f, 9.24f); lineTo(14.81f, 8.63f); lineTo(12f, 2f); lineTo(9.19f, 8.63f); lineTo(2f, 9.24f); lineTo(7.46f, 13.97f); lineTo(5.82f, 21f); close()
        }
    }.build()

    val Check: ImageVector = ImageVector.Builder("Check", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 6f); lineTo(9f, 17f); lineTo(4f, 12f)
        }
    }.build()

    val Admin: ImageVector = ImageVector.Builder("Admin", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 12f); arcTo(4f, 4f, 0f, true, true, 8f, 8f); arcTo(4f, 4f, 0f, false, true, 12f, 12f); close()
            moveTo(12f, 14f); arcTo(6f, 6f, 0f, false, false, 6f, 20f); verticalLineToRelative(2f); horizontalLineToRelative(12f); verticalLineToRelative(-2f); arcTo(6f, 6f, 0f, false, false, 12f, 14f); close()
        }
    }.build()

    val AspectRatio: ImageVector = ImageVector.Builder("AspectRatio", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19f, 12f); horizontalLineToRelative(-2f); verticalLineToRelative(3f); horizontalLineToRelative(-3f); verticalLineToRelative(2f); horizontalLineToRelative(5f); verticalLineToRelative(-5f); close()
            moveTo(7f, 9f); horizontalLineToRelative(3f); verticalLineToRelative(-2f); horizontalLineToRelative(-5f); verticalLineToRelative(5f); horizontalLineToRelative(2f); verticalLineToRelative(-3f); close()
            moveTo(21f, 3f); horizontalLineToRelative(-18f); arcTo(2f, 2f, 0f, false, false, 1f, 5f); verticalLineToRelative(14f); arcTo(2f, 2f, 0f, false, false, 3f, 21f); horizontalLineToRelative(18f); arcTo(2f, 2f, 0f, false, false, 23f, 19f); verticalLineToRelative(-14f); arcTo(2f, 2f, 0f, false, false, 21f, 3f); close()
            moveTo(21f, 19f); horizontalLineToRelative(-18f); verticalLineToRelative(-14f); horizontalLineToRelative(18f); verticalLineToRelative(14f); close()
        }
    }.build()

    val PiP: ImageVector = ImageVector.Builder("PiP", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19f, 7f); horizontalLineToRelative(-8f); verticalLineToRelative(6f); horizontalLineToRelative(8f); verticalLineToRelative(-6f); close()
            moveTo(21f, 3f); horizontalLineToRelative(-18f); arcTo(2f, 2f, 0f, false, false, 1f, 5f); verticalLineToRelative(14f); arcTo(2f, 2f, 0f, false, false, 3f, 21f); horizontalLineToRelative(18f); arcTo(2f, 2f, 0f, false, false, 23f, 19f); verticalLineToRelative(-14f); arcTo(2f, 2f, 0f, false, false, 21f, 3f); close()
            moveTo(21f, 19f); horizontalLineToRelative(-18f); verticalLineToRelative(-14f); horizontalLineToRelative(18f); verticalLineToRelative(14f); close()
        }
    }.build()

    val Cast: ImageVector = ImageVector.Builder("Cast", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(21.5f, 3f); lineTo(2.5f, 3f); arcTo(1f, 1f, 0f, false, false, 1.5f, 4f); lineTo(1.5f, 18.5f); lineTo(5.5f, 18.5f); lineTo(5.5f, 22.5f); lineTo(21.5f, 22.5f); lineTo(21.5f, 4f); arcTo(1f, 1f, 0f, false, false, 21.5f, 3f); close()
            moveTo(5.5f, 15.5f); lineTo(5.5f, 18.5f); lineTo(18.5f, 18.5f); lineTo(18.5f, 15.5f); close()
            moveTo(5.5f, 11.5f); lineTo(5.5f, 14.5f); lineTo(18.5f, 14.5f); lineTo(18.5f, 11.5f); close()
            moveTo(5.5f, 7.5f); lineTo(5.5f, 10.5f); lineTo(18.5f, 10.5f); lineTo(18.5f, 7.5f); close()
        }
    }.build()
}
    }.build()
}
