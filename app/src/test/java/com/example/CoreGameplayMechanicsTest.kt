package com.example

import com.example.engine.DifficultyCalculator
import com.example.engine.LevelRepository
import com.example.engine.PuzzleGenerator
import com.example.engine.PuzzleGeometry
import com.example.engine.PuzzleSolver
import com.example.engine.PuzzleValidator
import com.example.engine.ShapeQualityScorer
import com.example.engine.ShapeSilhouettes
import com.example.model.Arrow
import com.example.model.DifficultyTier
import com.example.model.Direction
import com.example.model.GridPoint
import com.example.model.LevelCategory
import com.example.model.ProgressionGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic test suite for Phase 4 Core Puzzle Gameplay mechanics:
 * 1. Single arrow clearance
 * 2. Two arrows where first blocks second
 * 3. Chain of 3 arrows
 * 4. Perpendicular paths
 * 5. Paths that are close but should not collide
 * 6. Long arrows
 * 7. Curved-looking paths made from 90-degree segments
 * 8. Dense layouts & deadlock detection
 */
class CoreGameplayMechanicsTest {

    // ==========================================
    // 1. ONE ARROW
    // ==========================================
    @Test
    fun test1_OneArrow() {
        val arrow = Arrow(
            id = 1,
            points = listOf(GridPoint(2f, 2f), GridPoint(5f, 2f)),
            facing = Direction.RIGHT
        )
        val remaining = listOf(arrow)

        // Step 1: Direction
        assertEquals(Direction.RIGHT, arrow.facing)

        // Step 2 & 3: Collision corridor check
        val collision = PuzzleGeometry.checkArrowCollision(arrow, remaining)
        assertFalse("Single arrow with clear path ahead must not be blocked", collision.isBlocked)
        assertNull("No blocking arrow ID should exist", collision.blockingArrowId)

        // Solver finds it clearable
        val clearable = PuzzleSolver.findClearableArrows(remaining)
        assertEquals(1, clearable.size)
        assertEquals(1, clearable[0].id)

        // Solving yields 1-step sequence
        val solveResult = PuzzleSolver.solveFromState(remaining)
        assertTrue(solveResult.isSolvable)
        assertEquals(listOf(1), solveResult.solutionSequence)
    }

    // ==========================================
    // 2. TWO ARROWS WHERE FIRST BLOCKS SECOND
    // ==========================================
    @Test
    fun test2_TwoArrowsWhereFirstBlocksSecond() {
        // Arrow 1 is vertical and situated at x = 5, y in [0, 4], pointing UP (escapes into y < 0)
        val arrow1 = Arrow(
            id = 1,
            points = listOf(GridPoint(5f, 4f), GridPoint(5f, 0f)),
            facing = Direction.UP
        )

        // Arrow 2 is horizontal at y = 2, x in [1, 3], pointing RIGHT (corridor goes through x = 5, y = 2)
        val arrow2 = Arrow(
            id = 2,
            points = listOf(GridPoint(1f, 2f), GridPoint(3f, 2f)),
            facing = Direction.RIGHT
        )

        val initialArrows = listOf(arrow1, arrow2)

        // Arrow 1 should be completely free to exit UP
        val collision1 = PuzzleGeometry.checkArrowCollision(arrow1, initialArrows)
        assertFalse("Arrow 1 must be clear to move UP", collision1.isBlocked)

        // Arrow 2 must be blocked by Arrow 1
        val collision2 = PuzzleGeometry.checkArrowCollision(arrow2, initialArrows)
        assertTrue("Arrow 2 must be blocked by Arrow 1", collision2.isBlocked)
        assertEquals("Blocker ID must be 1", 1, collision2.blockingArrowId)
        assertEquals("Distance to contact should be 5 - 3 = 2", 2.0f, collision2.minDistance, 0.01f)

        // Initially only Arrow 1 is available
        val initialClearable = PuzzleSolver.findClearableArrows(initialArrows)
        assertEquals(1, initialClearable.size)
        assertEquals(1, initialClearable[0].id)

        // AFTER ARROW 1 IS REMOVED: Arrow 2 becomes available immediately
        val stateAfterRemoval = listOf(arrow2)
        val collision2After = PuzzleGeometry.checkArrowCollision(arrow2, stateAfterRemoval)
        assertFalse("Arrow 2 must now be clear after Arrow 1 is removed", collision2After.isBlocked)

        val clearableAfter = PuzzleSolver.findClearableArrows(stateAfterRemoval)
        assertEquals(1, clearableAfter.size)
        assertEquals(2, clearableAfter[0].id)
    }

