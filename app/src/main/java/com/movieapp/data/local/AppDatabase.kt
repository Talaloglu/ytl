package com.movieapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedMovieEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedMovieDao(): CachedMovieDao
}
