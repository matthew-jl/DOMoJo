package edu.bluejack24_2.domojo.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import edu.bluejack24_2.domojo.views.ui.ChallengeLeaderboardFragment
import edu.bluejack24_2.domojo.views.ui.ChallengePostsFragment

class ChallengeDetailPagerAdapter (activity: AppCompatActivity,
                                   private val challengeId: String): FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ChallengePostsFragment.newInstance(challengeId)
            1 -> ChallengeLeaderboardFragment.newInstance(challengeId)
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}