package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.adapters.PostCommentAdapter // Import your comment adapter
import edu.bluejack24_2.domojo.databinding.ActivityAllCommentsBinding // NEW: Import binding for this Activity
import edu.bluejack24_2.domojo.viewmodels.AllCommentsViewModel // NEW: Import ViewModel for this Activity (will create below)
import com.google.firebase.firestore.FirebaseFirestore // Needed for ViewModel init (or inject)
import edu.bluejack24_2.domojo.repositories.PostCommentRepository // Needed for ViewModel init (or inject)
import edu.bluejack24_2.domojo.repositories.UserRepository // Needed for PostCommentAdapter init (or inject)
import com.google.firebase.auth.FirebaseAuth // Needed for PostCommentRepository init (or inject)


class AllCommentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllCommentsBinding
    private lateinit var viewModel: AllCommentsViewModel // ViewModel for AllCommentsActivity
    private lateinit var commentsAdapter: PostCommentAdapter

    companion object {
        const val EXTRA_POST_ID = "extra_post_id" // Key for post ID passed via Intent
        const val EXTRA_COMMENT_COUNT = "extra_comment_count" // Key for total comment count
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_comments)

        // Retrieve postId and totalCommentCount from the Intent
        val postId = intent.getStringExtra(EXTRA_POST_ID)
        val totalCommentCount = intent.getIntExtra(EXTRA_COMMENT_COUNT, 0)

        if (postId == null) {
            Toast.makeText(this, "Post ID is missing for comments!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize ViewModel for this activity
        // You might use a ViewModelFactory here to inject repositories if preferred
        viewModel = ViewModelProvider(this, AllCommentsViewModelFactory(
            PostCommentRepository(),
            UserRepository()
        )).get(AllCommentsViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button
        supportActionBar?.title = "Comments (${totalCommentCount})" // Set title with count
        binding.toolbar.setNavigationOnClickListener { onBackPressed() } // Handle back button

        // Setup RecyclerView for all comments
        commentsAdapter = PostCommentAdapter(emptyList()) // Initialize empty
        binding.allCommentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AllCommentsActivity)
            adapter = commentsAdapter
        }

        // Observe LiveData from ViewModel
        viewModel.comments.observe(this, Observer { comments ->
            commentsAdapter.updateComments(comments)
            if (comments.isEmpty() && !viewModel.isLoading.value!!) {
                binding.noCommentsMessage.visibility = View.VISIBLE
            } else {
                binding.noCommentsMessage.visibility = View.GONE
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Optionally disable input while loading
            binding.commentInputLayout.isEnabled = !isLoading
            binding.submitCommentButton.isEnabled = !isLoading
        })

        viewModel.errorMessage.observe(this, Observer { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        })

        // Handle comment submission button
        binding.submitCommentButton.setOnClickListener {
            val commentContent = binding.commentEditText.text.toString()
            viewModel.addComment(postId, commentContent)
            binding.commentEditText.text?.clear() // Clear input field after submitting
        }

        // Fetch all comments when activity starts
        viewModel.fetchAllComments(postId)
    }
}


// NEW: ViewModel Factory for AllCommentsViewModel
class AllCommentsViewModelFactory(
    private val postCommentRepository: PostCommentRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AllCommentsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AllCommentsViewModel(postCommentRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}