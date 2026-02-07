package com.example.weatherapplication.data.api

import com.example.weatherapplication.data.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for OpenWeatherMap API.
 * Provides methods to fetch weather data by city name or geographic coordinates.
 * 
 * API Documentation: https://openweathermap.org/current
 * 
 * Note: The API returns temperature in Kelvin by default. Use 'units' parameter
 * to get Imperial (Fahrenheit) or Metric (Celsius) units.
 */
interface WeatherApiService {

    /**
     * Fetches weather data by city name.
     * Supports formats: "city", "city,state,country" (state only for US)
     * 
     * @param cityName City name, optionally with state and country codes
     * @param apiKey OpenWeatherMap API key
     * @param units Unit system: "imperial" for Fahrenheit, "metric" for Celsius
     * @return Response containing weather data or error
     * 
     * Example: getWeatherByCity("Austin,TX,US", apiKey, "imperial")
     */
    @GET("data/2.5/weather")
    suspend fun getWeatherByCity(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "imperial"
    ): Response<WeatherResponse>

    /**
     * Fetches weather data by geographic coordinates.
     * This is the recommended approach as city name search is deprecated.
     * 
     * @param latitude Geographic latitude
     * @param longitude Geographic longitude
     * @param apiKey OpenWeatherMap API key
     * @param units Unit system: "imperial" for Fahrenheit, "metric" for Celsius
     * @return Response containing weather data or error
     */
    @GET("data/2.5/weather")
    suspend fun getWeatherByCoordinates(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "imperial"
    ): Response<WeatherResponse>
}

