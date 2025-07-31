package edu.bluejack24_2.domojo.views.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivityAboutUsBinding

class AboutUsActivity : BaseActivity() {
    private lateinit var binding : ActivityAboutUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_about_us)
//        TODO: ViewModel
        binding.lifecycleOwner = this

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}