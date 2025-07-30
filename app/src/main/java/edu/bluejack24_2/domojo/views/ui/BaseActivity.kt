package edu.bluejack24_2.domojo.views.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import edu.bluejack24_2.domojo.utils.LocaleHelper
import edu.bluejack24_2.domojo.utils.MyFirebaseMessagingService
import edu.bluejack24_2.domojo.utils.ThemeHelper

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.getSavedTheme(this))
        super.onCreate(savedInstanceState)
        initializeFCM()
    }

    private fun initializeFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "FCM Token: $token")
            MyFirebaseMessagingService.currentToken = token
        }
    }
}