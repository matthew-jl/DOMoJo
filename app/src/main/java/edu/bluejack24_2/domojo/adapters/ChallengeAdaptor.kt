package edu.bluejack24_2.domojo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_2.domojo.databinding.ItemChallengeBinding
import edu.bluejack24_2.domojo.models.Challenge

class ChallengeAdaptor (private val challenges: ArrayList<Challenge>) : RecyclerView.Adapter<ChallengeAdaptor.MyViewModel>() {
    class MyViewModel (public val binding: ItemChallengeBinding) : RecyclerView.ViewHolder(binding.root){
        fun binding (challenge: Challenge) {
            binding.challenge = challenge
            binding.isJoined = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewModel {
        val view = ItemChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewModel(view)
    }

    override fun getItemCount(): Int {
        return challenges.size
    }

    override fun onBindViewHolder(holder: MyViewModel, position: Int) {
        val challenge = challenges[position]
        holder.binding(challenge)
    }

}