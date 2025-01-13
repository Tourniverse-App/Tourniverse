package com.example.tourniverse.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.tourniverse.R

object NotificationHelper {

    private const val CHANNEL_ID = "tourniverse_notifications"
    private const val CHANNEL_NAME = "Tourniverse Notifications"

    /**
     * Initializes the notification channel for Android 8.0+ (Oreo and above).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for Tourniverse app"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Displays a notification with the given title and message.
     *
     * @param context The application context.
     * @param title The notification title.
     * @param message The notification message.
     * @param notificationId Optional notification ID for reusing/updating notifications.
     * @param silent Whether the notification should be silent (no sound or vibration).
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int? = null,
        silent: Boolean = false
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Expandable style
            .setSmallIcon(R.drawable.t32) // Replace with your app's notification icon
            .setAutoCancel(true)

        if (silent) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_LOW)
            notificationBuilder.setNotificationSilent()
        }

        notificationManager.notify(notificationId ?: System.currentTimeMillis().toInt(), notificationBuilder.build())

        // Log the notification
        android.util.Log.d("NotificationHelper", "Notification sent: Title = $title, Message = $message")
    }
}
