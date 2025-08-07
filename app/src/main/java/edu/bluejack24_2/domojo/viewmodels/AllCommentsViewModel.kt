package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import edu.bluejack24_2.domojo.models.PostComment
import edu.bluejack24_2.domojo.repositories.PostCommentRepository

class AllCommentsViewModel() : ViewModel() {
    private val postCommentRepository: PostCommentRepository = PostCommentRepository()

    private val _comments = MutableLiveData<List<PostComment>>()
    val comments: LiveData<List<PostComment>> get() = _comments

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private var currentPostId: String? = null

    fun fetchAllComments(postId: String) {
        if (postId.isBlank()) {
            _errorMessage.value = "Post ID is invalid for fetching comments."
            return
        }
        currentPostId = postId

        _isLoading.value = true
        _errorMessage.value = null

        postCommentRepository.getAllComments(
            postId,
            onSuccess = { allComments ->
                _comments.value = allComments
                _isLoading.value = false
            },
            onFailure = { message ->
                _errorMessage.value = message
                _isLoading.value = false
            }
        )
    }

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
            createdAt = null
        )

        postCommentRepository.addComment(
            newComment,
            onSuccess = {
                _isLoading.value = false
                currentPostId?.let { fetchAllComments(it) }
            },
            onFailure = { message ->
                _isLoading.value = false
                _errorMessage.value = message
            }
        )
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}