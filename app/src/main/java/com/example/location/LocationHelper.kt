package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    companion object {
        private const val TAG = "LocationHelper"

        fun hasLocationPermission(context: Context): Boolean {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        onSuccess: (GeoPoint) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Permisos de ubicación no otorgados")
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val geoPoint = resolveGeoPoint(location.latitude, location.longitude)
                    onSuccess(geoPoint)
                } else {
                    // Fallback to last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            val geoPoint = resolveGeoPoint(lastLoc.latitude, lastLoc.longitude)
                            onSuccess(geoPoint)
                        } else {
                            onError("No se pudo obtener la posición GPS exacta")
                        }
                    }.addOnFailureListener {
                        onError("Error al leer última ubicación: ${it.localizedMessage}")
                    }
                }
            }.addOnFailureListener { exception ->
                onError("Error de GPS: ${exception.localizedMessage}")
            }
        } catch (e: SecurityException) {
            onError("Permiso de seguridad denegado: ${e.localizedMessage}")
        } catch (e: Exception) {
            onError("Excepción de ubicación: ${e.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startRealtimeLocationUpdates(
        intervalMs: Long = 10000L,
        onLocationChanged: (GeoPoint) -> Unit
    ) {
        if (!hasLocationPermission(context)) return

        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(5000L)
            .setMinUpdateDistanceMeters(20f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val geoPoint = resolveGeoPoint(loc.latitude, loc.longitude)
                    onLocationChanged(geoPoint)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on requestLocationUpdates", e)
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun resolveGeoPoint(lat: Double, lng: Double): GeoPoint {
        var placeName = "Mi Ubicación Actual"
        var addressName = "${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)}"

        try {
            val geocoder = Geocoder(context, Locale("es", "AR"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val addr = addresses[0]
                        placeName = addr.subLocality ?: addr.locality ?: "Mi Posición GPS"
                        addressName = addr.getAddressLine(0) ?: ""
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    placeName = addr.subLocality ?: addr.locality ?: "Mi Posición GPS"
                    addressName = addr.getAddressLine(0) ?: ""
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder reverse lookup failed", e)
        }

        return GeoPoint(lat = lat, lng = lng, name = placeName, address = addressName)
    }
}
