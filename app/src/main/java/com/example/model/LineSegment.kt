package com.example.model

/**
 * Represents a single line segment connecting two points in continuous or grid space.
 */
data class LineSegment(
    val start: GridPoint,
    val end: GridPoint
) {
    val length: Float get() = start.distanceTo(end)
    val isHorizontal: Boolean get() = kotlin.math.abs(start.y - end.y) < 0.001f
    val isVertical: Boolean get() = kotlin.math.abs(start.x - end.x) < 0.001f

    val minX: Float get() = kotlin.math.min(start.x, end.x)
    val maxX: Float get() = kotlin.math.max(start.x, end.x)
    val minY: Float get() = kotlin.math.min(start.y, end.y)
    val maxY: Float get() = kotlin.math.max(start.y, end.y)

    val direction: Direction? get() {
        return when {
            isHorizontal && end.x > start.x -> Direction.RIGHT
            isHorizontal && end.x < start.x -> Direction.LEFT
            isVertical && end.y > start.y -> Direction.DOWN
            isVertical && end.y < start.y -> Direction.UP
            else -> null
        }
    }

    fun boundingBox(padding: Float = 0f): RectF {
        return RectF(
            minX = minX - padding,
            minY = minY - padding,
            maxX = maxX + padding,
            maxY = maxY + padding
        )
    }
}
