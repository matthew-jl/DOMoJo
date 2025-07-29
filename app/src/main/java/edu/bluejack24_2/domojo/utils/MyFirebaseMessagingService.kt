package edu.bluejack24_2.domojo.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FCMService"
        var currentToken: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received: ${remoteMessage.messageId}")

        // Check if notifications are enabled in preferences
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) {
            Log.d(TAG, "Notifications disabled in app settings")
            return
        }

        remoteMessage.notification?.let { notification ->
            try {
                showNotification(
                    notification.title ?: "New Notification",
                    notification.body ?: "You have a new message"
                )
                Log.d(TAG, "Notification shown: ${notification.title}")
            } catch (e: SecurityException) {
                Log.e(TAG, "Notification permission revoked", e)
                prefs.edit().putBoolean("notifications_enabled", false).apply()
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(title: String, message: String) {
        if (hasNotificationPermission()) {
            NotificationHelper.showNotification(applicationContext, title, message)
        } else {
            Log.w(TAG, "Can't show notification - permission missing")
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
        Log.d(TAG, "Refreshed token: $token")
        currentToken = token
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // Implement this to send token to backend server
        Log.d(TAG, "Sending token to server: $token")
    }
}