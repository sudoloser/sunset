package dev.sudoloser.sunset.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "settings")

object PrefKeys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val USER_ID = stringPreferencesKey("user_id")
    val USERNAME = stringPreferencesKey("username")
    val IS_ADMIN = stringPreferencesKey("is_admin")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
    val SUBTITLE_BACKGROUND_OPACITY = floatPreferencesKey("subtitle_background_opacity")
    val SUBTITLE_SIZE = intPreferencesKey("subtitle_size")
    val SUBTITLE_FONT = stringPreferencesKey("subtitle_font")
    val SUBTITLE_BOLD = booleanPreferencesKey("subtitle_bold")
    val DOWNLOAD_PATH = stringPreferencesKey("download_path")
}
