package com.example.weatherapplication.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for user preferences operations.
 * Handles persistence of user settings like last searched city.
 */
interface UserPreferencesRepository {
    /**
     * Flow of the last searched city name.
     * Emits null if no city has been searched yet.
     */
    val lastSearchedCity: Flow<String?>

    /**
     * Saves the last searched city name.
     * @param cityName The city name to persist
     */
    suspend fun saveLastSearchedCity(cityName: String)

    /**
     * Clears the last searched city.
     * Called when user explicitly wants to reset.
     */
    suspend fun clearLastSearchedCity()
}

/**
 * Implementation using Jetpack DataStore for preferences.
 * 
 * DataStore advantages over SharedPreferences:
 * - Asynchronous API (won't block UI thread)
 * - Type safety with Preferences keys
 * - Data consistency with transactional API
 * - Error handling via Kotlin Flow
 * 
 * @property dataStore Jetpack DataStore instance
 */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    /**
     * Keys for stored preferences.
     * Using companion object to ensure key consistency.
     */
    private companion object PreferencesKeys {
        val LAST_SEARCHED_CITY = stringPreferencesKey("last_searched_city")
    }

    /**
     * Flow that emits the last searched city whenever it changes.
     * Returns null if no city has been saved.
     * 
     * This Flow is observed by the ViewModel to:
     * - Auto-load weather on app launch
     * - Keep UI in sync with stored data
     */
    override val lastSearchedCity: Flow<String?>
        get() = dataStore.data.map { preferences ->
            preferences[LAST_SEARCHED_CITY]
        }

    /**
     * Saves the city name to DataStore.
     * Uses DataStore's edit function which provides:
     * - Atomic updates
     * - Automatic persistence
     * - Exception handling
     */
    override suspend fun saveLastSearchedCity(cityName: String) {
        dataStore.edit { preferences ->
            preferences[LAST_SEARCHED_CITY] = cityName
        }
    }

    /**
     * Clears the stored city name.
     */
    override suspend fun clearLastSearchedCity() {
        dataStore.edit { preferences ->
            preferences.remove(LAST_SEARCHED_CITY)
        }
    }
}

