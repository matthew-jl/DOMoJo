package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.adapters.PostCommentAdapter
import edu.bluejack24_2.domojo.databinding.ActivityAllCommentsBinding
import edu.bluejack24_2.domojo.viewmodels.AllCommentsViewModel

class AllCommentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllCommentsBinding
    private lateinit var viewModel: AllCommentsViewModel
    private lateinit var commentsAdapter: PostCommentAdapter

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_COMMENT_COUNT = "extra_comment_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_comments)

        val postId = intent.getStringExtra(EXTRA_POST_ID)
        val totalCommentCount = intent.getIntExtra(EXTRA_COMMENT_COUNT, 0)

        if (postId == null) {
            Toast.makeText(this, "Post ID is missing for comments!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // <ViewModel Setup Section>
        viewModel = ViewModelProvider(this)[AllCommentsViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // <Toolbar Setup Section>
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Comments (${totalCommentCount})"
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        // <RecyclerView Setup Section>
        commentsAdapter = PostCommentAdapter(emptyList())
        binding.allCommentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AllCommentsActivity)
            adapter = commentsAdapter
        }

        // <Observer Setup Section>
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
            binding.commentInputLayout.isEnabled = !isLoading
            binding.submitCommentButton.isEnabled = !isLoading
        })

        viewModel.errorMessage.observe(this, Observer { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        })

        // <Submit Comment Setup Section>
        binding.submitCommentButton.setOnClickListener {
            val commentContent = binding.commentEditText.text.toString()
            viewModel.addComment(postId, commentContent)
            binding.commentEditText.text?.clear()
        }

        viewModel.fetchAllComments(postId)
    }
}
