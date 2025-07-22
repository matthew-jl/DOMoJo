package edu.bluejack24_2.domojo.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.models.ChallengeMember
import edu.bluejack24_2.domojo.repositories.ChallengeMemberRepository
import edu.bluejack24_2.domojo.repositories.ChallengeRepository

class ChallengeViewModel: ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val challengeRepository: ChallengeRepository = ChallengeRepository()
    private val challengeMemberRepository: ChallengeMemberRepository = ChallengeMemberRepository()

    private val _navigateToCreateChallenge = MutableLiveData<Boolean>()
    val navigateToCreateChallenge: LiveData<Boolean> get() = _navigateToCreateChallenge

    private val _challengeList = MutableLiveData<List<Challenge>>()
    val challengeList: LiveData<List<Challenge>> get() = _challengeList

    private val _allChallenges = MutableLiveData<List<Challenge>>()

    val searchQuery = MutableLiveData<String>("")
    val selectedCategoryFilter = MutableLiveData<String?>()

    private val _availableCategories = MutableLiveData<List<String>>() // NEW: For filter dialog options
    val availableCategories: LiveData<List<String>> get() = _availableCategories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()

    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        Log.d("ChallengeViewModel", "Initializing ChallengeViewModel")
        fetchChallenges()

        searchQuery.observeForever {
            Log.d("ChallengeViewModel", "searchQuery changed to: '$it'. Triggering filterChallenges().")
            filterChallenges()
        }
        selectedCategoryFilter.observeForever {
            Log.d("ChallengeViewModel", "selectedCategoryFilter changed to: '$it'. Triggering filterChallenges().")
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

    fun clearFilters() {
        searchQuery.value = ""
        selectedCategoryFilter.value = null
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
                Log.d("ChallengeViewModel", "Challenges fetched successfully from repo. Count: ${challenges.size}.")
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
                        challenge.id, // Pass the ID of the current challenge
                        onSuccess = { member ->
                            // Create a new Challenge object with updated membership status and streak
                            val updatedChallenge = challenge.copy(
                                isJoined = (member != null), // True if a member object was found
                                userCurrentStreak = member?.currentStreak ?: 0 // Get streak, default to 0
                            )
                            challengesWithMembership.add(updatedChallenge)
                            challengesProcessed++

                            // Check if all challenges have been processed.
                            // IMPORTANT: This handles the asynchronous nature of getChallengeMemberForChallenge.
                            if (challengesProcessed == totalChallenges) {
                                // Sort the list to ensure consistent order after async processing
                                _allChallenges.value = challengesWithMembership.sortedBy { it.id }
                                _isLoading.value = false
                                filterChallenges() // Apply filters to the updated list
                                fetchAvailableCategories()
                            }
                        },
                        onFailure = { memberError ->
                            Log.e("Challenge", "Failed to get membership for challenge '${challenge.title}': $memberError")
                            // If membership check fails, assume not joined for display purposes
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

    fun onJoinChallengeClicked(challengeId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            Log.i("ChallengeViewModel", "User is not logged in. Cannot join challenge.")
            return
        }

        // Find the full Challenge object from the currently loaded _allChallenges
        val challengeToJoin = _allChallenges.value?.find { it.id == challengeId }
        if (challengeToJoin == null) {
            Log.i("ChallengeViewModel", "Challenge with ID ${challengeId} not found in current list. Cannot join.")
            return
        }
        if (challengeToJoin.isJoined) {
            Log.i("ChallengeViewModel", "User ${userId} is already a member of challenge ${challengeId}. No action taken.")
            return
        }

        _isLoading.value = true // Show loading indicator during join process

        // Create a new ChallengeMember object
        val newMember = ChallengeMember(
            challengeId = challengeId,
            userId = userId,
            currentStreak = 0, // New member starts with 0 streak
            longestStreak = 0, // Longest streak also starts at 0
            isActiveMember = true,
            hasCompleted = false
            // lastActivityDate is null initially
        )

        challengeMemberRepository.joinChallenge(
            challengeToJoin, // Pass the challenge object to the repo
            onSuccess = { createdMember ->
                _isLoading.value = false // Hide loading
                Log.d("ChallengeViewModel", "User ${userId} successfully joined challenge ${challengeId}. New Member ID: ${createdMember.id}")
                // Refresh the challenges list to update UI (this will show streak info instead of Join button)
                fetchChallenges()
            },
            onFailure = { errorMessage ->
                _isLoading.value = false // Hide loading
                Log.e("ChallengeViewModel", "Error joining challenge ${challengeId}: $errorMessage")
            }
        )
    }
}