package edu.bluejack24_2.domojo.views.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.adapters.ChallengeDetailPagerAdapter
import edu.bluejack24_2.domojo.databinding.ActivityChallengeDetailBinding
import edu.bluejack24_2.domojo.databinding.DialogCommentBinding
import edu.bluejack24_2.domojo.databinding.DialogPostBinding
import edu.bluejack24_2.domojo.viewmodels.ChallengeDetailViewModel
import java.io.File
import java.io.FileOutputStream

class ChallengeDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityChallengeDetailBinding
    private lateinit var viewModel: ChallengeDetailViewModel

    private val PICK_POST_IMAGE_REQUEST = 1003
    companion object {
        const val EXTRA_CHALLENGE_ID = "extra_challenge_id"
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_COMMENT_COUNT = "extra_comment_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_challenge_detail)

        val challengeId = intent.getStringExtra(EXTRA_CHALLENGE_ID)
        if (challengeId == null) {
            Toast.makeText(this, "Challenge ID is missing!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this).get(ChallengeDetailViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        val pagerAdapter = ChallengeDetailPagerAdapter(this, challengeId)
        binding.detailViewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.detailTabLayout, binding.detailViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Posts"
                1 -> "Leaderboard"
                else -> "Tab"
            }
        }.attach()

        viewModel.loadChallengeDetails(challengeId)

        viewModel.challengeDetails.observe(this, Observer { challenge ->
            supportActionBar?.title = challenge?.title ?: "Challenge Details"
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.actionButton.isEnabled = !isLoading
        })

        viewModel.errorMessage.observe(this, Observer { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        })

        viewModel.showPostDialog.observe(this, Observer { show ->
            if (show) {
                showPostActivityDialog()
                viewModel.onPostDialogShown()
            }
        })

        viewModel.showAddCommentDialog.observe(this, Observer { postId ->
            postId?.let {
                showAddCommentDialog(it)
                viewModel.onAddCommentDialogShown()
            }
        })

        viewModel.navigateToAllComments.observe(this, Observer { pair ->
            pair?.let { (postId, commentCount) ->
                val intent = Intent(this, AllCommentsActivity::class.java).apply {
                    putExtra(EXTRA_POST_ID, postId)
                    putExtra(EXTRA_COMMENT_COUNT, commentCount)
                }
                startActivity(intent)
                viewModel.onNavigationToAllCommentsHandled()
            }
        })

        viewModel.selectedPostImageUri.observe(this, Observer { uri ->
        })
    }

    private fun showAddCommentDialog(postId: String) {
        val dialogBinding: DialogCommentBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this), R.layout.dialog_comment, null, false
        )
        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)

        val dialog = builder.create()

        dialogBinding.submitCommentButton.setOnClickListener {
            val commentContent = dialogBinding.commentContentEditText.text.toString()
            viewModel.submitComment(postId, commentContent)
            dialog.dismiss()
        }

        dialogBinding.cancelCommentButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_POST_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            viewModel.selectedPostImageUri.value = uri
        }
    }

    fun getRealFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            Toast.makeText(context, "Error getting image file: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun showPostActivityDialog() {
        val dialogBinding: DialogPostBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this), R.layout.dialog_post, null, false
        )
        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)

        val dialog = builder.create()

        dialogBinding.selectPostImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_POST_IMAGE_REQUEST)
        }

        viewModel.selectedPostImageUri.observe(this, Observer { uri ->
            if (uri != null) {
                dialogBinding.postImagePreview.setImageURI(uri)
                dialogBinding.postImagePreview.visibility = View.VISIBLE
            } else {
                dialogBinding.postImagePreview.visibility = View.GONE
            }
        })

        if (viewModel.selectedPostImageUri.value != null) {
            dialogBinding.postImagePreview.setImageURI(viewModel.selectedPostImageUri.value)
            dialogBinding.postImagePreview.visibility = View.VISIBLE
        } else {
            dialogBinding.postImagePreview.visibility = View.GONE
        }

        dialogBinding.postButton.setOnClickListener {
            val content = dialogBinding.postContentEditText.text.toString()
            val imageFile: File? = viewModel.selectedPostImageUri.value?.let { uri ->
                getRealFileFromUri(this, uri)
            }

            if(content == "" || content.isBlank()) {
                Toast.makeText(this, "Content cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.postActivity(
                context = this,
                content = content,
                imageFile = imageFile
            )
            dialog.dismiss()
        }

        dialogBinding.cancelButton.setOnClickListener {
            viewModel.selectedPostImageUri.value = null
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.detachLeaderboardListener()
    }
}