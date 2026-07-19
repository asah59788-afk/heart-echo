package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {

    val playerStats: Flow<PlayerStats> = gameDao.getPlayerStatsFlow().map { stats ->
        stats ?: PlayerStats().also {
            // Seed default stats if not existing
            gameDao.insertPlayerStats(it)
        }
    }

    val journalEntries: Flow<List<JournalEntry>> = gameDao.getAllJournalEntries()

    suspend fun getStatsDirect(): PlayerStats {
        return gameDao.getPlayerStatsDirect() ?: PlayerStats().also {
            gameDao.insertPlayerStats(it)
        }
    }

    suspend fun saveJournalEntry(title: String, content: String, courageEarned: Int, sincerityEarned: Int, aiFeedback: String = "") {
        val entry = JournalEntry(
            title = title,
            content = content,
            courageEarned = courageEarned,
            sincerityEarned = sincerityEarned,
            aiFeedback = aiFeedback
        )
        gameDao.insertJournalEntry(entry)

        // Increment stats on journal save
        val currentStats = getStatsDirect()
        val newStats = currentStats.copy(
            courage = currentStats.courage + courageEarned,
            sincerity = currentStats.sincerity + sincerityEarned
        )
        gameDao.insertPlayerStats(newStats)
    }

    suspend fun updateStat(statType: String, delta: Int) {
        val currentStats = getStatsDirect()
        val newStats = when (statType) {
            "courage" -> currentStats.copy(courage = (currentStats.courage + delta).coerceIn(0, 100))
            "sincerity" -> currentStats.copy(sincerity = (currentStats.sincerity + delta).coerceIn(0, 100))
            "thoughtfulness" -> currentStats.copy(thoughtfulness = (currentStats.thoughtfulness + delta).coerceIn(0, 100))
            "eloquence" -> currentStats.copy(eloquence = (currentStats.eloquence + delta).coerceIn(0, 100))
            else -> currentStats
        }
        gameDao.insertPlayerStats(newStats)
    }

    suspend fun unlockGalleryItem(itemId: String): Boolean {
        val currentStats = getStatsDirect()
        val currentList = currentStats.unlockedGallery.split(",").toMutableList()
        if (!currentList.contains(itemId)) {
            currentList.add(itemId)
            val newStats = currentStats.copy(unlockedGallery = currentList.joinToString(","))
            gameDao.insertPlayerStats(newStats)
            return true
        }
        return false
    }

    suspend fun advanceChapter(): Boolean {
        val currentStats = getStatsDirect()
        if (currentStats.currentChapter < 3) {
            val newStats = currentStats.copy(currentChapter = currentStats.currentChapter + 1)
            gameDao.insertPlayerStats(newStats)
            return true
        }
        return false
    }

    suspend fun deleteJournalById(id: Int) {
        gameDao.deleteJournalEntryById(id)
    }

    suspend fun resetGame() {
        gameDao.deleteAllJournalEntries()
        gameDao.insertPlayerStats(PlayerStats())
    }
}
