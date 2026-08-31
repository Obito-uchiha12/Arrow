package com.example.engine

import com.example.model.LevelCategory
import com.example.model.ProgressionGroup
import com.example.model.PuzzleLevel
import java.util.concurrent.ConcurrentHashMap

object LevelRepository {

    const val TOTAL_LEVELS = 1000

    private val levelCache = ConcurrentHashMap<Int, PuzzleLevel>()

    /**
     * Retrieves or generates level with given [levelId] (1..1000+).
     */
    fun getLevel(levelId: Int): PuzzleLevel {
        val clampedId = levelId.coerceAtLeast(1)
        return levelCache.getOrPut(clampedId) {
            ManualLevels.getManualLevelOrNull(clampedId) ?: PuzzleGenerator.generateLevel(clampedId)
        }
    }

    /**
     * Generates or regenerates a level with a specific custom seed.
     */
    fun getLevelWithSeed(levelId: Int, seed: Long): PuzzleLevel {
        val level = PuzzleGenerator.generateLevelWithSeed(levelId, seed)
        levelCache[levelId] = level
        return level
    }

    /**
     * Gets total number of levels available in this release.
     */
    fun getTotalLevelsCount(): Int = TOTAL_LEVELS

    /**
     * Gets a list of level IDs matching the specified category.
     */
    fun getLevelIdsForCategory(category: LevelCategory): List<Int> {
        if (category == LevelCategory.ALL) {
            return (1..TOTAL_LEVELS).toList()
        }
        val result = mutableListOf<Int>()
        for (i in 1..TOTAL_LEVELS) {
            val level = getLevel(i)
            if (level.category == category) {
                result.add(i)
            }
        }
        return result
    }

    /**
     * Gets level IDs belonging to a specific ProgressionGroup.
     */
    fun getLevelIdsForGroup(group: ProgressionGroup): List<Int> {
        val start = group.startLevel.coerceIn(1, TOTAL_LEVELS)
        val end = group.endLevel.coerceIn(1, TOTAL_LEVELS)
        return (start..end).toList()
    }

    /**
     * Prewarms cache for a range of levels around current level.
     */
    fun prewarm(currentLevel: Int, radius: Int = 3) {
        val start = (currentLevel - radius).coerceAtLeast(1)
        val end = (currentLevel + radius).coerceAtMost(TOTAL_LEVELS)
        for (i in start..end) {
            getLevel(i)
        }
    }
}