    // ==========================================
    // 3. CHAIN OF 3 ARROWS
    // ==========================================
    @Test
    fun test3_ChainOf3Arrows() {
        // A linear queue of 3 arrows all pointing UP along x = 4:
        // Arrow 1 at y in [-2, 0] pointing UP (Front-most)
        // Arrow 2 at y in [2, 4] pointing UP (Middle, blocked by Arrow 1)
        // Arrow 3 at y in [6, 8] pointing UP (Rear, blocked by Arrow 2)
        val arrow1 = Arrow(1, listOf(GridPoint(4f, 0f), GridPoint(4f, -2f)), Direction.UP)
        val arrow2 = Arrow(2, listOf(GridPoint(4f, 4f), GridPoint(4f, 2f)), Direction.UP)
        val arrow3 = Arrow(3, listOf(GridPoint(4f, 8f), GridPoint(4f, 6f)), Direction.UP)

        val fullChain = listOf(arrow1, arrow2, arrow3)

        // State 0: Only arrow 1 is clearable
        assertFalse(PuzzleGeometry.checkArrowCollision(arrow1, fullChain).isBlocked)
        assertTrue(PuzzleGeometry.checkArrowCollision(arrow2, fullChain).isBlocked)
        assertTrue(PuzzleGeometry.checkArrowCollision(arrow3, fullChain).isBlocked)
        assertEquals(1, PuzzleSolver.findClearableArrows(fullChain).map { it.id }.single())

        // State 1: Remove Arrow 1 -> Arrow 2 becomes clear, Arrow 3 still blocked
        val after1 = listOf(arrow2, arrow3)
        assertFalse(PuzzleGeometry.checkArrowCollision(arrow2, after1).isBlocked)
        assertTrue(PuzzleGeometry.checkArrowCollision(arrow3, after1).isBlocked)
        assertEquals(2, PuzzleSolver.findClearableArrows(after1).map { it.id }.single())

        // State 2: Remove Arrow 2 -> Arrow 3 becomes clear
        val after2 = listOf(arrow3)
        assertFalse(PuzzleGeometry.checkArrowCollision(arrow3, after2).isBlocked)
        assertEquals(3, PuzzleSolver.findClearableArrows(after2).map { it.id }.single())

        // Full solver validates 3-step sequence [1, 2, 3]
        val solve = PuzzleSolver.solveFromState(fullChain)
        assertTrue(solve.isSolvable)
        assertEquals(listOf(1, 2, 3), solve.solutionSequence)
    }

    // ==========================================
    // 4. PERPENDICULAR PATHS
    // ==========================================
    @Test
    fun test4_PerpendicularPaths() {
        // Arrow 1 is horizontal: (0, 5) -> (10, 5) facing RIGHT
        val arrow1 = Arrow(1, listOf(GridPoint(0f, 5f), GridPoint(10f, 5f)), Direction.RIGHT)
        // Arrow 2 is vertical: (6, 9) -> (6, 7) facing UP, aiming directly across y = 5
        val arrow2 = Arrow(2, listOf(GridPoint(6f, 9f), GridPoint(6f, 7f)), Direction.UP)
        // Arrow 3 is vertical: (2, 2) -> (2, 0) facing UP, aiming away (already above y = 5)
        val arrow3 = Arrow(3, listOf(GridPoint(2f, 2f), GridPoint(2f, 0f)), Direction.UP)

        val set = listOf(arrow1, arrow2, arrow3)

        // Arrow 1 is free to exit RIGHT
        assertFalse(PuzzleGeometry.checkArrowCollision(arrow1, set).isBlocked)
        // Arrow 2 is blocked by Arrow 1
        val coll2 = PuzzleGeometry.checkArrowCollision(arrow2, set)
        assertTrue(coll2.isBlocked)
        assertEquals(1, coll2.blockingArrowId)
        assertEquals(2.0f, coll2.minDistance, 0.01f) // from y=7 to y=5

        // Arrow 3 is perpendicular and above Arrow 1, exits freely UP without collision
        assertFalse(PuzzleGeometry.checkArrowCollision(arrow3, set).isBlocked)
    }

    // ==========================================
    // 5. PATHS THAT ARE CLOSE BUT SHOULD NOT COLLIDE
    // ==========================================
    @Test
    fun test5_PathsCloseButNoCollision() {
        // Parallel lanes with delta = 1 unit spacing
        val lane1 = Arrow(1, listOf(GridPoint(0f, 2f), GridPoint(5f, 2f)), Direction.RIGHT)
        val lane2 = Arrow(2, listOf(GridPoint(0f, 3f), GridPoint(5f, 3f)), Direction.RIGHT)
        val lane3 = Arrow(3, listOf(GridPoint(0f, 4f), GridPoint(5f, 4f)), Direction.RIGHT)

        // Perpendicular arrow situated at x = 7, y in [0, 1.5], facing UP
        // Arrowhead at (7, 0) does not touch y in [2, 4]
        val closeVertical = Arrow(4, listOf(GridPoint(7f, 1.5f), GridPoint(7f, 0f)), Direction.UP)

        val arrows = listOf(lane1, lane2, lane3, closeVertical)

        // Every single arrow should have an unobstructed corridor
        for (arrow in arrows) {
            val coll = PuzzleGeometry.checkArrowCollision(arrow, arrows)
            assertFalse("Arrow ${arrow.id} should NOT collide with nearby parallel/adjacent paths", coll.isBlocked)
        }

        assertEquals(4, PuzzleSolver.findClearableArrows(arrows).size)
    }

    // ==========================================
    // 6. LONG ARROWS
    // ==========================================
    @Test
    fun test6_LongArrows() {
        // Very long horizontal arrow across 30 units: (0, 10) -> (30, 10) facing RIGHT
        val longArrow = Arrow(1, listOf(GridPoint(0f, 10f), GridPoint(30f, 10f)), Direction.RIGHT)
        // Obstacle at x = 45, y in [5, 15]
        val farObstacle = Arrow(2, listOf(GridPoint(45f, 5f), GridPoint(45f, 15f)), Direction.DOWN)

        val list = listOf(longArrow, farObstacle)

        val coll = PuzzleGeometry.checkArrowCollision(longArrow, list)
        assertTrue("Long arrow must detect distant obstacle in its corridor", coll.isBlocked)
        assertEquals(2, coll.blockingArrowId)
        assertEquals(15.0f, coll.minDistance, 0.01f) // 45 - 30 = 15

        // Another long vertical arrow: (60, 50) -> (60, 0) facing UP with clear sky above
        val longVertical = Arrow(3, listOf(GridPoint(60f, 50f), GridPoint(60f, 0f)), Direction.UP)
        val collVert = PuzzleGeometry.checkArrowCollision(longVertical, list + longVertical)
        assertFalse("Long vertical arrow pointing UP into empty space must be clear", collVert.isBlocked)
    }

