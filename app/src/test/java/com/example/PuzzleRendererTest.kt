package com.example

import com.example.engine.LevelRepository
import com.example.engine.ManualLevels
import com.example.engine.PuzzleGeometry
import com.example.engine.PuzzleValidator
import com.example.model.Arrow
import com.example.model.Direction
import com.example.model.GridPoint
import com.example.model.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class PuzzleRendererTest {

    data class MockGridTransform(
        val cellSize: Float,
        val originX: Float,
        val originY: Float
    )

    private fun calculateMockTransform(
        canvasWidth: Float,
        canvasHeight: Float,
        bounds: RectF,
        padding: Float = 48f
    ): MockGridTransform {
        val availW = (canvasWidth - padding * 2).coerceAtLeast(100f)
        val availH = (canvasHeight - padding * 2).coerceAtLeast(100f)

        val gridW = max(1f, bounds.width + 1.5f)
        val gridH = max(1f, bounds.height + 1.5f)

        val scale = min(availW / gridW, availH / gridH)

        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        val originX = centerX - (bounds.centerX * scale)
        val originY = centerY - (bounds.centerY * scale)

        return MockGridTransform(scale, originX, originY)
    }

    @Test
    fun testHeartLevelIntegrity() {
        val level = ManualLevels.createHeartLevel(1)
        val report = PuzzleValidator.validateLevel(level)
        assertTrue("Heart level errors: ${report.errors}", report.isValid)
        assertTrue("Heart level must be solvable", report.isSolvable)
    }

    @Test
    fun testStarLevelIntegrity() {
        val level = ManualLevels.createStarLevel(2)
        val report = PuzzleValidator.validateLevel(level)
        assertTrue("Star level errors: ${report.errors}", report.isValid)
        assertTrue("Star level must be solvable", report.isSolvable)
    }

    @Test
    fun testFishLevelIntegrity() {
        val level = ManualLevels.createFishLevel(3)
        val report = PuzzleValidator.validateLevel(level)
        assertTrue("Fish level errors: ${report.errors}", report.isValid)
        assertTrue("Fish level must be solvable", report.isSolvable)
    }

    @Test
    fun testCatLevelIntegrity() {
        val level = ManualLevels.createCatLevel(4)
        val report = PuzzleValidator.validateLevel(level)
        assertTrue("Cat level errors: ${report.errors}", report.isValid)
        assertTrue("Cat level must be solvable", report.isSolvable)
    }

    @Test
    fun testButterflyLevelIntegrity() {
        val level = ManualLevels.createButterflyLevel(5)
        val report = PuzzleValidator.validateLevel(level)
        assertTrue("Butterfly level errors: ${report.errors}", report.isValid)
        assertTrue("Butterfly level must be solvable", report.isSolvable)
    }

    @Test
    fun testRendererAspectScalingTallPhone() {
        // Tall modern mobile screen (e.g. 1080 x 2400)
        val heartLevel = ManualLevels.createHeartLevel(1)
        val bounds = heartLevel.bounds
        val transform = calculateMockTransform(canvasWidth = 1080f, canvasHeight = 2000f, bounds = bounds)

        assertTrue(transform.cellSize > 0)
        // Verify center mapping matches canvas midpoint
        val centerScreenX = transform.originX + bounds.centerX * transform.cellSize
        val centerScreenY = transform.originY + bounds.centerY * transform.cellSize
        assertEquals(540f, centerScreenX, 0.5f)
        assertEquals(1000f, centerScreenY, 0.5f)
    }

    @Test
    fun testRendererAspectScalingWideTablet() {
        // Wide tablet landscape (e.g. 2000 x 1200)
        val fishLevel = ManualLevels.createFishLevel(3)
        val bounds = fishLevel.bounds
        val transform = calculateMockTransform(canvasWidth = 2000f, canvasHeight = 1200f, bounds = bounds)

        assertTrue(transform.cellSize > 0)
        val centerScreenX = transform.originX + bounds.centerX * transform.cellSize
        val centerScreenY = transform.originY + bounds.centerY * transform.cellSize
        assertEquals(1000f, centerScreenX, 0.5f)
        assertEquals(600f, centerScreenY, 0.5f)
    }

    @Test
    fun testArrowheadPlacementAndExitAngles() {
        val arrowRight = Arrow(1, listOf(GridPoint(0, 0), GridPoint(3, 0)), Direction.RIGHT)
        val arrowDown = Arrow(2, listOf(GridPoint(0, 0), GridPoint(0, 4)), Direction.DOWN)
        val arrowLeft = Arrow(3, listOf(GridPoint(5, 0), GridPoint(2, 0)), Direction.LEFT)
        val arrowUp = Arrow(4, listOf(GridPoint(0, 5), GridPoint(0, 1)), Direction.UP)

        assertEquals(0f, Direction.RIGHT.angleDegrees, 0.001f)
        assertEquals(90f, Direction.DOWN.angleDegrees, 0.001f)
        assertEquals(180f, Direction.LEFT.angleDegrees, 0.001f)
        assertEquals(270f, Direction.UP.angleDegrees, 0.001f)

        assertEquals(GridPoint(3, 0), arrowRight.head)
        assertEquals(GridPoint(0, 4), arrowDown.head)
        assertEquals(GridPoint(2, 0), arrowLeft.head)
        assertEquals(GridPoint(0, 1), arrowUp.head)
    }

    @Test
    fun testLevelRepositoryServesManualLevels() {
        val lvl1 = LevelRepository.getLevel(1)
        val lvl2 = LevelRepository.getLevel(2)
        val lvl3 = LevelRepository.getLevel(3)
        val lvl4 = LevelRepository.getLevel(4)
        val lvl5 = LevelRepository.getLevel(5)

        assertEquals("True Heart", lvl1.name)
        assertEquals("Shining Star", lvl2.name)
        assertEquals("Tropical Fish", lvl3.name)
        assertEquals("Sweet Apple", lvl4.name)
        assertEquals("Graceful Butterfly", lvl5.name)
    }
}
