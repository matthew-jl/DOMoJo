package edu.bluejack24_2.domojo.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.models.Post
import edu.bluejack24_2.domojo.models.ChallengeMember
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.repositories.ChallengeRepository
import edu.bluejack24_2.domojo.repositories.ChallengeMemberRepository
import edu.bluejack24_2.domojo.repositories.PostRepository
import edu.bluejack24_2.domojo.repositories.UserRepository
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import java.io.File
import java.util.Calendar
import java.util.Date

class ChallengeDetailViewModel : ViewModel() {
    private val challengeRepository: ChallengeRepository = ChallengeRepository()
    private val userRepository: UserRepository = UserRepository()
    private val challengeMemberRepository: ChallengeMemberRepository = ChallengeMemberRepository()
    private val challengeActivityPostRepository: PostRepository = PostRepository()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val TAG = "ChallengeDetailVM"

    // --- Challenge Details ---
    private val _challengeDetails = MutableLiveData<Challenge?>()
    val challengeDetails: LiveData<Challenge?> get() = _challengeDetails

    // --- Current User's Membership in THIS Challenge ---
    private val _currentUserChallengeMember = MutableLiveData<ChallengeMember?>()
    val currentUserChallengeMember: LiveData<ChallengeMember?> get() = _currentUserChallengeMember

    // --- Leaderboard Data ---
    private val _leaderboard = MutableLiveData<List<ChallengeMember>>()
    val leaderboard: LiveData<List<ChallengeMember>> get() = _leaderboard
    private var leaderboardListenerRegistration: ListenerRegistration? = null // To manage real-time listener

    // --- Post Activity Dialog State ---
    val selectedPostImageUri = MutableLiveData<Uri?>() // For image preview in post dialog
    private val _showPostDialog = MutableLiveData<Boolean>()
    val showPostDialog: LiveData<Boolean> get() = _showPostDialog

    // --- Daily Post Status for Current User ---
    private val _hasPostedToday = MutableLiveData<Boolean>(false) // True if user has made a streak-awarded post today
    val hasPostedToday: LiveData<Boolean> get() = _hasPostedToday

    // --- UI States and Messages ---
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage
    private val _showToast = MutableLiveData<String?>()
    val showToast: LiveData<String?> get() = _showToast

    // --- LiveData declarations ---
    private val _usersTodayPost = MutableLiveData<Post?>()
    val usersTodayPost: LiveData<Post?> get() = _usersTodayPost // This is public and exposed

    private val _usersTodayPostUser = MutableLiveData<User?>()
    val usersTodayPostUser: LiveData<User?> get() = _usersTodayPostUser // This is public and exposed


    // --- Post Data for Posts Tab --- (You'll expand this for the Posts Fragment)
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

    // --- Main Logic to Load Challenge Details and Related Data ---
    fun loadChallengeDetails(challengeId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        Log.d(TAG, "Loading details for challenge ID: $challengeId")

        // 1. Fetch Challenge basic details
        challengeRepository.getChallenge(
            challengeId,
            onSuccess = { challenge ->
                Log.d(TAG, "Challenge details fetched successfully: ${challenge}")
                _challengeDetails.value = challenge
                if (challenge == null) {
                    _errorMessage.value = "Challenge not found."
                    _isLoading.value = false
                    return@getChallenge
                }
                Log.d(TAG, "Challenge details loaded: ${challenge.title}")

                // 2. Fetch current user's membership status for this challenge
                fetchCurrentUserMembership(challenge.id)

                // 3. Start real-time leaderboard listener
                startLeaderboardListener(challenge.id)

                // 4. Check today's post status
                checkTodayPostStatus(challenge.id)

                // 5. Fetch posts for posts tab (if implemented)
                 fetchChallengePosts(challenge.id) // Implement this if you need to load all posts
            },
            onFailure = { message ->
                _errorMessage.value = message
                _isLoading.value = false
                Log.e(TAG, "Failed to load challenge details: $message")
            }
        )
    }

    private fun fetchCurrentUserMembership(challengeId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _currentUserChallengeMember.value = null // Not logged in
            return
        }

