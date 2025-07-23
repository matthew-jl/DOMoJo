package edu.bluejack24_2.domojo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore // For UserRepository init
import edu.bluejack24_2.domojo.databinding.ItemLeaderboardBinding
import edu.bluejack24_2.domojo.models.ChallengeMember
import edu.bluejack24_2.domojo.models.User // Import User model
import edu.bluejack24_2.domojo.repositories.UserRepository // To fetch user details

class LeaderboardAdapter(
    private var members: List<ChallengeMember>,
    // Initialize UserRepository here, or inject it if you're using a DI framework
    private val userRepository: UserRepository = UserRepository()
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    fun updateMembers(newMembers: List<ChallengeMember>) {
        this.members = newMembers
        notifyDataSetChanged() // Simplest way to update, consider DiffUtil for larger lists
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeaderboardViewHolder(binding, userRepository) // Pass userRepository to ViewHolder
    }

    override fun getItemCount(): Int = members.size

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member, position)
    }

    class LeaderboardViewHolder(
        private val binding: ItemLeaderboardBinding,
        private val userRepository: UserRepository // Receive UserRepository
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: ChallengeMember, position: Int) {
            binding.member = member
            binding.position = position // For rank display (0-indexed, so add 1 for actual rank)
            binding.executePendingBindings() // Crucial for immediate binding updates

            // Fetch username and avatar using UserRepository
            // This is done for each member because ChallengeMember only contains userId, not full user details
            userRepository.getUser(member.userId,
                onSuccess = { user ->
                    if (user != null) {
                        binding.user = user // Pass the User object to the XML's 'user' variable
                        // The XML will then automatically display user.username and handle user.avatar (via BindingAdapter)
                    } else {
                        binding.leaderboardMemberUsername.text = "[Deleted User]"
                        // Optionally set a default avatar if user is null
                        // Glide.with(binding.root.context).load(R.drawable.default_avatar).into(binding.leaderboardMemberAvatar)
                    }
                },
                onFailure = { errorMessage ->
                    binding.leaderboardMemberUsername.text = "[Error User]"
                    // Log the error: Log.e("LeaderboardViewHolder", "Failed to fetch user for ${member.userId}: $errorMessage")
                    // Optionally set a default avatar on error
                }
            )
        }
    }
}