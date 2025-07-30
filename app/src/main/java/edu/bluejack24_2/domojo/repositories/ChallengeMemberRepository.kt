package edu.bluejack24_2.domojo.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.models.ChallengeMember
import java.util.Date

class ChallengeMemberRepository() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "ChallengeMemberRepo"

    // <Get Challenge Member Section>
    fun getChallengeMemberForChallenge(
        challengeId: String,
        onSuccess: (ChallengeMember?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            onSuccess(null)
            return
        }

        firestore.collection("challenge_members")
            .whereEqualTo("challengeId", challengeId)
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val member = querySnapshot.documents[0].toObject<ChallengeMember>()
                    onSuccess(member)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to get challenge member data from Firestore."
                Log.e(TAG, "Error fetching ChallengeMember for $challengeId, user $currentUserId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    // <Join Challenge Section>
    fun joinChallenge(
        challenge: Challenge,
        onSuccess: (ChallengeMember) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            onFailure("User not logged in.")
            return
        }

        val newMember = ChallengeMember(
            challengeId = challenge.id,
            userId = currentUserId,
            currentStreak = 0,
            longestStreak = 0,
            isActiveMember = true,
            hasCompleted = false,
        )

        firestore.collection("challenge_members")
            .add(newMember)
            .addOnSuccessListener { documentReference ->
                val createdMemberWithId = newMember.copy(id = documentReference.id)
                onSuccess(createdMemberWithId)
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to join challenge."
                Log.e(TAG, "Error joining challenge ${challenge.id}: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    // <Update Streak Section>
    fun updateStreak(
        challengeMemberId: String,
        newCurrentStreak: Int,
        newLongestStreak: Int,
        newIsActive: Boolean,
        newHasCompleted: Boolean,
        lastActivityDate: Date?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = hashMapOf(
            "currentStreak" to newCurrentStreak,
            "longestStreak" to newLongestStreak,
            "isActiveMember" to newIsActive,
            "hasCompleted" to newHasCompleted,
            "lastActivityDate" to lastActivityDate
        )
        firestore.collection("challenge_members").document(challengeMemberId)
            .update(updates as Map<String, Any>)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to update streak."
                Log.e(TAG, "Error updating streak for ChallengeMember $challengeMemberId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    // <Get Leaderboard Section>
    fun getLeaderboard(
        challengeId: String,
        onData: (List<ChallengeMember>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return firestore.collection("challenge_members")
            .whereEqualTo("challengeId", challengeId)
            .orderBy("longestStreak", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { querySnapshot, e ->
                if (e != null) {
                    val errorMessage = e.localizedMessage ?: "Failed to get real-time leaderboard."
                    Log.w(TAG, "Listen failed on leaderboard for $challengeId: $errorMessage", e)
                    onError(errorMessage)
                    return@addSnapshotListener
                }

                val leaderboard = mutableListOf<ChallengeMember>()
                if (querySnapshot != null) {
                    for (doc in querySnapshot.documents) {
                        val member = doc.toObject<ChallengeMember>()
                        if (member != null) {
                            leaderboard.add(member.copy(id = doc.id))
                        }
                    }
                    onData(leaderboard)
                } else {
                    onData(emptyList())
                }
            }
    }
}