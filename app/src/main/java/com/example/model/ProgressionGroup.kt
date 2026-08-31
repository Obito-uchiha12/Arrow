package com.example.model

/**
 * Defines progression groups across the 1000+ offline levels.
 */
enum class ProgressionGroup(
    val groupNumber: Int,
    val title: String,
    val subtitle: String,
    val startLevel: Int,
    val endLevel: Int,
    val expectedTiers: List<DifficultyTier>,
    val targetDensity: Float
) {
    GROUP_1(1, "Origins", "Very Easy / Easy", 1, 50, listOf(DifficultyTier.VERY_EASY, DifficultyTier.EASY), 0.50f),
    GROUP_2(2, "Ascent", "Normal", 51, 150, listOf(DifficultyTier.NORMAL), 0.65f),
    GROUP_3(3, "Harmony", "Hard", 151, 350, listOf(DifficultyTier.HARD), 0.78f),
    GROUP_4(4, "Labyrinth", "Very Hard", 351, 700, listOf(DifficultyTier.VERY_HARD), 0.88f),
    GROUP_5(5, "Apex", "Extreme", 701, 1000, listOf(DifficultyTier.EXTREME), 0.96f),
    GROUP_INFINITE(6, "Beyond", "Endless Content Architecture", 1001, Int.MAX_VALUE, listOf(DifficultyTier.EXTREME), 1.0f);

    companion object {
        fun forLevel(levelId: Int): ProgressionGroup {
            return values().firstOrNull { levelId in it.startLevel..it.endLevel } ?: GROUP_INFINITE
        }
    }
}
