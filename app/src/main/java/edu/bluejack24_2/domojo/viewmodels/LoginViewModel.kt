package edu.bluejack24_2.domojo.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.repositories.AuthRepository
import edu.bluejack24_2.domojo.repositories.UserRepository

class LoginViewModel : ViewModel() {
    private val userRepository: UserRepository = UserRepository()
    private val authRepository: AuthRepository = AuthRepository(userRepository)

    val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> get() = _navigateToHome

    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    val emailError = MutableLiveData<String?>()
    val passwordError = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>()

    fun login() {
        val emailValue = email.value
        val passwordValue = password.value

        emailError.value = null
        passwordError.value = null

        if (emailValue.isNullOrBlank()) {
            emailError.value = "Email is required!"
            return
        }

        if (passwordValue.isNullOrBlank()) {
            passwordError.value = "Password is required!"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            emailError.value = "Invalid email address"
            return
        }

        if (emailValue.isNotBlank() && passwordValue.isNotBlank()) {
            emailError.value = null
            passwordError.value = null

            authRepository.loginUser(
                email = emailValue,
                password = passwordValue,
                onSuccess = {
                    isLoading.value = false
                    _navigateToHome.value = true
                    Log.i("Login Success", "Login successful, navigating to home")
                },
                onFailure = { errorMessage ->
                    isLoading.value = false
                    passwordError.value = errorMessage
                    _navigateToHome.value = false
                    Log.e("Login Error", errorMessage)
                }
            )
        }
    }
}