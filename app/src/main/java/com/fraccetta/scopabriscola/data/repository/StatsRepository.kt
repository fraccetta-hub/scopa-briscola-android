package com.fraccetta.scopabriscola.data.repository

import com.fraccetta.scopabriscola.data.db.StatisticsDao
import com.fraccetta.scopabriscola.data.db.StatisticsEntity
import kotlinx.coroutines.flow.Flow

class StatsRepository(private val dao: StatisticsDao) {

    fun getAllStats(): Flow<List<StatisticsEntity>> = dao.getAllStats()

    suspend fun recordResult(
        playerName: String,
        gameType: String,
        won: Boolean,
        points: Int,
        scope: Int = 0
    ) {
        val existing = dao.getStats(playerName, gameType)
        val updated = existing?.copy(
            sessionsPlayed = existing.sessionsPlayed + 1,
            sessionsWon = if (won) existing.sessionsWon + 1 else existing.sessionsWon,
            sessionsLost = if (!won) existing.sessionsLost + 1 else existing.sessionsLost,
            totalPoints = existing.totalPoints + points,
            totalScope = existing.totalScope + scope,
            lastPlayed = System.currentTimeMillis()
        ) ?: StatisticsEntity(
            id = "$playerName-$gameType",
            playerName = playerName,
            gameType = gameType,
            sessionsPlayed = 1,
            sessionsWon = if (won) 1 else 0,
            sessionsLost = if (!won) 1 else 0,
            totalPoints = points,
            totalScope = scope,
            lastPlayed = System.currentTimeMillis()
        )
        dao.upsert(updated)
    }
}
