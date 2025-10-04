package com.movieapp

import android.app.Application
import androidx.compose.animation.core.AnimationConstants
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import leakcanary.LeakCanary
import java.io.File

/**
 * Main Application class for MovieApp
 * Configures Coil image loading with optimized caching
 * Integrates LeakCanary for memory leak detection in debug builds
 */
class MovieAppApplication : Application(), ImageLoaderFactory {
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure LeakCanary for debug builds
        if (BuildConfig.DEBUG) {
            configureLeakCanary()
        }
        
        println("🚀 MovieAppApplication: Initialized")
    }
    
    /**
     * Configure LeakCanary for memory leak detection
     * Only runs in debug builds
     */
    private fun configureLeakCanary() {
        LeakCanary.config = LeakCanary.config.copy(
            dumpHeap = true,
            dumpHeapWhenDebugging = false,
            retainedVisibleThreshold = 5,
            showNotifications = false
        )
        println("🔍 LeakCanary: Configured for memory leak detection")
    }
    
    /**
     * Create optimized ImageLoader for Coil
     * - Memory Cache: 25% of available memory
     * - Disk Cache: 250MB max
     * - Crossfade animations enabled
     * - Debug logging in debug builds
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(250 * 1024 * 1024) // 250MB
                    .build()
            }
            .crossfade(true)
            .crossfade(AnimationConstants.DefaultDurationMillis)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
