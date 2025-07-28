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
import edu.bluejack24_2.domojo.models.Post // Using 'Post' model consistently
import edu.bluejack24_2.domojo.models.PostComment // Model for comments
import edu.bluejack24_2.domojo.models.ChallengeMember
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.repositories.ChallengeRepository
import edu.bluejack24_2.domojo.repositories.ChallengeMemberRepository
import edu.bluejack24_2.domojo.repositories.PostRepository // Repository for posts
import edu.bluejack24_2.domojo.repositories.PostLikeRepository // Repository for like/dislike actions
import edu.bluejack24_2.domojo.repositories.PostCommentRepository // Repository for comment actions
import edu.bluejack24_2.domojo.repositories.UserRepository
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import edu.bluejack24_2.domojo.utils.Event
import java.io.File
import java.util.Calendar
import java.util.Date

class ChallengeDetailViewModel : ViewModel() {
    // REPOSITORY INITIALIZATIONS
    // All repositories are initialized with required Firestore/FirebaseAuth instances
    private val challengeRepository: ChallengeRepository = ChallengeRepository()
    private val userRepository: UserRepository = UserRepository()
    private val challengeMemberRepository: ChallengeMemberRepository = ChallengeMemberRepository()
    private val postRepository: PostRepository = PostRepository()
    private val postLikeRepository: PostLikeRepository = PostLikeRepository()
    private val postCommentRepository: PostCommentRepository = PostCommentRepository()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val TAG = "ChallengeDetailVM" // Tag for logging within this ViewModel

    // --- Challenge Details LiveData ---
    // Holds the currently loaded Challenge object, nullable if not yet fetched or not found.
    private val _challengeDetails = MutableLiveData<Challenge?>()
    val challengeDetails: LiveData<Challenge?> get() = _challengeDetails

    // --- Current User's Membership in THIS Challenge LiveData ---
    // Holds the ChallengeMember object for the current user for this specific challenge.
    // Null if user is not logged in or not a member.
    private val _currentUserChallengeMember = MutableLiveData<ChallengeMember?>()
    val currentUserChallengeMember: LiveData<ChallengeMember?> get() = _currentUserChallengeMember

    // --- Leaderboard Data LiveData ---
    // Holds the list of ChallengeMember objects for the leaderboard, updated in real-time.
    private val _leaderboard = MutableLiveData<List<ChallengeMember>>()
    val leaderboard: LiveData<List<ChallengeMember>> get() = _leaderboard
    private var leaderboardListenerRegistration: ListenerRegistration? = null // Manages real-time listener subscription

    // --- Post Activity Dialog State LiveData ---
    // Tracks the URI of a selected image for the post creation dialog.
    val selectedPostImageUri = MutableLiveData<Uri?>()
    // Triggers the Activity to show the post creation dialog.
    private val _showPostDialog = MutableLiveData<Boolean>()
    val showPostDialog: LiveData<Boolean> get() = _showPostDialog

    // --- Daily Post Status for Current User LiveData ---
    // True if the current user has made a streak-awarded post for this challenge today.
    private val _hasPostedToday = MutableLiveData<Boolean>(false)
    val hasPostedToday: LiveData<Boolean> get() = _hasPostedToday
    // Holds the actual Post object if the user has posted today (for "Post-mu" section).
    private val _usersTodayPost = MutableLiveData<Post?>()
    val usersTodayPost: LiveData<Post?> get() = _usersTodayPost
    // Holds the User object for the author of "Post-mu" (current user's details).
    private val _usersTodayPostUser = MutableLiveData<User?>()
    val usersTodayPostUser: LiveData<User?> get() = _usersTodayPostUser

    // --- Main Challenge Posts Data LiveData (for "Posts pengguna lain" section) ---
    // Holds the list of all posts for the challenge, excluding the current user's today's post.
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

    // --- UI States and Messages LiveData ---
    private val _isLoading = MutableLiveData<Boolean>(false) // General loading indicator for the page.
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>() // For displaying error messages (e.g., in a TextView or Toast).
    val errorMessage: LiveData<String?> get() = _errorMessage
    private val _showToast = MutableLiveData<String?>() // For showing short Toast messages to the user.
    val showToast: LiveData<String?> get() = _showToast

    // --- NEW: LiveData for Post Actions (Likes/Comments) ---
    // Maps postId to the current user's like/dislike action ("like", "dislike", or "none").
    // Used to set the correct drawable for like/dislike buttons.
    private val _postUserActions = MutableLiveData<MutableMap<String, String>>(mutableMapOf())
    fun getPostUserAction(postId: String): String = _postUserActions.value?.get(postId) ?: "none"

