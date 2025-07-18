package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivityProfileBinding
import edu.bluejack24_2.domojo.models.SettingItem
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.viewmodels.ProfileViewModel

private const val TAG = "ProfileFlow"

class ProfileActivity : AppCompatActivity() {
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
//        binding.settingChangeBadge.root.setOnClickListener {
//            startActivity(Intent(this, ChangeBadgeActivity::class.java))
//        }

        binding.settingChangeBadge.settingIcon
    }
}