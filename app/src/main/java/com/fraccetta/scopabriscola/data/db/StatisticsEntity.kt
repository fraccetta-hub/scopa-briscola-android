package com.fraccetta.scopabriscola.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statistics")
data class StatisticsEntity(
    @PrimaryKey val id: String, // "$playerName-$gameType"
    val playerName: String,
    val gameType: String, // "SCOPA" | "BRISCOLA"
    val sessionsPlayed: Int = 0,
    val sessionsWon: Int = 0,
    val sessionsLost: Int = 0,
    val totalPoints: Int = 0,
    val totalScope: Int = 0,
    val lastPlayed: Long = 0L
)
