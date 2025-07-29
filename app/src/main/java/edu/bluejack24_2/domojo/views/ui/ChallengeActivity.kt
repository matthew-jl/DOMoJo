package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.adapters.ChallengeAdapter
import edu.bluejack24_2.domojo.databinding.ActivityChallengeBinding
import edu.bluejack24_2.domojo.viewmodels.ChallengeViewModel

class ChallengeActivity : BaseActivity() {
    private lateinit var binding: ActivityChallengeBinding
    private lateinit var viewModel: ChallengeViewModel
    private lateinit var challengeAdapter: ChallengeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_challenge)
        viewModel = ViewModelProvider(this).get(ChallengeViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        challengeAdapter = ChallengeAdapter(ArrayList()).apply {
            setOnJoinClickListener { challengeId ->
                viewModel.onJoinChallengeClicked(challengeId)
            }
            setOnItemClickListener { challengeId ->
                viewModel.onChallengeItemClicked(challengeId)
            }
        }
        binding.challengesRecyclerView.adapter = challengeAdapter

        binding.challengesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChallengeActivity)
            adapter = challengeAdapter
        }

        viewModel.fetchChallenges()
        viewModel.fetchAvailableCategories()

        binding.addChallengeFab.setOnClickListener{
            val intent = Intent(this, CreateChallengeActivity::class.java)
            startActivity(intent)
        }

        binding.filterButton.setOnClickListener {
            showCategoryFilterDialog()
        }

        viewModel.challengeList.observe(this, Observer { challenges ->
            challengeAdapter.updateChallenges(challenges)
            if (challenges.isEmpty() && viewModel.isLoading.value == false && viewModel.errorMessage.value == null) {
                binding.errorTv.text = "No challenges found matching your criteria."
                binding.errorTv.visibility = View.VISIBLE
            } else {
                binding.errorTv.visibility = View.INVISIBLE
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.searchEditText.isEnabled = !isLoading
            binding.filterButton.isEnabled = !isLoading
        })

        viewModel.errorMessage.observe(this, Observer { error ->
            if (error != null) {
                binding.errorTv.text = error
                binding.errorTv.visibility = View.VISIBLE
            } else {
                binding.errorTv.visibility = View.INVISIBLE
            }
        })

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

    override fun onResume() {
        super.onResume()
        viewModel.fetchChallenges()
        viewModel.fetchAvailableCategories()
    }

    private fun showCategoryFilterDialog() {
        val categories = viewModel.availableCategories.value ?: emptyList()
        val allCategoriesOption = "All Categories"

        val dialogItems = mutableListOf(allCategoriesOption)
        dialogItems.addAll(categories)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Filter by Category")
            .setItems(dialogItems.toTypedArray()) { dialog, which ->
                val selected = dialogItems[which]
                if (selected == allCategoriesOption) {
                    viewModel.setCategoryFilter(null)
                } else {
                    viewModel.setCategoryFilter(selected)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}