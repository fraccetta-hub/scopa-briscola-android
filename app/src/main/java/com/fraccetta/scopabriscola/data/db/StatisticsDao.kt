package com.fraccetta.scopabriscola.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Query("SELECT * FROM statistics ORDER BY sessionsWon DESC")
    fun getAllStats(): Flow<List<StatisticsEntity>>

    @Query("SELECT * FROM statistics WHERE playerName=:name AND gameType=:type LIMIT 1")
    suspend fun getStats(name: String, type: String): StatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StatisticsEntity)
}
