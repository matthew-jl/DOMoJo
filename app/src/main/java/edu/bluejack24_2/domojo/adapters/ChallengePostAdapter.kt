package edu.bluejack24_2.domojo.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View // Import View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner // Make sure this is imported
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemPostBinding
import edu.bluejack24_2.domojo.models.Post
import edu.bluejack24_2.domojo.repositories.UserRepository
import edu.bluejack24_2.domojo.viewmodels.ChallengeDetailViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class ChallengePostAdapter(private var posts: List<Post>, private val viewModel: ChallengeDetailViewModel) :
    RecyclerView.Adapter<ChallengePostAdapter.ChallengePostViewHolder>() {

    fun updatePosts(newPosts: List<Post>) {
        this.posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengePostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChallengePostViewHolder(binding, viewModel)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ChallengePostViewHolder, position: Int) {
        holder.binding.lifecycleOwner = holder.itemView.findViewTreeLifecycleOwner()
        Log.d("ChallengePostAdapter", "Lifecycle owner set for post at position $position: ${holder.binding.lifecycleOwner?.javaClass?.simpleName}")
        holder.bind(posts[position])
        // The lifecycleOwner for the binding is already set by the onViewAttachedToWindow listener
        // You can remove this line now: holder.binding.lifecycleOwner = holder.itemView.findViewTreeLifecycleOwner() as LifecycleOwner?
    }

    class ChallengePostViewHolder(val binding: ItemPostBinding,
                                  private val viewModel: ChallengeDetailViewModel) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        private val userRepository: UserRepository = UserRepository()
        private val commentAdapter: PostCommentAdapter // Declare here, initialize in init

        init {
            commentAdapter = PostCommentAdapter(comments = emptyList(), userRepository) // Initialize here

            binding.commentsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = commentAdapter // Set the adapter
                isNestedScrollingEnabled = false
            }

            binding.likeButton.setOnClickListener {
                binding.post?.id?.let { postId ->
                    viewModel.onLikeClicked(postId, "like")
                }
            }

            binding.dislikeButton.setOnClickListener {
                binding.post?.id?.let { postId ->
                    // Assuming you have onDislikeClicked in your ViewModel
                    viewModel.onLikeClicked(postId, "dislike") // Use onLikeClicked with "dislike" type
                }
            }

            binding.commentButton.setOnClickListener {
                binding.post?.id?.let { postId ->
                    viewModel.onCommentClicked(postId)
                }
            }

            // Move the LiveData observers here, inside the init block
            // because onViewAttachedToWindow provides the lifecycleOwner
            // AND guarantees that the view (and its adapter) are attached.
            itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    val lifecycleOwner = v.findViewTreeLifecycleOwner()
                    binding.lifecycleOwner = lifecycleOwner // Set for data binding directly

                    // First, fetch the comments BEFORE setting up observers
                    val postId = binding.post?.id
                    if (postId != null) {
                        viewModel.fetchRecentCommentsForPost(postId)
                    }


                    if (lifecycleOwner != null) {
                        // Always clear previous observers for this lifecycle owner (important in RecyclerView)
                        viewModel.recentCommentsMap.removeObservers(lifecycleOwner)

                        viewModel.recentCommentsMap.observe(lifecycleOwner) { commentsMap ->
                            val comments = commentsMap[binding.post?.id] ?: emptyList()
                            Log.d("ChallengePostAdapter", "Observed ${comments.size} comments for post ${binding.post?.id}")
                            commentAdapter.updateComments(comments)
                        }
                    }

                    lifecycleOwner?.let { owner ->
                        Log.d("ChallengePostAdapter", "Lifecycle owner attached: ${owner.javaClass.simpleName}")

                        // Observe recent comments map
                        viewModel.recentCommentsMap.removeObservers(owner) // Always remove old observers
                        viewModel.recentCommentsMap.observe(owner) { commentsMap ->

                            if (commentsMap == null) {
                                Log.w("ChallengePostAdapter", "recentCommentsMap delivered a null map for post: ${binding.post?.id}")
                                commentAdapter.updateComments(emptyList()) // Update with empty list if map is null
                                return@observe // Exit early
                            }

                            val postId = binding.post?.id ?: return@observe
                            val recentComments = commentsMap[postId] ?: emptyList()

                            // **No need for the cast 'as PostCommentAdapter' now**
                            // Because 'commentAdapter' is directly accessible and is known type
                            commentAdapter.updateComments(recentComments)
                            Log.d("Challenge Post Adapter", "Updated comments for post $postId: ${recentComments.size}")
                        }

                        // Observe post user actions
                        viewModel.postUserActions.removeObservers(owner) // Always remove old observers
                        viewModel.postUserActions.observe(owner) { actionsMap ->
                            val postId = binding.post?.id ?: return@observe // Get postId from bound post
                            val userAction = actionsMap[postId] ?: "none"
                            Log.d("ChallengePostAdapter", "Post ${postId} user action changed to: $userAction - Updating image resources.")
                            when (userAction) {
                                "like" -> {
                                    binding.likeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_like_filled)
                                    binding.dislikeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_dislike_outline)
                                }
                                "dislike" -> {
                                    binding.likeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_like_outline)
                                    binding.dislikeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_dislike_filled)
                                }
                                else -> {
                                    binding.likeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_like_outline)
                                    binding.dislikeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_dislike_outline)
                                }
                            }
                        }


                    }
                }

                override fun onViewDetachedFromWindow(v: View) {
                    // LiveData observers tied to 'owner' will automatically stop when 'owner' is destroyed.
                    // However, if the ViewHolder is just recycled, 'owner' might still be active.
                    // For safety, you might want to manually remove observers that are *not* tied to the item's own lifecycle,
                    // but usually, LiveData's lifecycle awareness handles this correctly for the attached owner.
                    Log.d("ChallengePostAdapter", "View detached from window for post: ${binding.post?.id}")
                }
            })
        }



        // The bind method is now purely for setting data, not for lifecycle-dependent observer registration
        fun bind(post: Post) {
            binding.post = post
            binding.viewModel = viewModel

            userRepository.getUser(post.userId,
                onSuccess = { user ->
                    if (user != null) {
                        binding.user = user
                    } else {
                        binding.postUsernameTextView.text = "[User Not Found]"
                    }
                },
                onFailure = { errorMessage ->
                    binding.postUsernameTextView.text = "[Error User]"
                    Log.e("ChallengePostAdapter", "Failed to fetch user for post ${post.id}: $errorMessage")
                }
            )

            post.createdAt?.let {
                binding.postDateTextView.text = dateFormatter.format(it)
            } ?: run {
                binding.postDateTextView.text = "Date Unavailable"
            }

            binding.executePendingBindings()
        }
    }
}