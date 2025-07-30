package edu.bluejack24_2.domojo.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import edu.bluejack24_2.domojo.models.Post
import java.util.Calendar
import java.util.Date

class PostRepository() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val TAG = "ChallengePostRepo"
    private val postsCollection = firestore.collection("challenge_activity_posts")

    // <Add Activity Post Section>
    fun addActivityPost(
        post: Post,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        postsCollection.add(post)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to add activity post."
                Log.e(TAG, "Error adding activity post: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    // <Get Today's Streak Post Section>
    fun getTodayStreakAwardedPost(
        challengeId: String,
        userId: String,
        onSuccess: (Post?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.time

        postsCollection
            .whereEqualTo("challengeId", challengeId)
            .whereEqualTo("userId", userId)
            .whereEqualTo("streakAwarded", true)
            .whereGreaterThanOrEqualTo("createdAt", startOfToday)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val post = querySnapshot.documents[0].toObject<Post>()
                    onSuccess(post)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to check today's post status."
                Log.e(TAG, "Error checking today's post status: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    // <Get All Challenge Posts Section>
    fun getAllChallengePosts(
        challengeId: String,
        onSuccess: (List<Post>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        postsCollection
            .whereEqualTo("challengeId", challengeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val posts = mutableListOf<Post>()
                if (querySnapshot.isEmpty) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    try {
                        val post = document.toObject<Post>()
                        if (post != null) {
                            posts.add(post.copy(id = document.id)) // Ensure document ID is set
                        } else {
                            Log.w(TAG, "Document ${document.id} could not be parsed to Post.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document ${document.id}: ${e.message}", e)
                    }
                }
                onSuccess(posts)
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to load challenge posts."
                Log.e(TAG, "Failed to fetch posts for $challengeId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }
}