package edu.bluejack24_2.domojo.repositories

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import edu.bluejack24_2.domojo.models.PostComment

class PostCommentRepository() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val commentsCollection = firestore.collection("post_comments")
    private val postsCollection = firestore.collection("challenge_activity_posts")

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
                        onFailure(
                            e.localizedMessage ?: "Comment added, but failed to update count."
                        )
                    }
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to add comment."
                onFailure(errorMessage)
            }
    }

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
                onFailure(errorMessage)
            }
    }

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
                onFailure(errorMessage)
            }
    }
}