# 🎬 Movie App Supabase Integration Setup

## 📋 Overview
This guide helps you integrate Supabase with your Movie App to enable video streaming functionality. The app will show only movies that exist in your Supabase database with valid video URLs, while fetching all other movie data from TMDB.

**New Architecture**: 
- **TMDB**: Provides all movie metadata (title, overview, poster, rating, etc.)
- **Supabase**: Provides only video URLs for streaming
- **Matching**: Movies are matched by title (case-insensitive)
- **Filtering**: Only shows movies that have video URLs in Supabase

## 🚀 Quick Setup

### 1. Supabase Project Setup

1. **Create Supabase Project:**
   - Go to [https://supabase.com/](https://supabase.com/)
   - Create a new project
   - Note your Project URL and Anon Key

2. **Update API Configuration:**
   - Open `app/src/main/java/com/movieapp/utils/ApiConfig.kt`
   - Replace the placeholder values:
   ```kotlin
   const val SUPABASE_URL = "YOUR_SUPABASE_PROJECT_URL"
   const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"
   ```

### 2. Database Schema

Your existing `movies` table schema is already configured:

```sql
CREATE TABLE IF NOT EXISTS movies (
id TEXT PRIMARY KEY,
title TEXT NOT NULL,
thumbnailurl TEXT,
videourl TEXT NOT NULL,
duration TEXT,
category TEXT,
subcategory TEXT,
description TEXT,
viewcount INTEGER DEFAULT 0,
publishedat TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
isfavorite BOOLEAN DEFAULT FALSE
);
```

**Important Notes:**
- The app matches TMDB movies with your Supabase movies using the `title` field
- Only movies with non-null `videourl` will be shown as streamable
- All other movie data (poster, rating, overview, etc.) comes from TMDB
- Ensure your movie titles in Supabase match TMDB movie titles exactly for best results
```

### 3. Sample Data

Add some sample movies to test the integration. **Important**: Make sure the titles match TMDB movie titles exactly:

```sql
-- Insert sample movies (use exact TMDB titles)
INSERT INTO movies (id, title, videourl, duration, category, description) VALUES
('1', 'Fight Club', 'https://your-cdn.com/movies/fight-club.mp4', '139 min', 'Drama', 'A depressed man fights his insomnia by attending support groups.'),
('2', 'Forrest Gump', 'https://your-cdn.com/movies/forrest-gump.mp4', '142 min', 'Drama', 'The presidencies of Kennedy and Johnson through the eyes of an Alabama man.'),
('3', 'Pulp Fiction', 'https://your-cdn.com/movies/pulp-fiction.mp4', '154 min', 'Crime', 'The lives of two mob hitmen, a boxer, and others intertwine.'),
('4', 'The Dark Knight', 'https://your-cdn.com/movies/dark-knight.mp4', '152 min', 'Action', 'Batman faces the Joker in Gotham City.'),
('5', 'Inception', 'https://your-cdn.com/movies/inception.mp4', '148 min', 'Sci-Fi', 'A thief who steals corporate secrets through dream-sharing technology.');
```

## 🛠 Advanced Configuration

### Row Level Security Policies

For production, you might want more restrictive policies:

```sql
-- Policy for specific user access
CREATE POLICY "Users can only see their movies" ON movies
    FOR SELECT USING (auth.uid() = user_id);

-- Policy for admin access
CREATE POLICY "Admins can manage all movies" ON movies
    FOR ALL USING (auth.jwt() ->> 'role' = 'admin');
```

### Custom Functions

Create a function to search movies:

```sql
-- Function to search movies with full-text search
CREATE OR REPLACE FUNCTION search_movies(search_term TEXT)
RETURNS SETOF movies AS $$
BEGIN
    RETURN QUERY
    SELECT * FROM movies
    WHERE is_active = true
    AND (
        title ILIKE '%' || search_term || '%'
        OR to_tsvector('english', title) @@ plainto_tsquery('english', search_term)
    )
    ORDER BY title;
END;
$$ LANGUAGE plpgsql;
```

## 📱 App Usage

### Navigation Updates

Add the new screens to your navigation:

```kotlin
// In MovieAppNavigation.kt, add these routes:
composable("streaming_home") {
    StreamingHomeScreen(
        onMovieClick = { movieId ->
            navController.navigate("movie_player/$movieId")
        },
        onWatchClick = { movie ->
            navController.navigate("movie_player/${movie.id}")
        }
    )
}

composable(
    "movie_player/{movieId}",
    arguments = listOf(navArgument("movieId") { type = NavType.IntType })
) { backStackEntry ->
    val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
    MoviePlayerScreen(
        movieId = movieId,
        onBackClick = { navController.popBackStack() }
    )
}
```

### ViewModel Integration

Use the StreamingViewModel in your screens:

```kotlin
@Composable
fun YourScreen() {
    val streamingViewModel: StreamingViewModel = viewModel()
    
    // Load streaming movies
    LaunchedEffect(Unit) {
        streamingViewModel.loadStreamingMovies()
    }
    
    // Observe streaming movies
    val streamingMovies by streamingViewModel.streamingMovies.collectAsState()
    
    // Use the movies in your UI
}
```

## 🔧 Data Flow

1. **App starts** → Loads popular movies from TMDB API
2. **In parallel** → Loads all movies with video URLs from Supabase
3. **Title matching** → Matches TMDB movies with Supabase movies by title (case-insensitive)
4. **Filtering** → Shows only TMDB movies that have matching video URLs in Supabase
5. **Data combination** → Creates `CombinedMovie` with TMDB metadata + Supabase video URL
6. **User clicks Watch** → Opens `MoviePlayerScreen` with video URL from Supabase

**Key Benefits:**
- ✅ **Full TMDB Data**: Rich movie metadata (posters, ratings, overviews)
- ✅ **Your Video Content**: Only your curated movies with video URLs
- ✅ **Flexible Matching**: No need to maintain TMDB IDs in your database
- ✅ **Easy Management**: Just add movies with exact titles to Supabase

## 🎯 Key Features

- ✅ **Hybrid Content**: TMDB metadata + Your video URLs
- ✅ **Title-Based Matching**: No need to maintain TMDB IDs
- ✅ **Filtered Content**: Only shows movies available in your database
- ✅ **Rich Metadata**: Full TMDB info (posters, ratings, cast, etc.)
- ✅ **Search**: Search through available movies
- ✅ **Duration Display**: Shows video duration from your database
- ✅ **Category Support**: Organize movies by category/subcategory
- ✅ **Error Handling**: Graceful handling of missing data
- ✅ **Type Safety**: Full Kotlin type safety with navigation

## 🔍 Troubleshooting

### Common Issues:

1. **"No streaming movies available"**
   - Check if movies exist in Supabase table
   - Verify `is_active = true`
   - Check API keys configuration

2. **"Failed to fetch Supabase movies"**
   - Verify Supabase URL and API key
   - Check Row Level Security policies
   - Ensure table exists with correct schema

3. **Movies show but no TMDB details**
   - Verify TMDB API key is valid
   - Check `tmdb_id` values in database
   - Ensure network connectivity

4. **Video player not working**
   - This is a placeholder implementation
   - Integrate with ExoPlayer or similar for actual video playback
   - Ensure streaming URLs are valid and accessible

## 📦 Dependencies Added

The integration adds these dependencies:
- `io.github.jan-tennert.supabase:postgrest-kt:2.0.4`
- `io.github.jan-tennert.supabase:realtime-kt:2.0.4`
- `io.ktor:ktor-client-android:2.3.5`
- `io.ktor:ktor-client-core:2.3.5`
- `io.ktor:ktor-utils:2.3.5`

## 🚀 Next Steps

1. **Set up your Supabase project and database**
2. **Update API configuration with your credentials**
3. **Add sample movie data to test**
4. **Integrate video player (ExoPlayer recommended)**
5. **Add authentication for admin features**
6. **Implement movie management (add/edit/delete)**

Your Movie App now shows only movies with streaming URLs from your Supabase database while maintaining all TMDB metadata and functionality!