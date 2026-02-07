package com.example.weatherapplication.di

import android.content.Context
import com.example.weatherapplication.location.LocationService
import com.example.weatherapplication.location.LocationServiceImpl
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing location-related dependencies.
 * Separates location concerns into its own module for better organization.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    /**
     * Provides FusedLocationProviderClient for accessing device location.
     * Uses Google Play Services Location API for best accuracy and battery efficiency.
     */
    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}

/**
 * Separate module for binding location service interface.
 * Split from LocationModule due to @Binds requiring abstract module.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationServiceModule {

    /**
     * Binds LocationService interface to implementation.
     */
    @Binds
    @Singleton
    abstract fun bindLocationService(
        locationServiceImpl: LocationServiceImpl
    ): LocationService
}

