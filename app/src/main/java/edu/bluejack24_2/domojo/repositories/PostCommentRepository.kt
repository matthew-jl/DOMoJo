package edu.bluejack24_2.domojo.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import edu.bluejack24_2.domojo.models.PostComment // The new PostComment model
import edu.bluejack24_2.domojo.models.Post // Your main Post model

class PostCommentRepository() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "PostCommentRepo"
    private val commentsCollection = firestore.collection("post_comments")
    private val postsCollection = firestore.collection("challenge_activity_posts")

    fun addComment(
        comment: PostComment, // <--- CHANGED: Now accepts the PostComment object
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Validation: Check userId directly from the passed comment object
        if (comment.userId.isBlank()) { // Use comment.userId directly
            onFailure("User ID is missing in comment object.") // More specific error
            return
        }

        commentsCollection.add(comment) // <--- Use the 'comment' object passed as parameter
            .addOnSuccessListener {
                // Increment comment count on the main post document
                postsCollection.document(comment.postId) // Use comment.postId
                    .update("commentCount", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.d(TAG, "Comment added and count incremented for post ${comment.postId}.")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to increment comment count for post ${comment.postId}: ${e.message}", e)
                        onFailure(e.localizedMessage ?: "Comment added, but failed to update count.")
                    }
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to add comment."
                Log.e(TAG, "Error adding comment: $errorMessage", e)
                onFailure(errorMessage)
            }
    }


    /**
     * Fetches a limited number of most recent comments for a given post.
     * Used for initial display of comments directly on the post item.
     * @param limit The maximum number of comments to fetch.
     */
    fun getRecentComments(
        postId: String,
        limit: Int,
        onSuccess: (List<PostComment>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        commentsCollection
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                val comments = mutableListOf<PostComment>()
                for (doc in querySnapshot.documents) {
                    val comment = doc.toObject<PostComment>()
                    if (comment != null) {
                        comments.add(comment.copy(id = doc.id))
                    }
                }
                Log.d(TAG, "Fetched ${comments.size} recent comments for post $postId (limit $limit).")
                onSuccess(comments)
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to get recent comments."
                Log.e(TAG, "Error getting recent comments for post $postId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    /**
     * Fetches all comments for a given post.
     * This would typically be used for a separate "All Comments" screen or dialog.
     */
    fun getAllComments(
        postId: String,
        onSuccess: (List<PostComment>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        commentsCollection
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.ASCENDING) // Oldest first for chronological display
            .get()
            .addOnSuccessListener { querySnapshot ->
                val comments = mutableListOf<PostComment>()
                for (doc in querySnapshot.documents) {
                    val comment = doc.toObject<PostComment>()
                    if (comment != null) {
                        comments.add(comment.copy(id = doc.id))
                    }
                }
                Log.d(TAG, "Fetched all ${comments.size} comments for post $postId.")
                onSuccess(comments)
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to get all comments."
                Log.e(TAG, "Error getting all comments for post $postId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }
}