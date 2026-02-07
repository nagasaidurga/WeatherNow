package com.example.weatherapplication.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.weatherapplication.data.repository.WeatherException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Interface for location-related operations.
 * Abstracts location access for easier testing and flexibility.
 */
interface LocationService {
    /**
     * Checks if location permissions are granted.
     * @return true if either fine or coarse location permission is granted
     */
    fun hasLocationPermission(): Boolean

    /**
     * Gets the current device location.
     * Requires location permission to be granted.
     * 
     * @return Result containing Location on success or exception on failure
     */
    suspend fun getCurrentLocation(): Result<Location>
}

/**
 * Implementation of LocationService using Google Play Services Location API.
 * 
 * Uses FusedLocationProviderClient for:
 * - Best accuracy across all location providers
 * - Battery-efficient location updates
 * - Automatic provider selection
 * 
 * @property context Application context for permission checks
 * @property fusedLocationClient Play Services location client
 */
@Singleton
class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationService {

    /**
     * Checks if either fine or coarse location permission is granted.
     * Both are sufficient for weather purposes - coarse location (city-level)
     * is adequate for weather data.
     */
    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Gets current location using coroutines.
     * 
     * Uses getCurrentLocation() API instead of getLastLocation() because:
     * - More reliable (last location may be null or stale)
     * - Explicitly requests fresh location
     * - Uses BALANCED_POWER_ACCURACY as city-level accuracy is sufficient
     * 
     * Given more time, I would implement:
     * - Location settings check and prompt user to enable location
     * - Fallback to last known location if current location fails
     * - Location caching to reduce battery usage
     */
    override suspend fun getCurrentLocation(): Result<Location> {
        if (!hasLocationPermission()) {
            return Result.failure(
                WeatherException.LocationError("Location permission not granted")
            )
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            // Use try-catch for SecurityException in case permission state changes
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(Result.success(location))
                    } else {
                        // Location is null - could be GPS disabled or no location available
                        continuation.resume(
                            Result.failure(
                                WeatherException.LocationError(
                                    "Unable to get current location. Please ensure location services are enabled."
                                )
                            )
                        )
                    }
                }.addOnFailureListener { exception ->
                    continuation.resume(
                        Result.failure(
                            WeatherException.LocationError(
                                "Failed to get location: ${exception.message}"
                            )
                        )
                    )
                }
            } catch (e: SecurityException) {
                continuation.resume(
                    Result.failure(
                        WeatherException.LocationError("Location permission was revoked")
                    )
                )
            }

            // Cancel the location request if the coroutine is cancelled
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
}

