package com.example.service

import android.util.Log
import com.example.notification.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        var lastToken: String? = null
            private set
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token generated: $token")
        lastToken = token
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Nueva promoción de combustible"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "¡Hay nuevas ofertas disponibles cerca de tu ubicación!"

        NotificationHelper.showPushNotification(
            context = applicationContext,
            title = title,
            body = body,
            data = remoteMessage.data
        )
    }
}
