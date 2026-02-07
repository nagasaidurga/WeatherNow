package com.example.weatherapplication

import com.example.weatherapplication.data.model.Clouds
import com.example.weatherapplication.data.model.Coordinates
import com.example.weatherapplication.data.model.MainWeatherData
import com.example.weatherapplication.data.model.Sys
import com.example.weatherapplication.data.model.Weather
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.model.Wind
import com.example.weatherapplication.util.WeatherMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WeatherMapper.
 * 
 * Tests verify correct data transformation from API response to UI model,
 * including:
 * - Temperature formatting
 * - Wind direction conversion
 * - Time formatting
 * - Icon URL generation
 * - String capitalization
 */
class WeatherMapperTest {

    private lateinit var mapper: WeatherMapper

    private val sampleResponse = WeatherResponse(
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
            temperature = 75.4,
            feelsLike = 77.8,
            tempMin = 70.1,
            tempMax = 80.9,
            pressure = 1015,
            humidity = 65
        ),
        visibility = 16093,
        wind = Wind(speed = 10.5, degrees = 315, gust = 15.0),
        clouds = Clouds(cloudiness = 5),
        dateTime = 1609459200,
        sys = Sys(
            type = 2,
            id = 12345,
            country = "US",
            sunrise = 1609416000,
            sunset = 1609455600
        ),
        timezone = -21600, // CST (UTC-6)
        cityId = 4671654,
        cityName = "Austin",
        responseCode = 200
    )

    @Before
    fun setup() {
        mapper = WeatherMapper()
    }

    @Test
    fun `mapToWeatherInfo should map city name correctly`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: City name should be mapped
        assertEquals("Austin", result.cityName)
    }

    @Test
    fun `mapToWeatherInfo should map country correctly`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Country should be mapped
        assertEquals("US", result.country)
    }

    @Test
    fun `mapToWeatherInfo should format temperature as integer with degree symbol`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Temperature should be rounded to integer with °F
        assertEquals("75°F", result.temperature)
    }

    @Test
    fun `mapToWeatherInfo should format feels like temperature`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Feels like should be formatted
        assertEquals("78°F", result.feelsLike) // 77.8 rounds to 78
    }

    @Test
    fun `mapToWeatherInfo should format min and max temperatures`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Min and max should be formatted
        assertEquals("70°F", result.tempMin)
        assertEquals("81°F", result.tempMax) // 80.9 rounds to 81
    }

    @Test
    fun `mapToWeatherInfo should format humidity with percent symbol`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Humidity should include %
        assertEquals("65%", result.humidity)
    }

    @Test
    fun `mapToWeatherInfo should format pressure with hPa unit`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Pressure should include hPa
        assertEquals("1015 hPa", result.pressure)
    }

    @Test
    fun `mapToWeatherInfo should format wind speed in mph`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Wind speed should be formatted
        assertEquals("11 mph", result.windSpeed) // 10.5 rounds to 11
    }

    @Test
    fun `mapToWeatherInfo should convert wind degrees to cardinal direction NW`() {
        // Given: Wind at 315 degrees (NW)
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Direction should be NW
        assertEquals("NW", result.windDirection)
    }

    @Test
    fun `mapToWeatherInfo should convert wind degrees to cardinal direction N`() {
        // Given: Wind at 0 degrees (N)
        val responseNorth = sampleResponse.copy(
            wind = Wind(speed = 10.0, degrees = 0, gust = null)
        )
        val result = mapper.mapToWeatherInfo(responseNorth)

        // Then: Direction should be N
        assertEquals("N", result.windDirection)
    }

    @Test
    fun `mapToWeatherInfo should convert wind degrees to cardinal direction E`() {
        // Given: Wind at 90 degrees (E)
        val responseEast = sampleResponse.copy(
            wind = Wind(speed = 10.0, degrees = 90, gust = null)
        )
        val result = mapper.mapToWeatherInfo(responseEast)

        // Then: Direction should be E
        assertEquals("E", result.windDirection)
    }

    @Test
    fun `mapToWeatherInfo should convert wind degrees to cardinal direction S`() {
        // Given: Wind at 180 degrees (S)
        val responseSouth = sampleResponse.copy(
            wind = Wind(speed = 10.0, degrees = 180, gust = null)
        )
        val result = mapper.mapToWeatherInfo(responseSouth)

        // Then: Direction should be S
        assertEquals("S", result.windDirection)
    }

    @Test
    fun `mapToWeatherInfo should capitalize weather description`() {
        // When: Mapping the response with lowercase description
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Description should be capitalized
        assertEquals("Clear Sky", result.description)
    }

    @Test
    fun `mapToWeatherInfo should set main condition`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Main condition should be mapped
        assertEquals("Clear", result.mainCondition)
    }

    @Test
    fun `mapToWeatherInfo should build correct icon URL`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Icon URL should be built with @2x for high resolution
        assertTrue(result.iconUrl.contains("01d@2x.png"))
        assertTrue(result.iconUrl.startsWith("https://"))
    }

    @Test
    fun `mapToWeatherInfo should format visibility in miles`() {
        // When: Mapping the response (16093 meters ≈ 10 miles)
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Visibility should be in miles
        assertEquals("10.0 mi", result.visibility)
    }

    @Test
    fun `mapToWeatherInfo should format cloudiness with percent`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Cloudiness should include %
        assertEquals("5%", result.cloudiness)
    }

    @Test
    fun `mapToWeatherInfo should handle missing weather data gracefully`() {
        // Given: Response with empty weather list
        val responseNoWeather = sampleResponse.copy(weather = emptyList())

        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(responseNoWeather)

        // Then: Should use default values
        assertEquals("Unknown", result.description)
        assertEquals("Unknown", result.mainCondition)
    }

    @Test
    fun `mapToWeatherInfo should format times correctly`() {
        // When: Mapping the response
        val result = mapper.mapToWeatherInfo(sampleResponse)

        // Then: Times should be formatted in 12-hour format
        assertTrue(result.sunrise.contains("AM") || result.sunrise.contains("PM"))
        assertTrue(result.sunset.contains("AM") || result.sunset.contains("PM"))
        assertTrue(result.lastUpdated.contains("AM") || result.lastUpdated.contains("PM"))
    }
}

