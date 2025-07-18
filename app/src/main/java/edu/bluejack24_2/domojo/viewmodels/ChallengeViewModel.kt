package edu.bluejack24_2.domojo.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.repositories.ChallengeRepository

class ChallengeViewModel: ViewModel() {
    private val challengeRepository: ChallengeRepository = ChallengeRepository()

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
                _allChallenges.value = challenges
                _isLoading.value = false
                filterChallenges()
                if (challenges.isEmpty()) {
                    _errorMessage.value = "No challenges available at the moment."
                }
                fetchAvailableCategories()
            },
            onFailure = { message ->
                _errorMessage.value = message
                _isLoading.value = false
                _challengeList.value = emptyList()
                _allChallenges.value = emptyList()
            }
        )
    }
}