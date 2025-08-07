package edu.bluejack24_2.domojo.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission

class DailyNotificationReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.showDailyNotification(context)

        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("notifications_enabled", true)) {
            val hour = prefs.getInt("notification_hour", 8)
            val minute = prefs.getInt("notification_minute", 0)
            NotificationScheduler.scheduleDailyNotification(context, hour, minute)
        }
    }
}