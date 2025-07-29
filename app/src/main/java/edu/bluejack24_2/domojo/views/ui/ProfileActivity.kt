package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivityProfileBinding
import edu.bluejack24_2.domojo.models.SettingItem
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.viewmodels.ProfileViewModel

private const val TAG = "ProfileFlow"

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        binding.user = User()

        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

//        Setup observers
        viewModel.currentUser.observe(this) { user ->
            Log.d(TAG, "User data received: ${user?.toString().orEmpty()}")
            Log.d(TAG, "Avatar URL: ${user?.avatar.orEmpty()}")
            user?.let {
                binding.user = it
                Log.d(TAG, "Data binding updated with user")
            }
        }

//        Setup Edit Profile button
        binding.editProfileButton.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

//        Setup Setting Item values
        binding.settingChangeBadge.setting = SettingItem(
            icon = ContextCompat.getDrawable(this, R.drawable.ic_setting_badge)!!,
            name = getString(R.string.setting_title_change_badge),
            description = getString(R.string.setting_description_change_badge),
        )
        binding.settingMain.setting = SettingItem(
            icon = ContextCompat.getDrawable(this, R.drawable.ic_settings)!!,
            name = getString(R.string.setting_title_main),
            description = getString(R.string.setting_description_main),
        )
        binding.settingAboutUs.setting = SettingItem(
            icon = ContextCompat.getDrawable(this, R.drawable.ic_setting_about_us)!!,
            name = getString(R.string.setting_title_about_us),
            description = getString(R.string.setting_description_about_us),
        )
        binding.settingLogout.setting = SettingItem(
            icon = ContextCompat.getDrawable(this, R.drawable.ic_setting_logout)!!,
            name = getString(R.string.setting_title_logout),
            description = getString(R.string.setting_description_logout),
        )
        binding.settingDeleteAccount.setting = SettingItem(
            icon = ContextCompat.getDrawable(this, R.drawable.ic_setting_delete_account)!!,
            name = getString(R.string.setting_title_delete_account),
            description = getString(R.string.setting_description_delete_account),
        )

//        Set Setting Item navigation
        binding.settingChangeBadge.materialCardConstraint.setOnClickListener {
            startActivity(Intent(this, ChangeBadgeActivity::class.java))
        }
        binding.settingMain.materialCardConstraint.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.settingAboutUs.materialCardConstraint.setOnClickListener {
            startActivity(Intent(this, AboutUsActivity::class.java))
        }
        binding.settingLogout.materialCardConstraint.setOnClickListener {
            Log.d(TAG, "Click")
            showLogoutConfirmationDialog()
        }
        binding.settingDeleteAccount.materialCardConstraint.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

//         Observe logout results
        viewModel.logoutSuccess.observe(this) { success ->
            if (success) {
                navigateToLanding()
            }
        }
        viewModel.logoutError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe delete account results
        viewModel.deleteSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this,
                    getString(R.string.account_deleted_successfully),
                    Toast.LENGTH_SHORT).show()
                navigateToLanding()
            }
        }
        viewModel.deleteError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout_confirmation_title))
            .setMessage(getString(R.string.logout_confirmation_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_account_confirmation_title))
            .setMessage(getString(R.string.delete_account_confirmation_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.deleteAccount()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun navigateToLanding() {
        val intent = Intent(this, LandingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}