# 🔍 Streaming URL Matching Verification Guide

## ✅ What Should Happen (Correct Behavior)

Your categorized home screen sections should **only show movies that exist in both TMDB and Supabase** with valid video URLs.

### Expected Flow:
1. **TMDB API Call** → Gets movie data (trending, popular, year-based, genre-based)
2. **Supabase API Call** → Gets your movies with video URLs  
3. **Title Matching** → Matches TMDB titles with Supabase titles
4. **URL Validation** → Only includes movies with valid HTTP/HTTPS URLs
5. **Combined Results** → Returns CombinedMovie objects with TMDB metadata + Supabase video URLs

### Sections That Use Streaming URL Matching:
- ✅ **Trending Section** → `getTrendingMovies()`
- ✅ **Popular Section** → `getPopularMovies()`
- ✅ **Top Rated Section** → `getTopRatedMovies()`
- ✅ **2024 Movies Section** → `getMoviesByYear(2024)`
- ✅ **2023 Movies Section** → `getMoviesByYear(2023)`
- ✅ **Horror Section** → `getMoviesByGenre(HORROR_GENRE_ID)`
- ✅ **Action Section** → `getMoviesByGenre(ACTION_GENRE_ID)`
- ✅ **Comedy Section** → `getMoviesByGenre(COMEDY_GENRE_ID)`
- ✅ **Drama Section** → `getMoviesByGenre(DRAMA_GENRE_ID)`

## 🚨 If You're Seeing Non-Streaming Movies

This indicates a **compilation issue**, not a logic issue. Here's how to fix it:

### Step 1: Run Project Refresh
```bash
# In project root directory
.\refresh_project.bat
```

### Step 2: Check Supabase Data
Ensure your Supabase movies table has movies with exact TMDB titles:
```sql
-- Example of correct data
INSERT INTO movies (id, title, videourl, duration) VALUES
('1', 'Fight Club', 'https://example.com/fight-club.mp4', '139 min'),
('2', 'The Dark Knight', 'https://example.com/dark-knight.mp4', '152 min'),
('3', 'Inception', 'https://example.com/inception.mp4', '148 min');
```

### Step 3: Verify API Configuration
Check `app/src/main/java/com/movieapp/utils/ApiConfig.kt`:
```kotlin
const val SUPABASE_URL = "https://your-project.supabase.co"
const val SUPABASE_ANON_KEY = "your-anon-key"
const val TMDB_API_KEY = "your-tmdb-key"
```

## 🔍 Debugging Steps

### Check 1: Repository Usage
Verify ViewModels are using `CombinedMovieRepository`:
- ✅ `CategorizedHomeViewModel` → Uses `CombinedMovieRepository()`
- ✅ `CategoryDetailViewModel` → Uses `CombinedMovieRepository()`
- ❌ `MovieViewModel` → Uses `MovieRepository()` (for other screens)

### Check 2: Data Flow
1. **Repository Methods** → Return `Result<List<CombinedMovie>>`
2. **ViewModel State** → `StateFlow<List<CombinedMovie>>`
3. **UI Components** → Receive `CombinedMovie` objects

### Check 3: URL Validation
Enhanced validation now checks:
- ✅ URL is not blank
- ✅ URL is not empty after trimming
- ✅ URL starts with `http://` or `https://`

## 🎯 Final Verification

After running the refresh script, you should see:

1. **Empty Sections** → If no movies in Supabase match TMDB titles
2. **Populated Sections** → Only with movies that have valid streaming URLs
3. **No Mixed Content** → All displayed movies should be streamable

### If Sections Are Empty:
- Add more movies to Supabase with exact TMDB titles
- Check network connectivity
- Verify API keys are correct

### If Still Seeing Non-Streaming Movies:
- Clear app data/cache
- Rebuild project completely
- Check for any cached fallback data

## 📱 User Experience

**Before Fix**: Sections show both streaming and non-streaming movies
**After Fix**: Sections show only movies with valid Supabase video URLs

The categorized home screen will now be a **true streaming catalog** showing only your available content! 🎬🍿