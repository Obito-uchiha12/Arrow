package com.example.model

/**
 * State of the arrow in the game lifecycle.
 */
enum class ArrowMovementState {
    IDLE,       // Resting on the puzzle silhouette board
    FLYING_OUT, // Clearable and animating outward along its exit vector
    BLOCKED_NUDGE, // Tap attempted but blocked, undergoing spring nudge & recoil
    REMOVED     // Fully exited from the board
}

/**
 * Strict Arrow State Machine definition:
 * ACTIVE: Normal state on board, responsive to taps.
 * BLOCKED: Collision detected, undergoing visual nudge recoil feedback.
 * MOVING: Valid clearable arrow animating outward beyond board perimeter.
 * REMOVED: Successfully cleared and removed from active board.
 */
enum class ArrowState {
    ACTIVE,
    BLOCKED,
    MOVING,
    REMOVED;

    val isInteractive: Boolean get() = this == ACTIVE

    fun canTransitionTo(next: ArrowState): Boolean = when (this) {
        ACTIVE -> next == MOVING || next == BLOCKED
        BLOCKED -> next == ACTIVE || next == MOVING
        MOVING -> next == REMOVED
        REMOVED -> false // Cannot transition from REMOVED
    }
}

/**
 * Robust geometric and visual data model for individual puzzle arrows.
 * Supports multi-segment polylines with 90-degree orthogonal turns (e.g. L-shaped, U-shaped, S-shaped),
 * arrowhead position, final exit direction, forward movement corridor, visual customization, and lifecycle states.
 */
data class Arrow(
    val id: Int,
    val points: List<GridPoint>,
    val facing: Direction,
    val strokeWidth: Float = 1.0f,
    val colorOverride: Long? = null,
    val movementState: ArrowMovementState = ArrowMovementState.IDLE,
    val state: ArrowState = ArrowState.ACTIVE,
    val isActive: Boolean = true
) {
    init {
        require(points.size >= 2) { "Arrow must contain at least 2 points to define path geometry. Id: $id" }
    }

    /** The starting tail position of the arrow path. */
    val tail: GridPoint get() = points.first()

    /** The terminal arrowhead position where the arrow tip resides and points in [facing] direction. */
    val head: GridPoint get() = points.last()

    /** All contiguous line segments making up this arrow polyline. */
    val segments: List<LineSegment> get() {
        val segs = ArrayList<LineSegment>(points.size - 1)
        for (i in 0 until points.size - 1) {
            segs.add(LineSegment(points[i], points[i + 1]))
        }
        return segs
    }

    /** Returns total linear length of all segments combined. */
    val length: Float get() = segments.sumOf { it.length.toDouble() }.toFloat()

    /** Axis-aligned bounding box enclosing all points in this arrow. */
    val boundingBox: RectF get() {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        return RectF(minX, minY, maxX, maxY)
    }

    /**
     * Computes the swept 2D forward movement corridor bounding box when this arrow travels outward
     * up to [corridorDistance] units along its [facing] exit trajectory.
     */
    fun forwardMovementCorridor(corridorDistance: Float = 100f): RectF {
        val bb = boundingBox
        return when (facing) {
            Direction.RIGHT -> RectF(bb.minX, bb.minY, bb.maxX + corridorDistance, bb.maxY)
            Direction.LEFT -> RectF(bb.minX - corridorDistance, bb.minY, bb.maxX, bb.maxY)
            Direction.DOWN -> RectF(bb.minX, bb.minY, bb.maxX, bb.maxY + corridorDistance)
            Direction.UP -> RectF(bb.minX, bb.minY - corridorDistance, bb.maxX, bb.maxY)
        }
    }

    /**
     * Checks if this arrow contains any 90-degree orthogonal turns along its path.
     */
    val hasTurns: Boolean get() = points.size > 2

    /**
     * Returns a copy of the arrow translated by [dx] and [dy].
     */
    fun translated(dx: Float, dy: Float): Arrow {
        return copy(points = points.map { GridPoint(it.x + dx, it.y + dy) })
    }
}