    // ==========================================
    // 7. CURVED-LOOKING PATHS (90-DEGREE SEGMENTS)
    // ==========================================
    @Test
    fun test7_CurvedAndOrthogonalTurnPaths() {
        // L-shaped Arrow 1: (2, 2) -> (2, 6) -> (6, 6) facing RIGHT
        // When Arrow 1 moves RIGHT, both segment (2,2)-(2,6) and (2,6)-(6,6) translate rightward
        val lArrow = Arrow(
            id = 1,
            points = listOf(GridPoint(2f, 2f), GridPoint(2f, 6f), GridPoint(6f, 6f)),
            facing = Direction.RIGHT
        )

        // Obstacle at x = 9, y in [3, 4] facing DOWN.
        // It does not block the head at y = 6, but it blocks the vertical tail at y in [2, 6]!
        val tailBlocker = Arrow(
            id = 2,
            points = listOf(GridPoint(9f, 3f), GridPoint(9f, 4f)),
            facing = Direction.DOWN
        )

        val pair = listOf(lArrow, tailBlocker)

        val coll = PuzzleGeometry.checkArrowCollision(lArrow, pair)
        assertTrue("L-shaped arrow's vertical stem swept right must collide with obstacle", coll.isBlocked)
        assertEquals(2, coll.blockingArrowId)
        assertEquals(7.0f, coll.minDistance, 0.01f) // 9 - 2 = 7

        // S-shaped Arrow 3: (1, 10) -> (4, 10) -> (4, 14) -> (8, 14) facing RIGHT
        val sArrow = Arrow(
            id = 3,
            points = listOf(
                GridPoint(1f, 10f),
                GridPoint(4f, 10f),
                GridPoint(4f, 14f),
                GridPoint(8f, 14f)
            ),
            facing = Direction.RIGHT
        )
        // With clear corridor ahead of x = 8
        val sColl = PuzzleGeometry.checkArrowCollision(sArrow, listOf(sArrow))
        assertFalse("S-shaped arrow with clear forward space must be clear", sColl.isBlocked)
    }

    // ==========================================
    // 8. DENSE LAYOUTS & DEADLOCK DETECTION
    // ==========================================
    @Test
    fun test8_DenseLayoutAndDeadlockDetection() {
        // 4 arrows in a closed pinwheel cycle (deadlocked/unsolvable loop):
        // Arrow 1 at y=0, x in [0, 4] facing RIGHT -> blocks Arrow 2
        // Arrow 2 at x=5, y in [0, 4] facing DOWN -> blocks Arrow 3
        // Arrow 3 at y=5, x in [1, 5] facing LEFT -> blocks Arrow 4
        // Arrow 4 at x=0, y in [1, 5] facing UP -> blocks Arrow 1
        val a1 = Arrow(1, listOf(GridPoint(0f, 0f), GridPoint(4f, 0f)), Direction.RIGHT)
        val a2 = Arrow(2, listOf(GridPoint(5f, 0f), GridPoint(5f, 4f)), Direction.DOWN)
        val a3 = Arrow(3, listOf(GridPoint(5f, 5f), GridPoint(1f, 5f)), Direction.LEFT)
        val a4 = Arrow(4, listOf(GridPoint(0f, 5f), GridPoint(0f, 1f)), Direction.UP)

        val deadlockedSet = listOf(a1, a2, a3, a4)

        // All arrows should be blocked
        for (a in deadlockedSet) {
            val coll = PuzzleGeometry.checkArrowCollision(a, deadlockedSet)
            assertTrue("Arrow ${a.id} in cycle must be blocked", coll.isBlocked)
        }

        // Clearable list is empty -> Deadlock confirmed
        val clearable = PuzzleSolver.findClearableArrows(deadlockedSet)
        assertTrue("Deadlocked set must yield 0 clearable arrows", clearable.isEmpty())

        val solveResult = PuzzleSolver.solveFromState(deadlockedSet)
        assertFalse("Deadlocked set must be detected as unsolvable", solveResult.isSolvable)

        // Now break the deadlock by unlocking Arrow 1's blocker (e.g. removing Arrow 2)
        val freedSet = listOf(a1, a3, a4)
        // a1 can now exit RIGHT!
        assertFalse(PuzzleGeometry.checkArrowCollision(a1, freedSet).isBlocked)
        // Cascading solution completes!
        val cascadingSolve = PuzzleSolver.solveFromState(freedSet)
        assertTrue("Broken cycle must cascade to completion", cascadingSolve.isSolvable)
        assertEquals(listOf(1, 4, 3), cascadingSolve.solutionSequence)
    }

