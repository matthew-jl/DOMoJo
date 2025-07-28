package edu.bluejack24_2.domojo.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemPostBinding
import edu.bluejack24_2.domojo.models.Post
import edu.bluejack24_2.domojo.repositories.UserRepository
import edu.bluejack24_2.domojo.viewmodels.ChallengeDetailViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class ChallengePostAdapter(private var posts: List<Post>) :
    RecyclerView.Adapter<ChallengePostAdapter.ChallengePostViewHolder>() {

    fun updatePosts(newPosts: List<Post>) {
        this.posts = newPosts
        notifyDataSetChanged() // Simplest way to update, consider DiffUtil for larger lists
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengePostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChallengePostViewHolder(binding)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ChallengePostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    class ChallengePostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Date formatter for display
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        private val userRepository: UserRepository = UserRepository()
        private val viewModel: ChallengeDetailViewModel = ChallengeDetailViewModel()


        fun bind(post: Post) {
            binding.post = post // Bind the ChallengeActivityPost object
            binding.viewModel = viewModel
            binding.executePendingBindings()

            userRepository.getUser(post.userId,
                onSuccess = { user ->
                    if (user != null) {
                        binding.user = user // <--- THIS IS THE KEY: Set the 'user' variable in the XML
                        // The XML's @{user.username} and @{user.avatar} bindings will now automatically update
                    } else {
                        binding.postUsernameTextView.text = "[User Not Found]"
                        // Optionally set a specific default avatar for not found users
                    }
                },
                onFailure = { errorMessage ->
                    binding.postUsernameTextView.text = "[Error User]"
                    Log.e("ChallengePostAdapter", "Failed to fetch user for post ${post.id}: $errorMessage")
                    // Optionally set a specific default avatar for error users
                }
            )


            // Handle date display if createdAt is available
            post.createdAt?.let {
                binding.postDateTextView.text = dateFormatter.format(it)
            } ?: run {
                binding.postDateTextView.text = "Date Unavailable"
            }



            // You'll need to fetch the username and avatar for the post's userId
            // This is complex and usually involves a UserRepository or passing user data down.
            // For now, these TextViews will be empty unless set manually.
            // binding.postUsernameTextView.text = "Loading User..."
            // Glide.with(binding.root.context).load(userAvatarUrl).into(binding.postUserAvatarImageView)
        }
    }
}