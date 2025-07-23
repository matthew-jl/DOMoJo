package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {
    // Setting states
    val notificationsEnabled = MutableLiveData<Boolean>(true)
    val darkModeEnabled = MutableLiveData<Boolean>(false)
    val selectedLanguage = MutableLiveData<String>(null)

    fun updateSelectedLanguage(language: String) {
        selectedLanguage.value = language
    }

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Navigation
    private val _navigateBack = MutableLiveData<Boolean>(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    fun onSaveSettings() {
        _isLoading.value = true
        // Here you would save to SharedPreferences or backend
        // Simulate network delay
        android.os.Handler().postDelayed({
            _isLoading.value = false
            _navigateBack.value = true
        }, 1000)
    }

    fun onNavigationComplete() {
        _navigateBack.value = false
    }
}