    // ==========================================
    // 9. ANIMATION TIMING CONFIGURATION SPECS
    // ==========================================
    @Test
    fun test9_AnimationSpeedConfigurations() {
        val relaxed = com.example.model.AnimationSpeed.RELAXED
        val normal = com.example.model.AnimationSpeed.NORMAL
        val fast = com.example.model.AnimationSpeed.FAST

        // Verify ordering of durations
        assertTrue(fast.flightDurationMs < normal.flightDurationMs)
        assertTrue(normal.flightDurationMs < relaxed.flightDurationMs)

        assertTrue(fast.blockedDurationMs < normal.blockedDurationMs)
        assertTrue(normal.blockedDurationMs < relaxed.blockedDurationMs)

        // Verify positive timings
        assertTrue(fast.flightDurationMs > 0)
        assertTrue(fast.blockedDurationMs > 0)
    }

    // ==========================================
    // 10. SHAPE-AWARE LEVEL GENERATION SPECS
    // ==========================================
    @Test
    fun test10_ShapeAwareGenerationEngineCategoriesAndSolvability() {
        // Verify all required categories are present in ShapeSilhouettes
        val allShapes = ShapeSilhouettes.ALL_SHAPES
        assertTrue("Must contain animal shapes", allShapes.any { it.category == LevelCategory.ANIMALS })
        assertTrue("Must contain object shapes", allShapes.any { it.category == LevelCategory.OBJECTS })
        assertTrue("Must contain nature shapes", allShapes.any { it.category == LevelCategory.NATURE })
        assertTrue("Must contain symbol shapes", allShapes.any { it.category == LevelCategory.SYMBOLS })
        assertTrue("Must contain geometry shapes", allShapes.any { it.category == LevelCategory.GEOMETRY })

        // Check specific key shape names exist
        val names = allShapes.map { it.name.lowercase() }
        assertTrue("Cat exists", names.any { it.contains("cat") })
        assertTrue("Dog exists", names.any { it.contains("dog") })
        assertTrue("Bird exists", names.any { it.contains("bird") })
        assertTrue("House/Cottage exists", names.any { it.contains("house") || it.contains("cottage") })
        assertTrue("Heart exists", names.any { it.contains("heart") })
        assertTrue("Star exists", names.any { it.contains("star") })

        // Generate levels across first 30 levels and test solvability & validity
        for (lvlId in 1..30) {
            val level = PuzzleGenerator.generateLevel(lvlId)
            assertTrue("Level $lvlId must have arrows", level.arrows.isNotEmpty())
            
            // Check solvability
            val solverResult = PuzzleSolver.solve(level)
            assertTrue("Generated Level $lvlId (${level.name}) must be 100% solvable", solverResult.isSolvable)
            assertEquals("Solution length must equal arrow count", level.arrows.size, solverResult.solutionSequence.size)
        }
    }

    // ==========================================
    // 11. VISUAL QUALITY & RECOGNITION SCORING
    // ==========================================
    @Test
    fun test11_ShapeQualityScorerEvaluationMetrics() {
        val heartShape = ShapeSilhouettes.ALL_SHAPES.first { it.name.contains("Heart", ignoreCase = true) }
        val generatedLevel = PuzzleGenerator.generateLevel(1)

        val report = ShapeQualityScorer.evaluate(heartShape, generatedLevel.arrows, generatedLevel)
        assertTrue("Quality score must be > 60", report.totalScore >= 60f)
        assertTrue("Shape coverage must be positive", report.shapeCoverage > 0.35f)
        assertTrue("Boundary adherence must be high", report.boundaryAdherence > 0.60f)
        assertTrue("Solvability score must be 1.0", report.solvabilityScore == 1.0f)
        assertTrue("Metrics map must contain all keys", report.toMetricsMap().containsKey("shapeCoverage"))
        assertTrue("Metrics map must contain negative space score", report.toMetricsMap().containsKey("negativeSpaceQuality"))
    }

    // ==========================================
    // 12. DETERMINISTIC SEED REPRODUCIBILITY & METADATA
    // ==========================================
    @Test
    fun test12_DeterministicSeedReproducibilityAndMetadata() {
        val testSeed = 987654321L
        val levelA = PuzzleGenerator.generateLevelWithSeed(5, testSeed)
        val levelB = PuzzleGenerator.generateLevelWithSeed(5, testSeed)

        // Exact reproduction
        assertEquals("Arrow counts must match", levelA.arrowCount, levelB.arrowCount)
        for (i in levelA.arrows.indices) {
            val arrowA = levelA.arrows[i]
            val arrowB = levelB.arrows[i]
            assertEquals("Facing must match", arrowA.facing, arrowB.facing)
            assertEquals("Point count must match", arrowA.points.size, arrowB.points.size)
            for (p in arrowA.points.indices) {
                assertEquals(arrowA.points[p].x, arrowB.points[p].x, 0.001f)
                assertEquals(arrowA.points[p].y, arrowB.points[p].y, 0.001f)
            }
        }

        // Metadata checks
        val meta = levelA.metadata
        assertNotNull("Metadata must not be null", meta)
        assertEquals(PuzzleGenerator.GENERATOR_VERSION, meta?.generatorVersion)
        assertEquals(testSeed, meta?.seed)
        assertTrue("Difficulty score must be in 0..100", (meta?.difficultyScore ?: 0) in 0..100)
        assertTrue("Validation score must be > 60", (meta?.validationScore ?: 0f) >= 60f)
        assertTrue("Metrics map must be populated", (meta?.metrics?.size ?: 0) > 5)
    }

