package edu.bluejack24_2.domojo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemJoinedChallengeBinding
import edu.bluejack24_2.domojo.models.JoinedChallengeDisplay

class JoinedChallengeAdapter(private val joinedChallenges: ArrayList<JoinedChallengeDisplay>) :
    RecyclerView.Adapter<JoinedChallengeAdapter.JoinedChallengeViewHolder>() {

    private var onItemClickListener: ((String) -> Unit)? = null

    fun setOnItemClickListener(listener: (String) -> Unit) {
        this.onItemClickListener = listener
    }

    class JoinedChallengeViewHolder(
        val binding: ItemJoinedChallengeBinding,
        private val onItemClickListener: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                binding.joinedChallenge?.challenge?.id?.let { challengeId ->
                    onItemClickListener?.invoke(challengeId)
                }
            }
        }

        fun bind(joinedChallenge: JoinedChallengeDisplay) {
            binding.joinedChallenge = joinedChallenge
            binding.executePendingBindings()
        }
    }

    fun updateJoinedChallenges(newChallenges: List<JoinedChallengeDisplay>) {
        joinedChallenges.clear()
        joinedChallenges.addAll(newChallenges)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JoinedChallengeViewHolder {
        val binding = ItemJoinedChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JoinedChallengeViewHolder(binding, onItemClickListener)
    }

    override fun getItemCount(): Int {
        return joinedChallenges.size
    }

    override fun onBindViewHolder(holder: JoinedChallengeViewHolder, position: Int) {
        val joinedChallenge = joinedChallenges[position]
        holder.bind(joinedChallenge)
    }
}