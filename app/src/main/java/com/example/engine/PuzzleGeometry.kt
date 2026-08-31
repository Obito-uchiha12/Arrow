package com.example.engine

import com.example.model.Arrow
import com.example.model.Direction
import com.example.model.GridPoint
import com.example.model.LineSegment
import com.example.model.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Geometric intersection engine and swept-path collision analysis for arbitrary polygonal arrows.
 * Evaluates orthogonal segments, L-turns, U-turns, hitboxes, and infinite forward exit corridors.
 */
object PuzzleGeometry {

    const val EPSILON = 0.05f

    data class CollisionResult(
        val isBlocked: Boolean,
        val minDistance: Float = Float.MAX_VALUE,
        val blockingArrowId: Int? = null
    )

    /**
     * Checks if movingArrow can freely travel outward along its exit trajectory [facing]
     * without colliding with any segment of other arrows currently present on the board.
     */
    fun checkArrowCollision(
        movingArrow: Arrow,
        otherArrows: List<Arrow>
    ): CollisionResult {
        var minDistance = Float.MAX_VALUE
        var blockerId: Int? = null

        val dir = movingArrow.facing
        val movingSegments = movingArrow.segments
        val sweptCorridor = movingArrow.forwardMovementCorridor(1000f)

        for (other in otherArrows) {
            if (other.id == movingArrow.id || !other.isActive) continue

            // Broad-phase AABB test against swept corridor
            if (!sweptCorridor.intersects(other.boundingBox.expanded(EPSILON))) {
                continue
            }

            val otherSegments = other.segments
            for (segA in movingSegments) {
                for (segB in otherSegments) {
                    val dist = sweptSegmentIntersectionDistance(segA, segB, dir)
                    if (dist != null && dist > EPSILON && dist < minDistance) {
                        minDistance = dist
                        blockerId = other.id
                    }
                }
            }
        }

        return if (minDistance < Float.MAX_VALUE / 2f) {
            CollisionResult(isBlocked = true, minDistance = minDistance, blockingArrowId = blockerId)
        } else {
            CollisionResult(isBlocked = false, minDistance = Float.MAX_VALUE, blockingArrowId = null)
        }
    }

    /**
     * Calculates distance along [dir] that [segA] must travel before contacting [segB].
     * Returns null if [segA] swept along [dir] does not intersect [segB].
     */
    fun sweptSegmentIntersectionDistance(
        segA: LineSegment,
        segB: LineSegment,
        dir: Direction
    ): Float? {
        val aMinX = segA.minX
        val aMaxX = segA.maxX
        val aMinY = segA.minY
        val aMaxY = segA.maxY

        val bMinX = segB.minX
        val bMaxX = segB.maxX
        val bMinY = segB.minY
        val bMaxY = segB.maxY

        when (dir) {
            Direction.RIGHT -> {
                // Must have vertical overlap in Y
                val overlapMinY = max(aMinY, bMinY)
                val overlapMaxY = min(aMaxY, bMaxY)
                if (overlapMaxY < overlapMinY - EPSILON) return null

                val dist = bMinX - aMaxX
                return if (dist > -EPSILON) max(0f, dist) else null
            }
            Direction.LEFT -> {
                // Must have vertical overlap in Y
                val overlapMinY = max(aMinY, bMinY)
                val overlapMaxY = min(aMaxY, bMaxY)
                if (overlapMaxY < overlapMinY - EPSILON) return null

                val dist = aMinX - bMaxX
                return if (dist > -EPSILON) max(0f, dist) else null
            }
            Direction.DOWN -> {
                // Must have horizontal overlap in X
                val overlapMinX = max(aMinX, bMinX)
                val overlapMaxX = min(aMaxX, bMaxX)
                if (overlapMaxX < overlapMinX - EPSILON) return null

                val dist = bMinY - aMaxY
                return if (dist > -EPSILON) max(0f, dist) else null
            }
            Direction.UP -> {
                // Must have horizontal overlap in X
                val overlapMinX = max(aMinX, bMinX)
                val overlapMaxX = min(aMaxX, bMaxX)
                if (overlapMaxX < overlapMinX - EPSILON) return null

                val dist = aMinY - bMaxY
                return if (dist > -EPSILON) max(0f, dist) else null
            }
        }
    }

    /**
     * Checks if two static line segments directly intersect or overlap each other.
     */
    fun segmentsIntersect(seg1: LineSegment, seg2: LineSegment): Boolean {
        val p1 = seg1.start
        val p2 = seg1.end
        val p3 = seg2.start
        val p4 = seg2.end

        fun ccw(a: GridPoint, b: GridPoint, c: GridPoint): Float {
            return (c.y - a.y) * (b.x - a.x) - (b.y - a.y) * (c.x - a.x)
        }

        val d1 = ccw(p3, p4, p1)
        val d2 = ccw(p3, p4, p2)
        val d3 = ccw(p1, p2, p3)
        val d4 = ccw(p1, p2, p4)

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true
        }

        // Collinear / touching overlap check
        if (abs(d1) < 0.001f && isPointOnSegment(p1, seg2)) return true
        if (abs(d2) < 0.001f && isPointOnSegment(p2, seg2)) return true
        if (abs(d3) < 0.001f && isPointOnSegment(p3, seg1)) return true
        if (abs(d4) < 0.001f && isPointOnSegment(p4, seg1)) return true

        return false
    }

    fun isPointOnSegment(p: GridPoint, seg: LineSegment): Boolean {
        return p.x in (seg.minX - EPSILON)..(seg.maxX + EPSILON) &&
                p.y in (seg.minY - EPSILON)..(seg.maxY + EPSILON) &&
                distancePointToSegment(p, seg) < EPSILON
    }

    /**
     * Finds the closest arrow to the given (tapX, tapY) coordinates.
     */
    fun findTappedArrow(
        tapX: Float,
        tapY: Float,
        arrows: List<Arrow>,
        hitRadius: Float = 0.75f
    ): Arrow? {
        var closestArrow: Arrow? = null
        var minDistance = Float.MAX_VALUE
        val tapPoint = GridPoint(tapX, tapY)

        for (arrow in arrows) {
            if (!arrow.isActive) continue

            // Check distance to all segments
            for (seg in arrow.segments) {
                val dist = distancePointToSegment(tapPoint, seg)
                if (dist < hitRadius && dist < minDistance) {
                    minDistance = dist
                    closestArrow = arrow
                }
            }

            // Check distance to arrowhead tip
            val headDist = tapPoint.distanceTo(arrow.head)
            if (headDist < hitRadius && headDist < minDistance) {
                minDistance = headDist
                closestArrow = arrow
            }
        }

        return closestArrow
    }

    fun distancePointToSegment(p: GridPoint, seg: LineSegment): Float {
        return distancePointToSegment(p, seg.start, seg.end)
    }

    fun distancePointToSegment(p: GridPoint, a: GridPoint, b: GridPoint): Float {
        val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
        if (l2 < 0.0001f) return p.distanceTo(a)
        val t = max(0f, min(1f, ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2))
        val proj = GridPoint(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
        return p.distanceTo(proj)
    }
}
