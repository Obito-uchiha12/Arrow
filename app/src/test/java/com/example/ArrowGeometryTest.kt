package com.example

import com.example.engine.PuzzleGeometry
import com.example.model.Arrow
import com.example.model.ArrowMovementState
import com.example.model.Direction
import com.example.model.GridPoint
import com.example.model.LineSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowGeometryTest {

    @Test
    fun testHorizontalArrowGeometry() {
        // Horizontal straight arrow from (2, 5) to (6, 5) pointing RIGHT
        val arrow = Arrow(
            id = 1,
            points = listOf(GridPoint(2, 5), GridPoint(6, 5)),
            facing = Direction.RIGHT,
            strokeWidth = 2.0f,
            movementState = ArrowMovementState.IDLE
        )

        assertEquals(GridPoint(2, 5), arrow.tail)
        assertEquals(GridPoint(6, 5), arrow.head)
        assertEquals(1, arrow.segments.size)
        assertEquals(4.0f, arrow.length, 0.001f)
        assertFalse(arrow.hasTurns)

        val bb = arrow.boundingBox
        assertEquals(2.0f, bb.minX, 0.001f)
        assertEquals(6.0f, bb.maxX, 0.001f)
        assertEquals(5.0f, bb.minY, 0.001f)
        assertEquals(5.0f, bb.maxY, 0.001f)

        // Forward corridor along exit direction RIGHT
        val corridor = arrow.forwardMovementCorridor(corridorDistance = 50f)
        assertEquals(2.0f, corridor.minX, 0.001f)
        assertEquals(56.0f, corridor.maxX, 0.001f)
    }

    @Test
    fun testVerticalArrowGeometry() {
        // Vertical straight arrow from (4, 8) to (4, 2) pointing UP
        val arrow = Arrow(
            id = 2,
            points = listOf(GridPoint(4, 8), GridPoint(4, 2)),
            facing = Direction.UP
        )

        assertEquals(GridPoint(4, 8), arrow.tail)
        assertEquals(GridPoint(4, 2), arrow.head)
        assertEquals(1, arrow.segments.size)
        assertEquals(6.0f, arrow.length, 0.001f)
        assertFalse(arrow.hasTurns)

        val bb = arrow.boundingBox
        assertEquals(4.0f, bb.minX, 0.001f)
        assertEquals(4.0f, bb.maxX, 0.001f)
        assertEquals(2.0f, bb.minY, 0.001f)
        assertEquals(8.0f, bb.maxY, 0.001f)

        val corridor = arrow.forwardMovementCorridor(corridorDistance = 20f)
        assertEquals(-18.0f, corridor.minY, 0.001f)
        assertEquals(8.0f, corridor.maxY, 0.001f)
    }

    @Test
    fun testLShapedArrowGeometry() {
        // L-shaped arrow: Starts at (0, 0) -> goes RIGHT to (4, 0) -> turns 90-deg DOWN to (4, 5) pointing DOWN
        val lArrow = Arrow(
            id = 3,
            points = listOf(
                GridPoint(0, 0),
                GridPoint(4, 0),
                GridPoint(4, 5)
            ),
            facing = Direction.DOWN
        )

        assertEquals(GridPoint(0, 0), lArrow.tail)
        assertEquals(GridPoint(4, 5), lArrow.head)
        assertEquals(2, lArrow.segments.size)
        assertTrue(lArrow.hasTurns)
        assertEquals(9.0f, lArrow.length, 0.001f) // 4 + 5 = 9

        val bb = lArrow.boundingBox
        assertEquals(0.0f, bb.minX, 0.001f)
        assertEquals(4.0f, bb.maxX, 0.001f)
        assertEquals(0.0f, bb.minY, 0.001f)
        assertEquals(5.0f, bb.maxY, 0.001f)

        val corridor = lArrow.forwardMovementCorridor(corridorDistance = 30f)
        assertEquals(35.0f, corridor.maxY, 0.001f)
    }

    @Test
    fun testUShapedArrowGeometry() {
        // U-shaped arrow: (2, 0) -> DOWN to (2, 4) -> RIGHT to (6, 4) -> UP to (6, 1) pointing UP
        val uArrow = Arrow(
            id = 4,
            points = listOf(
                GridPoint(2, 0),
                GridPoint(2, 4),
                GridPoint(6, 4),
                GridPoint(6, 1)
            ),
            facing = Direction.UP
        )

        assertEquals(GridPoint(2, 0), uArrow.tail)
        assertEquals(GridPoint(6, 1), uArrow.head)
        assertEquals(3, uArrow.segments.size)
        assertTrue(uArrow.hasTurns)
        assertEquals(11.0f, uArrow.length, 0.001f) // 4 + 4 + 3 = 11

        val bb = uArrow.boundingBox
        assertEquals(2.0f, bb.minX, 0.001f)
        assertEquals(6.0f, bb.maxX, 0.001f)
        assertEquals(0.0f, bb.minY, 0.001f)
        assertEquals(4.0f, bb.maxY, 0.001f)
    }

    @Test
    fun testOverlappingPathsIntersection() {
        // Segment A on horizontal line (0, 2) to (6, 2)
        val segA = LineSegment(GridPoint(0, 2), GridPoint(6, 2))
        // Segment B crossing perpendicularly at (3, 0) to (3, 5)
        val segB = LineSegment(GridPoint(3, 0), GridPoint(3, 5))

        assertTrue(PuzzleGeometry.segmentsIntersect(segA, segB))

        // Collinear overlapping segments
        val segC = LineSegment(GridPoint(2, 2), GridPoint(8, 2))
        assertTrue(PuzzleGeometry.segmentsIntersect(segA, segC))
    }

    @Test
    fun testNearbyNonCollidingPaths() {
        // Arrow 1: Horizontal at y=2, pointing RIGHT
        val arrow1 = Arrow(
            id = 10,
            points = listOf(GridPoint(0, 2), GridPoint(4, 2)),
            facing = Direction.RIGHT
        )
        // Arrow 2: Parallel horizontal at y=4 (distance of 2 units apart), pointing RIGHT
        val arrow2 = Arrow(
            id = 11,
            points = listOf(GridPoint(6, 4), GridPoint(10, 4)),
            facing = Direction.RIGHT
        )

        val collision1 = PuzzleGeometry.checkArrowCollision(arrow1, listOf(arrow1, arrow2))
        assertFalse("Nearby parallel arrow on different lane should not block", collision1.isBlocked)

        val collision2 = PuzzleGeometry.checkArrowCollision(arrow2, listOf(arrow1, arrow2))
        assertFalse("Arrow 2 should also not be blocked", collision2.isBlocked)
    }

    @Test
    fun testPerpendicularPathsCollision() {
        // Arrow A: pointing RIGHT at y=3 from x=0 to x=3
        val arrowA = Arrow(
            id = 20,
            points = listOf(GridPoint(0, 3), GridPoint(3, 3)),
            facing = Direction.RIGHT
        )
        // Arrow B: vertical obstacle in front of A at x=6, running from y=1 to y=5, pointing UP
        val arrowB = Arrow(
            id = 21,
            points = listOf(GridPoint(6, 5), GridPoint(6, 1)),
            facing = Direction.UP
        )

        // Arrow A moving RIGHT will hit Arrow B at x=6
        val collisionA = PuzzleGeometry.checkArrowCollision(arrowA, listOf(arrowA, arrowB))
        assertTrue("Arrow A should be blocked by perpendicular Arrow B", collisionA.isBlocked)
        assertEquals(21, collisionA.blockingArrowId)
        assertEquals(3.0f, collisionA.minDistance, 0.01f) // from x=3 to x=6 is distance 3

        // Arrow B moving UP does not have any obstacle above it (Arrow A is at y=3, B head is at y=1, exiting UP)
        val collisionB = PuzzleGeometry.checkArrowCollision(arrowB, listOf(arrowA, arrowB))
        assertFalse("Arrow B moving UP should be clear to exit", collisionB.isBlocked)
    }

    @Test
    fun testParallelPathsCollision() {
        // Arrow 1: at y=5, x from 0 to 4, pointing RIGHT
        val arrow1 = Arrow(
            id = 30,
            points = listOf(GridPoint(0, 5), GridPoint(4, 5)),
            facing = Direction.RIGHT
        )
        // Arrow 2: directly in front on the same lane y=5, x from 7 to 11, pointing RIGHT
        val arrow2 = Arrow(
            id = 31,
            points = listOf(GridPoint(7, 5), GridPoint(11, 5)),
            facing = Direction.RIGHT
        )

        // Arrow 1 moving RIGHT will collide with Arrow 2
        val col1 = PuzzleGeometry.checkArrowCollision(arrow1, listOf(arrow1, arrow2))
        assertTrue("Arrow 1 should be blocked by Arrow 2 in its line of flight", col1.isBlocked)
        assertEquals(31, col1.blockingArrowId)
        assertEquals(3.0f, col1.minDistance, 0.01f) // from x=4 to x=7 is 3 units

        // Arrow 2 has a clear runway ahead
        val col2 = PuzzleGeometry.checkArrowCollision(arrow2, listOf(arrow1, arrow2))
        assertFalse("Arrow 2 is at front of line and should not be blocked", col2.isBlocked)
    }

    @Test
    fun testLShapedObstacleCollision() {
        // L-shaped arrow: (0,0) -> (4,0) -> (4,4) pointing DOWN
        val lArrow = Arrow(
            id = 40,
            points = listOf(GridPoint(0, 0), GridPoint(4, 0), GridPoint(4, 4)),
            facing = Direction.DOWN
        )

        // Obstacle arrow beneath the exit corridor at y=7, x from 2 to 6
        val obstacle = Arrow(
            id = 41,
            points = listOf(GridPoint(2, 7), GridPoint(6, 7)),
            facing = Direction.RIGHT
        )

        val col = PuzzleGeometry.checkArrowCollision(lArrow, listOf(lArrow, obstacle))
        assertTrue("L-shaped arrow moving down should collide with horizontal obstacle below", col.isBlocked)
        assertEquals(41, col.blockingArrowId)
        assertEquals(3.0f, col.minDistance, 0.01f) // y=4 to y=7 is distance 3
    }
}
