package com.example.weatherapplication.di

import com.example.weatherapplication.data.repository.WeatherRepository
import com.example.weatherapplication.data.repository.WeatherRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations.
 * Uses @Binds for efficient interface-implementation binding.
 * 
 * This separation allows for:
 * - Easy mocking in tests
 * - Swapping implementations without changing consumers
 * - Proper dependency inversion (SOLID principles)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds WeatherRepository interface to its implementation.
     * The implementation is provided as a Singleton to share weather data caching.
     */
    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        weatherRepositoryImpl: WeatherRepositoryImpl
    ): WeatherRepository
}

