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
                imageUrl = "https://res.cloudinary.com/dr09lvoly/image/upload/v1743751700/hhfaqrmyu7akmn92dwzk.jpg", // Replace with your actual image URLs
                heading = "Discover New Challenges",
                description = "Join exciting challenges across various categories and push your limits!"
            ),
            CarouselItem(
                imageUrl = "https://res.cloudinary.com/dr09lvoly/image/upload/v1752805832/vm4xyyii67vn0prypesa.jpg", // Replace
                heading = "Connect & Collaborate",
                description = "Team up with friends or find new partners to conquer goals together."
            ),
            CarouselItem(
                imageUrl = "https://res.cloudinary.com/dr09lvoly/image/upload/v1752851976/aj1lrums7xiajy1td8df.jpg", // Replace
                heading = "Track Your Progress",
                description = "Monitor your achievements, earn rewards, and celebrate your success."
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