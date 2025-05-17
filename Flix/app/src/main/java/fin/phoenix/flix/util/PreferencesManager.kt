package fin.phoenix.flix.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val settingsPrefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private val userPrefs: SharedPreferences = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)

    // Settings preferences
    var isDarkMode: Boolean
        get() = settingsPrefs.getBoolean("dark_mode", false)
        set(value) = settingsPrefs.edit().putBoolean("dark_mode", value).apply()

    var notificationsEnabled: Boolean
        get() = settingsPrefs.getBoolean("notifications_enabled", true)
        set(value) = settingsPrefs.edit().putBoolean("notifications_enabled", value).apply()

    var language: String
        get() = settingsPrefs.getString("language", "简体中文") ?: "简体中文"
        set(value) = settingsPrefs.edit().putString("language", value).apply()

    // User preferences
    var userId: String?
        get() = userPrefs.getString("user_id", null)
        set(value) = userPrefs.edit().putString("user_id", value).apply()

    var authToken: String?
        get() = userPrefs.getString("auth_token", null)
        set(value) = userPrefs.edit().putString("auth_token", value).apply()

    fun clearUserData() {
        userPrefs.edit().apply {
            remove("auth_token")
            remove("user_id")
            apply()
        }
    }
}