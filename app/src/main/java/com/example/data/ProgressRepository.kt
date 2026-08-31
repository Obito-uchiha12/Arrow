package com.example.data

import com.example.model.ArrowColorPreset
import com.example.model.ArrowThickness
import com.example.model.BoardTheme
import com.example.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressRepository(
    private val progressDao: LevelProgressDao,
    private val settingsDao: UserSettingsDao
) {
    val allProgress: Flow<List<LevelProgressEntity>> = progressDao.getAllProgress()
    val completedLevelsCount: Flow<Int> = progressDao.observeCompletedCount()
    val completedLevels: Flow<List<LevelProgressEntity>> = progressDao.observeCompletedLevels()
    val totalStars: Flow<Int> = progressDao.observeTotalStars().map { it ?: 0 }
    val highestUnlockedLevel: Flow<Int> = progressDao.observeHighestUnlockedLevel().map { it ?: 1 }

    val userSettings: Flow<UserSettings> = settingsDao.observeSettings().map { entity ->
        if (entity != null) {
            UserSettings(
                boardTheme = BoardTheme.values().find { it.id == entity.themeId } ?: BoardTheme.LIGHT_ZEN,
                arrowColor = ArrowColorPreset.fromId(entity.arrowColorId),
                arrowThickness = try { ArrowThickness.valueOf(entity.arrowThickness) } catch (e: Exception) { ArrowThickness.MEDIUM },
                animationSpeed = try { com.example.model.AnimationSpeed.valueOf(entity.animationSpeed) } catch (e: Exception) { com.example.model.AnimationSpeed.NORMAL },
                soundEnabled = entity.soundEnabled,
                hapticsEnabled = entity.hapticsEnabled,
                zenMode = entity.zenMode,
                highlightHintsCount = entity.highlightHintsCount,
                autoMoveHintsCount = entity.autoMoveHintsCount,
                heartsCount = entity.heartsCount
            )
        } else {
            UserSettings()
        }
    }

    suspend fun getProgressForLevel(levelId: Int): LevelProgressEntity {
        return progressDao.getProgressForLevel(levelId) ?: LevelProgressEntity(
            levelId = levelId,
            isUnlocked = (levelId == 1)
        )
    }

    suspend fun getHighestUnlockedLevel(): Int {
        return progressDao.getHighestUnlockedLevel() ?: 1
    }

    suspend fun getCompletedLevelsList(): List<LevelProgressEntity> {
        return progressDao.getCompletedLevels()
    }

    suspend fun completeLevel(
        levelId: Int,
        timeSeconds: Int,
        moves: Int
    ) {
        val existing = getProgressForLevel(levelId)
        val calculatedStars = when {
            moves <= existing.bestMoves || existing.bestMoves == 0 -> 3
            moves <= existing.bestMoves + 3 -> 2
            else -> 1
        }

        val updated = existing.copy(
            isCompleted = true,
            isUnlocked = true,
            bestTimeSeconds = if (existing.bestTimeSeconds == 0) timeSeconds else minOf(existing.bestTimeSeconds, timeSeconds),
            bestMoves = if (existing.bestMoves == 0) moves else minOf(existing.bestMoves, moves),
            stars = maxOf(existing.stars, calculatedStars),
            completedTimestamp = System.currentTimeMillis()
        )
        progressDao.insertOrUpdate(updated)

        // Unlock next level (up to 1000+)
        val nextLevelId = levelId + 1
        if (nextLevelId <= 1000) {
            val nextProgress = getProgressForLevel(nextLevelId)
            if (!nextProgress.isUnlocked) {
                progressDao.insertOrUpdate(nextProgress.copy(isUnlocked = true))
            }
        }

        // Reward hints on 3-star victory
        if (calculatedStars == 3 && existing.stars < 3) {
            addHints(highlight = 1, autoMove = 1)
        }
    }

    suspend fun updateSettings(settings: UserSettings) {
        val current = settingsDao.getSettings() ?: UserSettingsEntity()
        val entity = current.copy(
            themeId = settings.boardTheme.id,
            arrowColorId = settings.arrowColor.id,
            arrowThickness = settings.arrowThickness.name,
            animationSpeed = settings.animationSpeed.name,
            soundEnabled = settings.soundEnabled,
            hapticsEnabled = settings.hapticsEnabled,
            zenMode = settings.zenMode,
            highlightHintsCount = settings.highlightHintsCount,
            autoMoveHintsCount = settings.autoMoveHintsCount,
            heartsCount = settings.heartsCount
        )
        settingsDao.saveSettings(entity)
    }

    suspend fun consumeHighlightHint(): Boolean {
        val current = settingsDao.getSettings() ?: UserSettingsEntity()
        if (current.highlightHintsCount > 0) {
            settingsDao.saveSettings(current.copy(highlightHintsCount = current.highlightHintsCount - 1))
            return true
        }
        return false
    }

    suspend fun consumeAutoMoveHint(): Boolean {
        val current = settingsDao.getSettings() ?: UserSettingsEntity()
        if (current.autoMoveHintsCount > 0) {
            settingsDao.saveSettings(current.copy(autoMoveHintsCount = current.autoMoveHintsCount - 1))
            return true
        }
        return false
    }

    suspend fun addHints(highlight: Int, autoMove: Int) {
        val current = settingsDao.getSettings() ?: UserSettingsEntity()
        settingsDao.saveSettings(
            current.copy(
                highlightHintsCount = current.highlightHintsCount + highlight,
                autoMoveHintsCount = current.autoMoveHintsCount + autoMove
            )
        )
    }

    suspend fun updateLastPlayedLevel(levelId: Int) {
        val current = settingsDao.getSettings() ?: UserSettingsEntity()
        settingsDao.saveSettings(current.copy(lastPlayedLevel = levelId))
    }

    suspend fun getLastPlayedLevel(): Int {
        return settingsDao.getSettings()?.lastPlayedLevel ?: 1
    }
}
