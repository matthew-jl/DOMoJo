package edu.bluejack24_2.domojo.models

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class ChallengeMember(
    @DocumentId
    val id: String = "",
    val challengeId: String = "",
    val userId: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isActiveMember: Boolean = true,
    val hasCompleted: Boolean = false,
    var lastActivityDate: Date? = null
)
