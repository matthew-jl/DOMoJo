package edu.bluejack24_2.domojo.repositories

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

    fun deleteUser(uid: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        firestore.collection("users").document(uid).delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Failed to delete user data from Firestore.")
            }
    }
}