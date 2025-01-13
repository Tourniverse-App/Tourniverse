package com.example.tourniverse.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.tourniverse.R

class FirebaseNotificationService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "tourniverse_notifications"
        private const val CHANNEL_NAME = "Tourniverse Notifications"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extract notification data
        val type = remoteMessage.data["type"] ?: "default"
        val tournamentName = remoteMessage.data["tournamentName"] ?: "Tournament"
        val username = remoteMessage.data["username"] ?: ""
        val message = remoteMessage.data["message"] ?: ""
        val comment = remoteMessage.data["comment"] ?: ""

        // Generate notification content based on type
        val title: String
        val body: String

        when (type) {
            "score_update" -> {
                title = tournamentName
                body = "Scores have been updated!"
            }
            "message" -> {
                title = tournamentName
                body = "$username: $message"
            }
            "comment" -> {
                title = tournamentName
                body = "$username commented on your message: $comment"
            }
            "like" -> {
                title = tournamentName
                body = "$username liked your message: $message"
            }
            else -> {
                title = "Tourniverse"
                body = "You have a new notification"
            }
        }

        // Display the notification
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Optionally, send the updated token to your server for targeted notifications
    }

    /**
     * Display the notification to the user
     */
    private fun showNotification(title: String, body: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel for Android 8.0+ devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for Tourniverse app"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build and show the notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.t32)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
