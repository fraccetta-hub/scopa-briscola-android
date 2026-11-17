package com.fraccetta.scopabriscola.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: GameResultEntity)

    @Query("SELECT * FROM game_results ORDER BY timestamp DESC")
    fun getAll(): Flow<List<GameResultEntity>>

    @Query("SELECT * FROM game_results WHERE gameType=:type ORDER BY timestamp DESC")
    fun getByGameType(type: String): Flow<List<GameResultEntity>>

    @Query("SELECT * FROM game_results WHERE opponentName=:opponent ORDER BY timestamp DESC")
    fun getByOpponent(opponent: String): Flow<List<GameResultEntity>>

    @Query("SELECT MAX(maxConsecutiveScope) FROM game_results WHERE gameType='scopa'")
    suspend fun getMaxConsecutiveScope(): Int?

    @Query("SELECT DISTINCT opponentName FROM game_results WHERE NOT isSolo ORDER BY opponentName ASC")
    fun getOpponents(): Flow<List<String>>
}
