package com.example.model

data class GridPoint(
    val x: Float,
    val y: Float
) {
    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

    fun distanceTo(other: GridPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun plus(dx: Float, dy: Float): GridPoint = GridPoint(x + dx, y + dy)
    fun plus(dir: Direction, step: Float = 1f): GridPoint = GridPoint(x + dir.dx * step, y + dir.dy * step)
}
