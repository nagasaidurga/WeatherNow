package com.example.weatherapplication.data.repository

import com.example.weatherapplication.BuildConfig
import com.example.weatherapplication.data.api.WeatherApiService
import com.example.weatherapplication.data.model.WeatherInfo
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.util.WeatherMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface defining weather data operations.
 * Follows repository pattern to abstract data source from ViewModel.
 */
interface WeatherRepository {
    /**
     * Fetches weather data for a given city name.
     * @param cityName City name (can include state and country for US cities)
     * @return Result containing WeatherInfo on success or exception on failure
     */
    suspend fun getWeatherByCity(cityName: String): Result<WeatherInfo>

    /**
     * Fetches weather data for given geographic coordinates.
     * @param latitude Geographic latitude
     * @param longitude Geographic longitude
     * @return Result containing WeatherInfo on success or exception on failure
     */
    suspend fun getWeatherByCoordinates(latitude: Double, longitude: Double): Result<WeatherInfo>
}

/**
 * Implementation of WeatherRepository that fetches data from OpenWeatherMap API.
 * 
 * @property apiService Retrofit service for API calls
 * @property weatherMapper Utility class for converting API response to UI model
 * 
 * Note: All network operations are performed on IO dispatcher for proper threading.
 */
@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val apiService: WeatherApiService,
    private val weatherMapper: WeatherMapper
) : WeatherRepository {

    /**
     * Fetches weather by city name with proper error handling.
     * 
     * The city name can be in the following formats:
     * - "Austin" - Just city name
     * - "Austin,TX" - City with state (US only)
     * - "Austin,TX,US" - City with state and country
     * 
     * @param cityName The city name to search for
     * @return Result wrapping WeatherInfo or an exception
     */
    override suspend fun getWeatherByCity(cityName: String): Result<WeatherInfo> {
        return withContext(Dispatchers.IO) {
            try {
                // Append US country code for US-focused app if not already specified
                val formattedCityName = formatCityNameForUS(cityName)
                
                val response = apiService.getWeatherByCity(
                    cityName = formattedCityName,
                    apiKey = BuildConfig.WEATHER_API_KEY
                )

                handleWeatherResponse(response)
            } catch (e: Exception) {
                // Log exception in production app
                // Given more time, I would implement proper logging with Timber or similar
                Result.failure(WeatherException.NetworkError("Unable to fetch weather data. Please check your internet connection."))
            }
        }
    }

    /**
     * Fetches weather by coordinates - useful for location-based weather.
     * This is the preferred method as city name search is deprecated by OpenWeatherMap.
     */
    override suspend fun getWeatherByCoordinates(
        latitude: Double,
        longitude: Double
    ): Result<WeatherInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWeatherByCoordinates(
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = BuildConfig.WEATHER_API_KEY
                )

                handleWeatherResponse(response)
            } catch (e: Exception) {
                Result.failure(WeatherException.NetworkError("Unable to fetch weather data. Please check your internet connection."))
            }
        }
    }

    /**
     * Processes the API response and converts to Result type.
     * Handles various HTTP status codes with appropriate error messages.
     */
    private fun handleWeatherResponse(
        response: retrofit2.Response<WeatherResponse>
    ): Result<WeatherInfo> {
        return when {
            response.isSuccessful -> {
                response.body()?.let { weatherResponse ->
                    Result.success(weatherMapper.mapToWeatherInfo(weatherResponse))
                } ?: Result.failure(WeatherException.EmptyResponse("No weather data available"))
            }
            response.code() == 404 -> {
                Result.failure(WeatherException.CityNotFound("City not found. Please check the city name and try again."))
            }
            response.code() == 401 -> {
                // Given more time, I would implement API key rotation or refresh mechanism
                Result.failure(WeatherException.ApiKeyError("API authentication error. Please try again later."))
            }
            response.code() == 429 -> {
                Result.failure(WeatherException.RateLimitExceeded("Too many requests. Please wait a moment and try again."))
            }
            else -> {
                Result.failure(WeatherException.ServerError("Server error (${response.code()}). Please try again later."))
            }
        }
    }

    /**
     * Formats city name to include US country code if not already specified.
     * This improves search accuracy for US cities.
     * 
     * Examples:
     * - "Austin" -> "Austin,US"
     * - "Austin,TX" -> "Austin,TX,US"
     * - "Austin,TX,US" -> "Austin,TX,US" (unchanged)
     */
    private fun formatCityNameForUS(cityName: String): String {
        val parts = cityName.split(",").map { it.trim() }
        return when (parts.size) {
            1 -> "$cityName,US"
            2 -> "${parts[0]},${parts[1]},US"
            else -> cityName // Already has country code
        }
    }
}

/**
 * Custom exception hierarchy for weather-related errors.
 * Provides specific error types for better error handling and user feedback.
 */
sealed class WeatherException(message: String) : Exception(message) {
    class NetworkError(message: String) : WeatherException(message)
    class CityNotFound(message: String) : WeatherException(message)
    class ApiKeyError(message: String) : WeatherException(message)
    class RateLimitExceeded(message: String) : WeatherException(message)
    class ServerError(message: String) : WeatherException(message)
    class EmptyResponse(message: String) : WeatherException(message)
    class LocationError(message: String) : WeatherException(message)
}

