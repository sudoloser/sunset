package dev.sudoloser.sunset.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
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
            moveTo(6f, 4f); lineTo(10f, 4f); lineTo(10f, 20f); lineTo(6f, 20f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(14f, 4f); lineTo(18f, 4f); lineTo(18f, 20f); lineTo(14f, 20f); close()
        }
    }.build()

    val Home: ImageVector = ImageVector.Builder("Home", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(3f, 9f); lineTo(12f, 2f); lineTo(21f, 9f)
            lineTo(21f, 22f); lineTo(9f, 22f); lineTo(9f, 14f)
            lineTo(15f, 14f); lineTo(15f, 22f); lineTo(3f, 22f); close()
        }
    }.build()

    val Library: ImageVector = ImageVector.Builder("Library", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 6f); lineTo(6f, 6f); lineTo(6f, 20f); lineTo(4f, 20f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(9f, 4f); lineTo(11f, 4f); lineTo(11f, 20f); lineTo(9f, 20f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(14f, 7f); lineTo(16f, 7f); lineTo(16f, 20f); lineTo(14f, 20f); close()
        }
    }.build()

    val Search: ImageVector = ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(11f, 2f); lineTo(15.5f, 6.5f); lineTo(20f, 11f)
            lineTo(20f, 13f); lineTo(14f, 19f); lineTo(12f, 21f)
            lineTo(10f, 19f); lineTo(4f, 13f); lineTo(4f, 11f)
            lineTo(8.5f, 6.5f); lineTo(13f, 2f); lineTo(11f, 2f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(15f, 15f); lineTo(21f, 21f)
        }
    }.build()

    val Settings: ImageVector = ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 15f)
            arcTo(3f, 3f, 0f, true, false, 12f, 9f)
            arcTo(3f, 3f, 0f, true, false, 12f, 15f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(19.4f, 15f)
            arcTo(1.65f, 1.65f, 0f, false, false, 20.47f, 13.64f)
            arcTo(1.65f, 1.65f, 0f, false, false, 19.77f, 12.18f)
            lineTo(19.16f, 11.32f)
            arcTo(1.65f, 1.65f, 0f, false, false, 19.16f, 8.68f)
            lineTo(19.77f, 7.82f)
            arcTo(1.65f, 1.65f, 0f, false, false, 20.47f, 6.36f)
            arcTo(1.65f, 1.65f, 0f, false, false, 19.4f, 5f)
            lineTo(18.69f, 5.18f)
            arcTo(1.65f, 1.65f, 0f, false, true, 17.34f, 4.48f)
            lineTo(16.78f, 3.41f)
            arcTo(1.65f, 1.65f, 0f, false, false, 15.32f, 2.59f)
            arcTo(1.65f, 1.65f, 0f, false, false, 13.86f, 3.29f)
            lineTo(13f, 3.9f)
            arcTo(1.65f, 1.65f, 0f, false, true, 11f, 3.9f)
            lineTo(10.14f, 3.29f)
            arcTo(1.65f, 1.65f, 0f, false, false, 8.68f, 2.59f)
            arcTo(1.65f, 1.65f, 0f, false, false, 7.22f, 3.41f)
            lineTo(6.66f, 4.48f)
            arcTo(1.65f, 1.65f, 0f, false, true, 5.31f, 5.18f)
            lineTo(4.6f, 5f)
            arcTo(1.65f, 1.65f, 0f, false, false, 3.53f, 6.36f)
            arcTo(1.65f, 1.65f, 0f, false, false, 4.23f, 7.82f)
            lineTo(4.84f, 8.68f)
            arcTo(1.65f, 1.65f, 0f, false, true, 4.84f, 11.32f)
            lineTo(4.23f, 12.18f)
            arcTo(1.65f, 1.65f, 0f, false, false, 3.53f, 13.64f)
            arcTo(1.65f, 1.65f, 0f, false, false, 4.6f, 15f)
            lineTo(5.31f, 14.82f)
            arcTo(1.65f, 1.65f, 0f, false, true, 6.66f, 15.52f)
            lineTo(7.22f, 16.59f)
            arcTo(1.65f, 1.65f, 0f, false, false, 8.68f, 17.41f)
            arcTo(1.65f, 1.65f, 0f, false, false, 10.14f, 16.71f)
            lineTo(11f, 16.1f)
            arcTo(1.65f, 1.65f, 0f, false, true, 13f, 16.1f)
            lineTo(13.86f, 16.71f)
            arcTo(1.65f, 1.65f, 0f, false, false, 15.32f, 17.41f)
            arcTo(1.65f, 1.65f, 0f, false, false, 16.78f, 16.59f)
            lineTo(17.34f, 15.52f)
            arcTo(1.65f, 1.65f, 0f, false, true, 18.69f, 14.82f)
            close()
        }
    }.build()

    val VolumeHigh: ImageVector = ImageVector.Builder("VolumeHigh", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(3f, 9f); lineTo(7f, 9f); lineTo(12f, 4f); lineTo(12f, 20f)
            lineTo(7f, 15f); lineTo(3f, 15f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(18f, 7f); arcTo(5f, 5f, 0f, false, true, 18f, 17f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(15.5f, 9.5f); arcTo(2.5f, 2.5f, 0f, false, true, 15.5f, 14.5f)
        }
    }.build()

    val VolumeMuted: ImageVector = ImageVector.Builder("VolumeMuted", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(3f, 9f); lineTo(7f, 9f); lineTo(12f, 4f); lineTo(12f, 20f)
            lineTo(7f, 15f); lineTo(3f, 15f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(17f, 10f); lineTo(22f, 15f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(22f, 10f); lineTo(17f, 15f)
        }
    }.build()

    val Maximize: ImageVector = ImageVector.Builder("Maximize", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(8f, 3f); lineTo(5f, 3f)
            arcTo(2f, 2f, 0f, false, false, 3f, 5f); lineTo(3f, 8f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(21f, 8f); lineTo(21f, 5f)
            arcTo(2f, 2f, 0f, false, false, 19f, 3f); lineTo(16f, 3f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(16f, 21f); lineTo(19f, 21f)
            arcTo(2f, 2f, 0f, false, false, 21f, 19f); lineTo(21f, 16f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(3f, 16f); lineTo(3f, 19f)
            arcTo(2f, 2f, 0f, false, false, 5f, 21f); lineTo(8f, 21f)
        }
    }.build()

    val Minimize: ImageVector = ImageVector.Builder("Minimize", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(8f, 3f); lineTo(5f, 3f)
            arcTo(2f, 2f, 0f, false, false, 3f, 5f); lineTo(3f, 8f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(21f, 8f); lineTo(21f, 5f)
            arcTo(2f, 2f, 0f, false, false, 19f, 3f); lineTo(16f, 3f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(3f, 16f); lineTo(3f, 19f)
            arcTo(2f, 2f, 0f, false, false, 5f, 21f); lineTo(8f, 21f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(19f, 21f); lineTo(21f, 21f); lineTo(21f, 19f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(3f, 3f); lineTo(21f, 21f)
        }
    }.build()

    val SkipBack: ImageVector = ImageVector.Builder("SkipBack", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(11f, 5f); lineTo(5f, 12f); lineTo(11f, 19f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(19f, 5f); lineTo(13f, 12f); lineTo(19f, 19f)
        }
    }.build()

    val SkipForward: ImageVector = ImageVector.Builder("SkipForward", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(5f, 4f); lineTo(11f, 12f); lineTo(5f, 20f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(13f, 4f); lineTo(19f, 12f); lineTo(13f, 20f)
        }
    }.build()

    val Back: ImageVector = ImageVector.Builder("Back", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19f, 12f); lineTo(5f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 19f); lineTo(5f, 12f); lineTo(12f, 5f)
        }
    }.build()

    val Info: ImageVector = ImageVector.Builder("Info", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f); arcTo(10f, 10f, 0f, true, false, 12f, 22f)
            arcTo(10f, 10f, 0f, true, false, 12f, 2f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 11f); lineTo(12f, 17f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 7f); lineTo(12.01f, 7f)
        }
    }.build()

    val Plus: ImageVector = ImageVector.Builder("Plus", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 5f); lineTo(12f, 19f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }.build()

    val Check: ImageVector = ImageVector.Builder("Check", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(9f, 16f); lineTo(5f, 12f); lineTo(3.5f, 13.5f)
            lineTo(9f, 19f); lineTo(21f, 7f); lineTo(19.5f, 5.5f)
        }
    }.build()

    val Close: ImageVector = ImageVector.Builder("Close", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 6f); lineTo(18f, 18f)
        }
    }.build()

    val MoreVertical: ImageVector = ImageVector.Builder("MoreVertical", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 7f); arcTo(1.5f, 1.5f, 0f, true, false, 12f, 10f)
            arcTo(1.5f, 1.5f, 0f, true, false, 12f, 7f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 12f); arcTo(1.5f, 1.5f, 0f, true, false, 12f, 15f)
            arcTo(1.5f, 1.5f, 0f, true, false, 12f, 12f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 17f); arcTo(1.5f, 1.5f, 0f, true, false, 12f, 20f)
            arcTo(1.5f, 1.5f, 0f, true, false, 12f, 17f); close()
        }
    }.build()

    val Download: ImageVector = ImageVector.Builder("Download", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(21f, 15f); lineTo(21f, 19f)
            arcTo(2f, 2f, 0f, false, true, 19f, 21f); lineTo(5f, 21f)
            arcTo(2f, 2f, 0f, false, true, 3f, 19f); lineTo(3f, 15f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 15f); lineTo(12f, 3f)
        }
    }.build()

    val Subtitles: ImageVector = ImageVector.Builder("Subtitles", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(2f, 4f); lineTo(22f, 4f); lineTo(22f, 20f); lineTo(2f, 20f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 10f); lineTo(10f, 10f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(14f, 14f); lineTo(18f, 14f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 14f); lineTo(8f, 14f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(16f, 10f); lineTo(18f, 10f)
        }
    }.build()

    val Movie: ImageVector = ImageVector.Builder("Movie", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(2f, 4f); lineTo(22f, 4f); lineTo(22f, 20f); lineTo(2f, 20f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(7f, 4f); lineTo(7f, 20f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(17f, 4f); lineTo(17f, 20f)
        }
    }.build()

    val Tv: ImageVector = ImageVector.Builder("Tv", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(2f, 4f); lineTo(22f, 4f); lineTo(22f, 18f); lineTo(2f, 18f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(8f, 22f); lineTo(16f, 22f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 18f); lineTo(12f, 22f)
        }
    }.build()

    val Star: ImageVector = ImageVector.Builder("Star", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f); lineTo(15.09f, 8.26f); lineTo(22f, 9.27f)
            lineTo(17f, 14.14f); lineTo(18.18f, 21.02f); lineTo(12f, 17.77f)
            lineTo(5.82f, 21.02f); lineTo(7f, 14.14f); lineTo(2f, 9.27f)
            lineTo(8.91f, 8.26f); close()
        }
    }.build()

    val ArrowDown: ImageVector = ImageVector.Builder("ArrowDown", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
        }
    }.build()

    val Copy: ImageVector = ImageVector.Builder("Copy", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(16f, 4f); lineTo(18f, 4f)
            arcTo(2f, 2f, 0f, false, true, 20f, 6f); lineTo(20f, 20f)
            arcTo(2f, 2f, 0f, false, true, 18f, 22f); lineTo(6f, 22f)
            arcTo(2f, 2f, 0f, false, true, 4f, 20f); lineTo(4f, 6f)
            arcTo(2f, 2f, 0f, false, true, 6f, 4f); lineTo(8f, 4f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(15f, 2f); lineTo(9f, 2f)
            arcTo(1f, 1f, 0f, false, false, 8f, 3f); lineTo(8f, 5f)
            arcTo(1f, 1f, 0f, false, false, 9f, 6f); lineTo(15f, 6f)
            arcTo(1f, 1f, 0f, false, false, 16f, 5f); lineTo(16f, 3f)
            arcTo(1f, 1f, 0f, false, false, 15f, 2f); close()
        }
    }.build()

    val Refresh: ImageVector = ImageVector.Builder("Refresh", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(23f, 4f); lineTo(23f, 10f); lineTo(17f, 10f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(1f, 20f); lineTo(1f, 14f); lineTo(7f, 14f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(3.51f, 9f); arcTo(9f, 9f, 0f, false, true, 20.24f, 6.24f)
            lineTo(23f, 10f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(20.49f, 15f); arcTo(9f, 9f, 0f, false, true, 3.76f, 17.76f)
            lineTo(1f, 14f)
        }
    }.build()

    val Admin: ImageVector = ImageVector.Builder("Admin", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f); arcTo(3.5f, 3.5f, 0f, true, false, 12f, 9f)
            arcTo(3.5f, 3.5f, 0f, true, false, 12f, 2f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 11f); arcTo(5f, 5f, 0f, false, false, 7f, 16f)
            arcTo(5f, 5f, 0f, false, false, 17f, 16f); arcTo(5f, 5f, 0f, false, false, 12f, 11f); close()
        }
    }.build()

    val Logout: ImageVector = ImageVector.Builder("Logout", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(9f, 21f); lineTo(5f, 21f)
            arcTo(2f, 2f, 0f, false, true, 3f, 19f); lineTo(3f, 5f)
            arcTo(2f, 2f, 0f, false, true, 5f, 3f); lineTo(9f, 3f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(16f, 17f); lineTo(21f, 12f); lineTo(16f, 7f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(21f, 12f); lineTo(9f, 12f)
        }
    }.build()

    val Speed: ImageVector = ImageVector.Builder("Speed", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(20.4f, 6.4f); arcTo(1f, 1f, 0f, false, false, 19f, 7.36f)
            lineTo(18f, 9f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 12f); arcTo(8f, 8f, 0f, false, true, 19f, 7.36f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 12f); arcTo(8f, 8f, 0f, false, false, 20f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 20f); lineTo(12f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 12f); lineTo(14f, 10f)
        }
    }.build()

    val Episodes: ImageVector = ImageVector.Builder("Episodes", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); lineTo(4f, 20f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(8f, 8f); lineTo(12f, 8f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(8f, 12f); lineTo(16f, 12f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(8f, 16f); lineTo(14f, 16f)
        }
    }.build()
}
