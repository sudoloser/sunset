val data = bindings["data"] as Map<String, Any>
val success = data["success"] as? Boolean ?: false

if (!success) {
    val errorMsg = data["error"] as? String ?: "Connection failed"
    mapOf(
        "type" to "Card",
        "backgroundColor" to "#FFCDD2", // light red for error
        "children" to listOf(
            mapOf("type" to "Text", "text" to "SunSet Media", "style" to "Title"),
            mapOf("type" to "Text", "text" to "Error: $errorMsg", "style" to "Body")
        )
    )
} else {
    val status = data["server_status"] as? String ?: "unknown"
    val uptime = data["uptime_seconds"] as? Long ?: 0L
    val activeStreams = data["active_streams"] as? Int ?: 0
    val usedGb = data["used_gb"] as? Double ?: 0.0
    val totalGb = data["total_gb"] as? Double ?: 0.0
    val percentUsed = data["percent_used"] as? Double ?: 0.0

    val uptimeStr = "${uptime / 3600}h ${(uptime % 3600) / 60}m"
    val cardColor = if (status == "healthy") "#E8F5E9" else "#FFE0B2" // green or orange

    mapOf(
        "type" to "Card",
        "backgroundColor" to cardColor,
        "children" to listOf(
            mapOf("type" to "Text", "text" to "SunSet Media", "style" to "Title"),
            mapOf("type" to "Text", "text" to "Status: $status | Uptime: $uptimeStr", "style" to "Subtitle"),
            mapOf("type" to "Row", "children" to listOf(
                mapOf("type" to "Text", "text" to "Active Streams: $activeStreams", "style" to "Body"),
                mapOf("type" to "Spacer")
            )),
            mapOf("type" to "StorageBar", "used" to usedGb, "total" to totalGb, "percent" to percentUsed)
        )
    )
}
