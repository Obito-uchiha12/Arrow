package com.example.engine

import com.example.model.Arrow
import com.example.model.DifficultyTier
import com.example.model.Direction
import com.example.model.GridPoint
import com.example.model.LevelCategory
import com.example.model.LevelGenerationMetadata
import com.example.model.ProgressionGroup
import com.example.model.PuzzleLevel
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PuzzleGenerator {

    const val GENERATOR_VERSION = "v1.4.0"
    const val MAX_SEED_ATTEMPTS = 10
    const val MIN_QUALITY_SCORE_THRESHOLD = 60f

    /**
     * Generates level [levelId] (from 1 to 1000+) deterministically using its default seed.
     */
    fun generateLevel(levelId: Int): PuzzleLevel {
        val baseSeed = (levelId * 31337L) xor 0x5DEECE66DL
        return generateLevelWithSeed(levelId, baseSeed)
    }

    /**
     * Generates a puzzle level deterministically for a specific [seed].
     * Runs quality scoring and seed-retries to ensure the puzzle looks cohesive,
     * fits the silhouette, preserves negative space, and is 100% solvable.
     */
    fun generateLevelWithSeed(levelId: Int, seed: Long): PuzzleLevel {
        val shapes = ShapeSilhouettes.ALL_SHAPES
        val shapeIndex = (levelId - 1) % shapes.size
        val rawShape = shapes[shapeIndex]
        val iteration = (levelId - 1) / shapes.size + 1

        // Apply silhouette orientation/variation (flip/rotate) based on iteration
        val baseShape = transformShapeForIteration(rawShape, iteration)

        // Determine target arrow count and density based on progression group
        val group = ProgressionGroup.forLevel(levelId)
        val targetArrowCount = calculateTargetArrowCountForGroup(levelId, group)

        var bestArrows: List<Arrow> = emptyList()
        var bestReport: ShapeQualityScorer.QualityScoreReport? = null
        var chosenSeed = seed

        // Multi-seed candidate search with Quality Scorer
        for (attempt in 0 until MAX_SEED_ATTEMPTS) {
            val currentSeed = seed + (attempt * 10007L)
            val random = Random(currentSeed)

            val candidateArrows = synthesizeArrows(baseShape, targetArrowCount, random, levelId, group)
            val report = ShapeQualityScorer.evaluate(baseShape, candidateArrows)

            if (bestReport == null || report.totalScore > bestReport.totalScore) {
                bestReport = report
                bestArrows = candidateArrows
                chosenSeed = currentSeed
            }

            // Early exit if high quality is achieved
            if (report.isPassed && report.totalScore >= 75f) {
                break
            }
        }

        // If even the best attempt is invalid or empty, construct safe fallback
        if (bestArrows.isEmpty() || bestReport?.solvabilityScore != 1.0f) {
            val fallbackRandom = Random(chosenSeed)
            bestArrows = buildFallbackSafeArrows(baseShape, targetArrowCount, fallbackRandom)
            bestReport = ShapeQualityScorer.evaluate(baseShape, bestArrows)
        }

        val levelName = if (iteration == 1) {
            baseShape.name
        } else {
            "${baseShape.name} ${toRoman(iteration)}"
        }

        val tempLevel = PuzzleLevel(
            id = levelId,
            name = levelName,
            category = baseShape.category,
            arrows = bestArrows,
            authorNotes = "Act ${group.groupNumber}: ${group.title} • ${group.subtitle} • Level $levelId"
        )

        val diffAnalysis = DifficultyCalculator.analyze(tempLevel)
        val metadata = LevelGenerationMetadata(
            shapeId = baseShape.name,
            generatorVersion = GENERATOR_VERSION,
            seed = chosenSeed,
            difficultyScore = diffAnalysis.complexityScore,
            validationScore = bestReport?.totalScore ?: 100f,
            metrics = bestReport?.toMetricsMap() ?: emptyMap()
        )

        return tempLevel.copy(metadata = metadata)
    }

    private fun transformShapeForIteration(shape: ShapeSilhouettes.ShapeDef, iteration: Int): ShapeSilhouettes.ShapeDef {
        if (iteration <= 1) return shape
        val mode = iteration % 4
        val grid = shape.grid
        val newGrid = when (mode) {
            1 -> { // Horizontal flip
                grid.map { it.reversed() }
            }
            2 -> { // 90 degree rotation
                val rows = grid.size
                val cols = grid.maxOf { it.length }
                val padded = grid.map { it.padEnd(cols, ' ') }
                (0 until cols).map { c ->
                    (rows - 1 downTo 0).map { r -> padded[r][c] }.joinToString("")
                }
            }
            3 -> { // Vertical flip
                grid.reversed()
            }
            else -> grid
        }
        return shape.copy(grid = newGrid)
    }

    private fun calculateTargetArrowCountForGroup(levelId: Int, group: ProgressionGroup): Int {
        return when (group) {
            ProgressionGroup.GROUP_1 -> { // Levels 1-50 (Very Easy / Easy)
                if (levelId <= 10) 8 + (levelId * 14) / 10 // 9..22
                else 14 + ((levelId - 10) * 12) / 40 // 14..26
            }
            ProgressionGroup.GROUP_2 -> { // Levels 51-150 (Normal)
                24 + ((levelId - 50) * 16) / 100 // 24..40
            }
            ProgressionGroup.GROUP_3 -> { // Levels 151-350 (Hard)
                38 + ((levelId - 150) * 20) / 200 // 38..58
            }
            ProgressionGroup.GROUP_4 -> { // Levels 351-700 (Very Hard)
                56 + ((levelId - 350) * 24) / 350 // 56..80
            }
            ProgressionGroup.GROUP_5 -> { // Levels 701-1000 (Extreme)
                78 + ((levelId - 700) * 22) / 300 // 78..100
            }
            ProgressionGroup.GROUP_INFINITE -> { // 1001+
                95 + min(35, ((levelId - 1000) * 20) / 200) // 95..130
            }
        }
    }

    private fun synthesizeArrows(
        shape: ShapeSilhouettes.ShapeDef,
        targetCount: Int,
        random: Random,
        levelId: Int,
        group: ProgressionGroup = ProgressionGroup.forLevel(levelId)
    ): List<Arrow> {
        val grid = shape.grid
        val numRows = grid.size
        val numCols = grid.maxOf { it.length }

        // 1. Build Shape Mask
        val activeCells = mutableSetOf<Pair<Int, Int>>()
        val isCellActive = Array(numRows) { BooleanArray(numCols) }
        for (r in 0 until numRows) {
            val rowStr = grid[r]
            for (c in 0 until rowStr.length) {
                if (rowStr[c] == '#') {
                    activeCells.add(Pair(c, r))
                    isCellActive[r][c] = true
                }
            }
        }

        if (activeCells.isEmpty()) {
            return generateGeometricLevel(levelId, targetCount, random)
        }

        // 2. Identify Structural Regions:
        // - Primary Contour: outermost boundary cells
        // - Secondary Contour: cells adjacent to boundary
        // - Internal Fill: deeply embedded cells
        val boundaryCells = mutableSetOf<Pair<Int, Int>>()
        for ((c, r) in activeCells) {
            val isBoundary = (r == 0 || !isCellActive[r - 1][c]) ||
                    (r == numRows - 1 || !isCellActive[r + 1][c]) ||
                    (c == 0 || !isCellActive[r][c - 1]) ||
                    (c == numCols - 1 || !isCellActive[r][c + 1])
            if (isBoundary) {
                boundaryCells.add(Pair(c, r))
            }
        }

        val secondaryCells = mutableSetOf<Pair<Int, Int>>()
        for ((c, r) in activeCells) {
            if (!boundaryCells.contains(Pair(c, r))) {
                val nearBoundary = listOf(
                    Pair(c + 1, r), Pair(c - 1, r), Pair(c, r + 1), Pair(c, r - 1)
                ).any { boundaryCells.contains(it) }
                if (nearBoundary) {
                    secondaryCells.add(Pair(c, r))
                }
            }
        }

        val internalFillCells = activeCells.filter {
            !boundaryCells.contains(it) && !secondaryCells.contains(it)
        }.toSet()

        val centerX = numCols / 2f
        val centerY = numRows / 2f

        // 3. Path Generation along Structural Regions
        val placedArrows = mutableListOf<Arrow>()
        var arrowIdCounter = 1
        val usedSegments = mutableSetOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()

        fun markSegment(p1: GridPoint, p2: GridPoint) {
            val a = Pair((p1.x * 2).toInt(), (p1.y * 2).toInt())
            val b = Pair((p2.x * 2).toInt(), (p2.y * 2).toInt())
            usedSegments.add(Pair(a, b))
            usedSegments.add(Pair(b, a))
        }

        fun isSegmentFree(p1: GridPoint, p2: GridPoint): Boolean {
            val a = Pair((p1.x * 2).toInt(), (p1.y * 2).toInt())
            val b = Pair((p2.x * 2).toInt(), (p2.y * 2).toInt())
            return !usedSegments.contains(Pair(a, b))
        }

        // Phase A: Primary Contour Arrows (Follow perimeter with outward or tangential exit vectors)
        val sortedBoundary = boundaryCells.sortedWith(
            compareBy<Pair<Int, Int>> { (c, r) ->
                // Sort by polar angle around centroid to form a continuous perimeter circuit
                val angle = kotlin.math.atan2((r - centerY).toDouble(), (c - centerX).toDouble())
                angle
            }
        )

        var bIdx = 0
        while (bIdx < sortedBoundary.size && placedArrows.size < targetCount) {
            val cell = sortedBoundary[bIdx]
            val x = cell.first.toFloat()
            val y = cell.second.toFloat()

            val dx = x - centerX
            val dy = y - centerY

            // Determine natural outward facing direction
            val outwardDir = when {
                abs(dx) >= abs(dy) && dx >= 0 -> Direction.RIGHT
                abs(dx) >= abs(dy) && dx < 0 -> Direction.LEFT
                dy >= 0 -> Direction.DOWN
                else -> Direction.UP
            }

            // Also consider tangential contour direction
            val nextIdx = (bIdx + 1) % sortedBoundary.size
            val nextCell = sortedBoundary[nextIdx]
            val tDir = when {
                nextCell.first > cell.first -> Direction.RIGHT
                nextCell.first < cell.first -> Direction.LEFT
                nextCell.second > cell.second -> Direction.DOWN
                else -> Direction.UP
            }

            val dir = if (random.nextBoolean()) outwardDir else tDir
            val points = if (random.nextFloat() < 0.6f) {
                // L-turn contour along the edge
                val perp = when (dir) {
                    Direction.UP, Direction.DOWN -> if (dx >= 0) Direction.LEFT else Direction.RIGHT
                    Direction.LEFT, Direction.RIGHT -> if (dy >= 0) Direction.UP else Direction.DOWN
                }
                val tail = GridPoint(x - perp.dx * 0.9f, y - perp.dy * 0.9f)
                val corner = GridPoint(x, y)
                val head = GridPoint(x + dir.dx * 0.9f, y + dir.dy * 0.9f)
                listOf(tail, corner, head)
            } else {
                // Straight contour line
                val tail = GridPoint(x - dir.dx * 0.75f, y - dir.dy * 0.75f)
                val head = GridPoint(x + dir.dx * 0.75f, y + dir.dy * 0.75f)
                listOf(tail, head)
            }

            val candidate = Arrow(id = arrowIdCounter, points = points, facing = dir)
            if (isSegmentFree(points.first(), points.last())) {
                placedArrows.add(candidate)
                markSegment(points.first(), points.last())
                arrowIdCounter++
            }

            bIdx += if (targetCount > 30) 1 else 2
        }

        // Phase B: Internal Fill & Secondary Contour Arrows
        val internalCandidates = (secondaryCells + internalFillCells).shuffled(random)
        for (cell in internalCandidates) {
            if (placedArrows.size >= targetCount) break

            val x = cell.first.toFloat()
            val y = cell.second.toFloat()

            val dx = x - centerX
            val dy = y - centerY

            val preferredDir = when {
                abs(dx) >= abs(dy) && dx >= 0 -> Direction.RIGHT
                abs(dx) >= abs(dy) && dx < 0 -> Direction.LEFT
                dy >= 0 -> Direction.DOWN
                else -> Direction.UP
            }

            val dir = if (random.nextFloat() < 0.75f) preferredDir else Direction.values()[random.nextInt(4)]
            val tail = GridPoint(x - dir.dx * 0.7f, y - dir.dy * 0.7f)
            val head = GridPoint(x + dir.dx * 0.7f, y + dir.dy * 0.7f)
            val points = listOf(tail, head)

            if (isSegmentFree(tail, head)) {
                val candidate = Arrow(id = arrowIdCounter, points = points, facing = dir)
                placedArrows.add(candidate)
                markSegment(tail, head)
                arrowIdCounter++
            }
        }

        // Phase C: Solvability Verification & Clean Unblocking Order
        val solverResult = PuzzleSolver.solve(
            PuzzleLevel(id = levelId, name = shape.name, category = shape.category, arrows = placedArrows)
        )

        return if (solverResult.isSolvable && placedArrows.isNotEmpty()) {
            placedArrows
        } else {
            buildFallbackSafeArrows(shape, targetCount, random)
        }
    }

    private fun buildFallbackSafeArrows(
        shape: ShapeSilhouettes.ShapeDef,
        targetCount: Int,
        random: Random
    ): List<Arrow> {
        val arrows = mutableListOf<Arrow>()
        val grid = shape.grid
        var id = 1

        val numRows = grid.size
        val numCols = grid.maxOf { it.length }

        val activeCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until numRows) {
            val row = grid[r]
            for (c in 0 until row.length) {
                if (row[c] == '#') {
                    activeCells.add(Pair(c, r))
                }
            }
        }

        if (activeCells.isEmpty()) return emptyList()

        val centerX = numCols / 2f
        val centerY = numRows / 2f

        val step = max(1, activeCells.size / targetCount.coerceAtLeast(1))
        for (i in 0 until activeCells.size step step) {
            val (c, r) = activeCells[i]
            val dx = c - centerX
            val dy = r - centerY

            val dir = when {
                abs(dx) >= abs(dy) && dx >= 0 -> Direction.RIGHT
                abs(dx) >= abs(dy) && dx < 0 -> Direction.LEFT
                dy >= 0 -> Direction.DOWN
                else -> Direction.UP
            }

            val p1 = GridPoint(c.toFloat() - dir.dx * 0.45f, r.toFloat() - dir.dy * 0.45f)
            val p2 = GridPoint(c.toFloat() + dir.dx * 0.45f, r.toFloat() + dir.dy * 0.45f)

            arrows.add(
                Arrow(
                    id = id++,
                    points = listOf(p1, p2),
                    facing = dir
                )
            )

            if (arrows.size >= targetCount) break
        }

        return arrows
    }

    private fun generateGeometricLevel(levelId: Int, targetCount: Int, random: Random): List<Arrow> {
        val arrows = mutableListOf<Arrow>()
        var id = 1
        val rings = max(2, (targetCount / 8) + 1)

        for (ring in 1..rings) {
            val radius = ring * 1.5f
            val countInRing = min(targetCount - arrows.size, ring * 4)
            for (i in 0 until countInRing) {
                val angle = (i.toFloat() / countInRing) * 2f * Math.PI.toFloat()
                val x = kotlin.math.cos(angle) * radius
                val y = kotlin.math.sin(angle) * radius

                val dir = when {
                    abs(x) >= abs(y) && x >= 0 -> Direction.RIGHT
                    abs(x) >= abs(y) && x < 0 -> Direction.LEFT
                    y >= 0 -> Direction.DOWN
                    else -> Direction.UP
                }

                val p1 = GridPoint(x - dir.dx * 0.5f, y - dir.dy * 0.5f)
                val p2 = GridPoint(x + dir.dx * 0.5f, y + dir.dy * 0.5f)

                arrows.add(Arrow(id = id++, points = listOf(p1, p2), facing = dir))
                if (arrows.size >= targetCount) break
            }
            if (arrows.size >= targetCount) break
        }

        return arrows
    }

    private fun toRoman(number: Int): String {
        return when (number) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            7 -> "VII"
            8 -> "VIII"
            9 -> "IX"
            10 -> "X"
            else -> "$number"
        }
    }
}
