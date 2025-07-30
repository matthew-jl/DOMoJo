package edu.bluejack24_2.domojo.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.models.Post
import edu.bluejack24_2.domojo.models.PostComment
import edu.bluejack24_2.domojo.models.ChallengeMember
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.repositories.ChallengeRepository
import edu.bluejack24_2.domojo.repositories.ChallengeMemberRepository
import edu.bluejack24_2.domojo.repositories.PostRepository
import edu.bluejack24_2.domojo.repositories.PostLikeRepository
import edu.bluejack24_2.domojo.repositories.PostCommentRepository
import edu.bluejack24_2.domojo.repositories.UserRepository
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import java.io.File
import java.util.Calendar
import java.util.Date

class ChallengeDetailViewModel : ViewModel() {
    private val challengeRepository: ChallengeRepository = ChallengeRepository()
    private val userRepository: UserRepository = UserRepository()
    private val challengeMemberRepository: ChallengeMemberRepository = ChallengeMemberRepository()
    private val postRepository: PostRepository = PostRepository()
    private val postLikeRepository: PostLikeRepository = PostLikeRepository()
    private val postCommentRepository: PostCommentRepository = PostCommentRepository()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _challengeDetails = MutableLiveData<Challenge?>()
    val challengeDetails: LiveData<Challenge?> get() = _challengeDetails

    private val _currentUserChallengeMember = MutableLiveData<ChallengeMember?>()
    val currentUserChallengeMember: LiveData<ChallengeMember?> get() = _currentUserChallengeMember

    private val _leaderboard = MutableLiveData<List<ChallengeMember>>()
    val leaderboard: LiveData<List<ChallengeMember>> get() = _leaderboard
    private var leaderboardListenerRegistration: ListenerRegistration? = null

    private val _showPostDialog = MutableLiveData<Boolean>()
    val showPostDialog: LiveData<Boolean> get() = _showPostDialog

    private val _hasPostedToday = MutableLiveData<Boolean>(false)
    val hasPostedToday: LiveData<Boolean> get() = _hasPostedToday

    private val _usersTodayPost = MutableLiveData<Post?>()
    val usersTodayPost: LiveData<Post?> get() = _usersTodayPost

    private val _usersTodayPostUser = MutableLiveData<User?>()
    val usersTodayPostUser: LiveData<User?> get() = _usersTodayPostUser

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage
    private val _showToast = MutableLiveData<String?>()
    val showToast: LiveData<String?> get() = _showToast

    private val _postUserActions = MutableLiveData<MutableMap<String, String>>(mutableMapOf())
    val postUserActions: LiveData<MutableMap<String, String>> get() = _postUserActions

    fun getPostUserAction(postId: String): String = _postUserActions.value?.get(postId) ?: "none"

    private val _recentCommentsMap = MutableLiveData<MutableMap<String, List<PostComment>>>(mutableMapOf())
    val recentCommentsMap: LiveData<MutableMap<String, List<PostComment>>> get() = _recentCommentsMap // Make this publicly observable

    private val _navigateToAllComments = MutableLiveData<Pair<String, Int>?>()
    val navigateToAllComments: LiveData<Pair<String, Int>?> get() = _navigateToAllComments

    private val _showAddCommentDialog = MutableLiveData<String>()
    val showAddCommentDialog: LiveData<String> = _showAddCommentDialog

    private val TAG = "ChallengeDetailVM"
    val selectedPostImageUri = MutableLiveData<Uri?>()

