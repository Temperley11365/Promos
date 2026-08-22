package com.example.service

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.model.Bank
import com.example.model.CardNetwork
import com.example.model.CardType
import com.example.notification.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

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
        Log.d(TAG, "From: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val data = remoteMessage.data
        val bankId = data["bank_id"] ?: data["bank"]
        val cardNetwork = data["card_network"]
        val cardType = data["card_type"]
        val promoTitle = data["title"] ?: remoteMessage.notification?.title ?: "Nueva Promoción"
        val storeName = data["store_name"] ?: data["store"] ?: "Estaciones y Comercios"
        val discountPercent = data["discount_percent"]?.toDoubleOrNull()
            ?: data["discount"]?.replace("%", "")?.trim()?.toDoubleOrNull()
            ?: 0.0
        val promoId = data["promo_id"] ?: data["id"]
        val body = remoteMessage.notification?.body ?: data["body"]

        // If bankId or card parameters are present, check against saved user cards
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val userCards = db.userCardDao().getAllCards().firstOrNull() ?: emptyList()

                // Check for exact or matching card
                val matchingCard = if (!bankId.isNullOrBlank()) {
                    userCards.firstOrNull { card ->
                        val bankMatches = card.bankId.equals(bankId, ignoreCase = true)
                        val netMatches = cardNetwork.isNullOrBlank() || card.cardNetwork.equals(cardNetwork, ignoreCase = true)
                        val typeMatches = cardType.isNullOrBlank() || card.cardType.equals(cardType, ignoreCase = true)
                        bankMatches && (netMatches || typeMatches)
                    }
                } else null

                if (matchingCard != null) {
                    // Match found for user's card!
                    NotificationHelper.showCardMatchedPromoNotification(
                        context = applicationContext,
                        cardName = matchingCard.cardName,
                        bankName = matchingCard.bankName,
                        promoTitle = promoTitle,
                        discountPercent = discountPercent,
                        storeName = storeName,
                        promoId = promoId
                    )
                } else if (!bankId.isNullOrBlank()) {
                    val bank = Bank.fromId(bankId)
                    NotificationHelper.showPushNotification(
                        context = applicationContext,
                        title = "🏦 ${bank.displayName}: $promoTitle",
                        body = body ?: "${if (discountPercent > 0) "-${discountPercent.toInt()}% OFF" else "Beneficio"} en $storeName. ¡Aprovechá tus ahorros!",
                        data = data
                    )
                } else {
                    NotificationHelper.showPushNotification(
                        context = applicationContext,
                        title = promoTitle,
                        body = body ?: "¡Hay nuevas ofertas disponibles en tus estaciones y comercios cercanos!",
                        data = data
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error processing FCM notification: ${e.message}")
                NotificationHelper.showPushNotification(
                    context = applicationContext,
                    title = promoTitle,
                    body = body ?: "¡Nuevas promociones disponibles!",
                    data = data
                )
            }
        }
    }
}

