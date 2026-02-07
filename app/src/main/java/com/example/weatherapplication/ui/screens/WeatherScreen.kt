package com.example.weatherapplication.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.weatherapplication.data.model.WeatherUiState
import com.example.weatherapplication.ui.components.ErrorContent
import com.example.weatherapplication.ui.components.InitialContent
import com.example.weatherapplication.ui.components.LoadingContent
import com.example.weatherapplication.ui.components.LocationPermissionContent
import com.example.weatherapplication.ui.components.WeatherDetailsGrid
import com.example.weatherapplication.ui.components.WeatherDisplay
import com.example.weatherapplication.ui.components.WeatherSearchBar
import com.example.weatherapplication.ui.theme.DeepBlue
import com.example.weatherapplication.ui.theme.SkyBlueLight
import com.example.weatherapplication.ui.viewmodel.WeatherViewModel

/**
 * Main weather screen composable.
 * Displays search bar, weather information, and handles location permissions.
 * 
 * Features:
 * - City search with auto-load of last searched city
 * - Current location weather (requires permission)
 * - Pull-to-refresh for weather updates
 * - Responsive state handling (loading, error, success)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val locationPermissionDenied by viewModel.locationPermissionDenied.collectAsState()

    // Track if we should show the location permission prompt
    var showLocationPermissionPrompt by remember { mutableStateOf(false) }
    var hasCheckedPermission by remember { mutableStateOf(false) }

    // Pull to refresh state
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.onLocationPermissionGranted()
        } else {
            viewModel.onLocationPermissionDenied()
        }
        showLocationPermissionPrompt = false
    }

    // Check if we should show location permission prompt on first launch
    LaunchedEffect(Unit) {
        if (!hasCheckedPermission) {
            hasCheckedPermission = true
            if (!viewModel.hasLocationPermission() && !locationPermissionDenied) {
                showLocationPermissionPrompt = true
            }
        }
    }

    // Handle refresh completion
    LaunchedEffect(uiState) {
        if (isRefreshing && uiState !is WeatherUiState.Loading) {
            isRefreshing = false
        }
    }

    // Background gradient based on time of day (could be enhanced with actual sunset/sunrise times)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refresh()
        },
        state = pullToRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 32.dp)
        ) {
            // Search bar - always visible at top
            WeatherSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = viewModel::onSearchSubmit,
                onLocationClick = {
                    if (viewModel.hasLocationPermission()) {
                        viewModel.fetchWeatherByLocation()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                showLocationButton = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Content based on state - removed weight(1f) to allow proper scrolling
            when {
                // Show location permission prompt if needed
                showLocationPermissionPrompt && uiState is WeatherUiState.Initial -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LocationPermissionContent(
                            onRequestPermission = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            onSkip = {
                                showLocationPermissionPrompt = false
                                viewModel.onLocationPermissionDenied()
                            }
                        )
                    }
                }

                // Loading state
                uiState is WeatherUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingContent()
                    }
                }

                // Error state
                uiState is WeatherUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorContent(
                            message = (uiState as WeatherUiState.Error).message,
                            onRetry = viewModel::refresh
                        )
                    }
                }

                // Success state - show weather (fully scrollable)
                uiState is WeatherUiState.Success -> {
                    val weatherInfo = (uiState as WeatherUiState.Success).weatherInfo
                    
                    WeatherDisplay(
                        weatherInfo = weatherInfo,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    WeatherDetailsGrid(
                        weatherInfo = weatherInfo,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bottom padding for scroll overscroll area
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Initial state - show search prompt
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        InitialContent()
                    }
                }
            }
        }
    }
}

