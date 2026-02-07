package com.example.weatherapplication

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import java.io.File

/**
 * Application class for Weather Application.
 * 
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * This triggers Hilt's code generation and sets up the dependency container.
 * 
 * Also implements ImageLoaderFactory to configure Coil image loading with caching.
 */
@HiltAndroidApp
class WeatherApplication : Application(), ImageLoaderFactory {

    /**
     * Creates a custom ImageLoader with optimized caching for weather icons.
     * 
     * Caching strategy:
     * - Memory cache: 25% of available memory for fast access
     * - Disk cache: 50MB for offline access and reduced API calls
     * - Network results are cached to disk for future use
     * 
     * Weather icons from OpenWeatherMap are relatively small and stable,
     * so aggressive caching is beneficial.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Memory cache configuration
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            // Disk cache configuration
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "weather_images"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            // Cache policies
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Cross-fade animation for smooth loading
            .crossfade(true)
            .crossfade(300)
            // Enable logging in debug builds
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}

