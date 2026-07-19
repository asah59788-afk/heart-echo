package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey val id: Int = 1,
    val courage: Int = 10,
    val sincerity: Int = 15,
    val thoughtfulness: Int = 10,
    val eloquence: Int = 5,
    val currentChapter: Int = 0, // 0 = The Passing Glance, 1 = The Library Helper, 2 = The Shared Umbrella, 3 = Under the Stars
    val unlockedGallery: String = "cherry_petal" // Comma-separated keys
) {
    fun hasUnlocked(itemId: String): Boolean {
        return unlockedGallery.split(",").contains(itemId)
    }
}
