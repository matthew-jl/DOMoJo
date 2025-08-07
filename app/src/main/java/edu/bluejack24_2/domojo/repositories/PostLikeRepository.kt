package edu.bluejack24_2.domojo.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import edu.bluejack24_2.domojo.models.PostLike

class PostLikeRepository() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val likesCollection = firestore.collection("post_likes")
    private val postsCollection = firestore.collection("challenge_activity_posts")

    fun toggleLike(
        postId: String,
        type: String,
        onSuccess: (newLikeCount: Int, newDislikeCount: Int, currentUserAction: String?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            onFailure("User not logged in.")
            return
        }

        likesCollection
            .whereEqualTo("postId", postId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val existingLikeDoc = querySnapshot.documents.firstOrNull()
                val existingType = existingLikeDoc?.getString("type")
                val existingLikeDocId = existingLikeDoc?.id

                firestore.runTransaction { transaction ->
                    val postRef = postsCollection.document(postId)
                    val postSnapshot = transaction.get(postRef)

                    var currentLikes = postSnapshot.getLong("likeCount")?.toInt() ?: 0
                    var currentDislikes = postSnapshot.getLong("dislikeCount")?.toInt() ?: 0
                    var currentUserAction: String? = null

                    if (existingLikeDoc != null) {
                        val existingLikeRef = likesCollection.document(existingLikeDocId!!)

                        if (existingType == type) {
                            transaction.delete(existingLikeRef)
                            if (type == "like") currentLikes-- else currentDislikes--
                            currentUserAction = "none"
                        } else {
                            transaction.update(existingLikeRef, "type", type)
                            if (type == "like") {
                                currentLikes++
                                currentDislikes--
                                currentUserAction = "like"
                            } else {
                                currentLikes--
                                currentDislikes++
                                currentUserAction = "dislike"
                            }
                        }
                    } else {
                        transaction.set(likesCollection.document(), PostLike(postId = postId, userId = userId, type = type))
                        if (type == "like") currentLikes++ else currentDislikes++
                        currentUserAction = type
                    }

                    transaction.update(postRef, "likeCount", currentLikes)
                    transaction.update(postRef, "dislikeCount", currentDislikes)

                    Pair(Pair(currentLikes, currentDislikes), currentUserAction)
                }
                    .addOnSuccessListener { result ->
                        val newLikes = result.first.first
                        val newDislikes = result.first.second
                        val currentUserAction = result.second
                        onSuccess(newLikes, newDislikes, currentUserAction)
                    }
                    .addOnFailureListener { e ->
                        val errorMessage = e.localizedMessage ?: "Failed to execute transaction."
                        onFailure(errorMessage)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Failed to check existing interaction.")
            }
    }

    fun getUserLikeStatus(
        postId: String,
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        likesCollection
            .whereEqualTo("postId", postId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val like = querySnapshot.documents[0].toObject<PostLike>()
                    onSuccess(like?.type ?: "none")
                } else {
                    onSuccess("none")
                }
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to get like status."
                onFailure(errorMessage)
            }
    }
}