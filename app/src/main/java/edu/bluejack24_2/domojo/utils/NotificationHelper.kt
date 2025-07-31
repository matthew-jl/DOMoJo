package edu.bluejack24_2.domojo.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import edu.bluejack24_2.domojo.R

object NotificationHelper {
    const val CHANNEL_ID = "default_channel_id"
    const val CHANNEL_NAME = "Default Channel"
    const val NOTIFICATION_ID = 1
    private const val DAILY_NOTIFICATION_ID = 2

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Default notifications channel"
            }

            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(
        context: Context,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        }
    }

    @RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    fun showDailyNotification(context: Context) {
        createNotificationChannel(context)

        val title = context.getString(R.string.daily_reminder_title)
        val message = context.getString(R.string.daily_reminder_message)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            with(NotificationManagerCompat.from(context)) {
                notify(DAILY_NOTIFICATION_ID, builder.build())
            }
        }
    }
}