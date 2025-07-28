package edu.bluejack24_2.domojo.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.models.PostComment
import edu.bluejack24_2.domojo.repositories.PostCommentRepository
import edu.bluejack24_2.domojo.repositories.UserRepository // Needed for PostCommentAdapter
import com.google.firebase.auth.FirebaseAuth // Needed for PostCommentRepository

class AllCommentsViewModel(
    private val postCommentRepository: PostCommentRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val TAG = "AllCommentsViewModel"

    private val _comments = MutableLiveData<List<PostComment>>()
    val comments: LiveData<List<PostComment>> get() = _comments

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private var currentPostId: String? = null // Store the postId for which comments are being loaded

    /**
     * Fetches all comments for a specific post.
     * @param postId The ID of the post whose comments are to be fetched.
     */
    fun fetchAllComments(postId: String) {
        if (postId.isBlank()) {
            _errorMessage.value = "Post ID is invalid for fetching comments."
            return
        }
        currentPostId = postId // Store the post ID

        _isLoading.value = true
        _errorMessage.value = null

        postCommentRepository.getAllComments(
            postId,
            onSuccess = { allComments ->
                _comments.value = allComments
                _isLoading.value = false
                Log.d(TAG, "Fetched ${allComments.size} comments for post $postId.")
            },
            onFailure = { message ->
                _errorMessage.value = message
                _isLoading.value = false
                Log.e(TAG, "Failed to fetch all comments for post $postId: $message")
            }
        )
    }

    /**
     * Adds a new comment to the current post.
     * @param content The text content of the comment.
     */
    fun addComment(postId: String, content: String) {
        if (content.isBlank()) {
            _errorMessage.value = "Comment cannot be empty."
            return
        }
        if (postId.isBlank()) {
            _errorMessage.value = "Post ID missing to add comment."
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrBlank()) {
            _errorMessage.value = "Please log in to comment."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val newComment = PostComment(
            postId = postId,
            userId = userId,
            content = content,
            createdAt = null // Will be set by @ServerTimestamp
        )

        postCommentRepository.addComment(
            newComment,
            onSuccess = {
                _isLoading.value = false
                // After adding, refresh the list of comments
                currentPostId?.let { fetchAllComments(it) }
                Log.d(TAG, "Comment added successfully to post $postId.")
            },
            onFailure = { message ->
                _isLoading.value = false
                _errorMessage.value = message
                Log.e(TAG, "Failed to add comment to post $postId: $message")
            }
        )
    }

    // NEW: Public function to clear the error message (similar to ChallengeDetailViewModel)
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}