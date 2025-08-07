package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.adapters.JoinedChallengeAdapter
import edu.bluejack24_2.domojo.databinding.ActivityProfileOthersBinding
import edu.bluejack24_2.domojo.viewmodels.ProfileOthersViewModel

class ProfileOthersActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileOthersBinding
    private lateinit var viewModel: ProfileOthersViewModel
    private lateinit var challengeAdapter: JoinedChallengeAdapter

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_others)

        val userId = intent.getStringExtra(EXTRA_USER_ID)
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this).get(ProfileOthersViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        viewModel.loadUserProfile(userId)
    }

    private fun setupRecyclerView() {
        challengeAdapter = JoinedChallengeAdapter(ArrayList()).apply {
            setOnItemClickListener { challengeId ->
                viewModel.onChallengeClicked(challengeId)
            }
        }
        binding.ongoingChallengesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileOthersActivity)
            adapter = challengeAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        viewModel.user.observe(this) { user ->
            binding.user = user
        }

        viewModel.ongoingChallenges.observe(this) { challenges ->
            challengeAdapter.updateJoinedChallenges(challenges)
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.navigateToChallengeDetail.observe(this, Observer { challengeId ->
            challengeId?.let { id ->
                val intent = Intent(this, ChallengeDetailActivity::class.java).apply {
                    putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, id)
                }
                startActivity(intent)
                viewModel.onNavigationToChallengeDetailHandled()
            }
        })
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}