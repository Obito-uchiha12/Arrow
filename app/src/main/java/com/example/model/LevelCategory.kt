package com.example.model

enum class LevelCategory(val displayName: String, val iconName: String, val description: String) {
    ALL("All", "grid_view", "Browse all 1000+ levels"),
    ANIMALS("Animals", "pets", "Cats, dogs, birds, aquatic life & beasts"),
    OBJECTS("Objects", "handyman", "Everyday tools, musical instruments & items"),
    SYMBOLS("Symbols", "favorite", "Hearts, stars, badges & iconic emblems"),
    NATURE("Nature", "eco", "Trees, flowers, mountains, celestial wonders"),
    VEHICLES("Vehicles", "rocket_launch", "Rockets, planes, cars & ships"),
    GEOMETRY("Geometry", "polyline", "Intricate mandalas, weaves & spirals"),
    LETTERS("Letters & Numbers", "spellcheck", "Alphabets and numerals"),
    MASTER("Master", "psychology", "Complex high-density brain teasers")
}

enum class DifficultyTier(
    val displayName: String,
    val minScore: Int,
    val maxScore: Int,
    val badgeColor: Long
) {
    VERY_EASY("Very Easy", 0, 12, 0xFF10B981),
    EASY("Easy", 13, 24, 0xFF34D399),
    NORMAL("Normal", 25, 38, 0xFF3B82F6),
    MEDIUM("Medium", 39, 52, 0xFF6366F1),
    HARD("Hard", 53, 66, 0xFF8B5CF6),
    VERY_HARD("Very Hard", 67, 78, 0xFFF59E0B),
    EXTREME("Extreme", 79, 89, 0xFFF97316),
    NIGHTMARE("Nightmare", 90, 100, 0xFFEF4444);

    companion object {
        fun forLevelId(levelId: Int): DifficultyTier = when {
            levelId in 1..3 -> VERY_EASY
            levelId in 4..7 -> EASY
            levelId in 8..10 -> NORMAL
            levelId in 11..20 -> VERY_EASY
            levelId in 21..50 -> EASY
            levelId in 51..150 -> NORMAL
            levelId in 151..350 -> HARD
            levelId in 351..700 -> VERY_HARD
            else -> EXTREME
        }

        fun forScore(score: Int): DifficultyTier = when {
            score <= 12 -> VERY_EASY
            score <= 24 -> EASY
            score <= 38 -> NORMAL
            score <= 52 -> MEDIUM
            score <= 66 -> HARD
            score <= 78 -> VERY_HARD
            score <= 89 -> EXTREME
            else -> NIGHTMARE
        }

        fun forArrowCount(count: Int): DifficultyTier = when {
            count <= 12 -> VERY_EASY
            count <= 22 -> EASY
            count <= 34 -> NORMAL
            count <= 48 -> MEDIUM
            count <= 64 -> HARD
            count <= 78 -> VERY_HARD
            count <= 92 -> EXTREME
            else -> NIGHTMARE
        }
    }
}
