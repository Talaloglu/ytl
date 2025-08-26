package com.movieapp.data.repository

import com.google.gson.Gson
import com.movieapp.data.api.SupabaseApiInterface
import com.movieapp.data.local.CachedMovieDao
import com.movieapp.data.local.CachedMovieEntity
import com.movieapp.data.model.CombinedMovie
import com.movieapp.data.model.Movie
import com.movieapp.data.model.SupabaseEnrichedMovie
import com.movieapp.data.model.SupabaseMovie
import com.movieapp.utils.SupabaseRetrofitInstance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CombinedMovieRepositoryTest {

    private lateinit var supabaseApi: SupabaseApiInterface
    private lateinit var dao: CachedMovieDao
    private lateinit var gson: Gson
    private lateinit var repo: CombinedMovieRepository

    @Before
    fun setUp() {
        supabaseApi = mock()
        dao = mock()
        gson = Gson()
        repo = CombinedMovieRepository(
            supabaseApi = supabaseApi,
            cachedDao = dao,
            gson = gson
        )
    }

    @Test
    fun getAvailableMovies_networkSuccess_cachesAndReturns() = runTest {
        // Arrange: successful enriched page fetch from Supabase
        val enriched = listOf(
            SupabaseEnrichedMovie(
                id = "uuid-1",
                tmdbId = 10,
                title = "Net Movie",
                videoUrl = "https://example.com/v.mp4",
                overview = "o",
                tagline = null,
                posterPath = "/p.jpg",
                backdropPath = "/b.jpg",
                releaseDate = "2024-01-01",
                voteAverage = 7.0,
                voteCount = 10,
                popularity = 11.0,
                originalLanguage = "en",
                originalTitle = "Net Movie",
                genreIds = listOf(1, 2),
                genresJson = listOf(SupabaseEnrichedMovie.GenreItem(1, "Action")),
                runtime = 120,
                companiesJson = null,
                trailerKey = null,
                trailerSite = null,
                trailerUrl = null,
                publishedAt = "2024-08-01"
            )
        )
        whenever(
            supabaseApi.getEnrichedPopular(
                apiKey = any(),
                authorization = any(),
                range = any(),
                select = any(),
                order = any(),
                videoUrlFilter = any(),
                posterNotNull = any(),
                publishedNotNull = any()
            )
        ).thenReturn(Response.success(enriched))

        // Act
        val result = repo.getAvailableMovies(page = 1)

        // Assert
        assertTrue(result.isSuccess)
        val items = result.getOrNull().orEmpty()
        assertEquals(1, items.size)
        // Ensure cache upsert called
        verify(dao, times(1)).upsertAll(any())
    }

    @Test
    fun getAvailableMovies_networkFails_returnsCache() = runTest {
        // Arrange: network error
        val errorBody = ResponseBody.create("application/json".toMediaType(), "{}")
        whenever(
            supabaseApi.getEnrichedPopular(
                apiKey = any(),
                authorization = any(),
                range = any(),
                select = any(),
                order = any(),
                videoUrlFilter = any(),
                posterNotNull = any(),
                publishedNotNull = any()
            )
        ).thenReturn(Response.error(500, errorBody))

        // Prepare cached entity returned by DAO
        val cachedCombined = CombinedMovie(
            tmdbMovie = Movie(
                id = 99,
                title = "Cached Movie",
                overview = "",
                posterPath = null,
                backdropPath = null,
                releaseDate = "2023-01-01",
                voteAverage = 8.0,
                voteCount = 20,
                popularity = 20.0,
                genreIds = emptyList(),
                adult = false,
                video = true,
                originalLanguage = "en",
                originalTitle = "Cached Movie"
            ),
            supabaseMovie = SupabaseMovie(
                title = "Cached Movie",
                videoUrl = "https://example.com/cached.mp4"
            )
        )
        val entity = CachedMovieEntity.fromCombined(cachedCombined, gson)
        whenever(dao.getPaged(any(), any())).thenReturn(listOf(entity))

        // Act
        val result = repo.getAvailableMovies(page = 1)

        // Assert: falls back to cache
        assertTrue(result.isSuccess)
        val items = result.getOrNull().orEmpty()
        assertEquals(1, items.size)
        assertEquals(99, items[0].id)
    }

    @Test
    fun getMovieWithStreamDetails_offline_returnsCachedById() = runTest {
        // Arrange: make enriched fetch eventually fail by returning empty and then error path triggers cache
        val errorBody = ResponseBody.create("application/json".toMediaType(), "{}")
        whenever(
            supabaseApi.getEnrichedPopular(
                apiKey = any(),
                authorization = any(),
                range = any(),
                select = any(),
                order = any(),
                videoUrlFilter = any(),
                posterNotNull = any(),
                publishedNotNull = any()
            )
        ).thenReturn(Response.error(500, errorBody))

        val cachedCombined = CombinedMovie(
            tmdbMovie = Movie(
                id = 123,
                title = "Detail Cached",
                overview = "",
                posterPath = null,
                backdropPath = null,
                releaseDate = "2022-01-01",
                voteAverage = 7.5,
                voteCount = 15,
                popularity = 15.0,
                genreIds = emptyList(),
                adult = false,
                video = true,
                originalLanguage = "en",
                originalTitle = "Detail Cached"
            ),
            supabaseMovie = SupabaseMovie(
                title = "Detail Cached",
                videoUrl = "https://example.com/detail.mp4"
            )
        )
        val entity = CachedMovieEntity.fromCombined(cachedCombined, gson)
        whenever(dao.getById(123)).thenReturn(entity)

        // Act
        val result = repo.getMovieWithStreamDetails(123)

        // Assert
        assertTrue(result.isSuccess)
        val movie = result.getOrNull()
        assertEquals(123, movie?.id)
    }

    @Test
    fun searchAvailableMovies_success_cachesResults() = runTest {
        // Arrange: enriched list includes a title that matches query
        val enriched = listOf(
            SupabaseEnrichedMovie(
                id = "uuid-2",
                tmdbId = 200,
                title = "Matrix Revolutions",
                videoUrl = "https://example.com/matrix.mp4",
                overview = "The Matrix saga continues",
                tagline = null,
                posterPath = "/p.jpg",
                backdropPath = "/b.jpg",
                releaseDate = "2003-11-05",
                voteAverage = 7.0,
                voteCount = 100,
                popularity = 90.0,
                originalLanguage = "en",
                originalTitle = "Matrix Revolutions",
                genreIds = listOf(1),
                genresJson = listOf(SupabaseEnrichedMovie.GenreItem(1, "Action")),
                runtime = 129,
                companiesJson = null,
                trailerKey = null,
                trailerSite = null,
                trailerUrl = null,
                publishedAt = "2003-11-05"
            )
        )
        whenever(
            supabaseApi.getEnrichedPopular(
                apiKey = any(),
                authorization = any(),
                range = any(),
                select = any(),
                order = any(),
                videoUrlFilter = any(),
                posterNotNull = any(),
                publishedNotNull = any()
            )
        ).thenReturn(Response.success(enriched))

        // Act
        val result = repo.searchAvailableMovies("matrix")

        // Assert
        assertTrue(result.isSuccess)
        verify(dao, times(1)).upsertAll(any())
        val items = result.getOrNull().orEmpty()
        assertEquals(1, items.size)
        assertTrue(items[0].title.contains("Matrix", ignoreCase = true))
    }

    @Test
    fun searchAvailableMovies_networkFails_fallsBackToRoom() = runTest {
        // Arrange: network error
        val errorBody = ResponseBody.create("application/json".toMediaType(), "{}")
        whenever(
            supabaseApi.getEnrichedPopular(
                apiKey = any(),
                authorization = any(),
                range = any(),
                select = any(),
                order = any(),
                videoUrlFilter = any(),
                posterNotNull = any(),
                publishedNotNull = any()
            )
        ).thenReturn(Response.error(500, errorBody))

        val cachedCombined = CombinedMovie(
            tmdbMovie = Movie(
                id = 201,
                title = "Matrix Reloaded",
                overview = "",
                posterPath = null,
                backdropPath = null,
                releaseDate = "2003-05-15",
                voteAverage = 7.1,
                voteCount = 200,
                popularity = 95.0,
                genreIds = emptyList(),
                adult = false,
                video = true,
                originalLanguage = "en",
                originalTitle = "Matrix Reloaded"
            ),
            supabaseMovie = com.movieapp.data.model.SupabaseMovie(
                title = "Matrix Reloaded",
                videoUrl = "https://example.com/reloaded.mp4"
            )
        )
        val entity = CachedMovieEntity.fromCombined(cachedCombined, gson)
        whenever(dao.search(eq("matrix"), eq(40), eq(0))).thenReturn(listOf(entity))

        // Act
        val result = repo.searchAvailableMovies("matrix")

        // Assert
        assertTrue(result.isSuccess)
        val items = result.getOrNull().orEmpty()
        assertEquals(1, items.size)
        assertEquals(201, items[0].id)
    }

    @Test
    fun getAvailableMovies_networkFails_usesCorrectPaginationOffset() = runTest {
        // Arrange
        val errorBody = ResponseBody.create("application/json".toMediaType(), "{}")
        whenever(
            supabaseApi.getEnrichedPopular(
                apiKey = any(),
                authorization = any(),
                range = any(),
                select = any(),
                order = any(),
                videoUrlFilter = any(),
                posterNotNull = any(),
                publishedNotNull = any()
            )
        ).thenReturn(Response.error(500, errorBody))

        val page = 3
        val entity = CachedMovieEntity(
            tmdbId = 301,
            title = "Cached Paged",
            hasVideo = true,
            combinedJson = gson.toJson(
                CombinedMovie(
                    tmdbMovie = Movie(
                        id = 301,
                        title = "Cached Paged",
                        overview = "",
                        posterPath = null,
                        backdropPath = null,
                        releaseDate = "2021-01-01",
                        voteAverage = 7.0,
                        voteCount = 1,
                        popularity = 1.0,
                        genreIds = emptyList(),
                        adult = false,
                        video = true,
                        originalLanguage = "en",
                        originalTitle = "Cached Paged"
                    ),
                    supabaseMovie = com.movieapp.data.model.SupabaseMovie(
                        title = "Cached Paged",
                        videoUrl = "https://example.com/paged.mp4"
                    )
                )
            ),
            updatedAt = System.currentTimeMillis()
        )
        whenever(dao.getPaged(eq(20), eq(40))).thenReturn(listOf(entity))

        // Act
        val result = repo.getAvailableMovies(page)

        // Assert
        assertTrue(result.isSuccess)
        verify(dao, times(1)).getPaged(eq(20), eq(40)) // page=3 => offset 40
    }

    @Test
    fun searchAvailableMovies_emptyQuery_returnsImmediatelyWithoutDaoOrApi() = runTest {
        // Act
        val result = repo.searchAvailableMovies("   ")

        // Assert: success with empty list and no interactions with DAO/API
        assertTrue(result.isSuccess)
        val items = result.getOrNull().orEmpty()
        assertEquals(0, items.size)
        verifyNoInteractions(supabaseApi)
        verifyNoInteractions(dao)
    }

    @Test
    fun getAvailableMovies_cacheUpsertException_isSwallowedAndStillSuccess() = runTest {
        // Arrange: network success with one enriched movie
        val enriched = listOf(
            SupabaseEnrichedMovie(
                id = "uuid-x",
                tmdbId = 401,
                title = "Upsert Boom",
                videoUrl = "https://example.com/upsert.mp4",
                overview = "o",
                tagline = null,
                posterPath = "/p.jpg",
                backdropPath = "/b.jpg",
                releaseDate = "2024-01-01",
                voteAverage = 7.3,
                voteCount = 73,
                popularity = 33.0,
                originalLanguage = "en",
                originalTitle = "Upsert Boom",
                genreIds = listOf(1),
                genresJson = listOf(SupabaseEnrichedMovie.GenreItem(1, "Action")),
                runtime = 123,
                companiesJson = null,
                trailerKey = null,
                trailerSite = null,
                trailerUrl = null,
                publishedAt = "2024-01-01"
            )
        )
        whenever(
            supabaseApi.getEnrichedPopular(
                apiKey = any(),
                authorization = any(),
                range = any(),
                select = any(),
                order = any(),
                videoUrlFilter = any(),
                posterNotNull = any(),
                publishedNotNull = any()
            )
        ).thenReturn(Response.success(enriched))

        // Make DAO throw on upsert to simulate write failure
        whenever(dao.upsertAll(any())).thenThrow(RuntimeException("cache write failed"))

        // Act
        val result = repo.getAvailableMovies(page = 1)

        // Assert: still success and upsert attempted once
        assertTrue(result.isSuccess)
        verify(dao, times(1)).upsertAll(any())
        val items = result.getOrNull().orEmpty()
        assertEquals(1, items.size)
        assertEquals(401, items.first().id)
    }
}
