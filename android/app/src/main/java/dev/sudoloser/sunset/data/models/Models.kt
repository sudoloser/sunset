package dev.sudoloser.sunset.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    @SerialName("movie") MOVIE,
    @SerialName("episode") EPISODE
}

@Serializable
enum class LibraryType {
    @SerialName("movies") MOVIES,
    @SerialName("shows") SHOWS
}

@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    @SerialName("show_title") val showTitle: String? = null,
    @SerialName("collection_name") val collectionName: String? = null,
    @SerialName("media_type") val mediaType: MediaType,
    val year: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    @SerialName("added_at") val addedAt: String? = null,
    @SerialName("file_path") val filePath: String? = null,
    val description: String? = null,
    val cast: String? = null,
    val genres: String? = null,
    val rating: Double? = null,
    @SerialName("tmdb_id") val tmdbId: String? = null
)

@Serializable
data class User(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("discord_token") val discordToken: String? = null,
    @SerialName("discord_status") val discordStatus: String? = null,
    @SerialName("profile_picture") val profilePicture: String? = null
)

@Serializable
data class PlaybackState(
    @SerialName("item_id") val itemId: String,
    @SerialName("user_id") val userId: String? = null,
    val timestamp: Double,
    val duration: Double? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class StorageInfo(
    @SerialName("total_size") val totalSize: Long,
    @SerialName("item_count") val itemCount: Int,
    @SerialName("library_count") val libraryCount: Int,
    @SerialName("user_count") val userCount: Int
)

@Serializable
data class Library(
    val id: String,
    val name: String,
    val path: String,
    @SerialName("lib_type") val libType: LibraryType
)

@Serializable
data class SetupStatus(
    @SerialName("setup_complete") val setupComplete: Boolean,
    @SerialName("server_name") val serverName: String? = null
)

@Serializable
data class UserConfig(
    val username: String,
    @SerialName("password_hash") val passwordHash: String
)

@Serializable
data class OnboardRequest(
    @SerialName("server_name") val serverName: String,
    @SerialName("admin_user") val adminUser: UserConfig,
    val libraries: List<LibraryInput>
)

@Serializable
data class LibraryInput(
    val name: String,
    val path: String,
    @SerialName("lib_type") val libType: LibraryType
)

@Serializable
data class LoginRequest(
    val username: String,
    @SerialName("password_hash") val passwordHash: String
)

@Serializable
data class CreateUserRequest(
    val username: String,
    @SerialName("password_hash") val passwordHash: String,
    @SerialName("is_admin") val isAdmin: Boolean = false
)

@Serializable
data class DiscordConfig(
    val token: String,
    val status: String
)
