package edu.bluejack24_2.domojo.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemPostBinding
import edu.bluejack24_2.domojo.models.Post
import edu.bluejack24_2.domojo.repositories.UserRepository
import edu.bluejack24_2.domojo.viewmodels.ChallengeDetailViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class ChallengePostAdapter(
    private var posts: List<Post>,
    private val viewModel: ChallengeDetailViewModel,
    private val onCommentUserClicked: (userId: String) -> Unit
) :
    RecyclerView.Adapter<ChallengePostAdapter.ChallengePostViewHolder>() {

    fun updatePosts(newPosts: List<Post>) {
        this.posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengePostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChallengePostViewHolder(binding, viewModel, onCommentUserClicked)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ChallengePostViewHolder, position: Int) {
        holder.binding.lifecycleOwner = holder.itemView.findViewTreeLifecycleOwner()
        holder.bind(posts[position])
    }

    class ChallengePostViewHolder(
        val binding: ItemPostBinding,
        private val viewModel: ChallengeDetailViewModel,
        private val onCommentUserClicked: (userId: String) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        private val userRepository: UserRepository = UserRepository()
        private val commentAdapter: PostCommentAdapter

        init {
            commentAdapter = PostCommentAdapter(comments = emptyList(), userRepository) { userId ->
                onCommentUserClicked(userId)
            }

            binding.commentsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = commentAdapter
                isNestedScrollingEnabled = false
            }

            binding.likeButton.setOnClickListener {
                binding.post?.id?.let { postId ->
                    viewModel.onLikeClicked(postId, "like")
                }
            }

            binding.dislikeButton.setOnClickListener {
                binding.post?.id?.let { postId ->
                    viewModel.onLikeClicked(postId, "dislike")
                }
            }

            binding.commentButton.setOnClickListener {
                binding.post?.id?.let { postId ->
                    viewModel.onCommentClicked(postId)
                }
            }

            itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    val lifecycleOwner = v.findViewTreeLifecycleOwner()
                    binding.lifecycleOwner = lifecycleOwner

                    val postId = binding.post?.id
                    if (postId != null) {
                        viewModel.fetchRecentCommentsForPost(postId)
                    }

                    if (lifecycleOwner != null) {
                        viewModel.recentCommentsMap.removeObservers(lifecycleOwner)

                        viewModel.recentCommentsMap.observe(lifecycleOwner) { commentsMap ->
                            val comments = commentsMap[binding.post?.id] ?: emptyList()
                            commentAdapter.updateComments(comments)
                        }
                    }

                    lifecycleOwner?.let { owner ->
                        viewModel.recentCommentsMap.removeObservers(owner)
                        viewModel.recentCommentsMap.observe(owner) { commentsMap ->

                            if (commentsMap == null) {
                                commentAdapter.updateComments(emptyList())
                                return@observe
                            }

                            val postId = binding.post?.id ?: return@observe
                            val recentComments = commentsMap[postId] ?: emptyList()

                            commentAdapter.updateComments(recentComments)
                            Log.d(
                                "Challenge Post Adapter",
                                "Updated comments for post $postId: ${recentComments.size}"
                            )
                        }

                        viewModel.postUserActions.removeObservers(owner)
                        viewModel.postUserActions.observe(owner) { actionsMap ->
                            val postId = binding.post?.id ?: return@observe
                            val userAction = actionsMap[postId] ?: "none"
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
                    Log.d(
                        "ChallengePostAdapter",
                        "View detached from window for post: ${binding.post?.id}"
                    )
                }
            })
        }

        fun bind(post: Post) {
            binding.post = post
            binding.viewModel = viewModel

            userRepository.getUser(
                post.userId,
                onSuccess = { user ->
                    if (user != null) {
                        binding.user = user
                    } else {
                        binding.postUsernameTextView.text = "[User Not Found]"
                    }
                },
                onFailure = { errorMessage ->
                    binding.postUsernameTextView.text = "[Error User]"
                    Log.e(
                        "ChallengePostAdapter",
                        "Failed to fetch user for post ${post.id}: $errorMessage"
                    )
                }
            )

            post.createdAt?.let {
                binding.postDateTextView.text = dateFormatter.format(it)
            } ?: run {
                binding.postDateTextView.text = "Date Unavailable"
            }

            binding.root.setOnClickListener { }
            binding.postUserAvatarImageView.setOnClickListener { onCommentUserClicked(post.userId) }
            binding.postUsernameTextView.setOnClickListener { onCommentUserClicked(post.userId) }

            binding.executePendingBindings()
        }
    }
}