package com.example.weatherapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.weatherapplication.ui.screens.WeatherScreen
import com.example.weatherapplication.ui.theme.WeatherApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the Weather Application.
 * 
 * Annotated with @AndroidEntryPoint to enable Hilt injection in this Activity.
 * Uses Jetpack Compose for UI rendering with edge-to-edge display support.
 * 
 * The activity delegates all UI and business logic to Composable functions
 * and ViewModels, following the single-activity architecture pattern.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display for modern Android UI
        enableEdgeToEdge()
        
        setContent {
            WeatherApplicationTheme {
                // Surface provides the background color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main weather screen with all functionality
                    WeatherScreen()
                }
            }
        }
    }
}
