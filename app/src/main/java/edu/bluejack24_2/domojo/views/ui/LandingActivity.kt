package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.adapters.CarouselPagerAdapter
import edu.bluejack24_2.domojo.databinding.ActivityLandingBinding
import edu.bluejack24_2.domojo.models.CarouselItem
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

        binding.tabLayoutDots.setSelectedTabIndicatorHeight(0)

        TabLayoutMediator(binding.tabLayoutDots, binding.viewPagerCarousel) { tab, position ->
        }.attach()

        loadCarouselItems()

        binding.registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }


    override fun onStart(){
        super.onStart()
        if (viewModel.isUserLoggedIn()) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadCarouselItems() {
        val items = listOf(
            CarouselItem(
                "https://res.cloudinary.com/dbllc6nd9/image/upload/v1753852984/carousel_1_thg7z4.png",
                getString(R.string.carousel_heading_1),
                getString(R.string.carousel_description_1)
            ),
            CarouselItem(
                "https://res.cloudinary.com/dbllc6nd9/image/upload/v1753865713/carousel_2_q0mbag.png",
                getString(R.string.carousel_heading_2),
                getString(R.string.carousel_description_2)
            ),
            CarouselItem(
                "https://res.cloudinary.com/dbllc6nd9/image/upload/v1753865713/carousel_3_kobnm7.png",
                getString(R.string.carousel_heading_3),
                getString(R.string.carousel_description_3)
            )
        )
        carouselPagerAdapter.updateItems(items)
    }
}