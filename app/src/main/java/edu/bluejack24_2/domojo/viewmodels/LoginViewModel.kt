package edu.bluejack24_2.domojo.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import edu.bluejack24_2.domojo.views.ui.LoginActivity

class LoginViewModel : ViewModel() {
    val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> get() = _navigateToHome

    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    val emailError = MutableLiveData<String?>()
    val passwordError = MutableLiveData<String?>()

    private lateinit var activity: LoginActivity
    private lateinit var firebaseAuth: FirebaseAuth

    fun setActivity(activity: LoginActivity) {
        this.activity = activity
    }

    fun onLoginClicked() {
        firebaseAuth = FirebaseAuth.getInstance()

        val emailValue = email.value
        val passwordValue = password.value

        emailError.value = null
        passwordError.value = null

        if (emailValue.isNullOrBlank()) {
            emailError.value = "Email is required!"
            return
        }

        if(passwordValue.isNullOrBlank()){
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

            firebaseAuth.signInWithEmailAndPassword(
                email.value.toString(),
                password.value.toString()
            )
                .addOnSuccessListener {
                    _navigateToHome.value = true
                }
                .addOnFailureListener { exception ->
                    Log.w("FIRESTORE_ERROR", "Error checking Firestore collection (email)", exception)
                }

        }
    }
}