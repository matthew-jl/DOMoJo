package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_2.domojo.models.ChallengeMember
import edu.bluejack24_2.domojo.models.JoinedChallengeDisplay
import edu.bluejack24_2.domojo.repositories.ChallengeRepository
import java.util.Calendar
import java.util.Date

class HomeViewModel : ViewModel() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val challengeRepository: ChallengeRepository = ChallengeRepository()

    private val _joinedChallenges = MutableLiveData<List<JoinedChallengeDisplay>>()
    val joinedChallenges: LiveData<List<JoinedChallengeDisplay>> get() = _joinedChallenges
    private val _allJoinedChallengesMasterList = MutableLiveData<List<JoinedChallengeDisplay>>()

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    val selectedCategoryFilter = MutableLiveData<String?>()
    val sortByStreakDirection = MutableLiveData<String?>()

    private val _availableCategories = MutableLiveData<List<String>>()
    val availableCategories: LiveData<List<String>> get() = _availableCategories

    private val _navigateToChallengeDetail = MutableLiveData<String?>()
    val navigateToChallengeDetail: LiveData<String?> get() = _navigateToChallengeDetail

    init {
        selectedCategoryFilter.observeForever {
            filterAndSortChallenges()
        }
        sortByStreakDirection.observeForever {
            filterAndSortChallenges()
        }

        fetchJoinedChallenges()
    }

    fun fetchJoinedChallenges() {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _joinedChallenges.value = emptyList()
            _errorMessage.value = "Please log in to see your challenges."
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        firestore.collection("challenge_members")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    _joinedChallenges.value = emptyList()
                    _isLoading.value = false
                    filterAndSortChallenges()
                    fetchAvailableCategories()
                    return@addOnSuccessListener
                }

                val memberList = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChallengeMember::class.java)?.copy(id = doc.id)
                }

                val joinedChallengeDisplays = mutableListOf<JoinedChallengeDisplay>()
                var challengesProcessed = 0
                val totalMembers = memberList.size

                if (totalMembers == 0) {
                    _joinedChallenges.value = emptyList()
                    _isLoading.value = false
                    filterAndSortChallenges()
                    fetchAvailableCategories()
                    return@addOnSuccessListener
                }

                memberList.forEach { member ->
                    challengeRepository.getChallenge(
                        member.challengeId,
                        onSuccess = { challenge ->
                            challengesProcessed++
                            if (challenge != null) {
                                val hasPostedToday = isLastActivityToday(member.lastActivityDate)
                                joinedChallengeDisplays.add(
                                    JoinedChallengeDisplay(challenge, member, hasPostedToday)
                                )
                            } else {
                            }

                            if (challengesProcessed == totalMembers) {
                                _allJoinedChallengesMasterList.value = joinedChallengeDisplays.sortedBy { it.challenge.id }
                                _isLoading.value = false
                                filterAndSortChallenges()
                                fetchAvailableCategories()
                            }
                        },
                        onFailure = { challengeError ->
                            challengesProcessed++
                            if (challengesProcessed == totalMembers) {
                                _joinedChallenges.value = joinedChallengeDisplays
                                _isLoading.value = false
                                filterAndSortChallenges()
                                fetchAvailableCategories()
                            }
                        }
                    )
                }
            }
            .addOnFailureListener { e ->
                val errorMessage = e.localizedMessage ?: "Failed to fetch joined challenges from Firestore."
                _errorMessage.value = errorMessage
                _isLoading.value = false
                _joinedChallenges.value = emptyList()
                _allJoinedChallengesMasterList.value = emptyList()
                filterAndSortChallenges()
            }
    }

    private fun filterAndSortChallenges() {
        val masterList = _allJoinedChallengesMasterList.value ?: emptyList()

        val currentCategoryFilter = selectedCategoryFilter.value
        val currentSortDirection = sortByStreakDirection.value

        var filteredList = masterList.filter { display ->
            if (currentCategoryFilter.isNullOrBlank()) {
                true
            } else {
                display.challenge.category.equals(currentCategoryFilter, ignoreCase = true)
            }
        }

        filteredList = when (currentSortDirection) {
            "asc" -> filteredList.sortedBy { it.member.currentStreak }
            "desc" -> filteredList.sortedByDescending { it.member.currentStreak }
            else -> filteredList.sortedByDescending { it.challenge.id }
        }
        _joinedChallenges.value = filteredList
    }

    fun setCategoryFilter(category: String?) {
        selectedCategoryFilter.value = category
    }

    fun setSortByStreakDirection(direction: String?) {
        sortByStreakDirection.value = direction
    }

    fun fetchAvailableCategories() {
        val categories = _allJoinedChallengesMasterList.value?.map { it.challenge.category }?.distinct()?.sorted() ?: emptyList()
        _availableCategories.value = categories
    }

    fun onChallengeItemClicked(challengeId: String) {
        _navigateToChallengeDetail.value = challengeId
    }

    fun onNavigationToChallengeDetailHandled() {
        _navigateToChallengeDetail.value = null
    }

    private fun isLastActivityToday(lastActivityDate: Date?): Boolean {
        if (lastActivityDate == null) return false

        val lastActivityCal = Calendar.getInstance().apply { time = lastActivityDate }
        val todayCal = Calendar.getInstance()

        return lastActivityCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                lastActivityCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH) &&
                lastActivityCal.get(Calendar.DAY_OF_MONTH) == todayCal.get(Calendar.DAY_OF_MONTH)
    }
}