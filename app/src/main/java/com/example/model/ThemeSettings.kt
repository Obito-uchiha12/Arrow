package com.example.model

enum class BoardTheme(
    val id: String,
    val displayName: String,
    val backgroundColor: Long,
    val surfaceColor: Long,
    val primaryColor: Long,
    val accentColor: Long,
    val isDark: Boolean
) {
    LIGHT_ZEN("light_zen", "Zen Light", 0xFFF8FAFC, 0xFFFFFFFF, 0xFF0F172A, 0xFF6366F1, false),
    WARM_PAPER("warm_paper", "Warm Canvas", 0xFFFDF6E2, 0xFFFFFBEF, 0xFF292524, 0xFFD97706, false),
    MIDNIGHT("midnight", "Midnight Dark", 0xFF0F172A, 0xFF1E293B, 0xFFF8FAFC, 0xFF818CF8, true),
    OBSIDIAN("obsidian", "Obsidian Noir", 0xFF09090B, 0xFF18181B, 0xFFFAFAFA, 0xFF38BDF8, true),
    EMERALD_NIGHT("emerald_night", "Forest Night", 0xFF064E3B, 0xFF065F46, 0xFFECFDF5, 0xFF34D399, true),
    PURPLE_DREAM("purple_dream", "Velvet Dusk", 0xFF2E1065, 0xFF3B0764, 0xFFFAF5FF, 0xFFA855F7, true),
    SOLAR_AMBER("solar_amber", "Amber Sunset", 0xFFFFFBEB, 0xFFFEF3C7, 0xFF451A03, 0xFFEA580C, false)
}

enum class ArrowColorPreset(
    val id: String,
    val displayName: String,
    val lightColorValue: Long,
    val darkColorValue: Long
) {
    DEFAULT_DARK("default_dark", "Black", 0xFF0F172A, 0xFFF8FAFC),
    DARK_GRAY("dark_gray", "Dark Gray", 0xFF475569, 0xFFCBD5E1),
    BLUE("blue", "Blue", 0xFF2563EB, 0xFF60A5FA),
    RED("red", "Red", 0xFFDC2626, 0xFFF87171),
    GREEN("green", "Green", 0xFF059669, 0xFF34D399),
    PURPLE("purple", "Purple", 0xFF7C3AED, 0xFFA78BFA),
    ORANGE("orange", "Orange", 0xFFEA580C, 0xFFFB923C),
    CYBER_CYAN("cyber_cyan", "Cyan", 0xFF0284C7, 0xFF38BDF8),
    AMBER_GOLD("amber_gold", "Amber", 0xFFD97706, 0xFFFBBF24),
    ROSE_PINK("rose_pink", "Rose", 0xFFDB2777, 0xFFF472B6),
    CLEAN_WHITE("clean_white", "Frost White", 0xFF334155, 0xFFF8FAFC);

    val colorValue: Long get() = lightColorValue

    fun getColorForTheme(isDarkTheme: Boolean): Long {
        return if (isDarkTheme) darkColorValue else lightColorValue
    }

    companion object {
        fun fromId(id: String?): ArrowColorPreset {
            return when (id) {
                "black", "default_dark" -> DEFAULT_DARK
                "dark_gray" -> DARK_GRAY
                "blue", "midnight_blue" -> BLUE
                "red", "crimson_ruby" -> RED
                "green", "emerald" -> GREEN
                "purple", "amethyst" -> PURPLE
                "orange" -> ORANGE
                "cyber_cyan", "cyan" -> CYBER_CYAN
                "amber_gold", "amber" -> AMBER_GOLD
                "rose_pink", "rose" -> ROSE_PINK
                "clean_white", "white" -> CLEAN_WHITE
                else -> values().find { it.id == id } ?: DEFAULT_DARK
            }
        }
    }
}

enum class ArrowThickness(val displayName: String, val strokeDp: Float) {
    THIN("Fine (2.5dp)", 2.5f),
    MEDIUM("Standard (3.5dp)", 3.5f),
    BOLD("Bold (5.0dp)", 5.0f)
}

enum class AnimationSpeed(
    val id: String,
    val displayName: String,
    val flightDurationMs: Int,
    val blockedDurationMs: Int
) {
    RELAXED("relaxed", "Gentle (380ms)", 380, 80),
    NORMAL("normal", "Standard (260ms)", 260, 60),
    FAST("fast", "Brisk (160ms)", 160, 45)
}

data class UserSettings(
    val boardTheme: BoardTheme = BoardTheme.LIGHT_ZEN,
    val arrowColor: ArrowColorPreset = ArrowColorPreset.DEFAULT_DARK,
    val arrowThickness: ArrowThickness = ArrowThickness.MEDIUM,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val zenMode: Boolean = false,
    val autoNextLevel: Boolean = true,
    val highlightHintsCount: Int = 5,
    val autoMoveHintsCount: Int = 3,
    val heartsCount: Int = 3
)
