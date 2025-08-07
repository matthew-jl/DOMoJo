package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.repositories.ChallengeMemberRepository
import edu.bluejack24_2.domojo.repositories.ChallengeRepository

class ChallengeViewModel: ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val challengeRepository: ChallengeRepository = ChallengeRepository()
    private val challengeMemberRepository: ChallengeMemberRepository = ChallengeMemberRepository()

    private val _navigateToChallengeDetail = MutableLiveData<String>()
    val navigateToChallengeDetail: LiveData<String> get() = _navigateToChallengeDetail

    private val _challengeList = MutableLiveData<List<Challenge>>()
    val challengeList: LiveData<List<Challenge>> get() = _challengeList

    private val _allChallenges = MutableLiveData<List<Challenge>>()

    val searchQuery = MutableLiveData<String>("")
    val selectedCategoryFilter = MutableLiveData<String?>()

    private val _availableCategories = MutableLiveData<List<String>>()
    val availableCategories: LiveData<List<String>> get() = _availableCategories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()

    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        fetchChallenges()

        searchQuery.observeForever {
            filterChallenges()
        }

        selectedCategoryFilter.observeForever {
            filterChallenges()
        }
    }

    private fun filterChallenges() {
        val currentAllChallenges = _allChallenges.value ?: emptyList()
        val currentSearchQuery = searchQuery.value?.lowercase() ?: ""
        val currentCategoryFilter = selectedCategoryFilter.value

        val filteredList = currentAllChallenges.filter { challenge ->
            val matchesSearch = if (currentSearchQuery.isBlank()) {
                true
            } else {
                challenge.title.lowercase().contains(currentSearchQuery) ||
                        challenge.description.lowercase().contains(currentSearchQuery)
            }

            val matchesCategory = if (currentCategoryFilter.isNullOrBlank()) {
                true
            } else {
                challenge.category.equals(currentCategoryFilter, ignoreCase = true)
            }

            matchesSearch && matchesCategory
        }
        _challengeList.value = filteredList
    }

    fun setCategoryFilter(category: String?) {
        selectedCategoryFilter.value = category
    }

    fun fetchAvailableCategories() {
        val categories = _allChallenges.value?.map { it.category }?.distinct()?.sorted() ?: emptyList()
        _availableCategories.value = categories
    }

    fun fetchChallenges() {
        _isLoading.value = true
        _errorMessage.value = null

        challengeRepository.getAllChallenges(
            onSuccess = { challenges ->
                if (challenges.isEmpty()) {
                    _allChallenges.value = emptyList()
                    _challengeList.value = emptyList()
                    _isLoading.value = false
                    filterChallenges()
                    fetchAvailableCategories()
                    _errorMessage.value = "No challenges available at the moment."
                    return@getAllChallenges
                }

                val challengesWithMembership = mutableListOf<Challenge>()
                val totalChallenges = challenges.size
                var challengesProcessed = 0

                challenges.forEach { challenge ->
                    challengeMemberRepository.getChallengeMemberForChallenge(
                        challenge.id,
                        onSuccess = { member ->
                            val updatedChallenge = challenge.copy(
                                isJoined = (member != null),
                                userCurrentStreak = member?.currentStreak ?: 0
                            )
                            challengesWithMembership.add(updatedChallenge)
                            challengesProcessed++

                            if (challengesProcessed == totalChallenges) {
                                _allChallenges.value = challengesWithMembership.sortedBy { it.id }
                                _isLoading.value = false
                                filterChallenges()
                                fetchAvailableCategories()
                            }
                        },
                        onFailure = { memberError ->
                            challengesWithMembership.add(challenge.copy(isJoined = false, userCurrentStreak = 0))
                            challengesProcessed++
                            if (challengesProcessed == totalChallenges) {
                                _allChallenges.value = challengesWithMembership.sortedBy { it.id }
                                _isLoading.value = false
                                filterChallenges()
                                fetchAvailableCategories()
                            }
                        }
                    )
                }
            },
            onFailure = { message ->
                _errorMessage.value = message
                _isLoading.value = false
                _challengeList.value = emptyList()
                _allChallenges.value = emptyList()
            }
        )
    }

    fun onChallengeItemClicked(challengeId: String) {
        _navigateToChallengeDetail.value = challengeId
    }

    fun onNavigationToChallengeDetailHandled() {
        _navigateToChallengeDetail.value = null
    }

    fun onJoinChallengeClicked(challengeId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            return
        }

        val challengeToJoin = _allChallenges.value?.find { it.id == challengeId }
        if (challengeToJoin == null) {
            return
        }
        if (challengeToJoin.isJoined) {
            return
        }

        _isLoading.value = true

        challengeMemberRepository.joinChallenge(
            challengeToJoin,
            onSuccess = { createdMember ->
                _isLoading.value = false
                fetchChallenges()
            },
            onFailure = { errorMessage ->
                _isLoading.value = false
            }
        )
    }
}