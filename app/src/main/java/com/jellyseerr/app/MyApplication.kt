package com.jellyseerr.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Get FCM token (fără log-uri)
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Token obținut - nu afișăm nimic
                }
            }

        // Subscribe to topic (fără log-uri)
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
    }
}