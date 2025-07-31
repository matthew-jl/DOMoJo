package edu.bluejack24_2.domojo.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.models.ChallengeMember
import java.util.Calendar // Import Calendar
import java.util.Date

class ChallengeMemberRepository() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "ChallengeMemberRepo"

    private fun isDateToday(date: Date?): Boolean {
        if (date == null) return false
        val cal = Calendar.getInstance()
        val today = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        return date.after(today) || date.equals(today)
    }

    private fun isDateYesterday(date: Date?): Boolean {
        if (date == null) return false
        val cal = Calendar.getInstance()
        val yesterday = cal.apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        return date.before(todayStart) && date.after(yesterday) || date.equals(yesterday)
    }

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
                    var member = querySnapshot.documents[0].toObject<ChallengeMember>()?.copy(id = querySnapshot.documents[0].id)

                    if (member != null) {
                        val lastActivityDate = member.lastActivityDate
                        val currentStreak = member.currentStreak

                        if (currentStreak > 0) {
                            if (lastActivityDate == null || (!isDateToday(lastActivityDate) && !isDateYesterday(lastActivityDate))) {
                                Log.d(TAG, "Streak broken for member ${member.id}. Last activity: $lastActivityDate. Resetting streak to 0.")
                                member = member.copy(currentStreak = 0)
                                updateStreakInFirestore(
                                    member.id,
                                    0,
                                    member.longestStreak,
                                    member.isActiveMember,
                                    member.hasCompleted,
                                    member.lastActivityDate,
                                    onSuccess = {
                                        onSuccess(member)
                                    }
                                )
                                return@addOnSuccessListener
                            }
                            // If lastActivityDate is today or yesterday, streak is maintained.
                            // If it's yesterday, the user hasn't posted today yet, but the streak isn't broken.
                            // If it's today, streak is maintained.
                        }
                        onSuccess(member)
                    } else {
                        onSuccess(null)
                    }
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

    private fun updateStreakInFirestore(
        challengeMemberId: String,
        newCurrentStreak: Int,
        newLongestStreak: Int,
        newIsActive: Boolean,
        newHasCompleted: Boolean,
        lastActivityDate: Date?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {}
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
                Log.d(TAG, "Streak updated in Firestore for member $challengeMemberId. New streak: $newCurrentStreak")
                onSuccess()
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to update streak in Firestore."
                Log.e(TAG, "Error updating streak in Firestore for ChallengeMember $challengeMemberId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

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
        updateStreakInFirestore(
            challengeMemberId,
            newCurrentStreak,
            newLongestStreak,
            newIsActive,
            newHasCompleted,
            lastActivityDate,
            onSuccess,
            onFailure
        )
    }

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

    fun getUserMaxLongestStreak(
        userId: String,
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (userId.isBlank()) {
            onFailure("User ID cannot be blank.")
            return
        }

        firestore.collection("challenge_members")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    onSuccess(0)
                    return@addOnSuccessListener
                }

                val maxStreak = querySnapshot.documents.maxOfOrNull {
                    it.toObject<ChallengeMember>()?.longestStreak ?: 0
                } ?: 0

                Log.d(TAG, "User $userId max longest streak is $maxStreak")
                onSuccess(maxStreak)
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to get user's streak data."
                Log.e(TAG, "Error fetching max streak for user $userId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }
}