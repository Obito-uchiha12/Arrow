package com.example.model

data class LevelGenerationMetadata(
    val shapeId: String = "",
    val generatorVersion: String = "v1.3.0",
    val seed: Long = 0L,
    val difficultyScore: Int = 0,
    val validationScore: Float = 100f,
    val metrics: Map<String, Float> = emptyMap()
)

data class PuzzleLevel(
    val id: Int,
    val name: String,
    val category: LevelCategory,
    val arrows: List<Arrow>,
    val authorNotes: String = "",
    val metadata: LevelGenerationMetadata? = null
) {
    val arrowCount: Int get() = arrows.size
    val difficulty: DifficultyTier get() = if (metadata != null && metadata.difficultyScore > 0) {
        DifficultyTier.forScore(metadata.difficultyScore)
    } else {
        DifficultyTier.forLevelId(id)
    }

    val bounds: RectF get() {
        if (arrows.isEmpty()) return RectF(0f, 0f, 10f, 10f)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (a in arrows) {
            val bb = a.boundingBox
            if (bb.minX < minX) minX = bb.minX
            if (bb.minY < minY) minY = bb.minY
            if (bb.maxX > maxX) maxX = bb.maxX
            if (bb.maxY > maxY) maxY = bb.maxY
        }
        return RectF(minX, minY, maxX, maxY)
    }
}
