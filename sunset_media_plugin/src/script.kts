import okhttp3.Request
import okhttp3.OkHttpClient
import org.json.JSONObject

val client = bindings["httpClient"] as OkHttpClient
val serverIp = bindings["server_ip"] as String
val serverPort = bindings["server_port"] as String
val request = Request.Builder()
    .url("http://$serverIp:$serverPort/api/v1/dashboard/status")
    .build()

val result = mutableMapOf<String, Any>()

try {
    client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
            val bodyString = response.body?.string() ?: ""
            val json = JSONObject(bodyString)
            result["server_status"] = json.getString("server_status")
            result["uptime_seconds"] = json.getLong("uptime_seconds")
            result["active_streams"] = json.getInt("active_streams")
            result["transcode_tasks"] = json.getInt("transcode_tasks")
            
            val storage = json.getJSONObject("storage")
            result["used_gb"] = storage.getDouble("used_gb")
            result["total_gb"] = storage.getDouble("total_gb")
            result["percent_used"] = storage.getDouble("percent_used")
            result["success"] = true
        } else {
            result["success"] = false
            result["error"] = "HTTP error: ${response.code}"
        }
    }
} catch (e: Exception) {
    result["success"] = false
    result["error"] = e.message ?: "Network failure"
}

result
