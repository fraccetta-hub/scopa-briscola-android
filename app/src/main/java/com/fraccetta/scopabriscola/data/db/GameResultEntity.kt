package com.fraccetta.scopabriscola.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val playerName: String,
    val opponentName: String,
    val gameType: String,       // "scopa" | "briscola"
    val isSolo: Boolean,
    val won: Boolean,
    val points: Int,
    val scope: Int = 0,
    val maxConsecutiveScope: Int = 0
)
