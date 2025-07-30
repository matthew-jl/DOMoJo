package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import edu.bluejack24_2.domojo.models.CarouselItem

class LandingViewModel : ViewModel() {
    private val _carouselItems = MutableLiveData<List<CarouselItem>>()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val carouselItems: LiveData<List<CarouselItem>> get() = _carouselItems

    init {
        loadCarouselItems()
    }

    private fun loadCarouselItems() {
        val items = listOf(
            CarouselItem(
                imageUrl = "https://res.cloudinary.com/dbllc6nd9/image/upload/v1753852984/carousel_1_thg7z4.png",
                heading = "Discover New Challenges",
                description = "Join exciting challenges across various categories and grow your streak every day!"
            ),
            CarouselItem(
                imageUrl = "https://res.cloudinary.com/dbllc6nd9/image/upload/v1753865713/carousel_2_q0mbag.png",
                heading = "Connect with Communities",
                description = "Share your progress with friends and be a part of a community that conquers goals together."
            ),
            CarouselItem(
                imageUrl = "https://res.cloudinary.com/dbllc6nd9/image/upload/v1753865713/carousel_3_kobnm7.png",
                heading = "Track Your Progress",
                description = "Monitor your streaks, earn cosmetic badges, and celebrate your success."
            )
        )
        _carouselItems.value = items
    }

    fun isUserLoggedIn(): Boolean {
        if(firebaseAuth.currentUser != null) {
            return true
        }
        return false
    }
}