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
import androidx.fragment.app.Fragment // Import Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter // Import for ViewPager2 adapter
import com.google.android.material.tabs.TabLayoutMediator // Import for TabLayout with ViewPager2
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.adapters.ChallengeDetailPagerAdapter
import edu.bluejack24_2.domojo.databinding.ActivityChallengeDetailBinding
import edu.bluejack24_2.domojo.databinding.DialogCommentBinding
import edu.bluejack24_2.domojo.databinding.DialogPostBinding // <--- THIS IS THE IMPORT YOU NEEDED
import edu.bluejack24_2.domojo.utils.CloudinaryClient // For image upload in post dialog
import edu.bluejack24_2.domojo.viewmodels.ChallengeDetailViewModel
import java.io.File
import java.io.FileOutputStream

class ChallengeDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChallengeDetailBinding
    private lateinit var viewModel: ChallengeDetailViewModel

    // This URI is managed by the ViewModel, but we need a request code for ActivityResult
    private val PICK_POST_IMAGE_REQUEST = 1003 // A unique request code

    // Constant for passing Challenge ID via Intent
    companion object {
        const val EXTRA_CHALLENGE_ID = "extra_challenge_id"
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_COMMENT_COUNT = "extra_comment_count"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_challenge_detail)

        // Retrieve Challenge ID from the Intent that started this activity
        val challengeId = intent.getStringExtra(EXTRA_CHALLENGE_ID)
        if (challengeId == null) {
            Toast.makeText(this, "Challenge ID is missing!", Toast.LENGTH_SHORT).show()
            finish() // Close activity if no ID is provided
            return
        }

        viewModel = ViewModelProvider(this).get(ChallengeDetailViewModel::class.java)
        binding.viewModel = viewModel // Bind ViewModel to XML
        binding.lifecycleOwner = this // Enable LiveData observation

        // --- Setup Toolbar ---
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button
        binding.toolbar.setNavigationOnClickListener { onBackPressed() } // Handle back button click

        // --- Setup ViewPager2 and TabLayout for "Posts" and "Leaderboard" tabs ---
        val pagerAdapter = ChallengeDetailPagerAdapter(this, challengeId) // Pass activity and challengeId
        binding.detailViewPager.adapter = pagerAdapter // Assign the adapter to ViewPager2

        // Connect TabLayout (for tabs/dots) to ViewPager2
        TabLayoutMediator(binding.detailTabLayout, binding.detailViewPager) { tab, position ->
            tab.text = when (position) { // Set tab titles based on position
                0 -> "Posts"
                1 -> "Leaderboard"
                else -> "Tab" // Fallback, should not happen
            }
        }.attach() // Attaches the mediator, which creates and manages tabs/indicators

        // Load challenge details into the ViewModel
        viewModel.loadChallengeDetails(challengeId)

        // Observe challengeDetails for potential UI updates (e.g., toolbar title)
        viewModel.challengeDetails.observe(this, Observer { challenge ->
            // If challenge data is available, update the toolbar title
            supportActionBar?.title = challenge?.title ?: "Challenge Details"
        })

        // currentUserChallengeMember and leaderboard are observed by their respective Fragments,
        // and some data is bound directly in XML.

        // Observe loading state
        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.actionButton.isEnabled = !isLoading // Disable action button while loading
            // You might disable other inputs too if needed
        })

        // Observe error messages (for Toast feedback)
        viewModel.errorMessage.observe(this, Observer { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage() // Consume the event to prevent re-showing
            }
        })

        // Observe request to show Post Activity dialog
        viewModel.showPostDialog.observe(this, Observer { show ->
            if (show) {
                showPostActivityDialog()
                viewModel.onPostDialogShown() // Consume the event after showing
            }
        })


        // --- Observe LiveData from ViewModel ---
        viewModel.showAddCommentDialog.observe(this, Observer { event ->
//            Log.d("ChallengeDetailActivity", "showAddCommentDialog event received with postId: $postId")
//            postId?.let {
//                showAddCommentDialog(it) // Call dialog function with postId
//                viewModel.onAddCommentDialogShown() // Consume event
//            }

            event?.getContentIfNotHandled()?.let { postId ->
                Log.d("ChallengeDetailActivity", "showAddCommentDialog event received with postId: $postId")
                showAddCommentDialog(postId)
                viewModel.onAddCommentDialogShown()
            }
        })


        viewModel.navigateToAllComments.observe(this, Observer { pair ->
            pair?.let { (postId, commentCount) ->
                Log.d("ChallengeDetailActivity", "Navigating to AllCommentsActivity with postId: $postId, commentCount: $commentCount")
                val intent = Intent(this, AllCommentsActivity::class.java).apply {
                    putExtra(EXTRA_POST_ID, postId)
                    putExtra(EXTRA_COMMENT_COUNT, commentCount)
                }
                startActivity(intent)
                viewModel.onNavigationToAllCommentsHandled()
            }
        })


        // Observe selectedPostImageUri from ViewModel to update the image preview in the post dialog.
        // This observer handles reactivity if the dialog is still open when onActivityResult returns.
        viewModel.selectedPostImageUri.observe(this, Observer { uri ->
            // This observer is mainly for the dialog's internal ImageView.
            // No direct UI update on Activity here typically.
        })

        // The action button's onClick is handled via DataBinding in XML
        // android:onClick="@{() -> viewModel.challengeDetails.isJoined ? viewModel.onPostActivityClicked() : viewModel.onJoinChallengeClicked(viewModel.challengeDetails)}"
    }

    private fun showAddCommentDialog(postId: String) {
        Log.d("Showing Comment Dialog", "Post ID: $postId")
        val dialogBinding: DialogCommentBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this), R.layout.dialog_comment, null, false
        )
        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)

        val dialog = builder.create()

        dialogBinding.submitCommentButton.setOnClickListener {
            val commentContent = dialogBinding.commentContentEditText.text.toString()
            viewModel.submitComment(postId, commentContent) // Call ViewModel to submit
            dialog.dismiss()
        }

        dialogBinding.cancelCommentButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Handles results from other activities (like image picker)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_POST_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            viewModel.selectedPostImageUri.value = uri // Update ViewModel's LiveData
        }
    }

    // Utility function to get a File from a Uri (needed for image uploads)
    // This function resides in the Activity as it requires Context and ContentResolver
    fun getRealFileFromUri(context: Context, uri: Uri): File? {
        // Implement a robust URI to File conversion here.
        // This is a simplified version and might not work for all URIs.
        // For production, consider a more robust solution from libraries or Android's ContentResolver.
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

    // --- Function to Show the Post Activity Dialog (Popup Modal) ---
    private fun showPostActivityDialog() {
        val dialogBinding: DialogPostBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this), R.layout.dialog_post, null, false
        )
        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false) // User must explicitly cancel or post

        val dialog = builder.create()

        // Set up image picker button within the dialog
        dialogBinding.selectPostImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_POST_IMAGE_REQUEST) // Use Activity's result
        }

        // Observe ViewModel's selectedPostImageUri to update the dialog's image preview
        // This is crucial because onActivityResult updates the ViewModel, and the dialog
        // needs to react to that update if it's still open.
        viewModel.selectedPostImageUri.observe(this, Observer { uri ->
            if (uri != null) {
                dialogBinding.postImagePreview.setImageURI(uri)
                dialogBinding.postImagePreview.visibility = View.VISIBLE
            } else {
                dialogBinding.postImagePreview.visibility = View.GONE
            }
        })
        // Manually set initial state of preview if an image was selected before dialog was shown
        if (viewModel.selectedPostImageUri.value != null) {
            dialogBinding.postImagePreview.setImageURI(viewModel.selectedPostImageUri.value)
            dialogBinding.postImagePreview.visibility = View.VISIBLE
        } else {
            dialogBinding.postImagePreview.visibility = View.GONE
        }

        dialogBinding.postButton.setOnClickListener {
            val content = dialogBinding.postContentEditText.text.toString()
            // Get the image File from the URI stored in ViewModel
            val imageFile: File? = viewModel.selectedPostImageUri.value?.let { uri ->
                getRealFileFromUri(this, uri)
            }

            // Call ViewModel to handle the post creation logic
            viewModel.postActivity(
                context = this, // Pass context for image upload
                content = content,
                imageFile = imageFile
            )
            dialog.dismiss() // Dismiss dialog after initiating post
        }

        dialogBinding.cancelButton.setOnClickListener {
            viewModel.selectedPostImageUri.value = null // Clear selected image in ViewModel
            dialog.dismiss()
        }

        dialog.show() // Show the AlertDialog
    }

    // Detach real-time listener when Activity is destroyed to prevent memory leaks
    override fun onDestroy() {
        super.onDestroy()
        viewModel.detachLeaderboardListener()
    }
}