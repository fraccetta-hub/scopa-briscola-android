package com.fraccetta.scopabriscola.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [StatisticsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statisticsDao(): StatisticsDao
}
