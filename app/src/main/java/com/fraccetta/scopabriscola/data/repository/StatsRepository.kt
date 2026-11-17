package com.fraccetta.scopabriscola.data.repository

import com.fraccetta.scopabriscola.data.db.GameResultDao
import com.fraccetta.scopabriscola.data.db.GameResultEntity
import com.fraccetta.scopabriscola.data.db.StatisticsDao
import com.fraccetta.scopabriscola.data.db.StatisticsEntity
import kotlinx.coroutines.flow.Flow

class StatsRepository(private val dao: StatisticsDao, private val gameResultDao: GameResultDao) {

    fun getAllStats(): Flow<List<StatisticsEntity>> = dao.getAllStats()
    fun getAllResults(): Flow<List<GameResultEntity>> = gameResultDao.getAll()
    fun getResultsByGameType(type: String): Flow<List<GameResultEntity>> = gameResultDao.getByGameType(type)
    fun getResultsByOpponent(opponent: String): Flow<List<GameResultEntity>> = gameResultDao.getByOpponent(opponent)
    fun getOpponents(): Flow<List<String>> = gameResultDao.getOpponents()
    suspend fun getMaxConsecutiveScope(): Int = gameResultDao.getMaxConsecutiveScope() ?: 0

    suspend fun recordResult(
        playerName: String,
        gameType: String,
        won: Boolean,
        points: Int,
        scope: Int = 0,
        isSolo: Boolean = false,
        opponentName: String = "",
        maxConsecutiveScope: Int = 0
    ) {
        val mode = if (isSolo) "cpu" else "bt"
        // aggregate stats
        val existing = dao.getStats(playerName, gameType, isSolo)
        val updated = existing?.copy(
            sessionsPlayed = existing.sessionsPlayed + 1,
            sessionsWon = if (won) existing.sessionsWon + 1 else existing.sessionsWon,
            sessionsLost = if (!won) existing.sessionsLost + 1 else existing.sessionsLost,
            totalPoints = existing.totalPoints + points,
            totalScope = existing.totalScope + scope,
            lastPlayed = System.currentTimeMillis()
        ) ?: StatisticsEntity(
            id = "$playerName-$gameType-$mode",
            playerName = playerName,
            gameType = gameType,
            isSolo = isSolo,
            sessionsPlayed = 1,
            sessionsWon = if (won) 1 else 0,
            sessionsLost = if (!won) 1 else 0,
            totalPoints = points,
            totalScope = scope,
            lastPlayed = System.currentTimeMillis()
        )
        dao.upsert(updated)
        // per-game record
        gameResultDao.insert(GameResultEntity(
            playerName = playerName,
            opponentName = opponentName,
            gameType = gameType,
            isSolo = isSolo,
            won = won,
            points = points,
            scope = scope,
            maxConsecutiveScope = maxConsecutiveScope
        ))
    }
}
