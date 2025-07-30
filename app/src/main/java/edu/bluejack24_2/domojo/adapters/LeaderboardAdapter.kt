package edu.bluejack24_2.domojo.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemLeaderboardBinding
import edu.bluejack24_2.domojo.models.ChallengeMember
import edu.bluejack24_2.domojo.repositories.UserRepository

class LeaderboardAdapter(
    private var members: List<ChallengeMember>,
    private val userRepository: UserRepository = UserRepository()
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    fun updateMembers(newMembers: List<ChallengeMember>) {
        this.members = newMembers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeaderboardViewHolder(binding, userRepository)
    }

    override fun getItemCount(): Int = members.size

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member, position)
    }

    class LeaderboardViewHolder(
        private val binding: ItemLeaderboardBinding,
        private val userRepository: UserRepository
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: ChallengeMember, position: Int) {
            binding.member = member
            binding.position = position
            binding.executePendingBindings()

            userRepository.getUser(member.userId,
                onSuccess = { user ->
                    if (user != null) {
                        binding.user = user
                    } else {
                        binding.leaderboardMemberUsername.text = "[Deleted User]"
//                        Glide.with(binding.root.context).load(R.drawable.default_avatar).into(binding.leaderboardMemberAvatar)
                    }
                },
                onFailure = { errorMessage ->
                    binding.leaderboardMemberUsername.text = "[Error User]"
                    Log.e("LeaderboardViewHolder", "Failed to fetch user for ${member.userId}: $errorMessage")
                }
            )
        }
    }
}