    fun loadChallengeDetails(challengeId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        challengeRepository.getChallenge(
            challengeId,
            onSuccess = { challenge ->
                _challengeDetails.value = challenge
                if (challenge == null) {
                    _errorMessage.value = "Challenge not found."
                    _isLoading.value = false
                    return@getChallenge
                }

                fetchCurrentUserMembership(challenge.id)

                startLeaderboardListener(challenge.id)

                checkTodayPostStatus(challenge.id)

                fetchChallengePosts(challenge.id)
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
            _currentUserChallengeMember.value = null
            return
        }

        challengeMemberRepository.getChallengeMemberForChallenge(
            challengeId,
            onSuccess = { member ->
                _currentUserChallengeMember.value = member
                _challengeDetails.value = _challengeDetails.value?.copy(isJoined = (member != null))
            },
            onFailure = { message ->
                _errorMessage.value = message
                Log.e(TAG, "Failed to fetch current user's membership: $message")
                _challengeDetails.value = _challengeDetails.value?.copy(isJoined = false)
            }
        )
    }

    private fun startLeaderboardListener(challengeId: String) {
        leaderboardListenerRegistration?.remove()
        leaderboardListenerRegistration = challengeMemberRepository.getLeaderboard(
            challengeId,
            onData = { members ->
                _leaderboard.value = members
                _isLoading.value = false
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
    }

    fun fetchChallengePosts(challengeId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid

        postRepository.getAllChallengePosts(
            challengeId,
            onSuccess = { allPosts ->
                val filteredPosts = allPosts.filter { post ->
                    !(post.userId == currentUserId && post.streakAwarded && isPostFromToday(post))
                }.sortedByDescending { it.createdAt }

                _posts.value = filteredPosts

                filteredPosts.forEach { post ->
                    if (currentUserId != null) {
                        postLikeRepository.getUserLikeStatus(
                            post.id, currentUserId,
                            onSuccess = { action ->
                                val currentActions = _postUserActions.value?.toMutableMap() ?: mutableMapOf()
                                currentActions[post.id] = action
                                _postUserActions.value = currentActions
                            },
                            onFailure = { Log.e(TAG, "Failed to get user action for post ${post.id}: $it") }
                        )
                    }
                    fetchRecentCommentsForPost(post.id)
                }
            },
            onFailure = { message ->
                Log.e(TAG, "Failed to load all posts for challenge $challengeId: $message")
                _errorMessage.value = message
            }
        )
    }

    fun onJoinChallengeClicked(challenge: Challenge) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _showToast.value = "Please log in to join a challenge."
            return
        }
        if (challenge.isJoined) {
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
                _currentUserChallengeMember.value = newMember
                _challengeDetails.value = challenge.copy(isJoined = true)
                checkTodayPostStatus(challenge.id)
            },
            onFailure = { message ->
                _isLoading.value = false
                _showToast.value = "Failed to join challenge: $message"
            }
        )
    }

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
        if (member == null) {
            _showToast.value = "Please join the challenge first to post."
            return
        }

        if (_hasPostedToday.value == true) {
            _showToast.value = "You have already posted today's activity!"
            return
        }

