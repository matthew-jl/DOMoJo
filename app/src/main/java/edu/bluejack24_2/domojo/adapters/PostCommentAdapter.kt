package edu.bluejack24_2.domojo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemCommentBinding
import edu.bluejack24_2.domojo.models.PostComment
import edu.bluejack24_2.domojo.repositories.UserRepository
import java.text.SimpleDateFormat
import java.util.Locale

class PostCommentAdapter(
    private var comments: List<PostComment>,
    private val userRepository: UserRepository = UserRepository(),
    private val onItemClicked: (userId: String) -> Unit
) : RecyclerView.Adapter<PostCommentAdapter.PostCommentViewHolder>() {

    private val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    fun updateComments(newComments: List<PostComment>) {
        this.comments = newComments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostCommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostCommentViewHolder(binding, userRepository, dateFormatter, onItemClicked)
    }

    override fun getItemCount(): Int = comments.size

    override fun onBindViewHolder(holder: PostCommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    class PostCommentViewHolder(
        private val binding: ItemCommentBinding,
        private val userRepository: UserRepository,
        private val dateFormatter: SimpleDateFormat,
        private val onItemClicked: (userId: String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: PostComment) {
            binding.comment = comment

            itemView.setOnClickListener {
                onItemClicked(comment.userId)
            }

            userRepository.getUser(comment.userId,
                onSuccess = { user ->
                    if (user != null) {
                        binding.user = user
                        binding.executePendingBindings()
                    } else {
                        binding.commentUsername.text = "[Unknown User]"
                    }

                    binding.executePendingBindings()
                },
                onFailure = { errorMessage ->
                    binding.commentUsername.text = "[Error User]"
                }
            )

            comment.createdAt?.let {
                binding.commentDate.text = dateFormatter.format(it)
            } ?: run {
                binding.commentDate.text = "Date Unknown"
            }
        }
    }
}