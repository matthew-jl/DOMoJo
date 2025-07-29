package edu.bluejack24_2.domojo.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore // For UserRepository init
import edu.bluejack24_2.domojo.databinding.ItemCommentBinding // Import new comment item binding
import edu.bluejack24_2.domojo.models.PostComment
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.repositories.UserRepository
import java.text.SimpleDateFormat
import java.util.Locale

class PostCommentAdapter(
    private var comments: List<PostComment>,
    private val userRepository: UserRepository = UserRepository()
) : RecyclerView.Adapter<PostCommentAdapter.PostCommentViewHolder>() {
    private val TAG = "PostCommentAdapter"

    // Shorter date format for comments (e.g., "Jul 28, 10:30")
    private val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    fun updateComments(newComments: List<PostComment>) {
        Log.d(TAG, "updateComments called with ${newComments.size} new items.")
        this.comments = newComments
        notifyDataSetChanged()
        Log.d(TAG, "notifyDataSetChanged called. Adapter has ${itemCount} items.") // Add this

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostCommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostCommentViewHolder(binding, userRepository, dateFormatter)
    }

    override fun getItemCount(): Int = comments.size

    override fun onBindViewHolder(holder: PostCommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    // In PostCommentAdapter.kt
    class PostCommentViewHolder(
        private val binding: ItemCommentBinding,
        private val userRepository: UserRepository,
        private val dateFormatter: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        private val TAG = "PostCommentAdapter"
        fun bind(comment: PostComment) {
            binding.comment = comment // Binds the comment object to the XML variable
            binding.executePendingBindings() // Immediately processes the binding for 'comment' related views

            // Fetch user data for the comment author
            userRepository.getUser(comment.userId,
                onSuccess = { user ->
                    if (user != null) {
                        binding.user = user // Binds the user object to the XML variable
                        binding.executePendingBindings() // Important: Re-execute after 'user' is set
                    } else {
                        binding.commentUsername.text = "[Unknown User]"
                        // Consider setting a default avatar here using Glide or Picasso if imageUrl binding fails
                    }
                },
                onFailure = { errorMessage ->
                    binding.commentUsername.text = "[Error User]"
                    Log.e(TAG, "Failed to fetch user for comment ${comment.id}: $errorMessage")
                    // Consider setting a default avatar on error as well
                }
            )

            // Format and set comment date
            comment.createdAt?.let {
                binding.commentDate.text = dateFormatter.format(it)
            } ?: run {
                binding.commentDate.text = "Date Unknown"
            }
        }
    }
}