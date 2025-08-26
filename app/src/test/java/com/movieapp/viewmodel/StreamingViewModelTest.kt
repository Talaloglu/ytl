package com.movieapp.viewmodel

import app.cash.turbine.test
import com.movieapp.data.model.CombinedMovie
import com.movieapp.data.model.Movie
import com.movieapp.data.model.SupabaseMovie
import com.movieapp.data.repository.CombinedMovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class StreamingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CombinedMovieRepository
    private lateinit var viewModel: StreamingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        viewModel = StreamingViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadStreamingMovies_success_emitsSuccess() = runTest(testDispatcher) {
        // Given
        val movies = listOf(
            CombinedMovie(
                tmdbMovie = Movie(
                    id = 1,
                    title = "Test Movie",
                    overview = "",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = "2024-01-01",
                    voteAverage = 7.5,
                    voteCount = 100,
                    popularity = 10.0,
                    genreIds = emptyList(),
                    adult = false,
                    video = true,
                    originalLanguage = "en",
                    originalTitle = "Test Movie"
                ),
                supabaseMovie = SupabaseMovie(
                    title = "Test Movie",
                    videoUrl = "https://example.com/vid.mp4"
                )
            )
        )
        whenever(repository.getAvailableMovies(any())).thenReturn(Result.success(movies))

        // When
        viewModel.loadStreamingMovies(page = 1)
        // Advance until idle
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.streamingState.first()
        assertTrue(state is StreamingViewModel.UiState.Success)
        val data = (state as StreamingViewModel.UiState.Success).data
        assertEquals(1, data.size)
        assertEquals("Test Movie", data.first().title)
    }

    @Test
    fun getMovieWithStreamDetails_offline_error_setsErrorStateWithOfflineFlag() = runTest(testDispatcher) {
        // Given
        val offlineException = java.io.IOException("timeout")
        whenever(repository.getMovieWithStreamDetails(any())).thenReturn(Result.failure(offlineException))

        // When
        viewModel.getMovieWithStreamDetails(123)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.detailsState.first()
        assertTrue(state is StreamingViewModel.UiState.Error)
        val err = state as StreamingViewModel.UiState.Error
        assertTrue(err.isOffline)
        assertTrue(err.canRetry)
    }

    @Test
    fun searchStreamingMovies_success_emitsSuccess() = runTest(testDispatcher) {
        // Given
        val movies = listOf(
            CombinedMovie(
                tmdbMovie = Movie(
                    id = 2,
                    title = "Matrix",
                    overview = "",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = "1999-03-31",
                    voteAverage = 8.7,
                    voteCount = 1000,
                    popularity = 99.0,
                    genreIds = emptyList(),
                    adult = false,
                    video = true,
                    originalLanguage = "en",
                    originalTitle = "Matrix"
                ),
                supabaseMovie = SupabaseMovie(
                    title = "Matrix",
                    videoUrl = "https://example.com/matrix.mp4"
                )
            )
        )
        whenever(repository.searchAvailableMovies("Matrix")).thenReturn(Result.success(movies))

        // When
        viewModel.searchStreamingMovies("Matrix")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.searchState.first()
        assertTrue(state is StreamingViewModel.UiState.Success)
        val data = (state as StreamingViewModel.UiState.Success).data
        assertEquals(1, data.size)
        assertEquals("Matrix", data.first().title)
    }

    @Test
    fun loadStreamingMovies_transientFailure_thenRetrySuccess_updatesStateAccordingly() = runTest(testDispatcher) {
        // Given: first call fails (offline), second call succeeds
        val offlineException = java.io.IOException("network down")
        whenever(repository.getAvailableMovies(any()))
            .thenReturn(Result.failure(offlineException))

        // When: first attempt
        viewModel.loadStreamingMovies(page = 1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error state with offline flag
        val firstState = viewModel.streamingState.first()
        assertTrue(firstState is StreamingViewModel.UiState.Error)
        val err = firstState as StreamingViewModel.UiState.Error
        assertTrue(err.isOffline)
        assertTrue(err.canRetry)

        // Prepare success on retry
        val movies = listOf(
            CombinedMovie(
                tmdbMovie = Movie(
                    id = 5,
                    title = "Recovered",
                    overview = "",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = "2024-01-01",
                    voteAverage = 7.0,
                    voteCount = 10,
                    popularity = 10.0,
                    genreIds = emptyList(),
                    adult = false,
                    video = true,
                    originalLanguage = "en",
                    originalTitle = "Recovered"
                ),
                supabaseMovie = SupabaseMovie(
                    title = "Recovered",
                    videoUrl = "https://example.com/recovered.mp4"
                )
            )
        )
        whenever(repository.getAvailableMovies(any())).thenReturn(Result.success(movies))

        // When: retry
        viewModel.loadStreamingMovies(page = 1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Success state
        val secondState = viewModel.streamingState.first()
        assertTrue(secondState is StreamingViewModel.UiState.Success)
        val data = (secondState as StreamingViewModel.UiState.Success).data
        assertEquals(1, data.size)
        assertEquals("Recovered", data.first().title)
    }

    @Test
    fun searchStreamingMovies_emptyQuery_doesNotCallRepository_andResetsState() = runTest(testDispatcher) {
        // When
        viewModel.searchStreamingMovies("  ")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verifyNoInteractions(repository)
        val state = viewModel.searchState.first()
        assertTrue(state is StreamingViewModel.UiState.Idle)
        assertTrue(viewModel.searchResults.first().isEmpty())
    }
}
