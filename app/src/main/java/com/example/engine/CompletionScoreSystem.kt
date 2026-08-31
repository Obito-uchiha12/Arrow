package com.example.engine

import com.example.model.PuzzleLevel
import kotlin.math.max

/**
 * System for calculating completion scores, star ratings, and speed multipliers.
 */
object CompletionScoreSystem {

    data class CompletionScore(
        val stars: Int, // 1 to 3
        val baseScore: Int,
        val speedBonus: Int,
        val accuracyBonus: Int,
        val totalScore: Int
    )

    fun calculate(level: PuzzleLevel, movesTaken: Int, timeElapsedSeconds: Int): CompletionScore {
        val totalArrows = level.arrowCount
        val idealMoves = totalArrows

        // Star rating
        val extraMoves = (movesTaken - idealMoves).coerceAtLeast(0)
        val stars = when {
            extraMoves == 0 -> 3
            extraMoves <= max(2, totalArrows / 5) -> 2
            else -> 1
        }

        val baseScore = totalArrows * 100
        val targetTime = totalArrows * 2
        val speedBonus = max(0, (targetTime - timeElapsedSeconds) * 20)
        val accuracyBonus = max(0, (10 - extraMoves) * 50)

        val totalScore = baseScore + speedBonus + accuracyBonus

        return CompletionScore(
            stars = stars,
            baseScore = baseScore,
            speedBonus = speedBonus,
            accuracyBonus = accuracyBonus,
            totalScore = totalScore
        )
    }
}
