package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    fun getPlayerStatsFlow(): Flow<PlayerStats?>

    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    suspend fun getPlayerStatsDirect(): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStats(stats: PlayerStats)

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry): Long

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteJournalEntryById(id: Int)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllJournalEntries()
}
