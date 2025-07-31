package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.models.Badge
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.repositories.AuthRepository
import edu.bluejack24_2.domojo.repositories.ChallengeMemberRepository
import edu.bluejack24_2.domojo.repositories.UserRepository

class ChangeBadgeViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository(userRepository)
    private val challengeMemberRepository = ChallengeMemberRepository()

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> get() = _currentUser

    private val _badges = MutableLiveData<List<Badge>>()
    val badges: LiveData<List<Badge>> get() = _badges

    private val _selectedBadgeIndex = MutableLiveData<Int>(0)
    val selectedBadgeIndex: LiveData<Int> get() = _selectedBadgeIndex

    private val _currentBadge = MutableLiveData<Badge?>()
    val currentBadge: LiveData<Badge?> get() = _currentBadge

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> get() = _updateSuccess

    private val _updateError = MutableLiveData<String?>()
    val updateError: LiveData<String?> get() = _updateError

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private lateinit var initialBadgeList: List<Badge>

    fun initializeBadgesAndCheckStatus(badges: List<Badge>) {
        this.initialBadgeList = badges
        _badges.value = badges
        loadCurrentUserAndCheckBadges()
    }

    private fun loadCurrentUserAndCheckBadges() {
        _isLoading.value = true
        authRepository.getCurrentUser().observeForever { user ->
            if (user == null) {
                _isLoading.value = false
                _updateError.value = "User not found."
                return@observeForever
            }
            _currentUser.value = user

            challengeMemberRepository.getUserMaxLongestStreak(
                userId = user.id,
                onSuccess = { maxStreak ->
                    val updatedBadges = initialBadgeList.map { badge ->
                        badge.copy(isUnlocked = maxStreak >= badge.requirement)
                    }
                    _badges.value = updatedBadges

                    val currentIndex = updatedBadges.indexOfFirst { it.id == user.badge }.coerceAtLeast(0)
                    _selectedBadgeIndex.value = currentIndex
                    _currentBadge.value = updatedBadges.getOrNull(currentIndex)

                    _isLoading.value = false
                },
                onFailure = { error ->
                    _updateError.value = error
                    _isLoading.value = false
                }
            )
        }
    }

    fun selectNextBadge() {
        _badges.value?.let { badges ->
            if (badges.isEmpty()) return
            val currentIndex = _selectedBadgeIndex.value ?: 0
            val nextIndex = (currentIndex + 1) % badges.size
            _selectedBadgeIndex.value = nextIndex
            _currentBadge.value = badges.getOrNull(nextIndex)
        }
    }

    fun selectPreviousBadge() {
        _badges.value?.let { badges ->
            if (badges.isEmpty()) return
            val currentIndex = _selectedBadgeIndex.value ?: 0
            val prevIndex = (currentIndex - 1 + badges.size) % badges.size
            _selectedBadgeIndex.value = prevIndex
            _currentBadge.value = badges.getOrNull(prevIndex)
        }
    }

    fun saveBadgeChanges() {
        _isLoading.value = true
        val selectedBadge = currentBadge.value

        if (selectedBadge == null) {
            _isLoading.value = false
            _updateError.value = "No badge selected."
            return
        }

        if (!selectedBadge.isUnlocked) {
            _isLoading.value = false
            _updateError.value = "This badge is currently locked."
            return
        }

        authRepository.updateCurrentUserBadge(
            newBadge = selectedBadge?.id,
            onSuccess = {
                _isLoading.value = false
                _updateSuccess.value = true
                _currentUser.value = _currentUser.value?.copy(badge = selectedBadge?.id)
            },
            onFailure = { error ->
                _isLoading.value = false
                _updateError.value = error
            }
        )
    }
}