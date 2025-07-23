package edu.bluejack24_2.domojo.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.repositories.AuthRepository
import edu.bluejack24_2.domojo.repositories.UserRepository
import java.io.File

class EditProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository(userRepository)

    private val _navigateToProfile = MutableLiveData<Boolean>()
    val navigateToProfile: LiveData<Boolean> get() = _navigateToProfile

    val username = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val confirmPassword = MutableLiveData<String>()

    val usernameError = MutableLiveData<String?>()
    val passwordError = MutableLiveData<String?>()
    val confirmPasswordError = MutableLiveData<String?>()
    val avatarError = MutableLiveData<String?>()

    val isLoading = MutableLiveData<Boolean>()
    val currentUser = MutableLiveData<User?>()

    fun loadCurrentUser() {
        authRepository.getCurrentUser().observeForever { user ->
            currentUser.value = user
            username.value = user?.username ?: ""
            email.value = user?.email ?: ""
        }
    }

    fun onEditProfileClicked(context: Context, imageFile: File?) {
        val usernameValue = username.value
        val passwordValue = password.value
        val confirmPasswordValue = confirmPassword.value

        // Reset errors
        usernameError.value = null
        passwordError.value = null
        confirmPasswordError.value = null
        avatarError.value = null

        // Validation
        if (usernameValue.isNullOrBlank()) {
            usernameError.value = "Username is required"
            return
        }

        if (passwordValue != null && passwordValue.isBlank()) {
            passwordError.value = "Password cannot be empty if provided"
            return
        }

        if (passwordValue != null && confirmPasswordValue != null &&
            passwordValue != confirmPasswordValue) {
            confirmPasswordError.value = "Passwords do not match"
            return
        }

        isLoading.value = true

        authRepository.updateCurrentUser(
            context = context,
            newUsername = usernameValue,
            newPassword = if (!passwordValue.isNullOrBlank()) passwordValue else null,
            newAvatarFile = imageFile,
            onSuccess = { updatedUser ->
                isLoading.value = false
                _navigateToProfile.value = true
            },
            onFailure = { error ->
                isLoading.value = false
                avatarError.value = error
            }
        )
    }
}