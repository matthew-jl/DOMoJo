package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_2.domojo.models.ChallengeMember
import edu.bluejack24_2.domojo.models.JoinedChallengeDisplay
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.repositories.ChallengeRepository
import edu.bluejack24_2.domojo.repositories.UserRepository
import java.util.Calendar
import java.util.Date

class ProfileOthersViewModel : ViewModel() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()
    private val challengeRepository = ChallengeRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _ongoingChallenges = MutableLiveData<List<JoinedChallengeDisplay>>()
    val ongoingChallenges: LiveData<List<JoinedChallengeDisplay>> get() = _ongoingChallenges

    private val _isLoadingUser = MutableLiveData<Boolean>()
    val isLoadingUser: LiveData<Boolean> get() = _isLoadingUser

    private val _isLoadingChallenges = MutableLiveData<Boolean>()
    val isLoadingChallenges: LiveData<Boolean> get() = _isLoadingChallenges

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _navigateToChallengeDetail = MutableLiveData<String?>()
    val navigateToChallengeDetail: LiveData<String?> get() = _navigateToChallengeDetail

    fun loadUserProfile(userId: String) {
        if (userId.isBlank()) {
            _errorMessage.value = "User ID is missing."
            return
        }
        fetchUserData(userId)
        fetchUserChallenges(userId)
    }

    private fun fetchUserData(userId: String) {
        _isLoadingUser.value = true
        userRepository.getUser(userId,
            onSuccess = { fetchedUser ->
                _user.value = fetchedUser
                _isLoadingUser.value = false
            },
            onFailure = { error ->
                _errorMessage.value = "Failed to load user profile: $error"
                _isLoadingUser.value = false
            }
        )
    }

    private fun fetchUserChallenges(userId: String) {
        _isLoadingChallenges.value = true
        _errorMessage.value = null

        firestore.collection("challenge_members")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    _ongoingChallenges.value = emptyList()
                    _isLoadingChallenges.value = false
                    return@addOnSuccessListener
                }

                val memberList = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChallengeMember::class.java)?.copy(id = doc.id)
                }

                val challengeDisplays = mutableListOf<JoinedChallengeDisplay>()
                var processedCount = 0
                val totalChallenges = memberList.size

                if (totalChallenges == 0) {
                    _ongoingChallenges.value = emptyList()
                    _isLoadingChallenges.value = false
                    return@addOnSuccessListener
                }

                memberList.forEach { member ->
                    challengeRepository.getChallenge(member.challengeId,
                        onSuccess = { challenge ->
                            processedCount++
                            if (challenge != null) {
                                val hasPostedToday = isLastActivityToday(member.lastActivityDate)
                                challengeDisplays.add(JoinedChallengeDisplay(challenge, member, hasPostedToday))
                            }
                            if (processedCount == totalChallenges) {
                                // Sort by title
                                _ongoingChallenges.value = challengeDisplays.sortedBy { it.challenge.title }
                                _isLoadingChallenges.value = false
                            }
                        },
                        onFailure = { error ->
                            processedCount++
                            if (processedCount == totalChallenges) {
                                _ongoingChallenges.value = challengeDisplays.sortedBy { it.challenge.title }
                                _isLoadingChallenges.value = false
                            }
                        }
                    )
                }
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Failed to fetch user's challenges."
                _isLoadingChallenges.value = false
            }
    }

    private fun isLastActivityToday(lastActivityDate: Date?): Boolean {
        if (lastActivityDate == null) return false
        val lastActivityCal = Calendar.getInstance().apply { time = lastActivityDate }
        val todayCal = Calendar.getInstance()
        return lastActivityCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                lastActivityCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }

    fun onChallengeClicked(challengeId: String) {
        _navigateToChallengeDetail.value = challengeId
    }

    fun onNavigationToChallengeDetailHandled() {
        _navigateToChallengeDetail.value = null
    }
}