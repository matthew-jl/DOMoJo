package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivitySettingsBinding
import edu.bluejack24_2.domojo.viewmodels.SettingsViewModel

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupLanguageDropdown()
        setupClickListeners()
        setupObservers()
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

            // Set initial value from ViewModel
            setText(viewModel.selectedLanguage.value ?: languages[0], false)

            setOnClickListener {
                showDropDown()
            }

            // Update ViewModel when selection changes
            setOnItemClickListener { _, _, position, _ ->
                val selected = languages[position]
                viewModel.updateSelectedLanguage(selected)
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.saveSettingsButton.setOnClickListener {
            viewModel.onSaveSettings()
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
}