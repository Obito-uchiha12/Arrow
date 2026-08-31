package com.example.model

enum class Direction(val dx: Int, val dy: Int, val angleDegrees: Float) {
    UP(0, -1, 270f),
    RIGHT(1, 0, 0f),
    DOWN(0, 1, 90f),
    LEFT(-1, 0, 180f);

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}
