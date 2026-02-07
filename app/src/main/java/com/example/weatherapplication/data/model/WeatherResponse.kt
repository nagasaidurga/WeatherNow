package com.example.weatherapplication.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class representing the complete weather API response from OpenWeatherMap.
 * This model maps directly to the JSON structure returned by the API.
 * 
 * API Documentation: https://openweathermap.org/current
 */
@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "coord") val coordinates: Coordinates,
    @Json(name = "weather") val weather: List<Weather>,
    @Json(name = "base") val base: String,
    @Json(name = "main") val main: MainWeatherData,
    @Json(name = "visibility") val visibility: Int,
    @Json(name = "wind") val wind: Wind,
    @Json(name = "clouds") val clouds: Clouds,
    @Json(name = "dt") val dateTime: Long,
    @Json(name = "sys") val sys: Sys,
    @Json(name = "timezone") val timezone: Int,
    @Json(name = "id") val cityId: Long,
    @Json(name = "name") val cityName: String,
    @Json(name = "cod") val responseCode: Int
)

/**
 * Geographic coordinates of the location.
 */
@JsonClass(generateAdapter = true)
data class Coordinates(
    @Json(name = "lon") val longitude: Double,
    @Json(name = "lat") val latitude: Double
)

/**
 * Weather condition information including icon reference for UI display.
 */
@JsonClass(generateAdapter = true)
data class Weather(
    @Json(name = "id") val id: Int,
    @Json(name = "main") val main: String,
    @Json(name = "description") val description: String,
    @Json(name = "icon") val icon: String
)

/**
 * Main weather metrics including temperature, pressure, and humidity.
 * Note: Temperature is returned in Kelvin by default from API.
 */
@JsonClass(generateAdapter = true)
data class MainWeatherData(
    @Json(name = "temp") val temperature: Double,
    @Json(name = "feels_like") val feelsLike: Double,
    @Json(name = "temp_min") val tempMin: Double,
    @Json(name = "temp_max") val tempMax: Double,
    @Json(name = "pressure") val pressure: Int,
    @Json(name = "humidity") val humidity: Int,
    @Json(name = "sea_level") val seaLevel: Int? = null,
    @Json(name = "grnd_level") val groundLevel: Int? = null
)

/**
 * Wind information including speed and direction.
 */
@JsonClass(generateAdapter = true)
data class Wind(
    @Json(name = "speed") val speed: Double,
    @Json(name = "deg") val degrees: Int,
    @Json(name = "gust") val gust: Double? = null
)

/**
 * Cloud coverage percentage.
 */
@JsonClass(generateAdapter = true)
data class Clouds(
    @Json(name = "all") val cloudiness: Int
)

/**
 * System data including country code and sunrise/sunset times.
 */
@JsonClass(generateAdapter = true)
data class Sys(
    @Json(name = "type") val type: Int? = null,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "country") val country: String,
    @Json(name = "sunrise") val sunrise: Long,
    @Json(name = "sunset") val sunset: Long
)

