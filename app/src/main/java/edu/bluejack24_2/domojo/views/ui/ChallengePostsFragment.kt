package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment // Import the Fragment base class
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import edu.bluejack24_2.domojo.adapters.ChallengePostAdapter // Your adapter for posts
import edu.bluejack24_2.domojo.databinding.FragmentChallengePostsBinding // Your XML binding class
import edu.bluejack24_2.domojo.viewmodels.ChallengeDetailViewModel // Shared ViewModel

class ChallengePostsFragment : Fragment() {

    // Using a nullable binding variable for View Binding/Data Binding
    // and a non-null accessor for convenience
    private var _binding: FragmentChallengePostsBinding? = null
    private val binding get() = _binding!!

    // The ViewModel is shared with the parent Activity
    private lateinit var viewModel: ChallengeDetailViewModel
    private lateinit var postAdapter: ChallengePostAdapter

    // Challenge ID passed as argument to this Fragment
    private var challengeId: String? = null

    // --- Factory Method (Best Practice for passing arguments) ---
    companion object {
        private const val ARG_CHALLENGE_ID = "challengeId" // Key for argument bundle

        fun newInstance(challengeId: String): ChallengePostsFragment {
            val fragment = ChallengePostsFragment()
            val args = Bundle()
            args.putString(ARG_CHALLENGE_ID, challengeId) // Put challengeId into arguments
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
        // Use DataBindingUtil.inflate if you had a <data> tag in your fragment_challenge_posts.xml
        // If not using <data> tag, use FragmentChallengePostsBinding.inflate directly
        _binding = FragmentChallengePostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // --- Set up views and observe LiveData ---
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the shared ViewModel from the parent Activity
        viewModel = ViewModelProvider(requireActivity()).get(ChallengeDetailViewModel::class.java)
        binding.viewModel = viewModel // Bind ViewModel to Fragment's XML
        binding.lifecycleOwner = viewLifecycleOwner // Set lifecycle owner for LiveData observation

        // Setup RecyclerView for posts
        postAdapter = ChallengePostAdapter(emptyList()) // Initialize with empty list
        binding.challengePostsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context) // Set layout manager
            adapter = postAdapter // Set adapter
            isNestedScrollingEnabled = false // Important if parent Activity uses NestedScrollView
        }

        // Observe posts from ViewModel
        viewModel.posts.observe(viewLifecycleOwner, Observer { posts ->
            Log.d("ChallengePostsFragment", "Observed posts update: ${posts.size}")
            postAdapter.updatePosts(posts) // Update adapter with new posts
            // Visibility of no_posts_message is handled by binding expression in XML
        })

        // Observe hasPostedToday to control visibility of "Make Post" section
        viewModel.hasPostedToday.observe(viewLifecycleOwner, Observer { hasPosted ->
            // Visibility is handled by binding expression in XML (e.g., binding.noPostTodayMessage.visibility)
            Log.d("ChallengePostsFragment", "User has posted today: $hasPosted")
        })

        // Set click listener for "Buat Post" button
//        binding.p.setOnClickListener {
//            viewModel.onPostActivityClicked() // Trigger the post dialog in ViewModel
//        }

        // Ensure challengeId is valid and fetch posts if needed by fragment directly
        // (ViewModel's loadChallengeDetails already fetches posts, so this might be redundant
        // if the ViewModel handles the initial fetch when it's created for the Activity)
        challengeId?.let { id ->
            // You might call viewModel.fetchChallengePosts(id) here if posts are specific to this fragment's lifecycle
            // but for sharing a ViewModel with Activity, Activity usually triggers main fetches.
        } ?: Log.e("ChallengePostsFragment", "Challenge ID is null for posts fragment!")
    }

    // --- CRUCIAL FIX: Call fetch methods in onResume ---
    override fun onResume() {
        super.onResume()
        Log.d("ChallengePostsFragment", "onResume: Fetching posts and checking post status.")
        challengeId?.let { id ->
            viewModel.fetchChallengePosts(id) // Re-fetch other posts
            viewModel.checkTodayPostStatus(id) // Re-check user's today post status
        } ?: Log.e("ChallengePostsFragment", "onResume: Challenge ID is null!")
    }

    // --- Clean up binding when view is destroyed ---
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}