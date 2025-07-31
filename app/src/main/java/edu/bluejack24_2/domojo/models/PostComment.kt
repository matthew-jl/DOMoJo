package edu.bluejack24_2.domojo.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class PostComment(
    @DocumentId
    var id: String = "",
    var postId: String = "",
    var userId: String = "",
    var content: String = "",
    @ServerTimestamp
    var createdAt: Date? = null
)
