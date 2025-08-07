package edu.bluejack24_2.domojo.models

data class JoinedChallengeDisplay(
    val challenge: Challenge,
    val member: ChallengeMember,
    val hasPostedToday: Boolean
)