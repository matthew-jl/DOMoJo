package edu.bluejack24_2.domojo.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.models.Badge
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.repositories.AuthRepository
import edu.bluejack24_2.domojo.repositories.UserRepository

class ChangeBadgeViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository(userRepository)

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

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        authRepository.getCurrentUser().observeForever { user ->
            _currentUser.value = user
            _badges.value?.let { badges ->
                // Set initial selected badge based on user's current badge
                user?.badge?.let { currentBadgeId ->
//                    val badgesList = _badges.value ?: return@observeForever
                    val index = badges.indexOfFirst { it.id == currentBadgeId }.coerceAtLeast(0)
                    _selectedBadgeIndex.value = index
                    _currentBadge.value = badges.getOrNull(index)
                } ?: run {
                    // If user has no badge or badge is null, default to first badge
                    _selectedBadgeIndex.value = 0
                    _currentBadge.value = badges.firstOrNull()
                }
            }
        }
    }

//    private fun initializeBadges() {
//        val badgeList = listOf(
//            Badge(
//                id = "bronze",
//                name = getString(R.string.badge_bronze_name),
//                description = getString(R.string.badge_bronze_desc),
//                imageRes = R.drawable.ic_badge_bronze
//            ),
//            Badge(
//                id = "silver",
//                name = getString(R.string.badge_silver_name),
//                description = getString(R.string.badge_silver_desc),
//                imageRes = R.drawable.ic_badge_silver
//            ),
//            Badge(
//                id = "gold",
//                name = getString(R.string.badge_gold_name),
//                description = getString(R.string.badge_gold_desc),
//                imageRes = R.drawable.ic_badge_gold
//            ),
//            Badge(
//                id = "diamond",
//                name = getString(R.string.badge_diamond_name),
//                description = getString(R.string.badge_diamond_desc),
//                imageRes = R.drawable.ic_badge_diamond
//            ),
//            Badge(
//                id = "purple",
//                name = getString(R.string.badge_purple_name),
//                description = getString(R.string.badge_purple_desc),
//                imageRes = R.drawable.ic_badge_purple
//            )
//        )
//        _badges.value = badgeList
//    }

    fun setBadges(badgeList: List<Badge>) {
        _badges.value = badgeList
        if (_currentBadge.value == null) {
            _currentBadge.value = badgeList.firstOrNull()
        }
    }

    fun selectNextBadge() {
        _badges.value?.let { badges ->
            val currentIndex = _selectedBadgeIndex.value ?: 0
            val nextIndex = (currentIndex + 1) % badges.size
            _selectedBadgeIndex.value = nextIndex
            _currentBadge.value = badges.getOrNull(nextIndex)
        }
    }

    fun selectPreviousBadge() {
        _badges.value?.let { badges ->
            val currentIndex = _selectedBadgeIndex.value ?: 0
            val prevIndex = (currentIndex - 1 + badges.size) % badges.size
            _selectedBadgeIndex.value = prevIndex
            _currentBadge.value = badges.getOrNull(prevIndex)
        }
    }

    fun saveBadgeChanges() {
        _isLoading.value = true
        val selectedBadge = currentBadge.value

        authRepository.updateCurrentUserBadge(
            newBadge = selectedBadge?.id,
            onSuccess = {
                _isLoading.value = false
                _updateSuccess.value = true
                // Update local user data
                _currentUser.value = _currentUser.value?.copy(badge = selectedBadge?.id)
            },
            onFailure = { error ->
                _isLoading.value = false
                _updateError.value = error
            }
        )
    }
}