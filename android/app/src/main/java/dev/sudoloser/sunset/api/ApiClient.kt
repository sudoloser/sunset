package dev.sudoloser.sunset.api

import dev.sudoloser.sunset.data.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ApiClient(baseUrl: String) {
    private val baseUrl = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json".toMediaType()

    private suspend inline fun <reified T> get(endpoint: String): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api$endpoint")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<T>(body)
    }

    private suspend inline fun <reified T, reified B> post(endpoint: String, body: B?): T = withContext(Dispatchers.IO) {
        val requestBody = if (body == null || body is Unit) {
            "{}".toRequestBody(mediaType)
        } else {
            json.encodeToString(body).toRequestBody(mediaType)
        }
        val request = Request.Builder()
            .url("$baseUrl/api$endpoint")
            .post(requestBody)
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext json.decodeFromString<T>("null")
        json.decodeFromString<T>(responseBody)
    }

    private suspend inline fun <reified T> put(endpoint: String, body: Any): T = withContext(Dispatchers.IO) {
        val requestBody = json.encodeToString(body).toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$baseUrl/api$endpoint")
            .put(requestBody)
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<T>(responseBody)
    }

    private suspend fun delete(endpoint: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api$endpoint")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        response.isSuccessful
    }

    suspend fun getStatus(): SetupStatus = get("/status")
    suspend fun getUptime(): Long = get("/uptime")
    suspend fun onboard(data: OnboardRequest): Boolean = post("/onboard", data)
    suspend fun login(data: LoginRequest): User? = post("/login", data)
    suspend fun getUserProfile(id: String): User? = get("/users/$id")
    suspend fun getRecentlyAdded(userId: String? = null): List<MediaItem> = 
        get("/recently-added" + (if (userId != null) "?user_id=$userId" else ""))
    suspend fun getLibraries(): List<Library> = get("/libraries")
    suspend fun addLibrary(data: LibraryInput): Boolean = post("/libraries", data)
    suspend fun deleteLibrary(id: String): Boolean = delete("/libraries/$id")
    suspend fun getLibraryItems(id: String, userId: String? = null): List<MediaItem> = 
        get("/libraries/$id/items" + (if (userId != null) "?user_id=$userId" else ""))
    suspend fun getShowEpisodes(showTitle: String, userId: String? = null): List<MediaItem> = 
        get("/shows/$showTitle/episodes" + (if (userId != null) "?user_id=$userId" else ""))
    suspend fun search(query: String, userId: String? = null): List<MediaItem> = 
        get("/search?q=$query" + (if (userId != null) "&user_id=$userId" else ""))
    suspend fun triggerScan(): Boolean = post("/scan", Unit)
    fun getStreamUrl(itemId: String): String = "$baseUrl/api/stream/$itemId"
    suspend fun getSubtitles(itemId: String): List<String> = get("/media/$itemId/subtitles")
    suspend fun savePlayback(data: PlaybackState): Boolean = post("/playback", data)
    suspend fun getPlayback(itemId: String, userId: String? = null): PlaybackState {
        val suffix = if (userId != null) "/playback/$itemId?user_id=$userId" else "/playback/$itemId"
        return get(suffix)
    }
    suspend fun updateDiscordConfig(userId: String, token: String, status: String): Boolean =
        put("/users/$userId/discord-config", DiscordConfig(token, status))
    suspend fun stopDiscordRpc(userId: String): Boolean = post("/users/$userId/discord-stop", Unit)
    suspend fun getStorage(): StorageInfo = get("/storage")
    suspend fun refreshMedia(id: String): Boolean = post("/media/$id/refresh", Unit)
    suspend fun getGenres(): List<String> = get("/genres")
    suspend fun getGenreItems(genre: String, userId: String? = null): List<MediaItem> = 
        get("/genre/$genre" + (if (userId != null) "?user_id=$userId" else ""))
    suspend fun createInvite(): String = post("/invite", Unit)
    suspend fun redeemInvite(code: String): Boolean = post("/invite/redeem", mapOf("code" to code))
    suspend fun generateMediaToken(itemId: String): String = post("/media/$itemId/token", Unit)
    suspend fun getUsers(): List<User> = get("/users")
    suspend fun createUser(data: CreateUserRequest): Boolean = post("/users", data)
    suspend fun deleteUser(id: String): Boolean = delete("/users/$id")
    suspend fun changePassword(id: String, cur: String, new: String): Boolean =
        put("/users/$id/password", mapOf("current_password" to cur, "new_password" to new))
    suspend fun changeUsername(id: String, newUsername: String): Boolean =
        put("/users/$id/username", mapOf("new_username" to newUsername))
    fun getProfilePictureUrl(userId: String): String = "$baseUrl/api/users/$userId/profile-picture"
    suspend fun uploadProfilePicture(userId: String, image: String): Boolean =
        post("/users/$userId/profile-picture", mapOf("image" to image))
    suspend fun getContinueWatching(userId: String): List<MediaItem> = get("/continue-watching/$userId")
    suspend fun getUserItems(userId: String): List<MediaItem> = get("/user-items/$userId")
    suspend fun addUserItem(userId: String, itemId: String): Boolean =
        post("/user-items/$userId", mapOf("item_id" to itemId))
    suspend fun removeUserItem(userId: String, itemId: String): Boolean =
        delete("/user-items/$userId/$itemId")
    fun getAssetUrl(itemId: String, asset: String): String = "$baseUrl/api/media/$itemId/asset/$asset"
    fun getDownloadUrl(itemId: String, token: String): String = "$baseUrl/api/media/$itemId/download?token=$token"
    fun getDownloadZipUrl(itemId: String, token: String): String = "$baseUrl/api/media/$itemId/download-zip?token=$token"

    fun getSubtitleUrl(itemId: String, name: String): String =
        "$baseUrl/api/media/$itemId/subtitle/$name"
}
