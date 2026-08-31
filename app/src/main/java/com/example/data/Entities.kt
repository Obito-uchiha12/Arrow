package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelId: Int,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val bestTimeSeconds: Int = 0,
    val stars: Int = 0,
    val bestMoves: Int = 0,
    val completedTimestamp: Long = 0L
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeId: String = "light_zen",
    val arrowColorId: String = "default_dark",
    val arrowThickness: String = "MEDIUM",
    val animationSpeed: String = "NORMAL",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val zenMode: Boolean = false,
    val lastPlayedLevel: Int = 1,
    val highlightHintsCount: Int = 5,
    val autoMoveHintsCount: Int = 3,
    val heartsCount: Int = 3
)
