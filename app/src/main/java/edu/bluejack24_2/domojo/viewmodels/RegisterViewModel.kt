package edu.bluejack24_2.domojo.viewmodels

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import edu.bluejack24_2.domojo.views.ui.RegisterActivity
import java.io.File

class RegisterViewModel : ViewModel() {
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

    private lateinit var activity: RegisterActivity
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    fun setActivity(activity: RegisterActivity) {
        this.activity = activity
    }

    fun onRegisterClicked(image: File){
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

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

        if( passwordValue.isNullOrBlank()) {
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

        if (passwordValue != confirmPasswordValue){
            confirmPasswordError.value = "Passwords do not match"
            return
        }

        firestore.collection("users").whereEqualTo("username", usernameValue)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.d("USERNAME_NOT_FOUND", "Username is not in Firestore collection")
                } else {
                    usernameError.value = "Username must be unique!"
                    Log.d("USERNAME_FOUND", "Username is already in Firestore collection")
                    return@addOnSuccessListener
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FIRESTORE_ERROR", "Error checking Firestore collection (username)", exception)
                return@addOnFailureListener
            }

        firestore.collection("users").whereEqualTo("email", emailValue)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.d("EMAIL_NOT_FOUND", "Email is not in Firestore collection")
                } else {
                    emailError.value = "Email must be unique!"
                    Log.d("EMAIL_FOUND", "Email is already in Firestore collection")
                    return@addOnSuccessListener
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FIRESTORE_ERROR", "Error checking Firestore collection (email)", exception)
                return@addOnFailureListener
            }

        if(emailError.value != null){
            return
        }else if(usernameError.value != null) {
            return
        }else{
            CloudinaryClient.uploadImage(
                context = activity,
                Uri.fromFile(image),
                onSuccess = { result ->
                    firebaseAuth.createUserWithEmailAndPassword(emailValue, passwordValue)
                        .addOnCompleteListener(activity) { task ->
                            if (task.isSuccessful) {
                                Log.d("REGISTER_SUCCESS", "User registered successfully")
                                val userId = firebaseAuth.currentUser?.uid
                                userId?.let {
                                    val user = hashMapOf(
                                        "username" to usernameValue,
                                        "email" to emailValue,
                                        "avatar" to result,
                                    )

                                    firestore.collection("users")
                                        .document(userId)
                                        .set(user)
                                        .addOnSuccessListener {
                                            Log.d("FIRESTORE_SUCCESS", "User data saved successfully")
                                            _navigateToHome.value = true
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("FIRESTORE_ERROR", "Error saving user data", e)
                                        }
                                }
                            } else {
                                Log.w("REGISTER_ERROR", "User registration failed", task.exception)
                            }
                        }
                },
                onError = { message ->
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}