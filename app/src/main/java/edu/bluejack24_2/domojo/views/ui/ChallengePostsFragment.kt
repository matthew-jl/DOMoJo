package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import edu.bluejack24_2.domojo.adapters.ChallengePostAdapter
import edu.bluejack24_2.domojo.adapters.PostCommentAdapter
import edu.bluejack24_2.domojo.databinding.FragmentChallengePostsBinding
import edu.bluejack24_2.domojo.databinding.ItemPostBinding
import edu.bluejack24_2.domojo.models.Post
import edu.bluejack24_2.domojo.repositories.UserRepository
import edu.bluejack24_2.domojo.viewmodels.ChallengeDetailViewModel

class ChallengePostsFragment : Fragment() {
    private var _binding: FragmentChallengePostsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeDetailViewModel
    private lateinit var postAdapter: ChallengePostAdapter

    private var challengeId: String? = null

    companion object {
        private const val ARG_CHALLENGE_ID = "challengeId"

        fun newInstance(challengeId: String): ChallengePostsFragment {
            val fragment = ChallengePostsFragment()
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
        _binding = FragmentChallengePostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(ChallengeDetailViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        postAdapter = ChallengePostAdapter(emptyList(), viewModel)
        binding.challengePostsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
            isNestedScrollingEnabled = false
        }

        viewModel.posts.observe(viewLifecycleOwner, Observer { posts ->
            postAdapter.updatePosts(posts)
        })

        viewModel.hasPostedToday.observe(viewLifecycleOwner, Observer { hasPosted ->
        })

        challengeId?.let { id ->
        } ?: Log.e("ChallengePostsFragment", "Challenge ID is null for posts fragment!")

        viewModel.usersTodayPost.observe(viewLifecycleOwner) { post ->
            if (post != null) {
                val userPostBinding = binding.usersTodayPostItem
                userPostBinding.post = post
                userPostBinding.viewModel = viewModel
                setupPostUI(userPostBinding, post, viewModel, viewLifecycleOwner)
            }
        }
    }

    fun setupPostUI(
        binding: ItemPostBinding,
        post: Post,
        viewModel: ChallengeDetailViewModel,
        lifecycleOwner: LifecycleOwner
    ) {
        val commentAdapter = PostCommentAdapter(emptyList(), UserRepository())
        binding.commentsRecyclerView.adapter = commentAdapter
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)

        binding.likeButton.setOnClickListener {
            viewModel.onLikeClicked(post.id, "like")
        }

        binding.dislikeButton.setOnClickListener {
            viewModel.onLikeClicked(post.id, "dislike")
        }

        binding.commentButton.setOnClickListener {
            viewModel.onCommentClicked(post.id)
        }

        viewModel.recentCommentsMap.observe(lifecycleOwner) { commentsMap ->
            val recentComments = commentsMap?.get(post.id) ?: emptyList()

            commentAdapter.updateComments(recentComments)
        }

        viewModel.postUserActions.observe(lifecycleOwner) { actionsMap ->
            when (actionsMap[post.id]) {
                "like" -> {
                    binding.likeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_like_filled)
                    binding.dislikeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_dislike_outline)
                }
                "dislike" -> {
                    binding.likeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_like_outline)
                    binding.dislikeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_dislike_filled)
                }
                else -> {
                    binding.likeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_like_outline)
                    binding.dislikeButton.setImageResource(edu.bluejack24_2.domojo.R.drawable.ic_dislike_outline)
                }
            }
        }

        viewModel.fetchRecentCommentsForPost(post.id)
    }

    override fun onResume() {
        super.onResume()
        challengeId?.let { id ->
            viewModel.fetchChallengePosts(id)
            viewModel.checkTodayPostStatus(id)
        } ?: Log.e("ChallengePostsFragment", "onResume: Challenge ID is null!")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}