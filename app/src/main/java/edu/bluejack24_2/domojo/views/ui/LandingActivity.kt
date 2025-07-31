package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.adapters.CarouselPagerAdapter
import edu.bluejack24_2.domojo.databinding.ActivityLandingBinding
import edu.bluejack24_2.domojo.viewmodels.LandingViewModel

class LandingActivity : BaseActivity() {
    private lateinit var binding: ActivityLandingBinding
    private lateinit var viewModel: LandingViewModel
    private lateinit var carouselPagerAdapter: CarouselPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_landing)

        viewModel = ViewModelProvider(this).get(LandingViewModel::class.java)
        binding.lifecycleOwner = this

        carouselPagerAdapter = CarouselPagerAdapter()
        binding.viewPagerCarousel.adapter = carouselPagerAdapter

        TabLayoutMediator(binding.tabLayoutDots, binding.viewPagerCarousel) { tab, position ->
        }.attach()

        viewModel.carouselItems.observe(this, Observer { items ->
            carouselPagerAdapter.updateItems(items)
        })

        binding.registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.profileLink.setOnClickListener{
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    override fun onStart(){
        super.onStart()
//        if (viewModel.isUserLoggedIn()) {
//            val intent = Intent(this, ChallengeActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
    }
}