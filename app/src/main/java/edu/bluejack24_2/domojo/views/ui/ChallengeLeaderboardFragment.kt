package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import edu.bluejack24_2.domojo.adapters.LeaderboardAdapter
import edu.bluejack24_2.domojo.databinding.FragmentChallengeLeaderboardBinding
import edu.bluejack24_2.domojo.viewmodels.ChallengeDetailViewModel

class ChallengeLeaderboardFragment : Fragment() {
    private var _binding: FragmentChallengeLeaderboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeDetailViewModel
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    private var challengeId: String? = null

    companion object {
        private const val ARG_CHALLENGE_ID = "challengeId"

        fun newInstance(challengeId: String): ChallengeLeaderboardFragment {
            val fragment = ChallengeLeaderboardFragment()
            val args = Bundle()
            args.putString(ARG_CHALLENGE_ID, challengeId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            challengeId = it.getString(ARG_CHALLENGE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChallengeLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(ChallengeDetailViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        leaderboardAdapter = LeaderboardAdapter(emptyList()) { userId ->
            // Block to execute when leaderboard item is clicked
            val intent = Intent(requireActivity(), ProfileOthersActivity::class.java).apply {
                putExtra(ProfileOthersActivity.EXTRA_USER_ID, userId)
            }
            startActivity(intent)
        }
        binding.leaderboardFragmentRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = leaderboardAdapter
            isNestedScrollingEnabled = false
        }

        viewModel.leaderboard.observe(viewLifecycleOwner, Observer { members ->
            leaderboardAdapter.updateMembers(members)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}