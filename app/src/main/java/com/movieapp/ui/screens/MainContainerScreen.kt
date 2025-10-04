package com.movieapp.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.movieapp.ui.navigation.BottomNavItem
import com.movieapp.ui.navigation.MovieAppBottomNavigation
import com.movieapp.ui.navigation.MovieAppRoutes

/**
 * Main Container Screen
 * Wraps the main navigation with bottom navigation bar
 */
@Composable
fun MainContainerScreen(
    mainNavController: NavHostController
) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            MovieAppBottomNavigation(navController = bottomNavController)
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Home Tab
            composable(BottomNavItem.Home.route) {
                EnhancedHomeScreen(
                    onMovieClick = { movieId ->
                        mainNavController.navigate(
                            MovieAppRoutes.createDetailsRoute(movieId)
                        )
                    },
                    onSearchClick = {
                        bottomNavController.navigate(BottomNavItem.Search.route)
                    },
                    onSettingsClick = {
                        mainNavController.navigate(MovieAppRoutes.SETTINGS)
                    },
                    onCategoryClick = { categoryType, categoryTitle ->
                        mainNavController.navigate(
                            MovieAppRoutes.createCategoryDetailRoute(categoryType, categoryTitle)
                        )
                    }
                )
            }
            
            // Browse Tab
            composable(BottomNavItem.Browse.route) {
                BrowseScreen(
                    onCategoryClick = { categoryType, categoryTitle ->
                        mainNavController.navigate(
                            MovieAppRoutes.createCategoryDetailRoute(categoryType, categoryTitle)
                        )
                    },
                    onMovieClick = { movieId ->
                        mainNavController.navigate(
                            MovieAppRoutes.createDetailsRoute(movieId)
                        )
                    }
                )
            }
            
            // Search Tab
            composable(BottomNavItem.Search.route) {
                SearchScreen(
                    onBack = {
                        bottomNavController.navigate(BottomNavItem.Home.route)
                    },
                    onMovieClick = { movie ->
                        mainNavController.navigate(
                            MovieAppRoutes.createDetailsRoute(movie.id)
                        )
                    }
                )
            }
            
            // Profile Tab
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onNavigateToSettings = {
                        mainNavController.navigate(MovieAppRoutes.SETTINGS)
                    }
                )
            }
        }
    }
}
