package com.example.model

/**
 * Axis-Aligned 2D Bounding Rectangle.
 */
data class RectF(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f

    fun contains(p: GridPoint): Boolean {
        return p.x in minX..maxX && p.y in minY..maxY
    }

    fun intersects(other: RectF): Boolean {
        return minX <= other.maxX && maxX >= other.minX &&
                minY <= other.maxY && maxY >= other.minY
    }

    fun expanded(padding: Float): RectF {
        return RectF(minX - padding, minY - padding, maxX + padding, maxY + padding)
    }
}
