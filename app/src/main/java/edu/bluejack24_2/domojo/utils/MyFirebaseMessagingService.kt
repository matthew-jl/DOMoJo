package edu.bluejack24_2.domojo.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        var currentToken: String? = null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if notifications are enabled in preferences
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) {
            return
        }

        remoteMessage.notification?.let { notification ->
            try {
                showNotification(
                    notification.title ?: "New Notification",
                    notification.body ?: "You have a new message"
                )
            } catch (e: SecurityException) {
                prefs.edit().putBoolean("notifications_enabled", false).apply()
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(title: String, message: String) {
        if (hasNotificationPermission()) {
            NotificationHelper.showNotification(applicationContext, title, message)
        } else {
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        currentToken = token
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
    }
}