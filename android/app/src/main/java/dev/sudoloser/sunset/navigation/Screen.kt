package dev.sudoloser.sunset.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable data object Loading : Screen()
    @Serializable data object ServerSelection : Screen()
    @Serializable data object Onboarding : Screen()
    @Serializable data object Login : Screen()
    @Serializable data object Dashboard : Screen()
    @Serializable data object Library : Screen()
    @Serializable data object Settings : Screen()
    @Serializable data object Search : Screen()
    @Serializable data object Admin : Screen()
    @Serializable data class MediaDetail(val itemId: String) : Screen()
    @Serializable data class Player(val videoUrl: String, val videoTitle: String, val itemId: String, val baseUrl: String, val userId: String?, val showTitle: String?, val mediaType: String) : Screen()
}
