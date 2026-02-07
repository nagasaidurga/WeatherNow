package com.example.weatherapplication

import android.location.Location
import app.cash.turbine.test
import com.example.weatherapplication.data.model.WeatherInfo
import com.example.weatherapplication.data.model.WeatherUiState
import com.example.weatherapplication.data.preferences.UserPreferencesRepository
import com.example.weatherapplication.data.repository.WeatherException
import com.example.weatherapplication.data.repository.WeatherRepository
import com.example.weatherapplication.location.LocationService
import com.example.weatherapplication.ui.viewmodel.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for WeatherViewModel.
 * 
 * Tests cover:
 * - Search functionality
 * - Error handling
 * - Location-based weather fetching
 * - State management
 * - Preference persistence
 * 
 * Uses Mockito for mocking dependencies and Turbine for Flow testing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var weatherRepository: WeatherRepository

    @Mock
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Mock
    private lateinit var locationService: LocationService

    private lateinit var viewModel: WeatherViewModel

    private val mockWeatherInfo = WeatherInfo(
        cityName = "Austin",
        country = "US",
        temperature = "75°F",
        feelsLike = "77°F",
        tempMin = "70°F",
        tempMax = "80°F",
        humidity = "65%",
        pressure = "1015 hPa",
        windSpeed = "10 mph",
        windDirection = "NW",
        description = "Clear Sky",
        mainCondition = "Clear",
        iconUrl = "https://openweathermap.org/img/wn/01d@2x.png",
        visibility = "10.0 mi",
        cloudiness = "5%",
        sunrise = "6:30 AM",
        sunset = "7:45 PM",
        lastUpdated = "3:00 PM"
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Default mock behaviors
        whenever(locationService.hasLocationPermission()).thenReturn(false)
        whenever(userPreferencesRepository.lastSearchedCity).thenReturn(flowOf(null))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Helper to create ViewModel after mocks are configured.
     */
    private fun createViewModel(): WeatherViewModel {
        return WeatherViewModel(
            weatherRepository = weatherRepository,
            userPreferencesRepository = userPreferencesRepository,
            locationService = locationService
        )
    }

    @Test
    fun `initial state should be Initial when no saved city and no location permission`() = runTest {
        // Given: No saved city and no location permission (default mock setup)
        
        // When: ViewModel is created
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then: State should be Initial
        assertTrue(viewModel.uiState.value is WeatherUiState.Initial)
    }

    @Test
    fun `should load last searched city on init when available`() = runTest {
        // Given: A saved city exists
        whenever(userPreferencesRepository.lastSearchedCity).thenReturn(flowOf("Austin"))
        whenever(weatherRepository.getWeatherByCity("Austin")).thenReturn(Result.success(mockWeatherInfo))

        // When: ViewModel is created
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then: Weather should be loaded for saved city
        val state = viewModel.uiState.value
        assertTrue(state is WeatherUiState.Success)
        assertEquals("Austin", (state as WeatherUiState.Success).weatherInfo.cityName)
    }

    @Test
    fun `search query change should update searchQuery state`() = runTest {
        // Given: ViewModel is created
        viewModel = createViewModel()
        advanceUntilIdle()

        // When: Search query is changed
        viewModel.onSearchQueryChange("New York")

        // Then: Query state should be updated
        assertEquals("New York", viewModel.searchQuery.value)
    }

    @Test
    fun `search submit with empty query should show error`() = runTest {
        // Given: ViewModel with empty search query
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("")

        // When: Search is submitted
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        // Then: Should show error state
        val state = viewModel.uiState.value
        assertTrue(state is WeatherUiState.Error)
        assertEquals("Please enter a city name", (state as WeatherUiState.Error).message)
    }

    @Test
    fun `successful search should update state to Success`() = runTest {
        // Given: Repository returns success
        whenever(weatherRepository.getWeatherByCity(any())).thenReturn(Result.success(mockWeatherInfo))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When: Search is performed
        viewModel.onSearchQueryChange("Austin")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        // Then: State should be Success with correct data
        val state = viewModel.uiState.value
        assertTrue(state is WeatherUiState.Success)
        assertEquals("Austin", (state as WeatherUiState.Success).weatherInfo.cityName)
    }

    @Test
    fun `successful search should save city to preferences`() = runTest {
        // Given: Repository returns success
        whenever(weatherRepository.getWeatherByCity(any())).thenReturn(Result.success(mockWeatherInfo))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When: Search is performed
        viewModel.onSearchQueryChange("Austin")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        // Then: City should be saved
        verify(userPreferencesRepository).saveLastSearchedCity("Austin")
    }

    @Test
    fun `failed search should update state to Error`() = runTest {
        // Given: Repository returns failure
        val errorMessage = "City not found"
        whenever(weatherRepository.getWeatherByCity(any()))
            .thenReturn(Result.failure(WeatherException.CityNotFound(errorMessage)))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When: Search is performed
        viewModel.onSearchQueryChange("InvalidCity")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        // Then: State should be Error
        val state = viewModel.uiState.value
        assertTrue(state is WeatherUiState.Error)
        assertEquals(errorMessage, (state as WeatherUiState.Error).message)
    }

    @Test
    fun `network error should show appropriate message`() = runTest {
        // Given: Repository returns network error
        val errorMessage = "Unable to fetch weather data. Please check your internet connection."
        whenever(weatherRepository.getWeatherByCity(any()))
            .thenReturn(Result.failure(WeatherException.NetworkError(errorMessage)))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When: Search is performed
        viewModel.onSearchQueryChange("Austin")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        // Then: State should show network error
        val state = viewModel.uiState.value
        assertTrue(state is WeatherUiState.Error)
        assertTrue((state as WeatherUiState.Error).message.contains("internet"))
    }

    @Test
    fun `location permission granted should fetch weather by location`() = runTest {
        // Given: Location returns valid coordinates
        val mockLocation = org.mockito.Mockito.mock(Location::class.java)
        whenever(mockLocation.latitude).thenReturn(30.2672)
        whenever(mockLocation.longitude).thenReturn(-97.7431)
        whenever(locationService.hasLocationPermission()).thenReturn(true)
        whenever(locationService.getCurrentLocation()).thenReturn(Result.success(mockLocation))
        whenever(weatherRepository.getWeatherByCoordinates(any(), any()))
            .thenReturn(Result.success(mockWeatherInfo))
        
        viewModel = createViewModel()
        advanceUntilIdle()

        // When: Permission is granted
        viewModel.onLocationPermissionGranted()
        advanceUntilIdle()

        // Then: Weather should be fetched by coordinates
        verify(weatherRepository).getWeatherByCoordinates(30.2672, -97.7431)
    }

    @Test
    fun `location permission denied should set flag and load last city`() = runTest {
        // Given: A saved city exists
        whenever(userPreferencesRepository.lastSearchedCity).thenReturn(flowOf("Austin"))
        whenever(weatherRepository.getWeatherByCity(any())).thenReturn(Result.success(mockWeatherInfo))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When: Permission is denied
        viewModel.onLocationPermissionDenied()
        advanceUntilIdle()

        // Then: Flag should be set
        assertTrue(viewModel.locationPermissionDenied.value)
    }

    @Test
    fun `refresh should re-fetch current weather`() = runTest {
        // Given: Initial successful search
        whenever(weatherRepository.getWeatherByCity(any())).thenReturn(Result.success(mockWeatherInfo))
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("Austin")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        // When: Refresh is called
        viewModel.refresh()
        advanceUntilIdle()

        // Then: Weather should be re-fetched
        // The city with country format is used for re-fetch
        verify(weatherRepository).getWeatherByCity("Austin, US")
    }

    @Test
    fun `hasLocationPermission should return location service value`() = runTest {
        // Given: Location service has permission
        whenever(locationService.hasLocationPermission()).thenReturn(true)
        viewModel = createViewModel()

        // When: Checking permission
        val hasPermission = viewModel.hasLocationPermission()

        // Then: Should return true
        assertTrue(hasPermission)
    }

    @Test
    fun `clearError should reset to Initial state when in Error state`() = runTest {
        // Given: ViewModel in Error state
        whenever(weatherRepository.getWeatherByCity(any()))
            .thenReturn(Result.failure(WeatherException.CityNotFound("Not found")))
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("InvalidCity")
        viewModel.onSearchSubmit()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is WeatherUiState.Error)

        // When: Error is cleared
        viewModel.clearError()

        // Then: State should be Initial
        assertTrue(viewModel.uiState.value is WeatherUiState.Initial)
    }

    @Test
    fun `state should be Loading during API call`() = runTest {
        // Given: Repository with delayed response
        whenever(weatherRepository.getWeatherByCity(any())).thenReturn(Result.success(mockWeatherInfo))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When: Search starts (using Turbine for Flow testing)
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)
            
            viewModel.onSearchQueryChange("Austin")
            viewModel.onSearchSubmit()
            
            // Then: Loading state should be emitted
            val loadingState = awaitItem()
            assertTrue(loadingState is WeatherUiState.Loading)
            
            // And then Success
            advanceUntilIdle()
            val successState = awaitItem()
            assertTrue(successState is WeatherUiState.Success)
            
            cancelAndConsumeRemainingEvents()
        }
    }
}

