package edu.bluejack24_2.domojo.models

import com.google.firebase.firestore.DocumentId

data class Challenge(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val bannerUrl: String = "",

    var isJoined: Boolean = false,
    var userCurrentStreak: Int = 0,
)
