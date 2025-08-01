package edu.bluejack24_2.domojo.repositories

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_2.domojo.models.User

class UserRepository {
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun addUser(user: User, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        firestore.collection("users").document(user.id).set(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Failed to save user data to Firestore.")
            }
    }

    fun getUser(uid: String, onSuccess: (User?) -> Unit, onFailure: (String) -> Unit) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                onSuccess(user)
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Failed to fetch user data.")
            }
    }

    fun updateUser(user: User, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (user.id.isEmpty()) {
            onFailure("User ID cannot be empty")
            return
        }

        firestore.collection("users").document(user.id)
            .set(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Failed to update user data")
            }
    }

    fun deleteUser(uid: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        firestore.collection("users").document(uid).delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Failed to delete user data from Firestore.")
            }
    }

    fun updateUserBadge(uid: String, badge: String?, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val updates = hashMapOf<String, Any>()
        if (badge != null) {
            updates["badge"] = badge
        } else {
            updates["badge"] = FieldValue.delete()
        }

        firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Failed to update badge")
            }
    }

    fun isUsernameTaken(username: String, onResult: (Boolean) -> Unit, onError: (String) -> Unit) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                onResult(!documents.isEmpty)
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Failed to check username")
            }
    }

    fun isEmailTaken(email: String, onResult: (Boolean) -> Unit, onError: (String) -> Unit) {
        firestore.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                onResult(!documents.isEmpty)
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Failed to check email")
            }
    }
}