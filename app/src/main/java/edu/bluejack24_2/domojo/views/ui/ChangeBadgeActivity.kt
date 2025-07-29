package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivityChangeBadgeBinding
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
                binding.executePendingBindings()
            }
        }

        viewModel.updateSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Badge updated successfully", Toast.LENGTH_SHORT).show()
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