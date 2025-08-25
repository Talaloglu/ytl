# Movie App - Android Kotlin Project

A modern Android application built with Kotlin and Jetpack Compose for browsing movies using The Movie Database (TMDB) API.

## 🚀 Features

- **MVVM Architecture**: Clean separation of concerns using Model-View-ViewModel pattern
- **Jetpack Compose**: Modern declarative UI toolkit for Android
- **Retrofit**: Type-safe HTTP client for REST API consumption
- **Coil**: Image loading library for efficient image handling
- **Navigation Component**: Jetpack Navigation for screen transitions
- **Material Design 3**: Latest Material Design components and theming
- **Coroutines & Flow**: Asynchronous programming with StateFlow for reactive UI

## 📋 Prerequisites

Before running this project, ensure you have:

1. **Android Studio**: Latest version (recommended: Android Studio Hedgehog or newer)
2. **Kotlin**: Version 1.9.22 or higher
3. **Android SDK**: API level 24 (Android 7.0) or higher
4. **TMDB API Key**: Get your free API key from [The Movie Database](https://www.themoviedb.org/settings/api)

## 🛠️ Setup Instructions

### 1. Clone/Download the Project
- Download or clone this project to your local machine
- Open Android Studio
- Select "Open an Existing Project" and navigate to the project folder

### 2. Configure API Key
1. Open `MovieRepository.kt` file located at:
   ```
   app/src/main/java/com/movieapp/data/repository/MovieRepository.kt
   ```
2. Replace `YOUR_API_KEY_HERE` with your actual TMDB API key:
   ```kotlin
   private val apiKey = "your_actual_api_key_here"
   ```

### 3. Sync Project
- Android Studio will automatically detect the `build.gradle` files
- Click "Sync Project with Gradle Files" when prompted
- Wait for the sync to complete

### 4. Build and Run
- Connect an Android device or start an emulator
- Click the "Run" button (green play icon) in Android Studio
- Select your target device and click "OK"

## 📁 Project Structure

```
app/src/main/java/com/movieapp/
├── data/
│   ├── api/
│   │   ├── MovieApiService.kt      # API endpoint definitions
│   │   └── RetrofitInstance.kt     # Retrofit configuration
│   ├── model/
│   │   ├── Movie.kt                # Movie data model
│   │   └── MovieResponse.kt        # API response wrapper
│   └── repository/
│       └── MovieRepository.kt      # Data repository layer
├── domain/                         # Business logic (future expansion)
├── ui/
│   ├── components/                 # Reusable UI components
│   ├── navigation/
│   │   └── MovieAppNavigation.kt   # Navigation setup
│   ├── screens/                    # Individual screen composables
│   └── theme/
│       ├── Color.kt                # App color definitions
│       ├── Theme.kt                # Material Design 3 theme
│       └── Type.kt                 # Typography definitions
├── viewmodel/
│   └── MovieViewModel.kt           # ViewModel for state management
└── MainActivity.kt                 # Main entry point
```

## 🔧 Dependencies

The project includes the following key dependencies:

### Core Android
- `androidx.core:core-ktx` - Android KTX extensions
- `androidx.lifecycle:lifecycle-runtime-ktx` - Lifecycle components
- `androidx.activity:activity-compose` - Activity Compose integration

### Jetpack Compose
- `androidx.compose:compose-bom` - Compose Bill of Materials
- `androidx.compose.ui:ui` - Core Compose UI
- `androidx.compose.material3:material3` - Material Design 3 components

### Architecture Components
- `androidx.lifecycle:lifecycle-viewmodel-compose` - ViewModel integration
- `androidx.navigation:navigation-compose` - Navigation component

### Network & Data
- `com.squareup.retrofit2:retrofit` - HTTP client
- `com.squareup.retrofit2:converter-gson` - JSON converter
- `com.google.code.gson:gson` - JSON parsing

### Image Loading
- `io.coil-kt:coil-compose` - Image loading for Compose

### Async Programming
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` - Coroutines support

## 🎯 API Endpoints

The app integrates with TMDB API and supports:

- **Popular Movies**: `/movie/popular`
- **Top Rated Movies**: `/movie/top_rated`
- **Now Playing**: `/movie/now_playing`
- **Upcoming Movies**: `/movie/upcoming`
- **Movie Details**: `/movie/{movie_id}`
- **Search Movies**: `/search/movie`

## 🎨 UI Components

### Current Screens
- **Home Screen**: Landing page with movie categories
- **Movie Detail Screen**: Detailed view of selected movie
- **Search Screen**: Search functionality for movies

### Future Enhancements
- Movie listing with categories
- Detailed movie information display
- Search functionality with filters
- Favorites management
- User reviews and ratings

## 🔐 Permissions

The app requires the following permissions:
- `INTERNET`: For API calls to TMDB
- `ACCESS_NETWORK_STATE`: For network state monitoring

## 🚀 Getting Started for Development

1. **Run the App**: Follow the setup instructions above
2. **Add Features**: Extend the UI screens in the `ui/screens` package
3. **Add Components**: Create reusable components in `ui/components`
4. **Extend API**: Add new endpoints in `MovieApiService.kt`
5. **Update Models**: Modify data models in the `data/model` package

## 📱 Minimum Requirements

- **Android API Level**: 24 (Android 7.0)
- **Target API Level**: 34 (Android 14)
- **Kotlin Version**: 1.9.22
- **Compose Compiler**: 1.5.8

## 🛠️ Build Configuration

- **Compile SDK**: 34
- **Min SDK**: 24
- **Target SDK**: 34
- **Java Version**: 1.8

## 📖 Next Steps

After setting up the project:

1. **Get TMDB API Key**: Register at [TMDB](https://www.themoviedb.org/) and obtain your API key
2. **Configure API Key**: Update the `MovieRepository.kt` file with your key
3. **Build & Run**: Sync project and run on device/emulator
4. **Customize**: Modify themes, add new features, or extend functionality

## 🤝 Contributing

Feel free to contribute to this project by:
- Adding new features
- Improving UI/UX
- Fixing bugs
- Adding tests
- Improving documentation

## 📄 License

This project is for educational purposes. Please ensure you comply with TMDB's terms of service when using their API.

---

**Happy Coding! 🎬✨**