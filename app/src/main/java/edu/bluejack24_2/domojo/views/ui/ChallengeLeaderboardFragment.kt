package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import android.util.Log // Import Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment // Import the Fragment base class
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import edu.bluejack24_2.domojo.adapters.LeaderboardAdapter // Import your existing adapter
import edu.bluejack24_2.domojo.databinding.FragmentChallengeLeaderboardBinding // Auto-generated binding
import edu.bluejack24_2.domojo.viewmodels.ChallengeDetailViewModel

class ChallengeLeaderboardFragment : Fragment() {

    // Using a nullable binding variable for View Binding/Data Binding
    // and a non-null accessor for convenience
    private var _binding: FragmentChallengeLeaderboardBinding? = null
    private val binding get() = _binding!!

    // The ViewModel is shared with the parent Activity
    private lateinit var viewModel: ChallengeDetailViewModel
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    // Challenge ID passed as argument
    private var challengeId: String? = null

    // --- Factory Method (Best Practice for passing arguments) ---
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
        // Retrieve challengeId from arguments bundle
        arguments?.let {
            challengeId = it.getString(ARG_CHALLENGE_ID)
        }
    }

    // --- Inflate the Fragment's layout ---
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChallengeLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    // --- Set up views and observe LiveData ---
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the shared ViewModel from the parent Activity
        viewModel = ViewModelProvider(requireActivity()).get(ChallengeDetailViewModel::class.java)
        binding.viewModel = viewModel // Bind ViewModel to Fragment's XML
        binding.lifecycleOwner = viewLifecycleOwner // Set lifecycle owner for LiveData observation

        // Setup RecyclerView for leaderboard
        leaderboardAdapter = LeaderboardAdapter(emptyList()) // Initialize empty
        binding.leaderboardFragmentRecyclerView.apply {
            layoutManager = LinearLayoutManager(context) // Set layout manager
            adapter = leaderboardAdapter // Set adapter
            isNestedScrollingEnabled = false // Important if parent Activity uses NestedScrollView
        }

        // Observe leaderboard data from ViewModel (which gets real-time updates from repo)
        viewModel.leaderboard.observe(viewLifecycleOwner, Observer { members ->
            Log.d("ChallengeLeaderboardFrag", "Observed leaderboard update: ${members.size}")
            leaderboardAdapter.updateMembers(members) // Update adapter with new data
            // Visibility of no_leaderboard_message is handled by binding expression in XML
        })

        // The real-time listener for leaderboard is started in ChallengeDetailViewModel.loadChallengeDetails()
        // and managed across the Activity's lifecycle.
    }

    // --- Clean up binding when view is destroyed ---
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding reference
    }
}