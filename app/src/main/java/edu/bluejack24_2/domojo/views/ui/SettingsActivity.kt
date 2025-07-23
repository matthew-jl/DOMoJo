package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivitySettingsBinding
import edu.bluejack24_2.domojo.utils.LocaleHelper
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
        setupClickListeners()
        setupObservers()

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