        challengeMemberRepository.getChallengeMemberForChallenge(
            challengeId,
            onSuccess = { member ->
                _currentUserChallengeMember.value = member
                Log.d(TAG, "Current user membership loaded: ${member?.id ?: "Not a member"}")
                // If member exists, update the isJoined status in challengeDetails
                _challengeDetails.value = _challengeDetails.value?.copy(isJoined = (member != null))
            },
            onFailure = { message ->
                _errorMessage.value = message // Error fetching membership
                Log.e(TAG, "Failed to fetch current user's membership: $message")
                // Still set challengeDetails.isJoined to false on error
                _challengeDetails.value = _challengeDetails.value?.copy(isJoined = false)
            }
        )
    }

    private fun startLeaderboardListener(challengeId: String) {
        // Detach previous listener if exists
        leaderboardListenerRegistration?.remove()
        leaderboardListenerRegistration = challengeMemberRepository.getLeaderboard(
            challengeId,
            onData = { members ->
                _leaderboard.value = members
                _isLoading.value = false // Loading done once leaderboard is loaded
                Log.d(TAG, "Leaderboard updated: ${members.size} members.")
            },
            onError = { message ->
                _errorMessage.value = message
                _isLoading.value = false
                Log.e(TAG, "Leaderboard real-time update error: $message")
            }
        )
    }

    fun detachLeaderboardListener() {
        leaderboardListenerRegistration?.remove()
        leaderboardListenerRegistration = null
        Log.d(TAG, "Leaderboard real-time listener detached.")
    }

    // --- Action Button Logic ---

    // Called when user clicks "Join This Challenge"
    fun onJoinChallengeClicked(challenge: Challenge) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _showToast.value = "Please log in to join a challenge."
            return
        }
        if (challenge.isJoined) { // Should not happen if button is correctly hidden
            _showToast.value = "You have already joined this challenge."
            return
        }

        _isLoading.value = true
        _showToast.value = "Joining challenge..."

        challengeMemberRepository.joinChallenge(
            challenge,
            onSuccess = { newMember ->
                _isLoading.value = false
                _showToast.value = "Successfully joined '${challenge.title}'!"
                _currentUserChallengeMember.value = newMember // Update LiveData
                _challengeDetails.value = challenge.copy(isJoined = true) // Update challenge status
                // Leaderboard will refresh automatically due to real-time listener
            },
            onFailure = { message ->
                _isLoading.value = false
                _showToast.value = "Failed to join challenge: $message"
            }
        )
    }

    // Called when user clicks "Post Today's Activity" (from main button or Posts tab)
    fun onPostActivityClicked() {
        if (firebaseAuth.currentUser?.uid.isNullOrBlank()) {
            _showToast.value = "Please log in to post activity."
            return
        }
        if (_challengeDetails.value?.id == null) {
            _showToast.value = "Error: Challenge details not loaded."
            return
        }
        if (_currentUserChallengeMember.value == null) { // User needs to be a member
            _showToast.value = "Please join the challenge first to post."
            return
        }
        if (_hasPostedToday.value == true) { // Prevent multiple posts for streak
            _showToast.value = "You have already posted today's activity!"
            return
        }

        // Trigger showing the post dialog
        _showToast.value = null // Clear previous toast
        _showPostDialog.value = true
    }

    // Called by Activity after the Post Dialog has been shown
    fun onPostDialogShown() {
        _showPostDialog.value = false // Reset the flag
    }

    // --- Post Activity Logic (called from Post Dialog) ---
    fun postActivity(context: Context, content: String, imageFile: File?) {
        val challengeId = _challengeDetails.value?.id
        val userId = firebaseAuth.currentUser?.uid
        val member = _currentUserChallengeMember.value
        if (challengeId == null || userId.isNullOrBlank() || member == null) {
            _showToast.value = "Error: Invalid post context. Please try again."
            return
        }
        if (content.isBlank() && imageFile == null) {
            _showToast.value = "Post must have content or an image."
            return
        }

        _isLoading.value = true
        _showToast.value = "Posting activity..."

        // Determine if this post awards a streak
        // Streak logic: Check if lastActivityDate is before today
        val today = Calendar.getInstance().apply {
            time = Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val lastActivity = member.lastActivityDate
        Log.d(TAG, "Check Today: ${today}")
        Log.d(TAG, "Check Last Activity: ${lastActivity}")
        val isNewStreakDay = (lastActivity == null || lastActivity.before(today)) // If last activity was before today, it's a new streak day

        // 1. Upload image if provided
        if (imageFile != null) {
            CloudinaryClient.uploadImage(
                context = context,
                uri = Uri.fromFile(imageFile),
                onSuccess = { imageUrl ->
                    createChallengePostAndAwardStreak(
                        challengeId, userId, member.id, content, imageUrl, isNewStreakDay, member
                    )
                },
                onError = { message ->
                    _isLoading.value = false
                    _showToast.value = "Image upload failed: $message"
                }
            )
        } else {
            // No image, proceed with post creation directly
            createChallengePostAndAwardStreak(
                challengeId, userId, member.id, content, "", isNewStreakDay, member
            )
        }
    }

    private fun createChallengePostAndAwardStreak(
        challengeId: String,
        userId: String,
        memberId: String,
        content: String,
        imageUrl: String,
        isNewStreakDay: Boolean,
        currentMember: ChallengeMember
    ) {
        val newPost = Post(
            challengeId = challengeId,
            userId = userId,
            memberId = memberId,
            content = content,
            imageUrl = imageUrl,
            streakAwarded = isNewStreakDay, // Only award streak if it's a new streak day
        )

        // 2. Add the activity post
        challengeActivityPostRepository.addActivityPost(
            newPost,
            onSuccess = {
                Log.d(TAG, "Activity post created. Streak awarded: $isNewStreakDay")
                // 3. Update ChallengeMember streak if streak was awarded
                if (isNewStreakDay) {
                    val newCurrentStreak = (currentMember.currentStreak ?: 0) + 1
                    val newLongestStreak = maxOf(newCurrentStreak, (currentMember.longestStreak ?: 0))
                    val newIsActive = true // User is active as they just posted
                    val newHasCompleted = true // User completed today's post

                    challengeMemberRepository.updateStreak(
                        currentMember.id,
                        newCurrentStreak,
                        newLongestStreak,
                        newIsActive,
                        newHasCompleted,
                        Calendar.getInstance().time, // Update lastActivityDate to now
                        onSuccess = {
                            _isLoading.value = false
                            _showToast.value = "Activity posted! Streak updated to $newCurrentStreak!"
                            // Refresh membership data to update UI
                            fetchCurrentUserMembership(challengeId)
                            checkTodayPostStatus(challengeId) // Re-check hasPostedToday
                            // Leaderboard will update automatically
                        },
                        onFailure = { errorMessage ->
                            _isLoading.value = false
                            _showToast.value = "Activity posted, but failed to update streak: $errorMessage"
                        }
                    )
                } else {
                    // Post without streak update (e.g., duplicate post today)
                    _isLoading.value = false
                    _showToast.value = "Activity posted!"
                    checkTodayPostStatus(challengeId) // Re-check hasPostedToday
                }
            },
            onFailure = { errorMessage ->
                _isLoading.value = false
                _showToast.value = "Failed to post activity: $errorMessage"
            }
        )
    }


    // --- The method that updates these LiveData ---
    public fun checkTodayPostStatus(challengeId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _usersTodayPost.value = null
            _usersTodayPostUser.value = null
            _hasPostedToday.value = false // This will also be null if no user
            return
        }
        challengeActivityPostRepository.getTodayStreakAwardedPost(
            challengeId,
            userId,
            onSuccess = { post ->
                _usersTodayPost.value = post // <-- Sets the ChallengeActivityPost object
                _hasPostedToday.value = (post != null)

                if (post != null) {
                    userRepository.getUser(post.userId,
                        onSuccess = { user ->
                            _usersTodayPostUser.value = user // <-- Sets the User object
                        },
                        onFailure = { Log.e(TAG, "Failed to get user for today's post: $it") }
                    )
                } else {
                    _usersTodayPostUser.value = null // Clear user if no post
                }
            },
            onFailure = { /* ... error handling ... */ }
        )
    }

    // --- Check Daily Post Status ---
