package edu.bluejack24_2.domojo.models

import com.google.firebase.firestore.DocumentId

data class ChallengeMember(
    @DocumentId
    val id: String = "",
    // Reference to the Challenge document this member is associated with
    val challengeId: String = "",

    // Reference to the User document who is the member
    val userId: String = "",

    // Current consecutive streak count
    val currentStreak: Int = 0,

    // Longest streak achieved for this challenge
    val longestStreak: Int = 0,

    // True if user is actively maintaining a streak, false if current streak is 0
    val isActiveMember: Boolean = true,

    // True if user has posted a streak update for the current day, false otherwise
    val hasCompleted: Boolean = false
)
