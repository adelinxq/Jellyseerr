package com.jellyseerr.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Token actualizat - nu afișăm nimic
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Check if message contains a notification payload
        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Jellyseerr",
                body = notification.body ?: "New notification"
            )
        }

        // Check if message contains a data payload
        if (message.data.isNotEmpty()) {
            val title = message.data["title"] ?: "Jellyseerr"
            val body = message.data["body"] ?: "New notification"

            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, body: String) {
        createNotificationChannel()

        // Folosește iconița aplicației
        val iconResId = try {
            R.mipmap.jellyseerricon
        } catch (e: Exception) {
            android.R.drawable.ic_dialog_info
        }

        val builder = NotificationCompat.Builder(this, "jellyseerr_channel")
            .setSmallIcon(iconResId)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(
            System.currentTimeMillis().toInt(),
            builder.build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "jellyseerr_channel",
                "Jellyseerr Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from Jellyseerr app"
                enableLights(true)
                lightColor = Color.parseColor("#6D28D9")
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}