package com.movieapp.data.local

import android.content.Context
import androidx.room.Room

/**
 * Simple provider for Room database instance without full DI.
 */
object DatabaseProvider {
    @Volatile
    private var db: AppDatabase? = null

    fun init(appContext: Context) {
        if (db == null) {
            synchronized(this) {
                if (db == null) {
                    db = Room.databaseBuilder(
                        appContext,
                        AppDatabase::class.java,
                        "movieapp.db"
                    ).fallbackToDestructiveMigration()
                        .build()
                }
            }
        }
    }

    fun database(): AppDatabase {
        return checkNotNull(db) { "DatabaseProvider not initialized. Call DatabaseProvider.init(context) first." }
    }

    fun cachedMovieDao(): CachedMovieDao = database().cachedMovieDao()
}
