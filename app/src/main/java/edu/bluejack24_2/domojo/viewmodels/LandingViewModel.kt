package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class LandingViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun isUserLoggedIn(): Boolean {
        if(firebaseAuth.currentUser != null) {
            return true
        }
        return false
    }
}