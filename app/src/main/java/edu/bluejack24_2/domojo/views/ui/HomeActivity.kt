// HomeActivity.kt
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
import edu.bluejack24_2.domojo.adapters.JoinedChallengeAdapter
import edu.bluejack24_2.domojo.databinding.ActivityHomeBinding
import edu.bluejack24_2.domojo.viewmodels.HomeViewModel

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModel: HomeViewModel
    private lateinit var joinedChallengeAdapter: JoinedChallengeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        joinedChallengeAdapter = JoinedChallengeAdapter(ArrayList()).apply {
            setOnItemClickListener { challengeId ->
                viewModel.onChallengeItemClicked(challengeId)
            }
        }
        binding.joinedChallengesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = joinedChallengeAdapter
        }

        binding.filterCategoryButton.setOnClickListener {
            showCategoryFilterDialog()
        }
        binding.sortStreakButton.setOnClickListener {
            showSortByStreakDialog()
        }

        viewModel.joinedChallenges.observe(this, Observer { challenges ->
            joinedChallengeAdapter.updateJoinedChallenges(challenges)
            binding.noJoinedChallengesTv.visibility =
                if (challenges.isEmpty() && viewModel.isLoading.value == false && viewModel.errorMessage.value == null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.filterCategoryButton.isEnabled = !isLoading
            binding.sortStreakButton.isEnabled = !isLoading
        })

        viewModel.errorMessage.observe(this, Observer { message ->
            message?.let {
                binding.errorMessageTv.visibility = View.VISIBLE
                binding.errorMessageTv.text = it
            } ?: run {
                binding.errorMessageTv.visibility = View.GONE
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
        viewModel.fetchJoinedChallenges()
    }

    private fun showCategoryFilterDialog() {
        val categories = viewModel.availableCategories.value ?: emptyList()
        val allCategoriesOption = "All Categories"

        val dialogItems = mutableListOf(allCategoriesOption)
        dialogItems.addAll(categories)

        val selectedIndex = dialogItems.indexOf(viewModel.selectedCategoryFilter.value ?: allCategoriesOption)

        AlertDialog.Builder(this)
            .setTitle("Filter by Category")
            .setSingleChoiceItems(dialogItems.toTypedArray(), selectedIndex) { dialog, which ->
                val selected = dialogItems[which]
                viewModel.setCategoryFilter(if (selected == allCategoriesOption) null else selected)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSortByStreakDialog() {
        val options = arrayOf("No Sort", "Streak (Ascending)", "Streak (Descending)")
        val sortValues = arrayOf(null, "asc", "desc")
        val currentSort = viewModel.sortByStreakDirection.value

        val selectedIndex = sortValues.indexOf(currentSort)

        AlertDialog.Builder(this)
            .setTitle("Sort by Streak")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                viewModel.setSortByStreakDirection(sortValues[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}