package edu.bluejack24_2.domojo.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import java.io.File

class AuthRepository(private val userRepository: UserRepository) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun loginUser(email: String, password: String,
                  onSuccess: () -> Unit,
                  onFailure: (String) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                    authResult ->
                val uid = authResult.user?.uid
                if(uid.isNullOrEmpty()) {
                    Log.w("AuthRepository", "Login failed: User ID is null or empty")
                    return@addOnSuccessListener
                }else{
                    userRepository.getUser(uid,
                        onSuccess = { user ->
                            Log.i("AuthRepository", "User exists: ${user?.username}")
                        },
                        onFailure = { error ->
                            Log.e("AuthRepository", "Failed to fetch user: $error")
                        })
                }
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "An unknown error occurred")
            }
    }

    fun registerUser(context: Context,
                     username: String,
                     email: String,
                     password: String,
                     profilePicFile: File,
                     onResult: (String) -> Unit) {

        CloudinaryClient.uploadImage(
            context = context,
            uri = Uri.fromFile(profilePicFile),
            onSuccess = { imageUrl ->
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid
                        if (uid != null) {
                            val newUser = User(
                                id = uid,
                                avatar = imageUrl,
                                email = email,
                                username = username
                            )

                            userRepository.addUser(newUser,
                                onSuccess = {
                                    onResult("Registration successful! Welcome, $username!")
                                    Log.i("AuthRepo", "User registered and data saved successfully: $newUser")
                                },
                                onFailure = { errorMessage ->
                                    onResult("Registration failed: $errorMessage")
                                    Log.e("AuthRepo", "Failed to save user data: $errorMessage")
                                }
                            )
                        } else {
                            onResult("Registration failed: User UID not found.")
                        }
                    }
                    .addOnFailureListener { e ->
                        // Firebase Auth registration failed
                        onResult(e.localizedMessage ?: "Registration failed: Invalid credentials or user exists.")
                    }
            },
            onError = { message ->
                // Cloudinary upload failed
                onResult("Profile picture upload failed: $message")
            }
        )
    }
}