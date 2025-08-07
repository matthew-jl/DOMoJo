package edu.bluejack24_2.domojo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemLeaderboardBinding
import edu.bluejack24_2.domojo.models.ChallengeMember
import edu.bluejack24_2.domojo.repositories.UserRepository

class LeaderboardAdapter(
    private var members: List<ChallengeMember>,
    private val userRepository: UserRepository = UserRepository(),
    private val onItemClicked: (userId: String) -> Unit
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    fun updateMembers(newMembers: List<ChallengeMember>) {
        this.members = newMembers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeaderboardViewHolder(binding, userRepository, onItemClicked)
    }

    override fun getItemCount(): Int = members.size

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member, position)
    }

    class LeaderboardViewHolder(
        private val binding: ItemLeaderboardBinding,
        private val userRepository: UserRepository,
        private val onItemClicked: (userId: String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: ChallengeMember, position: Int) {
            binding.member = member
            binding.position = position
            itemView.setOnClickListener {
                onItemClicked(member.userId)
            }

            userRepository.getUser(member.userId,
                onSuccess = { user ->
                    if (user != null) {
                        binding.user = user
                    } else {
                        binding.leaderboardMemberUsername.text = "[Deleted User]"
                        itemView.isClickable = false
                    }
                    binding.executePendingBindings()
                },
                onFailure = { errorMessage ->
                    binding.leaderboardMemberUsername.text = "[Error User]"
                }
            )
        }
    }
}