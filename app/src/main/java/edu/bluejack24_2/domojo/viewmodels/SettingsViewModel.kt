package edu.bluejack24_2.domojo.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.utils.ThemeHelper
import java.util.Locale

class SettingsViewModel() : ViewModel() {
    // Setting states
    val notificationsEnabled = MutableLiveData<Boolean>(true)
    val darkModeEnabled = MutableLiveData<Boolean>(false)
    val selectedLanguage = MutableLiveData<String>(null)
    val notificationTime = MutableLiveData<String>("")
    private val _notificationHour = MutableLiveData<Int>(8)
    private val _notificationMinute = MutableLiveData<Int>(0)

    fun setNotificationTime(hour: Int, minute: Int) {
        _notificationHour.value = hour
        _notificationMinute.value = minute
        notificationTime.value = String.format(Locale.getDefault(),
            "%02d:%02d", hour, minute)
    }

    fun getNotificationHour() = _notificationHour.value ?: 8
    fun getNotificationMinute() = _notificationMinute.value ?: 0

    // Add this to save time prefs
    fun saveNotificationTime(context: Context) {
        context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE).edit()
            .putInt("notification_hour", getNotificationHour())
            .putInt("notification_minute", getNotificationMinute())
            .apply()
    }

    fun updateNotificationPreference(enabled: Boolean) {
        notificationsEnabled.value = enabled
    }

    fun updateSelectedLanguage(displayLanguage: String) {
        selectedLanguage.value = displayLanguage
    }

    fun updateDarkModeEnabled(context: Context, enabled: Boolean?) {
        darkModeEnabled.value = enabled
        val theme = when (enabled) {
            true -> ThemeHelper.THEME_DARK
            false -> ThemeHelper.THEME_LIGHT
            null -> ThemeHelper.THEME_SYSTEM
        }
        ThemeHelper.saveThemePreference(context, theme)
        ThemeHelper.applyTheme(theme)
    }

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Navigation
    private val _navigateBack = MutableLiveData<Boolean>(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    fun onSaveSettings() {
        _isLoading.value = true
        _isLoading.value = false
        _navigateBack.value = true
    }

    fun onNavigationComplete() {
        _navigateBack.value = false
    }
}