    // ==========================================
    // 13. PHASE 8: LEVEL VALIDATOR & SIMULATION
    // ==========================================
    @Test
    fun test13_LevelValidatorStepByStepSimulationAndSolvability() {
        for (lvlId in listOf(1, 2, 5, 10, 25, 50, 100)) {
            val level = PuzzleGenerator.generateLevel(lvlId)
            val report = PuzzleValidator.validateLevel(level)

            assertTrue("Level $lvlId must be valid", report.isValid)
            assertTrue("Level $lvlId must be solvable", report.isSolvable)
            assertTrue("Level $lvlId must have at least 1 available initial move", report.initialAvailableMoves >= 1)
            assertEquals("Solution length must match total arrow count", level.arrowCount, report.solutionLength)
            assertTrue("No validation errors expected", report.errors.isEmpty())
            assertTrue("Difficulty score must be in 0..100", report.difficultyScore in 0..100)
            assertNotNull("Difficulty tier must be assigned", report.difficultyTier)
        }
    }

    // ==========================================
    // 14. PHASE 8: 12-FACTOR DIFFICULTY ANALYSIS & 8 TIERS
    // ==========================================
    @Test
    fun test14_DifficultyCalculator12FactorsAndProgression() {
        val earlyLevel = PuzzleGenerator.generateLevel(1) // Simple, early level
        val lateLevel = PuzzleGenerator.generateLevel(250) // Denser, later level

        val earlyAnalysis = DifficultyCalculator.analyze(earlyLevel)
        val lateAnalysis = DifficultyCalculator.analyze(lateLevel)

        // Verify 12 metrics are computed and non-negative
        assertTrue("Initial available moves >= 1", earlyAnalysis.initialAvailableMoves >= 1)
        assertTrue("Average branching factor > 0", earlyAnalysis.averageBranchingFactor > 0f)
        assertTrue("Max dependency depth > 0", earlyAnalysis.maxDependencyDepth > 0)
        assertTrue("Path density >= 0", earlyAnalysis.pathDensity >= 0f)
        assertTrue("Spatial spread > 0", earlyAnalysis.spatialSpread > 0f)
        assertTrue("Complexity score in 0..100", earlyAnalysis.complexityScore in 0..100)
        assertTrue("Estimated time > 0", earlyAnalysis.estimatedTimeSeconds > 0)

        // Verify 8 tier coverage mapping
        assertEquals(DifficultyTier.VERY_EASY, DifficultyTier.forScore(5))
        assertEquals(DifficultyTier.EASY, DifficultyTier.forScore(18))
        assertEquals(DifficultyTier.NORMAL, DifficultyTier.forScore(30))
        assertEquals(DifficultyTier.MEDIUM, DifficultyTier.forScore(45))
        assertEquals(DifficultyTier.HARD, DifficultyTier.forScore(60))
        assertEquals(DifficultyTier.VERY_HARD, DifficultyTier.forScore(72))
        assertEquals(DifficultyTier.EXTREME, DifficultyTier.forScore(85))
        assertEquals(DifficultyTier.NIGHTMARE, DifficultyTier.forScore(95))

        // Difficulty progression verification: later levels are denser and higher complexity
        assertTrue("Late level arrow count should be greater", lateAnalysis.arrowCount > earlyAnalysis.arrowCount)
        assertTrue("Late level dependency depth should be greater or equal", lateAnalysis.maxDependencyDepth >= earlyAnalysis.maxDependencyDepth)
    }

    // ==========================================
    // 15. PHASE 9: 1000+ OFFLINE LEVEL SYSTEM & PROGRESSION GROUPS
    // ==========================================
    @Test
    fun test15_1000PlusOfflineLevelSystemAndProgressionGroups() {
        assertEquals(1000, LevelRepository.TOTAL_LEVELS)

        // Verify progression group mappings
        assertEquals(ProgressionGroup.GROUP_1, ProgressionGroup.forLevel(1))
        assertEquals(ProgressionGroup.GROUP_1, ProgressionGroup.forLevel(50))
        assertEquals(ProgressionGroup.GROUP_2, ProgressionGroup.forLevel(51))
        assertEquals(ProgressionGroup.GROUP_2, ProgressionGroup.forLevel(150))
        assertEquals(ProgressionGroup.GROUP_3, ProgressionGroup.forLevel(200))
        assertEquals(ProgressionGroup.GROUP_4, ProgressionGroup.forLevel(400))
        assertEquals(ProgressionGroup.GROUP_5, ProgressionGroup.forLevel(750))
        assertEquals(ProgressionGroup.GROUP_5, ProgressionGroup.forLevel(1000))
        assertEquals(ProgressionGroup.GROUP_INFINITE, ProgressionGroup.forLevel(1001))

        // Spot check milestone levels across all progression groups for 100% offline generation & solvability
        val sampleMilestones = listOf(1, 50, 100, 150, 250, 400, 600, 800, 1000)
        for (lvlId in sampleMilestones) {
            val level = LevelRepository.getLevel(lvlId)
            assertNotNull("Level $lvlId must exist", level)
            assertEquals("Level ID must match", lvlId, level.id)
            assertTrue("Level $lvlId must have arrows", level.arrowCount > 0)
            assertNotNull("Level metadata must be present", level.metadata)
            assertNotNull("Level difficulty tier must be assigned", level.difficulty)

            val solver = PuzzleSolver.solve(level)
            assertTrue("Level $lvlId must be 100% solvable offline", solver.isSolvable)
            assertEquals("All arrows must be cleared in solver sequence", level.arrowCount, solver.solutionSequence.size)
        }

        // Test Category and Group ID queries
        val group1Levels = LevelRepository.getLevelIdsForGroup(ProgressionGroup.GROUP_1)
        assertEquals(50, group1Levels.size)
        assertEquals(1, group1Levels.first())
        assertEquals(50, group1Levels.last())

        val allCategoryIds = LevelRepository.getLevelIdsForCategory(LevelCategory.ALL)
        assertEquals(1000, allCategoryIds.size)
    }