        _showToast.value = null
        _showPostDialog.value = true
    }

    fun onPostDialogShown() {
        _showPostDialog.value = false
        selectedPostImageUri.value = null
    }

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

        val todayStart = Calendar.getInstance().apply {
            time = Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val lastActivity = member.lastActivityDate
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
        val newPost = Post(
            challengeId = challengeId,
            userId = userId,
            memberId = memberId,
            content = content,
            imageUrl = imageUrl,
            streakAwarded = isNewStreakDay,
            createdAt = null
        )

        postRepository.addActivityPost(
            newPost,
            onSuccess = {
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
                        Calendar.getInstance().time,
                        onSuccess = {
                            _isLoading.value = false
                            _showToast.value = "Activity posted! Streak updated to $newCurrentStreak!"
                            _challengeDetails.value?.id?.let { fetchCurrentUserMembership(it) }
                            checkTodayPostStatus(challengeId)
                            fetchChallengePosts(challengeId)
                        },
                        onFailure = { errorMessage ->
                            _isLoading.value = false
                            _showToast.value = "Activity posted, but failed to update streak: $errorMessage"
                        }
                    )
                } else {
                    _isLoading.value = false
                    _showToast.value = "Activity posted!"
                    checkTodayPostStatus(challengeId)
                    fetchChallengePosts(challengeId)
                }
            },
            onFailure = { errorMessage ->
                _isLoading.value = false
                _showToast.value = "Failed to post activity: $errorMessage"
            }
        )
    }

    fun checkTodayPostStatus(challengeId: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _hasPostedToday.value = false
            _usersTodayPost.value = null
            _usersTodayPostUser.value = null
            return
        }
        postRepository.getTodayStreakAwardedPost(
            challengeId,
            userId,
            onSuccess = { post ->
                _hasPostedToday.value = (post != null)
                _usersTodayPost.value = post

                if (post != null) {
                    userRepository.getUser(post.userId,
                        onSuccess = { user ->
                            _usersTodayPostUser.value = user
                        },
                        onFailure = { Log.e(TAG, "Failed to get user for today's post: $it") }
                    )
                } else {
                    _usersTodayPostUser.value = null
                }
            },
            onFailure = { message ->
                Log.e(TAG, "Failed to check today's post status: $message")
                _hasPostedToday.value = false
                _usersTodayPost.value = null
                _usersTodayPostUser.value = null
            }
        )
    }

    fun onLikeClicked(postId: String, type: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _showToast.value = "Please log in to like/dislike posts."
            return
        }

        _showToast.value = "Updating action..."

        postLikeRepository.toggleLike(
            postId, type,
            onSuccess = { newLikeCount, newDislikeCount, currentUserAction ->
                _showToast.value = "Post action updated!"
                updatePostCountsInList(postId, newLikeCount, newDislikeCount)
                val currentActions = _postUserActions.value?.toMutableMap() ?: mutableMapOf()
                currentActions[postId] = currentUserAction ?: "none"
                _postUserActions.value = currentActions
            },
            onFailure = { message ->
                _showToast.value = "Failed to update post action: $message"
            }
        )
    }

    private fun updatePostCountsInList(postId: String, newLikeCount: Int, newDislikeCount: Int) {
        val currentPosts = _posts.value?.toMutableList() ?: mutableListOf()
        val index = currentPosts.indexOfFirst { it.id == postId }
        if (index != -1) {
            val updatedPost = currentPosts[index].copy(
                likeCount = newLikeCount,
                dislikeCount = newDislikeCount
            )
            currentPosts[index] = updatedPost
            _posts.value = currentPosts
        }

        if (usersTodayPost.value?.id == postId) {
            _usersTodayPost.value = usersTodayPost.value?.copy(
                likeCount = newLikeCount,
                dislikeCount = newDislikeCount
            )
        }
    }

    fun onCommentClicked(postId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _showToast.value = "Please log in to add a comment."
            return
        }
        _showAddCommentDialog.value = postId
        _showToast.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

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

        _isLoading.value = true
        _showToast.value = "Submitting comment..."

        val newComment = PostComment(
            postId = postId,
            userId = currentUserId,
            content = content,
            createdAt = null
        )

        postCommentRepository.addComment(
            newComment,
            onSuccess = {
                _isLoading.value = false
                _showToast.value = "Comment added successfully!"
                val currentMap = _recentCommentsMap.value?.toMutableMap() ?: mutableMapOf()
                val currentCommentsForPost = currentMap[postId]?.toMutableList() ?: mutableListOf()
                newComment.createdAt = Date()
                currentCommentsForPost.add(0, newComment)
                currentMap[postId] = currentCommentsForPost.take(3)

                _recentCommentsMap.value = null
                _recentCommentsMap.value = currentMap

                _challengeDetails.value?.id?.let { challengeId ->
                    fetchChallengePosts(challengeId)
                }
            },
            onFailure = { errorMessage ->
                _isLoading.value = false
                _showToast.value = "Failed to add comment: $errorMessage"
            }
        )
    }

    fun onAddCommentDialogShown() {
        _showAddCommentDialog.value = null
    }

    fun onNavigationToAllCommentsHandled() {
        _navigateToAllComments.value = null
    }

    fun fetchRecentCommentsForPost(postId: String) {
        postCommentRepository.getRecentComments(
            postId, 3,
            onSuccess = { comments ->
                val currentMap = _recentCommentsMap.value?.toMutableMap() ?: mutableMapOf()
                currentMap[postId] = comments
                _recentCommentsMap.value = null
                _recentCommentsMap.value = currentMap
            },
            onFailure = { errorMessage ->
                Log.e(TAG, "Failed to fetch recent comments for post $postId: $errorMessage")
                val currentMap = _recentCommentsMap.value?.toMutableMap() ?: mutableMapOf()
                currentMap[postId] = emptyList()
                _recentCommentsMap.value = null
                _recentCommentsMap.value = currentMap
            }
        )
    }

    fun onViewAllCommentsClicked(postId: String) {
        val totalCommentCount = _posts.value?.find { it.id == postId }?.commentCount ?:
        usersTodayPost.value?.commentCount ?: 0
        _navigateToAllComments.value = Pair(postId, totalCommentCount)
        _showToast.value = null
    }

    private fun isPostFromToday(post: Post): Boolean {
        post.createdAt ?: return false

        val postCalendar = Calendar.getInstance().apply { time = post.createdAt }
        val todayCalendar = Calendar.getInstance()

        return postCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                postCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                postCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)
    }

    override fun onCleared() {
        super.onCleared()
        detachLeaderboardListener()
    }
}