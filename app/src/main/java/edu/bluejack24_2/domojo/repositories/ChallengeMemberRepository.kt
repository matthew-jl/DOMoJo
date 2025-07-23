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

    fun getChallengeMemberForChallenge(
        challengeId: String,
        onSuccess: (ChallengeMember?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            Log.d(TAG, "No user logged in. Not fetching challenge member for $challengeId.")
            onSuccess(null) // Cannot be a member if no user is logged in
            return
        }

        // Query the 'challenge_members' collection
        // looking for documents where 'challengeId' matches and 'userId' matches the current user.
        firestore.collection("challenge_members")
            .whereEqualTo("challengeId", challengeId)
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Found a document, deserialize the first one (should be unique for a user-challenge pair)
                    val member = querySnapshot.documents[0].toObject<ChallengeMember>()
                    Log.d(TAG, "Found ChallengeMember for challenge $challengeId, user $currentUserId. Current Streak: ${member?.currentStreak}")
                    onSuccess(member)
                } else {
                    // No matching document found, user is not a member of this specific challenge
                    Log.d(TAG, "No ChallengeMember document found for challenge $challengeId, user $currentUserId.")
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
        challenge: Challenge, // Pass the Challenge object to ensure correct data
        onSuccess: (ChallengeMember) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            onFailure("User not logged in.")
            return
        }

        // Create the ChallengeMember object to be saved
        val newMember = ChallengeMember(
            challengeId = challenge.id, // Use the ID from the Challenge object
            userId = currentUserId,
            currentStreak = 0, // New member starts with 0 streak
            longestStreak = 0, // Longest streak also starts at 0
            isActiveMember = true,
            hasCompleted = false,
        )

        // Add a new document to the challenge_members collection. Firestore will generate the ID.
        firestore.collection("challenge_members")
            .add(newMember)
            .addOnSuccessListener { documentReference ->
                // After success, create a copy of the newMember with the Firestore-generated ID
                val createdMemberWithId = newMember.copy(id = documentReference.id)
                Log.d(TAG, "User ${currentUserId} joined challenge ${challenge.id}. Member ID: ${createdMemberWithId.id}")
                onSuccess(createdMemberWithId) // Pass the member with its new ID
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to join challenge."
                Log.e(TAG, "Error joining challenge ${challenge.id}: $errorMessage", e)
                onFailure(errorMessage)
            }
    }

    /**
     * Updates an existing ChallengeMember's streak data and activity status after a post.
     */
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
            .update(updates as Map<String, Any>) // Cast needed for HashMap type safety
            .addOnSuccessListener {
                Log.d(TAG, "Updated streak for ChallengeMember $challengeMemberId. Current: $newCurrentStreak, Longest: $newLongestStreak")
                onSuccess()
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to update streak."
                Log.e(TAG, "Error updating streak for ChallengeMember $challengeMemberId: $errorMessage", e)
                onFailure(errorMessage)
            }
    }


    /**
     * Provides a real-time leaderboard for a specific challenge.
     * Uses addSnapshotListener for real-time updates.
     */
    fun getLeaderboard(
        challengeId: String,
        onData: (List<ChallengeMember>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return firestore.collection("challenge_members")
            .whereEqualTo("challengeId", challengeId)
            .orderBy("longestStreak", Query.Direction.DESCENDING) // Order by longest streak
            .limit(10) // Optional: limit to top 10
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
                            leaderboard.add(member.copy(id = doc.id)) // Ensure ID is set
                        }
                    }
                    Log.d(TAG, "Leaderboard for $challengeId updated. Size: ${leaderboard.size}")
                    onData(leaderboard)
                } else {
                    Log.d(TAG, "Leaderboard for $challengeId is null (no data).")
                    onData(emptyList())
                }
            }
    }
}