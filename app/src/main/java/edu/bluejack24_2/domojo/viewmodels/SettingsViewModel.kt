package edu.bluejack24_2.domojo.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.utils.ThemeHelper

class SettingsViewModel() : ViewModel() {
    // Setting states
    val notificationsEnabled = MutableLiveData<Boolean>(true)
    val darkModeEnabled = MutableLiveData<Boolean>(false)
    val selectedLanguage = MutableLiveData<String>(null)

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