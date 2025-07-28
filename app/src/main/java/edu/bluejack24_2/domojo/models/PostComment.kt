package edu.bluejack24_2.domojo.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class PostComment(
    @DocumentId
    var id: String = "", // Firestore document ID for this comment
    var postId: String = "", // ID of the post this comment belongs to
    var userId: String = "", // ID of the user who made the comment
    var content: String = "", // The actual text content of the comment
    @ServerTimestamp
    var createdAt: Date? = null // Timestamp when the comment was created
)
