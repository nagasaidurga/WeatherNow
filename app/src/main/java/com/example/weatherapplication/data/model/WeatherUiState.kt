package com.example.weatherapplication.data.model

/**
 * Sealed class representing the different states of weather data fetching.
 * Used for proper state management in the ViewModel and UI layer.
 */
sealed class WeatherUiState {
    /**
     * Initial state when no search has been performed yet.
     */
    data object Initial : WeatherUiState()

    /**
     * Loading state while fetching weather data from the API.
     */
    data object Loading : WeatherUiState()

    /**
     * Success state containing the weather data to display.
     */
    data class Success(val weatherInfo: WeatherInfo) : WeatherUiState()

    /**
     * Error state containing the error message to display to the user.
     */
    data class Error(val message: String) : WeatherUiState()
}

/**
 * Domain model representing processed weather information for UI display.
 * This model transforms raw API data into user-friendly format.
 * 
 * @property cityName Name of the city
 * @property country Country code (e.g., "US")
 * @property temperature Current temperature in Fahrenheit (for US users)
 * @property feelsLike Feels like temperature in Fahrenheit
 * @property tempMin Minimum temperature in Fahrenheit
 * @property tempMax Maximum temperature in Fahrenheit
 * @property humidity Humidity percentage
 * @property pressure Atmospheric pressure in hPa
 * @property windSpeed Wind speed in mph
 * @property windDirection Wind direction description
 * @property description Weather description (e.g., "clear sky")
 * @property mainCondition Main weather condition (e.g., "Clear")
 * @property iconUrl URL for the weather condition icon
 * @property visibility Visibility in miles
 * @property cloudiness Cloud coverage percentage
 * @property sunrise Formatted sunrise time
 * @property sunset Formatted sunset time
 * @property lastUpdated Formatted last update time
 */
data class WeatherInfo(
    val cityName: String,
    val country: String,
    val temperature: String,
    val feelsLike: String,
    val tempMin: String,
    val tempMax: String,
    val humidity: String,
    val pressure: String,
    val windSpeed: String,
    val windDirection: String,
    val description: String,
    val mainCondition: String,
    val iconUrl: String,
    val visibility: String,
    val cloudiness: String,
    val sunrise: String,
    val sunset: String,
    val lastUpdated: String
)

