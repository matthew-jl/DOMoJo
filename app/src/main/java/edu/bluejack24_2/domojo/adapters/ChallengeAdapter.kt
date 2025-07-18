package edu.bluejack24_2.domojo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemChallengeBinding
import edu.bluejack24_2.domojo.models.Challenge

class ChallengeAdapter (private val challenges: ArrayList<Challenge>) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {
    class ChallengeViewHolder (public val binding: ItemChallengeBinding) : RecyclerView.ViewHolder(binding.root){
        fun binding (challenge: Challenge) {
            binding.challenge = challenge
            binding.isJoined = true
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
        return ChallengeViewHolder(view)
    }

    override fun getItemCount(): Int {
        return challenges.size
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        holder.binding(challenge)
    }

}