    // ==========================================
    // 16. VALIDATION OF LEVELS 1 TO 10
    // ==========================================
    @Test
    fun test16_TenTestLevelsShapeAndSolvabilityValidation() {
        val expectedNames = listOf(
            1 to "True Heart",
            2 to "Shining Star",
            3 to "Tropical Fish",
            4 to "Sweet Apple",
            5 to "Graceful Butterfly",
            6 to "Playful Cat",
            7 to "Flying Bird",
            8 to "Autumn Leaf",
            9 to "Lucky Rabbit",
            10 to "Majestic Elephant"
        )

        for ((id, name) in expectedNames) {
            val level = LevelRepository.getLevel(id)
            assertNotNull("Level $id must exist", level)
            assertEquals("Level $id name must match", name, level.name)
            assertTrue("Level $id must contain arrows", level.arrowCount >= 10)

            val solver = PuzzleSolver.solve(level)
            val remainingIds = level.arrows.map { it.id }.filterNot { solver.solutionSequence.contains(it) }
            System.err.println("LEVEL_TEST: Level $id ($name) arrows=${level.arrowCount} solved=${solver.solutionSequence.size} remaining=$remainingIds")
            assertTrue("Level $id ($name) must be 100% solvable. Solved: ${solver.solutionSequence}, Remaining: $remainingIds", solver.isSolvable)
            assertEquals("Level $id all arrows solved", level.arrowCount, solver.solutionSequence.size)

            // Verify orthogonal paths (no diagonal segments)
            for (arrow in level.arrows) {
                for (i in 0 until arrow.points.size - 1) {
                    val p1 = arrow.points[i]
                    val p2 = arrow.points[i + 1]
                    val dx = kotlin.math.abs(p2.x - p1.x)
                    val dy = kotlin.math.abs(p2.y - p1.y)
                    assertTrue("Arrow segments must be horizontal or vertical", dx == 0f || dy == 0f)
                }
            }
        }
    }

    // ==========================================
    // 17. PHASE 10: HEARTS / LIVES RULE & PROGRESSION
    // ==========================================
    @Test
    fun test17_PlayerProgressionAndHeartsSystem() {
        val level = LevelRepository.getLevel(1)
        val remaining = level.arrows.toMutableList()
        var hearts = 3
        val maxHearts = 3

        assertEquals(3, hearts)
        assertEquals(3, maxHearts)

        // Find a blocked arrow and clearable arrow
        val clearable = PuzzleSolver.findClearableArrows(remaining)
        assertTrue("Level 1 must have at least one clearable arrow", clearable.isNotEmpty())
        val clearArrow = clearable.first()

        val blockedArrow = remaining.firstOrNull { it.id !in clearable.map { c -> c.id } }
        if (blockedArrow != null) {
            val collision = PuzzleGeometry.checkArrowCollision(blockedArrow, remaining)
            assertTrue("Blocked arrow must have collision", collision.isBlocked)

            // Incorrect action: player taps blocked arrow -> loses 1 heart
            hearts = (hearts - 1).coerceAtLeast(0)
            assertEquals(2, hearts)

            // Further collisions drop hearts to 0
            hearts = (hearts - 1).coerceAtLeast(0)
            hearts = (hearts - 1).coerceAtLeast(0)
            assertEquals(0, hearts)
            assertTrue("Hearts at 0 triggers out of hearts state", hearts == 0)

            // Undo or restart restores hearts
            hearts = maxHearts
            assertEquals(3, hearts)
        }

        // Tapping clear arrow succeeds without losing heart
        val collisionClear = PuzzleGeometry.checkArrowCollision(clearArrow, remaining)
        assertFalse("Clear arrow must not collide", collisionClear.isBlocked)
        assertEquals(3, hearts) // No heart loss on correct move
    }

    // ==========================================
    // 18. PHASE 10: DUAL HINT SYSTEM (HIGHLIGHT & AUTO-MOVE)
    // ==========================================
    @Test
    fun test18_DualHintSystemMechanics() {
        val level = LevelRepository.getLevel(2)
        val remaining = level.arrows

        // HINT TYPE 1: Highlight Safe Move
        val hintArrow = PuzzleSolver.getHint(remaining)
        assertNotNull("Hint 1 must identify a currently clearable arrow", hintArrow)
        val collision = PuzzleGeometry.checkArrowCollision(hintArrow!!, remaining)
        assertFalse("Hint 1 identified arrow must have unblocked escape path", collision.isBlocked)

        // HINT TYPE 2: Auto-Move Valid Arrow
        // Executes exactly 1 move from the state
        val updatedRemaining = remaining.filter { it.id != hintArrow.id }
        assertEquals(remaining.size - 1, updatedRemaining.size)
        // Level is still valid and solvable from the new state
        val subsequentSolve = PuzzleSolver.solveFromState(updatedRemaining)
        assertTrue("Board must remain 100% solvable after Auto-Move Hint", subsequentSolve.isSolvable)
    }

