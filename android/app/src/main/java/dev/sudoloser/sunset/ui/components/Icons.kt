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
            moveTo(6f, 4f); h(4f); v(16f); h(-4f); close()
            moveTo(14f, 4f); h(4f); v(16f); h(-4f); close()
        }
    }.build()

    val Home: ImageVector = ImageVector.Builder("Home", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10f, 20f); v(-6f); h(4f); v(6f); h(5f); v(-8f); h(3f); lineTo(12f, 3f); lineTo(2f, 12f); h(3f); v(8f); close()
        }
    }.build()

    val Library: ImageVector = ImageVector.Builder("Library", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 6f); h(2f); v(14f); h(-2f); close()
            moveTo(8f, 4f); h(2f); v(16f); h(-2f); close()
            moveTo(12f, 7f); h(2f); v(13f); h(-2f); close()
            moveTo(16f, 2f); h(2f); v(18f); h(-2f); close()
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
            moveTo(20f, 4f); h(-16f); arcTo(2f, 2f, 0f, false, false, 2f, 6f); v(12f); arcTo(2f, 2f, 0f, false, false, 4f, 20f); h(16f); arcTo(2f, 2f, 0f, false, false, 22f, 18f); v(-12f); arcTo(2f, 2f, 0f, false, false, 20f, 4f); close()
            moveTo(7f, 10f); h(4f); moveTo(7f, 14f); h(2f); moveTo(13f, 14f); h(4f); moveTo(17f, 10f); h(-2f)
        }
    }.build()

    val SkipBack: ImageVector = ImageVector.Builder("SkipBack", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(11f, 18f); v(-12f); lineTo(2.5f, 12f); close()
            moveTo(21.5f, 18f); v(-12f); lineTo(13f, 12f); close()
        }
    }.build()

    val SkipForward: ImageVector = ImageVector.Builder("SkipForward", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(2.5f, 18f); v(-12f); lineTo(11f, 12f); close()
            moveTo(13f, 18f); v(-12f); lineTo(21.5f, 12f); close()
        }
    }.build()

    val Back: ImageVector = ImageVector.Builder("Back", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 12f); h(-14f); moveTo(12f, 19f); lineTo(5f, 12f); lineTo(12f, 5f)
        }
    }.build()

    val Plus: ImageVector = ImageVector.Builder("Plus", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 5f); v(14f); moveTo(5f, 12f); h(14f)
        }
    }.build()

    val Refresh: ImageVector = ImageVector.Builder("Refresh", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(23f, 4f); v(6f); h(-6f); moveTo(1f, 20f); v(-6f); h(6f);
            moveTo(3.51f, 9f); arcTo(9f, 9f, 0f, false, true, 21f, 12f); arcTo(9f, 9f, 0f, false, true, 20.49f, 15f);
            moveTo(20.49f, 15f); arcTo(9f, 9f, 0f, false, true, 3f, 12f); arcTo(9f, 9f, 0f, false, true, 3.51f, 9f);
        }
    }.build()
}
