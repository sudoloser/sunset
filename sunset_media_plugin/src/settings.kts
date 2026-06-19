// src/settings.kts
val bindings = bindings ?: emptyMap()
mapOf(
    "fields" to listOf(
        mapOf(
            "key" to "server_ip",
            "type" to "text",
            "label" to "Server IP",
            "defaultValue" to "10.0.2.2"
        ),
        mapOf(
            "key" to "server_port",
            "type" to "text",
            "label" to "Server Port",
            "defaultValue" to "7867"
        ),
        mapOf(
            "key" to "auth_token",
            "type" to "password",
            "label" to "Authentication Token",
            "defaultValue" to ""
        )
    )
)
