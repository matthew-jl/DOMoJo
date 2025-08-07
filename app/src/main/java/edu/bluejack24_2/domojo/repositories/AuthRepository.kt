package edu.bluejack24_2.domojo.repositories

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import java.io.File

class AuthRepository(private val userRepository: UserRepository) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getCurrentUser(): LiveData<User?> {
        val result = MutableLiveData<User?>()
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            userRepository.getUser(
                firebaseUser.uid,
                onSuccess = { user ->
                    result.value = user?.copy(id = firebaseUser.uid) ?: User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        username = firebaseUser.displayName ?: ""
                    )
                },
                onFailure = { error ->
                    result.value = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        username = firebaseUser.displayName ?: ""
                    )
                }
            )
        } else {
            result.value = null
        }

        return result
    }

    fun logout(): Boolean {
        return try {
            firebaseAuth.signOut()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun updateCurrentUser(
        context: Context,
        newUsername: String,
        newPassword: String? = null,
        newAvatarFile: File? = null,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            onFailure("No user is currently logged in")
            return
        }

        fun getCurrentAvatarUrl(onSuccess: (String?) -> Unit, onFailure: (String) -> Unit) {
            userRepository.getUser(
                currentUser.uid,
                onSuccess = { user ->
                    onSuccess(user?.avatar)
                },
                onFailure = { error ->
                    onFailure("Failed to fetch current avatar: $error")
                }
            )
        }

        // Function to handle the final user update
        fun updateUserData(avatarUrl: String? = null) {
            getCurrentAvatarUrl(
                onSuccess = { currentAvatarUrl ->
                    val updatedUser = User(
                        id = currentUser.uid,
                        avatar = avatarUrl ?: currentAvatarUrl ?: "",
                        email = currentUser.email ?: "",
                        username = newUsername
                    )

                    // Update in Firestore
                    userRepository.updateUser(
                        updatedUser,
                        onSuccess = {
                            // Update password if provided
                            newPassword?.let { password ->
                                currentUser.updatePassword(password)
                                    .addOnSuccessListener {
                                        onSuccess(updatedUser)
                                    }
                                    .addOnFailureListener { e ->
                                        onFailure("Password update failed: ${e.localizedMessage}")
                                    }
                            } ?: run {
                                onSuccess(updatedUser)
                            }
                        },
                        onFailure = { error ->
                            onFailure("User data update failed: $error")
                        }
                    )
                },
                onFailure = { error ->
                    onFailure(error)
                }
            )
        }

        // Handle avatar upload if provided
        newAvatarFile?.let { file ->
            CloudinaryClient.uploadImage(
                context = context,
                uri = Uri.fromFile(file),
                onSuccess = { imageUrl ->
                    updateUserData(imageUrl)
                },
                onError = { message ->
                    onFailure("Avatar upload failed: $message")
                }
            )
        } ?: run {
            updateUserData()
        }
    }

    fun updateCurrentUserBadge(
        newBadge: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            onFailure("No user is currently logged in")
            return
        }

        userRepository.updateUserBadge(
            currentUser.uid,
            newBadge,
            onSuccess = {
                onSuccess()
            },
            onFailure = { error ->
                onFailure("Failed to update badge: $error")
            }
        )
    }

    fun deleteCurrentUser(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            onFailure("No user is currently logged in")
            return
        }

        val uid = currentUser.uid

        currentUser.delete()
            .addOnSuccessListener {

                userRepository.deleteUser(uid,
                    onSuccess = {
                        onSuccess()
                    },
                    onFailure = { firestoreError ->
                        val errorMessage = "CRITICAL: Auth account deleted, but failed to delete Firestore data: $firestoreError"
                        onFailure(errorMessage)
                    }
                )
            }
            .addOnFailureListener { authError ->
                val errorMessage = "Failed to delete authentication account. Please re-authenticate and try again: ${authError.localizedMessage}"
                onFailure(errorMessage)
            }
    }

    fun loginUser(
        email: String, password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid.isNullOrEmpty()) {
                    return@addOnSuccessListener
                } else {
                    userRepository.getUser(
                        uid,
                        onSuccess = { user ->
                        },
                        onFailure = { error ->
                        })
                }
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "An unknown error occurred")
            }
    }

    fun registerUser(
        context: Context,
        username: String,
        email: String,
        password: String,
        profilePicFile: File,
        onResult: (String) -> Unit
    ) {

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
                                username = username,
                                badge = null,
                            )

                            userRepository.addUser(
                                newUser,
                                onSuccess = {
                                    onResult("Registration successful! Welcome, $username!")
                                },
                                onFailure = { errorMessage ->
                                    onResult("Registration failed: $errorMessage")
                                }
                            )
                        } else {
                            onResult("Registration failed: User UID not found.")
                        }
                    }
                    .addOnFailureListener { e ->
                        // Firebase Auth registration failed
                        onResult(
                            e.localizedMessage
                                ?: "Registration failed: Invalid credentials or user exists."
                        )
                    }
            },
            onError = { message ->
                onResult("Profile picture upload failed: $message")
            }
        )
    }
}