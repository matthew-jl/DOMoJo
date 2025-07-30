package edu.bluejack24_2.domojo.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    @DocumentId
    var id: String = "",
    var challengeId: String = "",
    var userId: String = "",
    var memberId: String = "",
    var content: String = "",
    var imageUrl: String = "",
    var streakAwarded: Boolean = false,
    @ServerTimestamp
    var createdAt: Date? = null,
    var likeCount: Int = 0,
    var dislikeCount: Int = 0,
    var commentCount: Int = 0
)
