package com.fraccetta.scopabriscola.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [StatisticsEntity::class, GameResultEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statisticsDao(): StatisticsDao
    abstract fun gameResultDao(): GameResultDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS game_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        playerName TEXT NOT NULL,
                        opponentName TEXT NOT NULL,
                        gameType TEXT NOT NULL,
                        isSolo INTEGER NOT NULL,
                        won INTEGER NOT NULL,
                        points INTEGER NOT NULL,
                        scope INTEGER NOT NULL DEFAULT 0,
                        maxConsecutiveScope INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
    }
}