    // Maps postId to its list of recent comments (limited, for nested RecyclerViews).
    private val _recentCommentsMap = MutableLiveData<MutableMap<String, List<PostComment>>>(mutableMapOf())
    fun getRecentCommentsForPost(postId: String): List<PostComment> = _recentCommentsMap.value?.get(postId) ?: emptyList()

    // Triggers navigation to a separate "All Comments" screen/dialog, passing post ID and total comment count.
    private val _navigateToAllComments = MutableLiveData<Pair<String, Int>?>()
    val navigateToAllComments: LiveData<Pair<String, Int>?> get() = _navigateToAllComments

//    private val _showAddCommentDialog = MutableLiveData<String?>()
//    val showAddCommentDialog: LiveData<String?> get() = _showAddCommentDialog


    private val _showAddCommentDialog = MutableLiveData<Event<String>>()
    val showAddCommentDialog: LiveData<Event<String>> = _showAddCommentDialog

    // --- Initialization Block ---
    init {
        // ViewModel is instantiated by ViewModelProvider.
        // loadChallengeDetails() will be called from the Activity/Fragment's onCreate/onResume.
    }

    // --- Main Logic to Load Challenge Details and All Related Data ---
    fun loadChallengeDetails(challengeId: String) {
        _isLoading.value = true
        _errorMessage.value = null // Clear any previous error message
        Log.d(TAG, "Loading details for challenge ID: $challengeId")

        // 1. Fetch Challenge basic details (title, banner, description etc.)
        challengeRepository.getChallenge(
            challengeId,
            onSuccess = { challenge ->
                Log.d(TAG, "Challenge details fetched successfully: ${challenge?.title ?: "null"}")
                _challengeDetails.value = challenge
                if (challenge == null) {
                    _errorMessage.value = "Challenge not found."
                    _isLoading.value = false
                    return@getChallenge // Exit if challenge is not found
                }

                // 2. Fetch current user's membership status for this specific challenge
                fetchCurrentUserMembership(challenge.id)

                // 3. Start real-time leaderboard listener for this challenge
                startLeaderboardListener(challenge.id)

                // 4. Check today's post status for the current user (for "Post-mu" section)
                checkTodayPostStatus(challenge.id)

                // 5. Fetch other posts for the "Posts pengguna lain" section
                fetchChallengePosts(challenge.id)
            },
            onFailure = { message ->
                _errorMessage.value = message // Set error message if challenge details fail to load
                _isLoading.value = false
                Log.e(TAG, "Failed to load challenge details: $message")
            }
        )
    }

    // --- Fetch Current User's Membership in this Challenge ---
    private fun fetchCurrentUserMembership(challengeId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _currentUserChallengeMember.value = null // Not logged in, so not a member
            return
        }

