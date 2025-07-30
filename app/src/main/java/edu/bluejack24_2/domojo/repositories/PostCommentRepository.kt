package edu.bluejack24_2.domojo.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import edu.bluejack24_2.domojo.models.PostComment
import edu.bluejack24_2.domojo.models.Post

class PostCommentRepository() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "PostCommentRepo"
    private val commentsCollection = firestore.collection("post_comments")
    private val postsCollection = firestore.collection("challenge_activity_posts")

    // <Add Comment Section>
    fun addComment(
        comment: PostComment,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (comment.userId.isBlank()) {
            onFailure("User ID is missing in comment object.")
            return
        }

        commentsCollection.add(comment)
            .addOnSuccessListener {
                postsCollection.document(comment.postId)
                    .update("commentCount", FieldValue.increment(1))
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            TAG,
                            "Failed to increment comment count for post ${comment.postId}: ${e.message}",
                            e
                        )
                        onFailure(
                            e.localizedMessage ?: "Comment added, but failed to update count."
                        )
                    }
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to add comment."
                Log.e(TAG, "Error adding comment: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    // <Get Recent Comments Section>
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
                onSuccess(comments)
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to get recent comments."
                Log.e(TAG, "Error getting recent comments for post $postId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    // <Get All Comments Section>
    fun getAllComments(
        postId: String,
        onSuccess: (List<PostComment>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        commentsCollection
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val comments = mutableListOf<PostComment>()
                for (doc in querySnapshot.documents) {
                    val comment = doc.toObject<PostComment>()
                    if (comment != null) {
                        comments.add(comment.copy(id = doc.id))
                    }
                }
                onSuccess(comments)
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to get all comments."
                Log.e(TAG, "Error getting all comments for post $postId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }
}