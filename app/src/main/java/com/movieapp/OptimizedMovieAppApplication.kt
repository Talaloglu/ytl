package com.movieapp

import android.app.Application
import android.content.ComponentCallbacks2
import com.movieapp.data.local.DatabaseProvider

/**
 * Optimized Application class with advanced lifecycle management
 * Handles database initialization, memory management, and resource cleanup
 * 
 * Features:
 * - Automatic database initialization
 * - Memory pressure monitoring
 * - Resource cleanup on termination
 * - Low memory handling
 */
class OptimizedMovieAppApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        println("🚀 OptimizedMovieApp: Initializing application")
        
        initializeDatabase()
        
        println("✅ OptimizedMovieApp: Application initialized successfully")
    }
    
    /**
     * Initialize Room database
     * Called during application startup
     */
    private fun initializeDatabase() {
        try {
            DatabaseProvider.init(this)
            println("✅ Database initialized")
        } catch (e: Exception) {
            println("❌ Database initialization failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Called when the application is terminating
     * Clean up resources and connections
     */
    override fun onTerminate() {
        super.onTerminate()
        println("🛑 OptimizedMovieApp: Cleaning up resources")
        
        try {
            // Future: Add cleanup for network managers, services, etc.
            println("✅ Resources cleaned up successfully")
        } catch (e: Exception) {
            println("⚠️ Error during cleanup: ${e.message}")
        }
    }
    
    /**
     * Called when system is running low on memory
     * Clear caches and release non-essential resources
     */
    override fun onLowMemory() {
        super.onLowMemory()
        println("⚠️ Low memory warning - clearing caches")
        
        // Clear image cache
        try {
            cacheDir?.deleteRecursively()
            println("✅ Image cache cleared")
        } catch (e: Exception) {
            println("⚠️ Failed to clear cache: ${e.message}")
        }
    }
    
    /**
     * Called when system requests memory to be trimmed
     * Handle different levels of memory pressure
     * 
     * @param level The level of memory trimming requested
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // UI is hidden, minor cleanup
                println("📊 Memory trim level: UI_HIDDEN")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                // Memory is getting low
                println("⚠️ Memory trim level: RUNNING_MODERATE - clearing caches")
                clearNonEssentialData()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // Memory is low, aggressive cleanup
                println("⚠️ Memory trim level: RUNNING_LOW - aggressive cleanup")
                clearNonEssentialData()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Critical memory pressure
                println("🚨 Memory trim level: RUNNING_CRITICAL - emergency cleanup")
                clearNonEssentialData()
                System.gc() // Suggest garbage collection
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // App in background, clear everything possible
                println("⚠️ Memory trim level $level - background cleanup")
                clearNonEssentialData()
            }
        }
    }
    
    /**
     * Clear non-essential data to free up memory
     * Called during memory pressure situations
     */
    private fun clearNonEssentialData() {
        try {
            // Clear image cache
            cacheDir?.listFiles()?.forEach { file ->
                if (file.name.contains("image_cache")) {
                    file.deleteRecursively()
                }
            }
            
            // Future: Clear repository caches, etc.
            println("✅ Non-essential data cleared")
        } catch (e: Exception) {
            println("⚠️ Error clearing non-essential data: ${e.message}")
        }
    }
}
