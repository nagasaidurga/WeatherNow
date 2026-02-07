package com.example.weatherapplication

import com.example.weatherapplication.data.api.WeatherApiService
import com.example.weatherapplication.data.model.Clouds
import com.example.weatherapplication.data.model.Coordinates
import com.example.weatherapplication.data.model.MainWeatherData
import com.example.weatherapplication.data.model.Sys
import com.example.weatherapplication.data.model.Weather
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.model.Wind
import com.example.weatherapplication.data.repository.WeatherException
import com.example.weatherapplication.data.repository.WeatherRepositoryImpl
import com.example.weatherapplication.util.WeatherMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import retrofit2.Response

/**
 * Unit tests for WeatherRepository implementation.
 * 
 * Tests cover:
 * - Successful API responses
 * - Various HTTP error codes (404, 401, 429, 500)
 * - Network errors
 * - City name formatting for US cities
 * - Coordinate-based weather fetching
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryTest {

    @Mock
    private lateinit var apiService: WeatherApiService

    private lateinit var weatherMapper: WeatherMapper
    private lateinit var repository: WeatherRepositoryImpl

    private val mockWeatherResponse = WeatherResponse(
        coordinates = Coordinates(longitude = -97.7431, latitude = 30.2672),
        weather = listOf(
            Weather(
                id = 800,
                main = "Clear",
                description = "clear sky",
                icon = "01d"
            )
        ),
        base = "stations",
        main = MainWeatherData(
            temperature = 75.0,
            feelsLike = 77.0,
            tempMin = 70.0,
            tempMax = 80.0,
            pressure = 1015,
            humidity = 65
        ),
        visibility = 16093,
        wind = Wind(speed = 10.0, degrees = 315, gust = 15.0),
        clouds = Clouds(cloudiness = 5),
        dateTime = 1609459200,
        sys = Sys(
            type = 2,
            id = 12345,
            country = "US",
            sunrise = 1609416000,
            sunset = 1609455600
        ),
        timezone = -21600,
        cityId = 4671654,
        cityName = "Austin",
        responseCode = 200
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        weatherMapper = WeatherMapper()
        repository = WeatherRepositoryImpl(apiService, weatherMapper)
    }

    @Test
    fun `getWeatherByCity should return success for valid city`() = runTest {
        // Given: API returns successful response
        whenever(apiService.getWeatherByCity(any(), any(), any()))
            .thenReturn(Response.success(mockWeatherResponse))

        // When: Fetching weather by city
        val result = repository.getWeatherByCity("Austin")

        // Then: Result should be success with correct data
        assertTrue(result.isSuccess)
        assertEquals("Austin", result.getOrNull()?.cityName)
        assertEquals("US", result.getOrNull()?.country)
    }

    @Test
    fun `getWeatherByCity should append US country code for single city name`() = runTest {
        // Given: API returns successful response
        whenever(apiService.getWeatherByCity(eq("Austin,US"), any(), any()))
            .thenReturn(Response.success(mockWeatherResponse))

        // When: Fetching weather by city without country code
        val result = repository.getWeatherByCity("Austin")

        // Then: Result should be success (proves US was appended)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getWeatherByCity should append US for city with state`() = runTest {
        // Given: API returns successful response
        whenever(apiService.getWeatherByCity(eq("Austin,TX,US"), any(), any()))
            .thenReturn(Response.success(mockWeatherResponse))

        // When: Fetching weather by city with state
        val result = repository.getWeatherByCity("Austin,TX")

        // Then: Result should be success (proves US was appended)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getWeatherByCity should not modify city name with country code`() = runTest {
        // Given: API returns successful response
        whenever(apiService.getWeatherByCity(eq("Austin,TX,US"), any(), any()))
            .thenReturn(Response.success(mockWeatherResponse))

        // When: Fetching weather by city with full format
        val result = repository.getWeatherByCity("Austin,TX,US")

        // Then: Result should be success
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getWeatherByCity should return CityNotFound for 404 response`() = runTest {
        // Given: API returns 404
        whenever(apiService.getWeatherByCity(any(), any(), any()))
            .thenReturn(Response.error(404, "Not found".toResponseBody()))

        // When: Fetching weather
        val result = repository.getWeatherByCity("InvalidCity")

        // Then: Result should be CityNotFound failure
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WeatherException.CityNotFound)
    }

    @Test
    fun `getWeatherByCity should return ApiKeyError for 401 response`() = runTest {
        // Given: API returns 401
        whenever(apiService.getWeatherByCity(any(), any(), any()))
            .thenReturn(Response.error(401, "Unauthorized".toResponseBody()))

        // When: Fetching weather
        val result = repository.getWeatherByCity("Austin")

        // Then: Result should be ApiKeyError failure
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WeatherException.ApiKeyError)
    }

    @Test
    fun `getWeatherByCity should return RateLimitExceeded for 429 response`() = runTest {
        // Given: API returns 429
        whenever(apiService.getWeatherByCity(any(), any(), any()))
            .thenReturn(Response.error(429, "Too many requests".toResponseBody()))

        // When: Fetching weather
        val result = repository.getWeatherByCity("Austin")

        // Then: Result should be RateLimitExceeded failure
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WeatherException.RateLimitExceeded)
    }

    @Test
    fun `getWeatherByCity should return ServerError for 500 response`() = runTest {
        // Given: API returns 500
        whenever(apiService.getWeatherByCity(any(), any(), any()))
            .thenReturn(Response.error(500, "Internal server error".toResponseBody()))

        // When: Fetching weather
        val result = repository.getWeatherByCity("Austin")

        // Then: Result should be ServerError failure
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WeatherException.ServerError)
    }

    @Test
    fun `getWeatherByCity should return NetworkError for exception`() = runTest {
        // Given: API throws exception
        whenever(apiService.getWeatherByCity(any(), any(), any()))
            .thenThrow(RuntimeException("Network error"))

        // When: Fetching weather
        val result = repository.getWeatherByCity("Austin")

        // Then: Result should be NetworkError failure
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WeatherException.NetworkError)
    }

    @Test
    fun `getWeatherByCoordinates should return success for valid coordinates`() = runTest {
        // Given: API returns successful response
        whenever(apiService.getWeatherByCoordinates(any(), any(), any(), any()))
            .thenReturn(Response.success(mockWeatherResponse))

        // When: Fetching weather by coordinates
        val result = repository.getWeatherByCoordinates(30.2672, -97.7431)

        // Then: Result should be success with correct data
        assertTrue(result.isSuccess)
        assertEquals("Austin", result.getOrNull()?.cityName)
    }

    @Test
    fun `getWeatherByCoordinates should return NetworkError for exception`() = runTest {
        // Given: API throws exception
        whenever(apiService.getWeatherByCoordinates(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Network error"))

        // When: Fetching weather
        val result = repository.getWeatherByCoordinates(30.2672, -97.7431)

        // Then: Result should be NetworkError failure
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WeatherException.NetworkError)
    }

    @Test
    fun `getWeatherByCity should return EmptyResponse for null body`() = runTest {
        // Given: API returns success with null body
        whenever(apiService.getWeatherByCity(any(), any(), any()))
            .thenReturn(Response.success(null))

        // When: Fetching weather
        val result = repository.getWeatherByCity("Austin")

        // Then: Result should be EmptyResponse failure
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WeatherException.EmptyResponse)
    }
}

