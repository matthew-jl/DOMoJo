package edu.bluejack24_2.domojo.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class PostLike(
    @DocumentId
    var id: String = "", // Firestore document ID for this like/dislike entry
    var postId: String = "", // ID of the post that was liked/disliked
    var userId: String = "", // ID of the user who performed the action
    var type: String = "", // "like" or "dislike"
    @ServerTimestamp
    var createdAt: Date? = null // Timestamp when the interaction occurred
)