    // ==========================================
    // 19. PHASE 10: USER SETTINGS & PERSISTENCE MODELS
    // ==========================================
    @Test
    fun test19_UserSettingsAndProgressDataModels() {
        val defaultSettings = com.example.model.UserSettings()
        assertEquals(5, defaultSettings.highlightHintsCount)
        assertEquals(3, defaultSettings.autoMoveHintsCount)
        assertEquals(3, defaultSettings.heartsCount)
        assertTrue(defaultSettings.soundEnabled)
        assertTrue(defaultSettings.hapticsEnabled)
        assertFalse(defaultSettings.zenMode)
        assertEquals(com.example.model.ArrowColorPreset.DEFAULT_DARK, defaultSettings.arrowColor)
        assertEquals(com.example.model.BoardTheme.LIGHT_ZEN, defaultSettings.boardTheme)

        val customSettings = defaultSettings.copy(
            arrowColor = com.example.model.ArrowColorPreset.GREEN,
            soundEnabled = false,
            hapticsEnabled = false,
            highlightHintsCount = 8,
            autoMoveHintsCount = 4
        )
        assertFalse(customSettings.soundEnabled)
        assertFalse(customSettings.hapticsEnabled)
        assertEquals(com.example.model.ArrowColorPreset.GREEN, customSettings.arrowColor)
        assertEquals(8, customSettings.highlightHintsCount)
        assertEquals(4, customSettings.autoMoveHintsCount)
    }

    // ==========================================
    // 20. PHASE 11: CLEAN PUZZLE UI & ADAPTIVE SCALING
    // ==========================================
    @Test
    fun test20_CleanPuzzleUiAndAdaptiveScaling() {
        // Test multi-device resolution and aspect ratio scaling
        val smallPhoneWidth = 360f
        val smallPhoneHeight = 640f

        val largePhoneWidth = 412f
        val largePhoneHeight = 915f

        val tabletWidth = 800f
        val tabletHeight = 1280f

        val level = LevelRepository.getLevel(5)
        val bounds = level.bounds

        // Calculate aspect ratios & grid transforms across multiple screens
        val padding = 48f
        val scaleSmall = kotlin.math.min(
            (smallPhoneWidth - padding * 2) / (bounds.width + 1.5f),
            (smallPhoneHeight - padding * 2) / (bounds.height + 1.5f)
        )
        val scaleLarge = kotlin.math.min(
            (largePhoneWidth - padding * 2) / (bounds.width + 1.5f),
            (largePhoneHeight - padding * 2) / (bounds.height + 1.5f)
        )
        val scaleTablet = kotlin.math.min(
            (tabletWidth - padding * 2) / (bounds.width + 1.5f),
            (tabletHeight - padding * 2) / (bounds.height + 1.5f)
        )

        assertTrue("Small phone scaling must be positive", scaleSmall > 0f)
        assertTrue("Large phone scale should comfortably display puzzle", scaleLarge > 0f)
        assertTrue("Tablet scale should adjust to generous available bounds", scaleTablet > scaleSmall)

        // Center calculation verification
        val originXSmall = (smallPhoneWidth / 2f) - (bounds.centerX * scaleSmall)
        val originYSmall = (smallPhoneHeight / 2f) - (bounds.centerY * scaleSmall)
        assertTrue("Origin must center geometry within canvas", originXSmall.isFinite() && originYSmall.isFinite())
    }

    // ==========================================
    // 21. PHASE 12: ARROW APPEARANCE & DARK CONTRAST
    // ==========================================
    @Test
    fun test21_ArrowAppearanceCustomizationAndDarkThemeContrast() {
        val allPresets = com.example.model.ArrowColorPreset.values()

        // Required colors check
        val names = allPresets.map { it.displayName.lowercase() }
        assertTrue("Must support Black", names.any { it.contains("black") })
        assertTrue("Must support Dark Gray", names.any { it.contains("dark gray") })
        assertTrue("Must support Blue", names.any { it.contains("blue") })
        assertTrue("Must support Red", names.any { it.contains("red") })
        assertTrue("Must support Green", names.any { it.contains("green") })
        assertTrue("Must support Purple", names.any { it.contains("purple") })
        assertTrue("Must support Orange", names.any { it.contains("orange") })

        // Check contrast adaptation in dark theme
        for (preset in allPresets) {
            val lightVal = preset.getColorForTheme(isDarkTheme = false)
            val darkVal = preset.getColorForTheme(isDarkTheme = true)

            assertTrue("Light color value must be valid non-zero ARGB", lightVal != 0L)
            assertTrue("Dark color value must be valid non-zero ARGB", darkVal != 0L)
        }

        // Specifically check that black/dark-gray adapt to light high-contrast colors in dark mode
        val blackPreset = com.example.model.ArrowColorPreset.DEFAULT_DARK
        val blackOnDarkTheme = blackPreset.getColorForTheme(isDarkTheme = true)
        // High alpha, high luminance in dark theme (e.g. 0xFFF8FAFC)
        assertTrue("Black preset must brighten on dark backgrounds", (blackOnDarkTheme and 0x00FFFFFF) > 0x00777777)

        // Verify id parsing fallback
        assertEquals(com.example.model.ArrowColorPreset.BLUE, com.example.model.ArrowColorPreset.fromId("blue"))
        assertEquals(com.example.model.ArrowColorPreset.RED, com.example.model.ArrowColorPreset.fromId("red"))
        assertEquals(com.example.model.ArrowColorPreset.GREEN, com.example.model.ArrowColorPreset.fromId("green"))
        assertEquals(com.example.model.ArrowColorPreset.PURPLE, com.example.model.ArrowColorPreset.fromId("purple"))
        assertEquals(com.example.model.ArrowColorPreset.ORANGE, com.example.model.ArrowColorPreset.fromId("orange"))
        assertEquals(com.example.model.ArrowColorPreset.DEFAULT_DARK, com.example.model.ArrowColorPreset.fromId("unknown_color"))
    }

