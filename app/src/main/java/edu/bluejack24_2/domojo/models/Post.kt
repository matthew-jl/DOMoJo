package edu.bluejack24_2.domojo.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    // The Firestore document ID for this post.
    @DocumentId
    var id: String = "",

    // Reference to the Challenge document this post belongs to.
    var challengeId: String = "",

    // Reference to the User who made this post.
    var userId: String = "",

    // Reference to the ChallengeMember document this post is associated with.
    // Useful for direct lookup if memberId is the document ID.
    var memberId: String = "",

    // The textual content of the post.
    var content: String = "",

    // URL of the image associated with the post.
    var imageUrl: String = "",

    // True if this post was counted towards the user's daily streak for this challenge.
    var streakAwarded: Boolean = false,
    @ServerTimestamp
    var createdAt: Date? = null,

    var likeCount: Int = 0, // Number of likes on this post
    var dislikeCount: Int = 0, // Number of dislikes on this post
    var commentCount: Int = 0 // Total number of comments on this post
)
