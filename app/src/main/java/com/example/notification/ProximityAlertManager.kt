package com.example.notification

import android.content.Context
import android.location.Location
import com.example.model.GasStation
import com.example.model.GeoPoint
import com.example.model.Promotion

class ProximityAlertManager(private val context: Context) {

    // Cooldown map: Station ID -> Timestamp of last notification
    private val notifiedStations = mutableMapOf<String, Long>()
    private val cooldownDurationMs = 15 * 60 * 1000L // 15 minutes cooldown

    var isEnabled: Boolean = true
    var proximityThresholdMeters: Int = 1200 // Alert when within 1.2 km

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Checks if current position is close to any gas station that has an active discount or promo.
     */
    fun checkProximity(
        currentLocation: GeoPoint,
        stations: List<GasStation>,
        promotions: List<Promotion>
    ) {
        if (!isEnabled) return

        val now = System.currentTimeMillis()

        for (station in stations) {
            val distMeters = calculateDistanceMeters(
                currentLocation.lat,
                currentLocation.lng,
                station.location.lat,
                station.location.lng
            ).toInt()

            if (distMeters <= proximityThresholdMeters) {
                val lastNotified = notifiedStations[station.id] ?: 0L
                if (now - lastNotified > cooldownDurationMs) {
                    // Check for active station promos
                    val stationPromos = promotions.filter {
                        it.storeName.contains(station.brand.displayName, ignoreCase = true) ||
                        it.storeName.contains(station.name, ignoreCase = true) ||
                        (station.specialPromo != null)
                    }

                    val bestPromo = stationPromos.maxByOrNull { it.discountPercent }

                    val promoTitle = bestPromo?.title
                        ?: station.specialPromo
                        ?: "Descuento en combustible"

                    val discountPercent = bestPromo?.discountPercent
                        ?: station.promoDiscountPercent
                        .takeIf { it > 0 } ?: 10.0

                    NotificationHelper.showProximityAlert(
                        context = context,
                        stationName = station.name,
                        promoTitle = promoTitle,
                        discountPercent = discountPercent,
                        distanceMeters = distMeters,
                        brandName = station.brand.displayName
                    )

                    notifiedStations[station.id] = now
                    break // Only alert one station per check cycle to avoid flooding
                }
            }
        }
    }

    /**
     * Sends an immediate test notification for demonstration/testing in UI.
     */
    fun sendTestNotification(stationName: String = "YPF Palermo Soho", discountPercent: Double = 15.0) {
        NotificationHelper.showProximityAlert(
            context = context,
            stationName = stationName,
            promoTitle = "15% OFF pagando con App YPF / Banco Galicia",
            discountPercent = discountPercent,
            distanceMeters = 350,
            brandName = "YPF"
        )
    }
}
