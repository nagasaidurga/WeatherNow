package com.example.weatherapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapplication.data.model.WeatherUiState
import com.example.weatherapplication.data.preferences.UserPreferencesRepository
import com.example.weatherapplication.data.repository.WeatherRepository
import com.example.weatherapplication.location.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for weather-related UI state management.
 * Follows MVVM pattern with unidirectional data flow.
 * 
 * Responsibilities:
 * - Manage weather UI state (loading, success, error)
 * - Handle search queries and API calls
 * - Persist and restore last searched city
 * - Handle location-based weather fetching
 * 
 * @property weatherRepository Repository for weather data operations
 * @property userPreferencesRepository Repository for user preferences (last city)
 * @property locationService Service for device location access
 */
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationService: LocationService
) : ViewModel() {

    /**
     * Current weather UI state exposed as StateFlow for Compose observation.
     * Uses StateFlow instead of LiveData for:
     * - Better Compose integration
     * - Null safety (initial value required)
     * - Kotlin coroutines native support
     */
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Initial)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    /**
     * Current search query for the search field.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Flag indicating if location permission was previously denied.
     * Used to avoid repeatedly asking for permission.
     */
    private val _locationPermissionDenied = MutableStateFlow(false)
    val locationPermissionDenied: StateFlow<Boolean> = _locationPermissionDenied.asStateFlow()

    /**
     * Initialize by checking for location permission and loading saved city.
     * Priority order:
     * 1. If location permission granted -> fetch weather by location
     * 2. Else if last searched city exists -> load that city's weather
     * 3. Else -> show initial state (search prompt)
     */
    init {
        initializeWeather()
    }

    /**
     * Initializes weather data based on available data sources.
     * Checks location first, then falls back to last searched city.
     */
    private fun initializeWeather() {
        viewModelScope.launch {
            // First, check if we have location permission and can get location
            if (locationService.hasLocationPermission()) {
                fetchWeatherByLocation()
            } else {
                // Fall back to last searched city
                loadLastSearchedCity()
            }
        }
    }

    /**
     * Loads weather for the last searched city from preferences.
     * Called on app launch if no location permission.
     */
    private suspend fun loadLastSearchedCity() {
        val lastCity = userPreferencesRepository.lastSearchedCity.first()
        if (!lastCity.isNullOrBlank()) {
            _searchQuery.value = lastCity
            fetchWeatherByCity(lastCity)
        }
        // If no last city, UI stays in Initial state showing search prompt
    }

    /**
     * Updates the search query as user types.
     * @param query The current text in search field
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Performs weather search for the current query.
     * Validates input and handles empty query gracefully.
     */
    fun onSearchSubmit() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) {
            _uiState.value = WeatherUiState.Error("Please enter a city name")
            return
        }
        fetchWeatherByCity(query)
    }

    /**
     * Fetches weather data for a given city name.
     * Also saves the city as last searched for auto-load on next launch.
     * 
     * @param cityName City name to search for
     */
    private fun fetchWeatherByCity(cityName: String) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading

            weatherRepository.getWeatherByCity(cityName)
                .onSuccess { weatherInfo ->
                    _uiState.value = WeatherUiState.Success(weatherInfo)
                    // Save successful search for auto-load
                    userPreferencesRepository.saveLastSearchedCity(cityName)
                }
                .onFailure { exception ->
                    _uiState.value = WeatherUiState.Error(
                        exception.message ?: "An unexpected error occurred"
                    )
                }
        }
    }

    /**
     * Fetches weather data using device's current location.
     * Called when user grants location permission or on app launch with permission.
     */
    fun fetchWeatherByLocation() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading

            locationService.getCurrentLocation()
                .onSuccess { location ->
                    weatherRepository.getWeatherByCoordinates(
                        latitude = location.latitude,
                        longitude = location.longitude
                    ).onSuccess { weatherInfo ->
                        _uiState.value = WeatherUiState.Success(weatherInfo)
                        // Update search query to show the resolved city name
                        _searchQuery.value = "${weatherInfo.cityName}, ${weatherInfo.country}"
                        // Save for auto-load (using coordinates-resolved city name)
                        userPreferencesRepository.saveLastSearchedCity(
                            "${weatherInfo.cityName}, ${weatherInfo.country}"
                        )
                    }.onFailure { exception ->
                        _uiState.value = WeatherUiState.Error(
                            exception.message ?: "Failed to fetch weather for your location"
                        )
                    }
                }
                .onFailure { exception ->
                    // Location failed, try to fall back to last searched city
                    val lastCity = userPreferencesRepository.lastSearchedCity.first()
                    if (!lastCity.isNullOrBlank()) {
                        fetchWeatherByCity(lastCity)
                    } else {
                        _uiState.value = WeatherUiState.Error(
                            exception.message ?: "Unable to get location"
                        )
                    }
                }
        }
    }

    /**
     * Called when user grants location permission.
     * Immediately fetches weather for current location.
     */
    fun onLocationPermissionGranted() {
        _locationPermissionDenied.value = false
        fetchWeatherByLocation()
    }

    /**
     * Called when user denies location permission.
     * Sets flag to avoid re-prompting and falls back to last city.
     */
    fun onLocationPermissionDenied() {
        _locationPermissionDenied.value = true
        viewModelScope.launch {
            loadLastSearchedCity()
        }
    }

    /**
     * Refreshes the current weather data.
     * Re-fetches using location if permission granted, otherwise uses search query.
     */
    fun refresh() {
        val currentState = _uiState.value
        if (currentState is WeatherUiState.Success) {
            // Re-fetch using the current city
            val cityWithCountry = "${currentState.weatherInfo.cityName}, ${currentState.weatherInfo.country}"
            fetchWeatherByCity(cityWithCountry)
        } else if (_searchQuery.value.isNotBlank()) {
            fetchWeatherByCity(_searchQuery.value)
        } else if (locationService.hasLocationPermission()) {
            fetchWeatherByLocation()
        }
    }

    /**
     * Checks if location permission is currently granted.
     * Used by UI to determine whether to show permission request.
     */
    fun hasLocationPermission(): Boolean {
        return locationService.hasLocationPermission()
    }

    /**
     * Clears any error state and resets to initial.
     * Used when user dismisses an error.
     */
    fun clearError() {
        if (_uiState.value is WeatherUiState.Error) {
            _uiState.value = WeatherUiState.Initial
        }
    }
}

