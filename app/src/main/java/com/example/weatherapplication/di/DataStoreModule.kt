package com.example.weatherapplication.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.weatherapplication.data.preferences.UserPreferencesRepository
import com.example.weatherapplication.data.preferences.UserPreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Extension property to create DataStore instance.
 * Using delegate pattern ensures single DataStore instance.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "weather_preferences"
)

/**
 * Hilt module providing DataStore dependencies for persisting user preferences.
 * Uses Jetpack DataStore instead of SharedPreferences for:
 * - Type safety
 * - Coroutines support (async operations)
 * - Data consistency guarantees
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Provides DataStore instance for preferences storage.
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}

/**
 * Separate module for binding preferences repository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    /**
     * Binds UserPreferencesRepository to its implementation.
     */
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository
}

