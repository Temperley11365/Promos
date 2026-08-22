package com.example

import android.app.Application
import android.util.Log
import com.example.notification.NotificationHelper
import com.google.firebase.FirebaseApp

class PromoApplication : Application() {

    companion object {
        private const val TAG = "PromoApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Notification Channels
        try {
            NotificationHelper.createNotificationChannels(this)
        } catch (e: Throwable) {
            Log.w(TAG, "Notification channels init warning: ${e.message}")
        }

        // 2. Safely initialize Firebase if configuration is available
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "FirebaseApp initialized successfully")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseApp initialization skipped: ${e.message}")
        }
    }
}
