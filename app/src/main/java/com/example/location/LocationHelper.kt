package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.GeoPoint
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

/**
 * Robust Location Service Helper using Google Play Services FusedLocationProviderClient
 * with comprehensive crash prevention, fallback strategies, and permission checks.
 */
class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val locationManager: LocationManager? by lazy {
        try {
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        } catch (e: Throwable) {
            Log.w(TAG, "Cannot get LocationManager: ${e.message}")
            null
        }
    }

    private var locationCallback: LocationCallback? = null
    private var systemLocationListener: LocationListener? = null
    private var currentCancellationTokenSource: CancellationTokenSource? = null

    companion object {
        private const val TAG = "LocationHelper"

        // Default fallback position: Buenos Aires / Palermo Soho
        val DEFAULT_LOCATION = GeoPoint(
            lat = -34.5875,
            lng = -58.4285,
            name = "Buenos Aires (Zona Centro)",
            address = "Palermo Soho, CABA"
        )

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

        fun isLocationEnabled(context: Context): Boolean {
            return try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
                val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                isGpsEnabled || isNetworkEnabled
            } catch (e: Throwable) {
                false
            }
        }
    }

    /**
     * Verifies whether Google Play Services is available on this device.
     */
    fun isGooglePlayServicesAvailable(): Boolean {
        return try {
            val availability = GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(context)
            resultCode == ConnectionResult.SUCCESS
        } catch (e: Throwable) {
            Log.w(TAG, "Google Play Services check exception: ${e.message}")
            false
        }
    }

    /**
     * Checks device location settings using Google Play Services SettingsClient.
     */
    fun checkLocationSettings(
        onSettingsSatisfied: () -> Unit,
        onSettingsNeedsResolution: (Exception) -> Unit
    ) {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).build()
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val client = LocationServices.getSettingsClient(context)
            val task = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                onSettingsSatisfied()
            }.addOnFailureListener { exception ->
                onSettingsNeedsResolution(exception)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "checkLocationSettings failed: ${e.message}")
            onSettingsSatisfied()
        }
    }

    /**
     * Safely fetches current location through Google Play Services Fused Location Client.
     * Guaranteed never to throw uncaught exceptions or cause application crashes.
     */
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        onSuccess: (GeoPoint) -> Unit,
        onError: (String) -> Unit
    ) {
        // 1. Verify runtime permissions safely
        if (!hasLocationPermission(context)) {
            Log.i(TAG, "Location permission not granted. Falling back to default position.")
            onError("Permiso de ubicación no concedido.")
            return
        }

        var isDispatched = false

        fun dispatchSuccess(loc: Location, source: String) {
            if (!isDispatched) {
                isDispatched = true
                Log.d(TAG, "Location successfully obtained via $source: (${loc.latitude}, ${loc.longitude})")
                val geoPoint = resolveGeoPoint(loc.latitude, loc.longitude)
                onSuccess(geoPoint)
            }
        }

        fun dispatchFallback(reason: String) {
            if (!isDispatched) {
                isDispatched = true
                Log.w(TAG, "Location fallback triggered ($reason). Using default zone.")
                onSuccess(DEFAULT_LOCATION)
            }
        }

        // Cancel previous pending token if any
        currentCancellationTokenSource?.cancel()
        val tokenSource = CancellationTokenSource()
        currentCancellationTokenSource = tokenSource

        // 2. Try Google Play Services FusedLocationProviderClient first
        if (isGooglePlayServicesAvailable()) {
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    tokenSource.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        dispatchSuccess(location, "Google Fused HighAccuracy")
                    } else {
                        // Try last known location from Google Location Services
                        tryFusedLastLocation(
                            onSuccess = { loc -> dispatchSuccess(loc, "Google Fused LastKnown") },
                            onFailure = {
                                trySystemLocationManager(
                                    onSuccess = { loc -> dispatchSuccess(loc, "System LocationManager") },
                                    onFallback = { dispatchFallback("No position from Google or System") }
                                )
                            }
                        )
                    }
                }.addOnFailureListener { e ->
                    Log.w(TAG, "Google getCurrentLocation error: ${e.message}, falling back to lastLocation/system")
                    tryFusedLastLocation(
                        onSuccess = { loc -> dispatchSuccess(loc, "Google Fused LastKnown") },
                        onFailure = {
                            trySystemLocationManager(
                                onSuccess = { loc -> dispatchSuccess(loc, "System LocationManager") },
                                onFallback = { dispatchFallback("Google getCurrentLocation and system failed") }
                            )
                        }
                    )
                }
                return
            } catch (e: Throwable) {
                Log.w(TAG, "FusedLocationProvider exception: ${e.message}")
            }
        }

        // 3. Google Play Services not available or threw exception -> Use standard LocationManager
        trySystemLocationManager(
            onSuccess = { loc -> dispatchSuccess(loc, "Native LocationManager") },
            onFallback = { dispatchFallback("Native LocationManager fallback") }
        )
    }

    @SuppressLint("MissingPermission")
    private fun tryFusedLastLocation(
        onSuccess: (Location) -> Unit,
        onFailure: () -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onFailure()
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        onSuccess(lastLoc)
                    } else {
                        onFailure()
                    }
                }
                .addOnFailureListener {
                    onFailure()
                }
        } catch (e: Throwable) {
            Log.w(TAG, "tryFusedLastLocation error: ${e.message}")
            onFailure()
        }
    }

    @SuppressLint("MissingPermission")
    private fun trySystemLocationManager(
        onSuccess: (Location) -> Unit,
        onFallback: () -> Unit
    ) {
        val lm = locationManager
        if (lm == null || !hasLocationPermission(context)) {
            onFallback()
            return
        }

        try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            var bestLocation: Location? = null
            for (provider in providers) {
                try {
                    if (lm.isProviderEnabled(provider)) {
                        val loc = lm.getLastKnownLocation(provider)
                        if (loc != null) {
                            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                bestLocation = loc
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error checking provider $provider: ${e.message}")
                }
            }

            if (bestLocation != null) {
                onSuccess(bestLocation)
                return
            }

            // Quick single update listener with available provider
            val enabledProvider = providers.firstOrNull {
                try { lm.isProviderEnabled(it) } catch (e: Throwable) { false }
            }

            if (enabledProvider != null) {
                var received = false
                val singleListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (!received) {
                            received = true
                            try {
                                lm.removeUpdates(this)
                            } catch (e: Throwable) {
                                // ignore
                            }
                            onSuccess(location)
                        }
                    }
                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }

                lm.requestLocationUpdates(
                    enabledProvider,
                    0L,
                    0f,
                    singleListener,
                    Looper.getMainLooper()
                )
            } else {
                onFallback()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "trySystemLocationManager error: ${e.message}")
            onFallback()
        }
    }

    /**
     * Starts continuous real-time location updates safely using Google Play Services.
     */
    @SuppressLint("MissingPermission")
    fun startRealtimeLocationUpdates(
        intervalMs: Long = 10000L,
        onLocationChanged: (GeoPoint) -> Unit
    ) {
        if (!hasLocationPermission(context)) return

        stopLocationUpdates()

        if (isGooglePlayServicesAvailable()) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
                    .setMinUpdateIntervalMillis(intervalMs / 2)
                    .setMinUpdateDistanceMeters(15f)
                    .build()

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { loc ->
                            val geoPoint = resolveGeoPoint(loc.latitude, loc.longitude)
                            onLocationChanged(geoPoint)
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback as LocationCallback,
                    Looper.getMainLooper()
                )
                return
            } catch (e: Throwable) {
                Log.w(TAG, "Google Location updates failed: ${e.message}, falling back to LocationManager")
            }
        }

        // Fallback to system LocationManager
        try {
            val lm = locationManager
            if (lm != null) {
                val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationManager.GPS_PROVIDER
                } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else null

                if (provider != null) {
                    systemLocationListener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            val geoPoint = resolveGeoPoint(location.latitude, location.longitude)
                            onLocationChanged(geoPoint)
                        }
                    }
                    lm.requestLocationUpdates(
                        provider,
                        intervalMs,
                        15f,
                        systemLocationListener as LocationListener,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (ex: Throwable) {
            Log.e(TAG, "System location updates failed: ${ex.message}")
        }
    }

    /**
     * Stops any active location listener or callback to prevent memory leaks and battery drain.
     */
    fun stopLocationUpdates() {
        try {
            currentCancellationTokenSource?.cancel()
            currentCancellationTokenSource = null
        } catch (e: Throwable) {
            // ignore
        }

        try {
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
                locationCallback = null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error removing fused updates: ${e.message}")
        }

        try {
            systemLocationListener?.let {
                locationManager?.removeUpdates(it)
                systemLocationListener = null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error removing system updates: ${e.message}")
        }
    }

    /**
     * Resolves human-readable address with maximum safety against IO and Geocoder failures.
     */
    private fun resolveGeoPoint(lat: Double, lng: Double): GeoPoint {
        var placeName = "Mi Posición GPS"
        var addressName = "${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)}"

        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("es", "AR"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    placeName = addr.subLocality ?: addr.locality ?: addr.featureName ?: "Mi Posición GPS"
                    addressName = addr.getAddressLine(0) ?: addressName
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Geocoder lookup note: ${e.message}")
        }

        return GeoPoint(lat = lat, lng = lng, name = placeName, address = addressName)
    }
}
