package edu.bluejack24_2.domojo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemChallengeBinding
import edu.bluejack24_2.domojo.models.Challenge

class ChallengeAdapter (private val challenges: ArrayList<Challenge>) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {
    private var onJoinClickListener: ((String) -> Unit)? = null

    fun setOnJoinClickListener(listener: (String) -> Unit) {
        this.onJoinClickListener = listener
    }

    class ChallengeViewHolder (public val binding: ItemChallengeBinding, private val onJoinClickListener: ((String) -> Unit)?) : RecyclerView.ViewHolder(binding.root){
        init{
            binding.joinButton.setOnClickListener{
                binding.challenge?.id?.let { challengeId ->
                    onJoinClickListener?.invoke(challengeId)
                }
            }
        }

        fun binding (challenge: Challenge) {
            binding.challenge = challenge
            binding.executePendingBindings()
        }
    }

    fun updateChallenges(newChallenges: List<Challenge>) {
        challenges.clear()
        challenges.addAll(newChallenges)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = ItemChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChallengeViewHolder(view, onJoinClickListener)
    }

    override fun getItemCount(): Int {
        return challenges.size
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        holder.binding(challenge)
    }

}