package edu.bluejack24_2.domojo.views.ui

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivitySettingsBinding
import edu.bluejack24_2.domojo.utils.LocaleHelper
import edu.bluejack24_2.domojo.utils.NotificationScheduler
import edu.bluejack24_2.domojo.utils.ThemeHelper
import edu.bluejack24_2.domojo.viewmodels.SettingsViewModel

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupLanguageDropdown()
        setupDarkModeSwitch()
        setupNotificationSwitch()
        setupTimePicker()
        setupClickListeners()
        setupObservers()
    }

    private fun setupTimePicker() {
        // Load saved time
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val hour = prefs.getInt("notification_hour", 8)
        val minute = prefs.getInt("notification_minute", 0)
        viewModel.setNotificationTime(hour, minute)

        binding.btnSetTime.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showTimePickerDialog() {
        val hour = viewModel.getNotificationHour()
        val minute = viewModel.getNotificationMinute()

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                viewModel.setNotificationTime(selectedHour, selectedMinute)
                NotificationScheduler.scheduleDailyNotification(this, selectedHour, selectedMinute)
            },
            hour,
            minute,
            true // 24-hour format
        ).show()
    }

    private fun setupNotificationSwitch() {
        // Initialize switch state from SharedPreferences
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        binding.notificationsSwitch.isChecked = prefs.getBoolean("notifications_enabled", true)

        // Handle toggle changes
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestNotificationPermission()
                // Schedule notification with saved time
                val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val hour = prefs.getInt("notification_hour", 8)
                val minute = prefs.getInt("notification_minute", 0)
                NotificationScheduler.scheduleDailyNotification(this, hour, minute)
            } else {
                saveNotificationPreference(false)
                NotificationScheduler.cancelDailyNotification(this)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestNotificationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                saveNotificationPreference(true)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) -> {
                // Explain why permission is needed
                showPermissionExplanationDialog()
            }

            else -> {
                // Request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_message)
            .setPositiveButton(R.string.continue_text) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                binding.notificationsSwitch.isChecked = false
            }
            .show()
    }

    private fun saveNotificationPreference(enabled: Boolean) {
        getSharedPreferences("AppSettings", Context.MODE_PRIVATE).edit()
            .putBoolean("notifications_enabled", enabled)
            .apply()
        viewModel.notificationsEnabled.value = enabled
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    saveNotificationPreference(true)
                } else {
                    // Permission denied
                    binding.notificationsSwitch.isChecked = false
                    saveNotificationPreference(false)
                }
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    private fun setupDarkModeSwitch() {
        // Initial state
        val currentTheme = ThemeHelper.getSavedTheme(this)
        binding.darkModeSwitch.isChecked = when (currentTheme) {
            ThemeHelper.THEME_DARK -> true
            ThemeHelper.THEME_LIGHT -> false
            else -> isSystemInDarkMode()
        }
        Log.d("SettingsActivity", "setupDarkModeSwitch currentTheme: $currentTheme")

        // Handle toggle changes
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateDarkModeEnabled(this, isChecked)
        }
    }

    private fun isSystemInDarkMode(): Boolean {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    private fun setupLanguageDropdown() {
        val languages = resources.getStringArray(R.array.language_options)
        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_item,
            languages
        )
        binding.languageDropdown.apply {
            setAdapter(adapter)

            // Set initial selection
            val prefs = PreferenceManager.getDefaultSharedPreferences(this@SettingsActivity)
            val savedLang = prefs.getString(LocaleHelper.SELECTED_LANGUAGE, "auto") ?: "auto"

            val displayLang = when (savedLang) {
                "auto" -> languages[0] // "Automatic" or "Otomatis"
                "in" -> languages[1] // "Bahasa Indonesia"
                else -> languages[2] // "English"
            }

            viewModel.updateSelectedLanguage(displayLang)
            setText(displayLang, false)

            setOnClickListener {
                showDropDown()
            }

            // Update ViewModel when selection changes
            setOnItemClickListener { _, _, position, _ ->
                val selected = languages[position]
                viewModel.updateSelectedLanguage(selected)

                val languageCode = when (selected) {
                    getString(R.string.language_automatic) -> "auto"
                    getString(R.string.language_indonesian) -> "in"
                    else -> "en"
                }

                LocaleHelper.setLocale(this@SettingsActivity, languageCode)
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.saveSettingsButton.setOnClickListener {
            viewModel.saveNotificationTime(this)
            showRestartDialog()
        }
    }

    private fun setupObservers() {
        viewModel.navigateBack.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                onBackPressed()
                viewModel.onNavigationComplete()
            }
        }
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.restart_required)
            .setMessage(R.string.restart_app_to_apply_changes)
            .setPositiveButton(R.string.restart_now) { _, _ ->
                // Fully restart the app
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                Runtime.getRuntime().exit(0) // Force kill process
            }
            .setNegativeButton(R.string.later) { dialog, _ ->
                dialog.dismiss()
                onBackPressed()
            }
            .setCancelable(false)
            .show()
    }
}