//    private fun checkTodayPostStatus(challengeId: String) {
//        val userId = firebaseAuth.currentUser?.uid
//        if (userId.isNullOrBlank()) {
//            _hasPostedToday.value = false // Not logged in, so no post for them
//            return
//        }
//        challengeActivityPostRepository.getTodayStreakAwardedPost(
//            challengeId,
//            userId,
//            onSuccess = { post ->
//                _hasPostedToday.value = (post != null)
//                Log.d(TAG, "User has posted today for streak: ${_hasPostedToday.value}")
//            },
//            onFailure = { message ->
//                Log.e(TAG, "Failed to check today's post status: $message")
//                _hasPostedToday.value = false // Assume not posted if check fails
//            }
//        )
//    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // NEW: Public function to clear the showToast message
    fun clearShowToastMessage() {
        _showToast.value = null
    }

    /**
     * Fetches all challenge posts for the given challenge, excluding the current user's
     * streak-awarded post for today (as it's displayed separately).
     */
    fun fetchChallengePosts(challengeId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid

        challengeActivityPostRepository.getAllChallengePosts(
            challengeId,
            onSuccess = { allPosts ->
                // Filter out the current user's streak-awarded post for today
                val filteredPosts = allPosts.filter { post ->
                    // Exclude if it's the current user's post AND it awarded a streak AND it's from today
                    !(post.userId == currentUserId && post.streakAwarded && isPostFromToday(post))
                }.sortedByDescending { it.createdAt } // Sort by creation date, newest first

                _posts.value = filteredPosts
                Log.d(TAG, "Other posts loaded for challenge $challengeId: ${filteredPosts.size}")
            },
            onFailure = { message ->
                Log.e(TAG, "Failed to load all posts for challenge $challengeId: $message")
                _errorMessage.value = message // You might want to set a specific error for posts fragment
            }
        )
    }

    // ... (existing onJoinChallengeClicked, onPostActivityClicked, onPostDialogShown, postActivity) ...

    /**
     * Helper function to determine if a given post's creation date falls within today.
     */
    private fun isPostFromToday(post: Post): Boolean {
        post.createdAt ?: return false // If no creation date, it's not from today

        val postCalendar = Calendar.getInstance().apply { time = post.createdAt }
        val todayCalendar = Calendar.getInstance()

        return postCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                postCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                postCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure real-time listener is detached when ViewModel is cleared
        detachLeaderboardListener()
    }
}