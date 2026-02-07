package com.example.weatherapplication.util

import com.example.weatherapplication.BuildConfig
import com.example.weatherapplication.data.model.WeatherInfo
import com.example.weatherapplication.data.model.WeatherResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Utility class for mapping API response to UI-friendly model.
 * Handles all data transformation and formatting logic.
 * 
 * Given more time, I would prefer to use a dedicated mapping library like MapStruct
 * or implement extension functions for cleaner code organization.
 */
@Singleton
class WeatherMapper @Inject constructor() {

    /**
     * Transforms raw API response into user-friendly WeatherInfo model.
     * 
     * @param response Raw API response from OpenWeatherMap
     * @return Formatted WeatherInfo ready for UI display
     */
    fun mapToWeatherInfo(response: WeatherResponse): WeatherInfo {
        val weatherData = response.weather.firstOrNull()
        
        return WeatherInfo(
            cityName = response.cityName,
            country = response.sys.country,
            temperature = formatTemperature(response.main.temperature),
            feelsLike = formatTemperature(response.main.feelsLike),
            tempMin = formatTemperature(response.main.tempMin),
            tempMax = formatTemperature(response.main.tempMax),
            humidity = "${response.main.humidity}%",
            pressure = "${response.main.pressure} hPa",
            windSpeed = formatWindSpeed(response.wind.speed),
            windDirection = getWindDirection(response.wind.degrees),
            description = weatherData?.description?.capitalizeWords() ?: "Unknown",
            mainCondition = weatherData?.main ?: "Unknown",
            iconUrl = buildIconUrl(weatherData?.icon ?: "01d"),
            visibility = formatVisibility(response.visibility),
            cloudiness = "${response.clouds.cloudiness}%",
            sunrise = formatTime(response.sys.sunrise, response.timezone),
            sunset = formatTime(response.sys.sunset, response.timezone),
            lastUpdated = formatTime(response.dateTime, response.timezone)
        )
    }

    /**
     * Formats temperature value with degree symbol.
     * API already returns Fahrenheit when using imperial units.
     */
    private fun formatTemperature(temp: Double): String {
        return "${temp.roundToInt()}°F"
    }

    /**
     * Formats wind speed in mph (imperial units from API).
     */
    private fun formatWindSpeed(speed: Double): String {
        return "${speed.roundToInt()} mph"
    }

    /**
     * Converts wind degrees to cardinal direction.
     * Uses 16-point compass for detailed direction.
     */
    private fun getWindDirection(degrees: Int): String {
        val directions = arrayOf(
            "N", "NNE", "NE", "ENE",
            "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW",
            "W", "WNW", "NW", "NNW"
        )
        val index = ((degrees + 11.25) / 22.5).toInt() % 16
        return directions[index]
    }

    /**
     * Builds the full URL for weather icon.
     * Uses @2x suffix for higher resolution icons.
     * 
     * Icon documentation: http://openweathermap.org/weather-conditions
     */
    private fun buildIconUrl(iconCode: String): String {
        return "${BuildConfig.WEATHER_ICON_URL}${iconCode}@2x.png"
    }

    /**
     * Converts visibility from meters to miles.
     */
    private fun formatVisibility(visibilityMeters: Int): String {
        val visibilityMiles = visibilityMeters / 1609.34
        return String.format(Locale.US, "%.1f mi", visibilityMiles)
    }

    /**
     * Formats Unix timestamp to readable time with timezone offset.
     * 
     * @param timestamp Unix timestamp in seconds
     * @param timezoneOffset Timezone offset in seconds from UTC
     */
    private fun formatTime(timestamp: Long, timezoneOffset: Int): String {
        val dateFormat = SimpleDateFormat("h:mm a", Locale.US)
        // Create timezone with the offset from API
        dateFormat.timeZone = TimeZone.getTimeZone("GMT").apply {
            rawOffset = timezoneOffset * 1000
        }
        return dateFormat.format(Date(timestamp * 1000))
    }

    /**
     * Extension function to capitalize first letter of each word.
     */
    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() 
            }
        }
    }
}

