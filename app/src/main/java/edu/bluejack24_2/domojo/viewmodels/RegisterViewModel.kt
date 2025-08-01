package edu.bluejack24_2.domojo.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_2.domojo.repositories.AuthRepository
import edu.bluejack24_2.domojo.repositories.UserRepository
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import edu.bluejack24_2.domojo.views.ui.RegisterActivity
import java.io.File

class RegisterViewModel : ViewModel() {
    private var userRepository: UserRepository = UserRepository()
    private var authRepository: AuthRepository = AuthRepository(userRepository)

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> get() = _navigateToHome

    val username = MutableLiveData<String>()
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val confirmPassword = MutableLiveData<String>()

    val usernameError = MutableLiveData<String?>()
    val emailError = MutableLiveData<String?>()
    val passwordError = MutableLiveData<String?>()
    val confirmPasswordError = MutableLiveData<String?>()
    val profilePicError = MutableLiveData<String?>()

    val isLoading = MutableLiveData<Boolean>()

    fun onRegisterClicked(context: Context, image: File) {
        val usernameValue = username.value
        val emailValue = email.value
        val passwordValue = password.value
        val confirmPasswordValue = confirmPassword.value

        usernameError.value = null
        emailError.value = null
        passwordError.value = null
        confirmPasswordError.value = null

        if (usernameValue.isNullOrBlank()) {
            usernameError.value = "Username is required!"
            return
        }

        if (emailValue.isNullOrBlank()) {
            emailError.value = "Email is required!"
            return
        }

        if (passwordValue.isNullOrBlank()) {
            passwordError.value = "Password is required!"
            return
        }

        if (confirmPasswordValue.isNullOrBlank()) {
            confirmPasswordError.value = "Confirm Password is required!"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            emailError.value = "Invalid email address"
            return
        }

        if (passwordValue != confirmPasswordValue) {
            confirmPasswordError.value = "Passwords do not match"
            return
        }

        isLoading.value = true

        userRepository.isUsernameTaken(usernameValue, { exists ->
            if (exists) {
                isLoading.value = false
                usernameError.value = "Username already taken"
                return@isUsernameTaken
            }

            userRepository.isEmailTaken(emailValue, { exists ->
                if (exists) {
                    isLoading.value = false
                    emailError.value = "Email already taken"
                    return@isEmailTaken
                }

                authRepository.registerUser(
                    context = context,
                    username = usernameValue,
                    email = emailValue,
                    password = passwordValue,
                    profilePicFile = image,
                    onResult = { result ->
                        isLoading.value = false
                        if (result.isNotEmpty()) {
                            _navigateToHome.value = true
                            Log.i("Register Success", "Registration successful, navigating to home")
                        } else {
                            Log.e("Register Error", "Registration failed")
                            Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                )
            }, { error ->
                isLoading.value = false
                Log.e("Register Error", "Error checking email: $error")
                Toast.makeText(context, "Error checking email: $error", Toast.LENGTH_SHORT)
                    .show()
            })
        }, { error ->
            isLoading.value = false
            Log.e("Register Error", "Error checking username: $error")
            Toast.makeText(context, "Error checking username: $error", Toast.LENGTH_SHORT).show()
        })
    }
}