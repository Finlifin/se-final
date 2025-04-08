package fin.phoenix.flix.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fin.phoenix.flix.util.PreferencesManager

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    private val _darkMode = MutableLiveData(preferencesManager.isDarkMode)
    val darkMode: LiveData<Boolean> = _darkMode

    private val _notificationsEnabled = MutableLiveData(preferencesManager.notificationsEnabled)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _language = MutableLiveData(preferencesManager.language)
    val language: LiveData<String> = _language

    fun setDarkMode(enabled: Boolean) {
        preferencesManager.isDarkMode = enabled
        _darkMode.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferencesManager.notificationsEnabled = enabled
        _notificationsEnabled.value = enabled
    }

    fun setLanguage(language: String) {
        preferencesManager.language = language
        _language.value = language
    }

    fun logout() {
        preferencesManager.clearUserData()
    }
}