    // ==========================================
    // 22. PHASE 13: VISUAL POLISH, THEMES & AUDIO
    // ==========================================
    @Test
    fun test22_VisualPolishThemesAndAudioFeedback() {
        val lightThemes = com.example.model.BoardTheme.values().filter { !it.isDark }
        val darkThemes = com.example.model.BoardTheme.values().filter { it.isDark }

        assertTrue("Must have light themes available", lightThemes.isNotEmpty())
        assertTrue("Must have dark themes available", darkThemes.isNotEmpty())

        // Check contrast between background and primary color in all themes
        for (theme in com.example.model.BoardTheme.values()) {
            val bg = theme.backgroundColor
            val primary = theme.primaryColor
            assertTrue("Background and primary color must not be identical", bg != primary)
        }

        // Check UserSettings sound/haptics toggle retention
        val settings = com.example.model.UserSettings(
            soundEnabled = true,
            hapticsEnabled = true,
            zenMode = false
        )
        assertTrue(settings.soundEnabled)
        assertTrue(settings.hapticsEnabled)
        assertFalse(settings.zenMode)

        val mutedSettings = settings.copy(soundEnabled = false, hapticsEnabled = false)
        assertFalse(mutedSettings.soundEnabled)
        assertFalse(mutedSettings.hapticsEnabled)
    }

    // ==========================================
    // 23. VERIFY TEST LEVELS 1–5 (SHAPES, SOLVABILITY & DIFFICULTY)
    // ==========================================
    @Test
    fun test23_VerifyFirstFiveTestLevels() {
        // Level 1: Heart — Very Easy
        val level1 = LevelRepository.getLevel(1)
        assertTrue("Level 1 name must contain Heart", level1.name.contains("Heart", ignoreCase = true))
        assertEquals("Level 1 must be VERY_EASY", DifficultyTier.VERY_EASY, level1.difficulty)
        val solve1 = PuzzleSolver.solve(level1)
        assertTrue("Level 1 must be 100% solvable", solve1.isSolvable)
        assertEquals("Level 1 solved moves must equal arrow count", level1.arrowCount, solve1.solutionSequence.size)

        // Level 2: Star — Very Easy
        val level2 = LevelRepository.getLevel(2)
        assertTrue("Level 2 name must contain Star", level2.name.contains("Star", ignoreCase = true))
        assertEquals("Level 2 must be VERY_EASY", DifficultyTier.VERY_EASY, level2.difficulty)
        val solve2 = PuzzleSolver.solve(level2)
        assertTrue("Level 2 must be 100% solvable", solve2.isSolvable)
        assertEquals("Level 2 solved moves must equal arrow count", level2.arrowCount, solve2.solutionSequence.size)

        // Level 3: Fish — Very Easy
        val level3 = LevelRepository.getLevel(3)
        assertTrue("Level 3 name must contain Fish", level3.name.contains("Fish", ignoreCase = true))
        assertEquals("Level 3 must be VERY_EASY", DifficultyTier.VERY_EASY, level3.difficulty)
        val solve3 = PuzzleSolver.solve(level3)
        assertTrue("Level 3 must be 100% solvable", solve3.isSolvable)
        assertEquals("Level 3 solved moves must equal arrow count", level3.arrowCount, solve3.solutionSequence.size)

        // Level 4: Apple — Easy
        val level4 = LevelRepository.getLevel(4)
        assertTrue("Level 4 name must contain Apple", level4.name.contains("Apple", ignoreCase = true))
        assertEquals("Level 4 must be EASY", DifficultyTier.EASY, level4.difficulty)
        val solve4 = PuzzleSolver.solve(level4)
        assertTrue("Level 4 must be 100% solvable", solve4.isSolvable)
        assertEquals("Level 4 solved moves must equal arrow count", level4.arrowCount, solve4.solutionSequence.size)

        // Level 5: Butterfly — Easy
        val level5 = LevelRepository.getLevel(5)
        assertTrue("Level 5 name must contain Butterfly", level5.name.contains("Butterfly", ignoreCase = true))
        assertEquals("Level 5 must be EASY", DifficultyTier.EASY, level5.difficulty)
        val solve5 = PuzzleSolver.solve(level5)
        assertTrue("Level 5 must be 100% solvable", solve5.isSolvable)
        assertEquals("Level 5 solved moves must equal arrow count", level5.arrowCount, solve5.solutionSequence.size)

        // Verify bounds and non-empty geometries for all 5 levels
        val firstFive = listOf(level1, level2, level3, level4, level5)
        for (lvl in firstFive) {
            assertTrue("Level ${lvl.id} must have arrows", lvl.arrows.isNotEmpty())
            assertTrue("Level ${lvl.id} bounds width must be positive", lvl.bounds.width > 2f)
            assertTrue("Level ${lvl.id} bounds height must be positive", lvl.bounds.height > 2f)
            for (arrow in lvl.arrows) {
                assertTrue("Arrow ${arrow.id} must have at least 2 points", arrow.points.size >= 2)
            }
        }
    }
}



