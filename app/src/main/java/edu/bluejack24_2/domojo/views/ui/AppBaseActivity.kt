package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import androidx.annotation.IdRes
import com.google.android.material.bottomnavigation.BottomNavigationView
import edu.bluejack24_2.domojo.R

abstract class AppBaseActivity : BaseActivity() {
    @get:IdRes
    protected abstract val currentBottomNavItemId: Int

    override fun onResume() {
        super.onResume()
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.menu?.findItem(currentBottomNavItemId)?.isChecked = true
    }

    protected fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.menu.findItem(currentBottomNavItemId)?.isChecked = true

        bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == currentBottomNavItemId) {
                return@setOnItemSelectedListener false
            }

            val intent = when (item.itemId) {
                R.id.nav_home -> Intent(this, HomeActivity::class.java)
                R.id.nav_challenges -> Intent(this, ChallengeActivity::class.java)
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                else -> null
            }

            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(it)
                overridePendingTransition(0, 0)
            }

            true
        }
    }
}