        challengeMemberRepository.getChallengeMemberForChallenge(
            challengeId,
            onSuccess = { member ->
                _currentUserChallengeMember.value = member // Update membership LiveData
                Log.d(TAG, "Current user membership loaded: ${member?.id ?: "Not a member"}")
                // Update isJoined status in challengeDetails (important for action button text)
                _challengeDetails.value = _challengeDetails.value?.copy(isJoined = (member != null))
            },
            onFailure = { message ->
                _errorMessage.value = message
                Log.e(TAG, "Failed to fetch current user's membership: $message")
                _challengeDetails.value = _challengeDetails.value?.copy(isJoined = false) // Assume not joined on error
            }
        )
    }

    // --- Leaderboard Real-time Listener Management ---
    private fun startLeaderboardListener(challengeId: String) {
        leaderboardListenerRegistration?.remove() // Remove any existing listener to prevent duplicates
        leaderboardListenerRegistration = challengeMemberRepository.getLeaderboard(
            challengeId,
            onData = { members ->
                _leaderboard.value = members // Update leaderboard LiveData
                _isLoading.value = false // General loading is complete once leaderboard data is received
                Log.d(TAG, "Leaderboard updated: ${members.size} members.")
            },
            onError = { message ->
                _errorMessage.value = message
                _isLoading.value = false
                Log.e(TAG, "Leaderboard real-time update error: $message")
            }
        )
    }

    // Detaches the real-time leaderboard listener (called from Activity's onDestroy or ViewModel's onCleared)
    fun detachLeaderboardListener() {
        leaderboardListenerRegistration?.remove()
        leaderboardListenerRegistration = null
        Log.d(TAG, "Leaderboard real-time listener detached.")
    }

    // --- Fetch Challenge Posts for "Posts pengguna lain" ---
    fun fetchChallengePosts(challengeId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid

        postRepository.getAllChallengePosts( // Fetches all posts for the challenge
            challengeId,
            onSuccess = { allPosts ->
                // Filter out the current user's streak-awarded post for today, as it's displayed separately ("Post-mu")
                val filteredPosts = allPosts.filter { post ->
                    !(post.userId == currentUserId && post.streakAwarded && isPostFromToday(post))
                }.sortedByDescending { it.createdAt } // Sort by creation date, newest first

                _posts.value = filteredPosts // Update LiveData for other posts
                Log.d(TAG, "Other posts loaded for challenge $challengeId: ${filteredPosts.size}")

                // Trigger fetching of like status and recent comments for each loaded post
                filteredPosts.forEach { post ->
                    if (currentUserId != null) {
                        // Fetch current user's like/dislike status for each post (for button icon)
                        postLikeRepository.getUserLikeStatus(
                            post.id, currentUserId,
                            onSuccess = { action ->
                                val currentActions = _postUserActions.value?.toMutableMap() ?: mutableMapOf()
                                currentActions[post.id] = action // Store user's action for this post
                                _postUserActions.value = currentActions // Trigger redraw for button icon
                            },
                            onFailure = { Log.e(TAG, "Failed to get user action for post ${post.id}: $it") }
                        )
                    }
                    // Fetch recent comments for each post (for nested RecyclerView)
                    fetchRecentCommentsForPost(post.id)
                }
            },
            onFailure = { message ->
                Log.e(TAG, "Failed to load all posts for challenge $challengeId: $message")
                _errorMessage.value = message
            }
        )
    }

    // --- Action Button Logic ---

    // Called when user clicks "Join This Challenge" (from action_button in Activity)
    fun onJoinChallengeClicked(challenge: Challenge) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _showToast.value = "Please log in to join a challenge."
            return
        }
        // Safety check (button should be hidden if already joined, but defensive coding)
        if (challenge.isJoined) {
            _showToast.value = "You have already joined this challenge."
            return
        }

        _isLoading.value = true // Show loading indicator
        _showToast.value = "Joining challenge..." // Provide immediate feedback

        challengeMemberRepository.joinChallenge(
            challenge,
            onSuccess = { newMember ->
                _isLoading.value = false
                _showToast.value = "Successfully joined '${challenge.title}'!"
                _currentUserChallengeMember.value = newMember // Update membership LiveData
                _challengeDetails.value = challenge.copy(isJoined = true) // Update challenge status in main details
                // Leaderboard will refresh automatically due to real-time listener
                checkTodayPostStatus(challenge.id) // Check post status after joining
            },
            onFailure = { message ->
                _isLoading.value = false
                _showToast.value = "Failed to join challenge: $message"
            }
        )
    }

    // Called when user clicks "Post Today's Activity" (from main button or "Buat Post" button in tab)
    fun onPostActivityClicked() {
        if (firebaseAuth.currentUser?.uid.isNullOrBlank()) {
            _showToast.value = "Please log in to post activity."
            return
        }
        val challengeId = _challengeDetails.value?.id
        if (challengeId == null) {
            _showToast.value = "Error: Challenge details not loaded."
            return
        }
        val member = _currentUserChallengeMember.value
        if (member == null) { // User needs to be a member to post activity
            _showToast.value = "Please join the challenge first to post."
            return
        }
        // Check hasPostedToday before showing dialog (prevent multiple posts for streak in a day)
        if (_hasPostedToday.value == true) {
            _showToast.value = "You have already posted today's activity!"
            return
        }

        _showToast.value = null // Clear any previous toast message
        _showPostDialog.value = true // Trigger showing the post creation dialog
    }

    // Called by Activity after the Post Dialog has been shown
    fun onPostDialogShown() {
        _showPostDialog.value = false // Reset the flag
        selectedPostImageUri.value = null // Clear selected image URI for a fresh start in dialog
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

        // Determine if this post awards a streak based on lastActivityDate
        val todayStart = Calendar.getInstance().apply {
            time = Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val lastActivity = member.lastActivityDate
        Log.d(TAG, "Post Activity - Check Today: ${todayStart}")
        Log.d(TAG, "Post Activity - Check Last Activity: ${lastActivity}")
        val isNewStreakDay = (lastActivity == null || lastActivity.before(todayStart))

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
        val newPost = Post( // Using 'Post' model
            challengeId = challengeId,
            userId = userId,
            memberId = memberId,
            content = content,
            imageUrl = imageUrl,
            streakAwarded = isNewStreakDay,
            createdAt = null // Will be set by @ServerTimestamp
        )

        postRepository.addActivityPost( // Using 'postRepository'
            newPost,
            onSuccess = {
                Log.d(TAG, "Activity post created. Streak awarded: $isNewStreakDay")
                // After successful post creation, update member streak if applicable
                if (isNewStreakDay) {
                    val newCurrentStreak = (currentMember.currentStreak ?: 0) + 1
                    val newLongestStreak = maxOf(newCurrentStreak, (currentMember.longestStreak ?: 0))
                    val newIsActive = true
                    val newHasCompleted = true

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
                            // Refresh all related data to update the UI
                            _challengeDetails.value?.id?.let { fetchCurrentUserMembership(it) } // Re-fetch membership for updated streak
                            checkTodayPostStatus(challengeId) // Re-check hasPostedToday and usersTodayPost
                            fetchChallengePosts(challengeId) // Refresh other posts list
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
                    fetchChallengePosts(challengeId) // Refresh other posts list
                }
            },
            onFailure = { errorMessage ->
                _isLoading.value = false
                _showToast.value = "Failed to post activity: $errorMessage"
            }
        )
    }

    // --- Check Daily Post Status and Fetch User's Today Post ---
    fun checkTodayPostStatus(challengeId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _hasPostedToday.value = false
            _usersTodayPost.value = null
            _usersTodayPostUser.value = null
            return
        }
        postRepository.getTodayStreakAwardedPost( // Fetches today's post if it awarded a streak
            challengeId,
            userId,
            onSuccess = { post ->
                _hasPostedToday.value = (post != null)
                _usersTodayPost.value = post // Update LiveData with the actual post object

                if (post != null) {
                    userRepository.getUser(post.userId, // Fetch user data for this post
                        onSuccess = { user ->
                            _usersTodayPostUser.value = user
                        },
                        onFailure = { Log.e(TAG, "Failed to get user for today's post: $it") }
                    )
                } else {
                    _usersTodayPostUser.value = null // Clear user if no post
                }
                Log.d(TAG, "User has posted today for streak: ${_hasPostedToday.value}. Post: ${post?.content ?: "None"}")
            },
            onFailure = { message ->
                Log.e(TAG, "Failed to check today's post status: $message")
                _hasPostedToday.value = false
                _usersTodayPost.value = null
                _usersTodayPostUser.value = null
            }
        )
    }


    // --- Post Actions Logic (Like/Dislike/Comments) ---

    /**
     * Handles like/dislike button click on a post.
     * Updates counts on the Post document and user's specific action status.
     * @param postId The ID of the post being liked/disliked.
     * @param type "like" or "dislike".
     */
    fun onLikeClicked(postId: String, type: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _showToast.value = "Please log in to like/dislike posts."
            return
        }

        Log.d(TAG, "onLikeClicked: postId=$postId, type=$type, userId=$currentUserId")

        _showToast.value = "Updating action..."

        postLikeRepository.toggleLike( // Uses PostLikeRepository for atomic toggle
            postId, type,
            onSuccess = { newLikeCount, newDislikeCount, currentUserAction ->
                _showToast.value = "Post action updated!"
                // Update the counts in the _posts LiveData list (for "Posts pengguna lain")
                updatePostCountsInList(postId, newLikeCount, newDislikeCount)
                // Update the user's action for this specific post (for button icon redraw)
                val currentActions = _postUserActions.value?.toMutableMap() ?: mutableMapOf()
                currentActions[postId] = currentUserAction ?: "none"
                _postUserActions.value = currentActions
            },
            onFailure = { message ->
                _showToast.value = "Failed to update post action: $message"
            }
        )
    }

    /**
     * Helper to update like/dislike counts for a specific post within the ViewModel's lists.
     * Triggers UI updates for `_posts` and `_usersTodayPost`.
     */
    private fun updatePostCountsInList(postId: String, newLikeCount: Int, newDislikeCount: Int) {
        val currentPosts = _posts.value?.toMutableList() ?: mutableListOf()
        val index = currentPosts.indexOfFirst { it.id == postId }
        if (index != -1) {
            val updatedPost = currentPosts[index].copy(
                likeCount = newLikeCount,
                dislikeCount = newDislikeCount
            )
            currentPosts[index] = updatedPost
            _posts.value = currentPosts // Trigger UI update for other posts
        }

        // Also update usersTodayPost if it's the post being acted on
        if (usersTodayPost.value?.id == postId) {
            _usersTodayPost.value = usersTodayPost.value?.copy(
                likeCount = newLikeCount,
                dislikeCount = newDislikeCount
            )
        }
    }
    
    

    /**
     * Handles comment button click. Triggers navigation to All Comments screen.
     * @param postId The ID of the post.
     * @param totalCommentCount The total count of comments on the post.
     */
    fun onCommentClicked(postId: String, totalCommentCount: Int) {
        Log.d(TAG, "onCommentClicked: postId=$postId, totalCommentCount=$totalCommentCount")
        Log.d(TAG, "Comment button clicked for post: $postId. Previous=${_showAddCommentDialog.value}\"")
        // You can use totalCommentCount if you want to pass it to the dialog
        // or just rely on postId.
//        _showAddCommentDialog.value = null
//        _showAddCommentDialog.value = postId // Set the postId to trigger the dialog
        _showAddCommentDialog.value = Event(postId)
        _showToast.value = null // Clear any pending toast
    }

    fun clearErrorMessage() {
        _errorMessage.value = null // Clear any error messages
    }

    // NEW: Method to submit a new comment from the dialog
    fun submitComment(postId: String, content: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _showToast.value = "Please log in to add a comment."
            return
        }
        if (content.isBlank()) {
            _showToast.value = "Comment cannot be empty."
            return
        }

        _isLoading.value = true // Show loading
        _showToast.value = "Submitting comment..."

        val newComment = PostComment(
            postId = postId,
            userId = currentUserId,
            content = content,
            createdAt = null // @ServerTimestamp will set this
        )

        postCommentRepository.addComment(
            newComment,
            onSuccess = {
                _isLoading.value = false
                _showToast.value = "Comment added successfully!"
                // Refresh relevant data to update UI
                // This is critical for recent comments and total comment count to update
                _challengeDetails.value?.id?.let { challengeId ->
                    fetchChallengePosts(challengeId) // Refresh all posts to update counts
                    fetchRecentCommentsForPost(postId) // Refresh recent comments for this specific post
                }
            },
            onFailure = { errorMessage ->
                _isLoading.value = false
                _showToast.value = "Failed to add comment: $errorMessage"
            }
        )
    }

    fun onAddCommentDialogShown() {
        _showAddCommentDialog.value = null // Reset the flag
    }

    // Called by Activity after navigation to All Comments is handled
    fun onNavigationToAllCommentsHandled() {
        _navigateToAllComments.value = null // Clear the navigation event
    }

    /**
     * Fetches recent comments for a specific post.
     * This is called by `ChallengePostAdapter.ChallengePostViewHolder.bind()` to populate
     * the nested RecyclerView for comments within a post item.
     * @param postId The ID of the post to fetch comments for.
     */
    fun fetchRecentCommentsForPost(postId: String) {
        // Prevent redundant fetches if comments for this post are already in the map
        if (_recentCommentsMap.value?.containsKey(postId) == true) {
            return
        }
        postCommentRepository.getRecentComments(
            postId, 3, // Fetch up to 3 recent comments
            onSuccess = { comments ->
                val currentMap = _recentCommentsMap.value?.toMutableMap() ?: mutableMapOf()
                currentMap[postId] = comments // Store comments in the map keyed by postId
                _recentCommentsMap.value = currentMap // Update LiveData, triggering redraw
                Log.d(TAG, "Fetched ${comments.size} recent comments for post $postId (limited to 3).")
            },
            onFailure = { errorMessage ->
                Log.e(TAG, "Failed to fetch recent comments for post $postId: $errorMessage")
                // On failure, ensure the map entry for this post is empty
                val currentMap = _recentCommentsMap.value?.toMutableMap() ?: mutableMapOf()
                currentMap[postId] = emptyList()
                _recentCommentsMap.value = currentMap
            }
        )
    }

    /**
     * Handles "See all comments..." click.
     * Triggers navigation to the full comments screen.
     */
    fun onViewAllCommentsClicked(postId: String) {
        // Get total comment count from the relevant post object in _posts or _usersTodayPost
        Log.d(TAG, "onViewAllCommentsClicked: postId=$postId")
        val totalCommentCount = _posts.value?.find { it.id == postId }?.commentCount ?:
        usersTodayPost.value?.commentCount ?: 0
        _navigateToAllComments.value = Pair(postId, totalCommentCount)
        _showToast.value = null // Clear any pending toast
    }

    // --- Helper function to determine if a given post's creation date falls within today ---
    private fun isPostFromToday(post: Post): Boolean {
        post.createdAt ?: return false // If no creation date, it's not from today

        val postCalendar = Calendar.getInstance().apply { time = post.createdAt }
        val todayCalendar = Calendar.getInstance()

        return postCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                postCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                postCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)
    }

    // Called when ViewModel is cleared (e.g., Activity onDestroy)
    override fun onCleared() {
        super.onCleared()
        // Ensure real-time listener for leaderboard is detached to prevent memory leaks
        detachLeaderboardListener()
    }
}