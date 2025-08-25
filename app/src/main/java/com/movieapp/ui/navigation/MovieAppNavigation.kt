package com.movieapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.movieapp.ui.screens.BannerScreen
import com.movieapp.ui.screens.StreamingHomeScreen
import com.movieapp.ui.screens.MoviePlayerScreen
import com.movieapp.ui.screens.DetailsScreen
import com.movieapp.ui.screens.CategorizedHomeScreen
import com.movieapp.ui.screens.CategoryDetailScreen

/**
 * Navigation routes for the movie app
 * Enhanced with better route management and validation
 */
object MovieAppRoutes {
    const val BANNER = "banner"
    const val HOME = "home"
    const val CATEGORIZED_HOME = "categorized_home"
    const val DETAILS = "details/{movieId}"
    const val MOVIE_PLAYER = "movie_player/{movieId}"
    const val CATEGORY_DETAIL = "category_detail/{categoryType}/{categoryTitle}"
    
    /**
     * Create details route with movie ID
     * @param movieId The movie ID to pass as navigation argument
     * @return Formatted route string with movie ID
     */
    fun createDetailsRoute(movieId: Int): String {
        return "details/$movieId"
    }
    
    /**
     * Create movie player route with movie ID
     * @param movieId The movie ID to pass as navigation argument
     * @return Formatted route string with movie ID
     */
    fun createMoviePlayerRoute(movieId: Int): String {
        return "movie_player/$movieId"
    }
    
    /**
     * Create category detail route
     * @param categoryType The category type identifier
     * @param categoryTitle The display title for the category
     * @return Formatted route string with category parameters
     */
    fun createCategoryDetailRoute(categoryType: String, categoryTitle: String): String {
        return "category_detail/$categoryType/$categoryTitle"
    }
    
    /**
     * Extract movie ID from details route
     * @param route The route string
     * @return Movie ID or null if invalid
     */
    fun extractMovieId(route: String): Int? {
        return try {
            route.removePrefix("details/").toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Main navigation component for the Movie App
 * Handles navigation between BannerScreen, HomeScreen, and DetailsScreen
 * Implements movie ID passing as navigation argument as specified
 */
@Composable
fun MovieAppNavigation(
    navController: NavHostController,
    startDestination: String = MovieAppRoutes.BANNER
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Banner Screen - App intro with explore button
        composable(route = MovieAppRoutes.BANNER) {
            BannerScreen(
                onExploreMoviesClick = {
                    // Navigate to categorized home screen when explore button is clicked
                    navController.navigate(MovieAppRoutes.CATEGORIZED_HOME) {
                        // Clear banner from back stack to prevent returning to it
                        popUpTo(MovieAppRoutes.BANNER) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        
        // Home Screen - Streaming movies with video URLs from Supabase
        composable(route = MovieAppRoutes.HOME) {
            StreamingHomeScreen(
                onMovieClick = { movieId ->
                    // Navigate to details screen with movie ID
                    navController.navigate(MovieAppRoutes.createDetailsRoute(movieId))
                }
            )
        }
        
        // Categorized Home Screen - New home screen with movie categories
        composable(route = MovieAppRoutes.CATEGORIZED_HOME) {
            CategorizedHomeScreen(
                onMovieClick = { movieId ->
                    // Navigate to details screen with movie ID
                    navController.navigate(MovieAppRoutes.createDetailsRoute(movieId))
                },
                onSectionClick = { categoryType, categoryTitle ->
                    // Navigate to category detail screen
                    navController.navigate(MovieAppRoutes.createCategoryDetailRoute(categoryType, categoryTitle))
                }
            )
        }
        
        // Details Screen - Individual movie details with enhanced argument handling
        composable(
            route = MovieAppRoutes.DETAILS,
            arguments = listOf(
                navArgument("movieId") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            // Extract movie ID from navigation arguments with validation
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
            
            // Validate movie ID before proceeding
            if (movieId > 0) {
                DetailsScreen(
                    movieId = movieId,
                    onBackClick = {
                        // Navigate back to previous screen
                        navController.popBackStack()
                    },
                    onWatchClick = { watchMovieId ->
                        // Navigate to movie player for streaming
                        navController.navigate(MovieAppRoutes.createMoviePlayerRoute(watchMovieId))
                    }
                )
            } else {
                // Handle invalid movie ID - show error or navigate back
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        // Movie Player Screen - For streaming videos
        composable(
            route = MovieAppRoutes.MOVIE_PLAYER,
            arguments = listOf(
                navArgument("movieId") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            // Extract movie ID from navigation arguments with validation
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
            
            // Validate movie ID before proceeding
            if (movieId > 0) {
                MoviePlayerScreen(
                    movieId = movieId,
                    onBackClick = {
                        // Navigate back to previous screen
                        navController.popBackStack()
                    }
                )
            } else {
                // Handle invalid movie ID - show error or navigate back
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        // Category Detail Screen - For showing full category lists
        composable(
            route = MovieAppRoutes.CATEGORY_DETAIL,
            arguments = listOf(
                navArgument("categoryType") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("categoryTitle") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            // Extract category parameters from navigation arguments
            val categoryType = backStackEntry.arguments?.getString("categoryType") ?: ""
            val categoryTitle = backStackEntry.arguments?.getString("categoryTitle") ?: ""
            
            // Validate parameters before proceeding
            if (categoryType.isNotEmpty() && categoryTitle.isNotEmpty()) {
                CategoryDetailScreen(
                    categoryType = categoryType,
                    categoryTitle = categoryTitle,
                    onBackClick = {
                        // Navigate back to previous screen
                        navController.popBackStack()
                    },
                    onMovieClick = { movieId ->
                        // Navigate to movie details
                        navController.navigate(MovieAppRoutes.createDetailsRoute(movieId))
                    }
                )
            } else {
                // Handle invalid parameters - navigate back
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}