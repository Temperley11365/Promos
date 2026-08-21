package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_PROXIMITY_ID = "channel_proximity_fuel_offers"
    const val CHANNEL_PUSH_ID = "channel_push_fuel_alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: Proximity fuel promos
            val proximityChannel = NotificationChannel(
                CHANNEL_PROXIMITY_ID,
                "Ofertas de Combustible Cercanas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas cuando estás cerca de una estación de servicio con descuentos activos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
            }

            // Channel 2: General push updates
            val pushChannel = NotificationChannel(
                CHANNEL_PUSH_ID,
                "Avisos y Promociones Push",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Novedades de precios, nuevos beneficios bancarios y alertas push"
            }

            notificationManager.createNotificationChannel(proximityChannel)
            notificationManager.createNotificationChannel(pushChannel)
        }
    }

    fun showProximityAlert(
        context: Context,
        stationName: String,
        promoTitle: String,
        discountPercent: Double,
        distanceMeters: Int,
        brandName: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ACTION", "SHOW_PROMO_NEARBY")
            putExtra("EXTRA_STATION", stationName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            stationName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceText = if (distanceMeters < 1000) {
            "a solo $distanceMeters m"
        } else {
            "a ${(distanceMeters / 100.0) / 10.0} km"
        }

        val discountText = if (discountPercent > 0) "-${discountPercent.toInt()}% OFF" else "OFERTA ACTIVA"

        val notification = NotificationCompat.Builder(context, CHANNEL_PROXIMITY_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⛽ ¡$discountText en $stationName!")
            .setContentText("$promoTitle ($distanceText). ¡Aprovechá a cargar ahora!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Estás cerca de **$stationName** ($brandName $distanceText).\nPromoción: **$promoTitle**.\n¡Abrí la app para ver medios de pago y calcular tu ahorro!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(stationName.hashCode(), notification)
            }
        } catch (e: SecurityException) {
            // Notifications permission not granted
        }
    }

    fun showPushNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PUSH_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            }
        } catch (e: SecurityException) {
            // Notifications permission not granted
        }
    }
}
