package edu.bluejack24_2.domojo.models

import com.google.firebase.firestore.DocumentId

data class Challenge(
    @DocumentId
    var id: String = "",
    var title: String = "",
    var category: String = "",
    var description: String = "",
    var iconUrl: String = "",
    var bannerUrl: String = "",

    var isJoined: Boolean = false,
    var userCurrentStreak: Int = 0,
)
