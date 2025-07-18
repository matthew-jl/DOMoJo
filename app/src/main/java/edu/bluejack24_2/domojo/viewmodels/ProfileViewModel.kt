package edu.bluejack24_2.domojo.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.models.UserRepository

private const val TAG = "ProfileFlow"

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> get() = _currentUser

    private val _logoutSuccess = MutableLiveData<Boolean>()
    val logoutSuccess: LiveData<Boolean> get() = _logoutSuccess

    private val _logoutError = MutableLiveData<String?>()
    val logoutError: LiveData<String?> get() = _logoutError

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        Log.d(TAG, "Loading current user from repository")
        userRepository.getCurrentUser().observeForever { user ->
            _currentUser.value = user
            Log.d(TAG, "Repository returned user: ${user?.toString().orEmpty()}")
            if (user?.avatar.isNullOrEmpty()) {
//                TODO: Default Avatar
                _currentUser.value = user?.copy(avatar = "DEFAULT_AVATAR_URL")
            }
        }
    }

    fun logout() {
        try {
            val success = userRepository.logout()
            if (success) {
                _logoutSuccess.value = true
                _currentUser.value = null
            } else {
                _logoutError.value = "Logout failed"
            }
        } catch (e: Exception) {
            _logoutError.value = e.message
        }
    }

    fun updateProfile(username: String) {
        _currentUser.value?.let { current ->
            val updatedUser = current.copy(username = username)
            userRepository.updateUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }
}