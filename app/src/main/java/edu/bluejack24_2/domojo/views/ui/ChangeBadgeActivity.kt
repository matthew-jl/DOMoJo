package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivityChangeBadgeBinding
import edu.bluejack24_2.domojo.models.Badge
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.viewmodels.ChangeBadgeViewModel

class ChangeBadgeActivity : BaseActivity() {
    private lateinit var binding: ActivityChangeBadgeBinding
    private lateinit var viewModel: ChangeBadgeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_change_badge)
        binding.user = User()
        viewModel = ViewModelProvider(this).get(ChangeBadgeViewModel::class.java)

        // Initialize badges
        val badgeList = listOf(
            Badge(
                id = "bronze",
                name = getString(R.string.badge_bronze_name),
                description = getString(R.string.badge_bronze_desc),
                imageRes = R.drawable.ic_badge_bronze,
                requirement = 10
            ),
            Badge(
                id = "silver",
                name = getString(R.string.badge_silver_name),
                description = getString(R.string.badge_silver_desc),
                imageRes = R.drawable.ic_badge_silver,
                requirement = 30
            ),
            Badge(
                id = "gold",
                name = getString(R.string.badge_gold_name),
                description = getString(R.string.badge_gold_desc),
                imageRes = R.drawable.ic_badge_gold,
                requirement = 50
            ),
            Badge(
                id = "diamond",
                name = getString(R.string.badge_diamond_name),
                description = getString(R.string.badge_diamond_desc),
                imageRes = R.drawable.ic_badge_diamond,
                requirement = 100
            ),
            Badge(
                id = "purple",
                name = getString(R.string.badge_purple_name),
                description = getString(R.string.badge_purple_desc),
                imageRes = R.drawable.ic_badge_purple,
                requirement = 365
            )
        )
        viewModel.initializeBadgesAndCheckStatus(badgeList)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.currentUser.observe(this) { user ->
            user?.let {
                binding.user = it
            }
        }

        viewModel.currentBadge.observe(this) { badge ->
            badge?.let {
                binding.badgeName.text = it.name
                binding.badgeDescription.text = it.description
                binding.badgeDisplay.setImageResource(it.imageRes)
                binding.badgePreview.setImageResource(it.imageRes)
                if (it.isUnlocked) {
                    binding.badgeDisplay.alpha = 1.0f
                    binding.lockIcon.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                    binding.saveButton.alpha = 1.0f
                    binding.saveButton.text = getString(R.string.save_changes)
                } else {
                    binding.badgeDisplay.alpha = 0.3f
                    binding.lockIcon.visibility = View.VISIBLE
                    binding.saveButton.isEnabled = false
                    binding.saveButton.alpha = 0.5f
                    binding.saveButton.text = getString(R.string.badge_locked)
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.updateSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.badge_toast_success), Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.updateError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Failed to update badge: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.btnPrev.setOnClickListener {
            viewModel.selectPreviousBadge()
        }

        binding.btnNext.setOnClickListener {
            viewModel.selectNextBadge()
        }

        binding.saveButton.setOnClickListener {
            viewModel.saveBadgeChanges()
        }
    }
}