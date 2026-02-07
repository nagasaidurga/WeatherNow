🌦️ WeatherNow – Android MVVM Weather App

WeatherNow is a production-style Android weather application built to demonstrate modern Android architecture, clean code principles, and best practices in networking, state management, and runtime permissions handling.

The app allows users to search for a US city, view real-time weather conditions, automatically load their last searched city, and fetch weather using device location when permission is granted.

✨ Features
🔎 City Weather Search

Users can search for any US city to get live weather updates using the OpenWeatherMap API.

The app displays:

Temperature (°C / °F)

Weather condition (Clear, Clouds, Rain, etc.)

Humidity

Wind Speed

Feels Like temperature

Atmospheric Pressure

Dynamic weather icon from API

📍 Location-Based Weather

Requests location permission at runtime

If permission is granted, the app automatically loads weather for the current location

If denied, the app falls back to the last searched city

🔄 Auto-Load Last City

The last searched city is stored locally using DataStore / SharedPreferences

Weather for this city automatically loads when the app launches

🖼 Weather Icon Handling

Weather icons are fetched dynamically from the API

Efficient image loading with caching for smooth performance

⚠️ Robust Error Handling

The app gracefully handles:

Network failures

Invalid city names

API errors

Location permission denial

All errors are shown with clear, user-friendly messages.

🏗 Architecture

The application follows the MVVM (Model–View–ViewModel) pattern to ensure scalability, maintainability, and testability.

Architecture Flow:

UI (Activity / Compose)
        ↓
ViewModel (State + Business Logic)
        ↓
Repository (Data abstraction)
        ↓
Remote Data Source (Retrofit API)
        ↓
OpenWeatherMap API

🛠 Tech Stack
Layer	Technology
Language	Kotlin
Architecture	MVVM
Networking	Retrofit
JSON Parsing	Moshi / Gson
Concurrency	Kotlin Coroutines
Dependency Injection	Hilt
UI	XML / Jetpack Compose
Navigation	Jetpack Navigation
Image Loading	Coil / Glide
Local Storage	DataStore / SharedPreferences
Permissions	Android Runtime Permissions API
