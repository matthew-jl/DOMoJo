package edu.bluejack24_2.domojo.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import edu.bluejack24_2.domojo.models.Post
import java.util.Calendar
import java.util.Date

class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val TAG = "ChallengePostRepo"
    private val postsCollection = firestore.collection("challenge_activity_posts")

    /**
     * Adds a new activity post for a challenge.
     */
    fun addActivityPost(
        post: Post,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        postsCollection.add(post) // Firestore generates document ID and @ServerTimestamp
            .addOnSuccessListener {
                Log.d(TAG, "Activity post added for challenge ${post.challengeId}, user ${post.userId}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to add activity post."
                Log.e(TAG, "Error adding activity post: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    /**
     * Checks if the user has already made a streak-awarded post for the given challenge today.
     */
    fun getTodayStreakAwardedPost(
        challengeId: String,
        userId: String,
        onSuccess: (Post?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = Date() // Current date/time
        // Set to the beginning of today (midnight) to query for posts made today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.time

        postsCollection
            .whereEqualTo("challengeId", challengeId)
            .whereEqualTo("userId", userId)
            .whereEqualTo("streakAwarded", true) // Only check for posts that awarded a streak
            .whereGreaterThanOrEqualTo("createdAt", startOfToday) // Posts created from today's midnight onwards
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val post = querySnapshot.documents[0].toObject<Post>()
                    Log.d(TAG, "Found today's streak-awarded post for challenge $challengeId, user $userId.")
                    onSuccess(post)
                } else {
                    Log.d(TAG, "No today's streak-awarded post found for challenge $challengeId, user $userId.")
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to check today's post status."
                Log.e(TAG, "Error checking today's post status: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    /**
     * Fetches all activity posts for a specific challenge, ordered by creation date (newest first).
     * This is used by the ChallengeDetailViewModel for the "Posts" tab.
     */
    fun getAllChallengePosts(
        challengeId: String,
        onSuccess: (List<Post>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(TAG, "getAllChallengePosts: Fetching posts for challenge ID: $challengeId")
        postsCollection
            .whereEqualTo("challengeId", challengeId) // Filter by the specific challenge
            .orderBy("createdAt", Query.Direction.DESCENDING) // Order by newest first
            .get()
            .addOnSuccessListener { querySnapshot ->
                val posts = mutableListOf<Post>()
                if (querySnapshot.isEmpty) {
                    Log.d(TAG, "getAllChallengePosts: No posts found for challenge $challengeId.")
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    try {
                        val post = document.toObject<Post>()
                        if (post != null) {
                            posts.add(post.copy(id = document.id)) // Ensure document ID is set
                            Log.d(TAG, "getAllChallengePosts: Parsed post ${post.id}: ${post.content}")
                        } else {
                            Log.w(TAG, "getAllChallengePosts: Document ${document.id} could not be parsed to ChallengeActivityPost.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "getAllChallengePosts: Error parsing document ${document.id}: ${e.message}", e)
                    }
                }
                Log.d(TAG, "getAllChallengePosts: Successfully fetched ${posts.size} posts for challenge $challengeId.")
                onSuccess(posts)
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to load challenge posts."
                Log.e(TAG, "getAllChallengePosts: Failed to fetch posts for $challengeId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }
}