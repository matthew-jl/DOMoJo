package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.repositories.AuthRepository
import edu.bluejack24_2.domojo.repositories.UserRepository

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository(userRepository)
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> get() = _currentUser

    private val _logoutSuccess = MutableLiveData<Boolean>()
    val logoutSuccess: LiveData<Boolean> get() = _logoutSuccess

    private val _logoutError = MutableLiveData<String?>()
    val logoutError: LiveData<String?> get() = _logoutError

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> get() = _deleteSuccess

    private val _deleteError = MutableLiveData<String?>()
    val deleteError: LiveData<String?> get() = _deleteError

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        authRepository.getCurrentUser().observeForever { user ->
            _currentUser.value = user
            if (user?.avatar.isNullOrEmpty()) {
                _currentUser.value = user?.copy(avatar = "DEFAULT_AVATAR_URL")
            }
        }
    }

    fun logout() {
        try {
            val success = authRepository.logout()
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

    fun deleteAccount() {
        authRepository.deleteCurrentUser(
            onSuccess = {
                _deleteSuccess.postValue(true)
                _currentUser.postValue(null)
            },
            onFailure = { error ->
                _deleteError.postValue(error)
            }
        )
    }
}