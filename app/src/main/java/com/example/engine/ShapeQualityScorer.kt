package com.example.engine

import com.example.model.Arrow
import com.example.model.GridPoint
import com.example.model.PuzzleLevel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Evaluates the visual quality, recognizability, and structural integrity of a generated puzzle.
 */
object ShapeQualityScorer {

    data class QualityScoreReport(
        val totalScore: Float, // 0.0 to 100.0
        val isPassed: Boolean,
        val shapeCoverage: Float, // 0.0 to 1.0
        val boundaryAdherence: Float, // 0.0 to 1.0
        val densityBalance: Float, // 0.0 to 1.0
        val continuityScore: Float, // 0.0 to 1.0
        val negativeSpaceQuality: Float, // 0.0 to 1.0
        val visualBalance: Float, // 0.0 to 1.0
        val symmetryScore: Float, // 0.0 to 1.0
        val solvabilityScore: Float, // 0.0 to 1.0
        val collisionDepthScore: Float, // 0.0 to 1.0
        val rejectionReasons: List<String> = emptyList()
    ) {
        fun toMetricsMap(): Map<String, Float> = mapOf(
            "totalScore" to totalScore,
            "shapeCoverage" to shapeCoverage,
            "boundaryAdherence" to boundaryAdherence,
            "densityBalance" to densityBalance,
            "continuityScore" to continuityScore,
            "negativeSpaceQuality" to negativeSpaceQuality,
            "visualBalance" to visualBalance,
            "symmetryScore" to symmetryScore,
            "solvabilityScore" to solvabilityScore,
            "collisionDepthScore" to collisionDepthScore
        )
    }

    /**
     * Evaluates a candidate arrow set against the target shape definition.
     */
    fun evaluate(
        shape: ShapeSilhouettes.ShapeDef,
        arrows: List<Arrow>,
        level: PuzzleLevel? = null
    ): QualityScoreReport {
        val rejectionReasons = mutableListOf<String>()

        if (arrows.isEmpty()) {
            return QualityScoreReport(
                totalScore = 0f,
                isPassed = false,
                shapeCoverage = 0f,
                boundaryAdherence = 0f,
                densityBalance = 0f,
                continuityScore = 0f,
                negativeSpaceQuality = 0f,
                visualBalance = 0f,
                symmetryScore = 0f,
                solvabilityScore = 0f,
                collisionDepthScore = 0f,
                rejectionReasons = listOf("Empty arrow set")
            )
        }

        val grid = shape.grid
        val numRows = grid.size
        val numCols = grid.maxOf { it.length }

        val activeCells = mutableSetOf<Pair<Int, Int>>()
        val inactiveCells = mutableSetOf<Pair<Int, Int>>()

        var shapeCentroidX = 0f
        var shapeCentroidY = 0f

        for (r in 0 until numRows) {
            val rowStr = grid[r]
            for (c in 0 until numCols) {
                if (c < rowStr.length && rowStr[c] == '#') {
                    activeCells.add(Pair(c, r))
                    shapeCentroidX += c
                    shapeCentroidY += r
                } else {
                    inactiveCells.add(Pair(c, r))
                }
            }
        }

        if (activeCells.isNotEmpty()) {
            shapeCentroidX /= activeCells.size
            shapeCentroidY /= activeCells.size
        }

        // 1. Shape Coverage: which active cells have at least one arrow point nearby (< 1.2 units)
        var coveredActiveCells = 0
        for ((c, r) in activeCells) {
            val cellCenter = GridPoint(c.toFloat(), r.toFloat())
            val isCovered = arrows.any { arrow ->
                arrow.points.any { p -> p.distanceTo(cellCenter) <= 1.25f }
            }
            if (isCovered) coveredActiveCells++
        }
        val shapeCoverage = if (activeCells.isNotEmpty()) {
            (coveredActiveCells.toFloat() / activeCells.size).coerceIn(0f, 1f)
        } else 1f

        if (shapeCoverage < 0.35f) {
            rejectionReasons.add("Insufficient shape coverage (${(shapeCoverage * 100).toInt()}%)")
        }

        // 2. Boundary Adherence: fraction of arrow points strictly inside active cells or within 0.8 units of active cells
        var pointsInsideOrNear = 0
        var totalPoints = 0
        var arrowCentroidX = 0f
        var arrowCentroidY = 0f

        for (arrow in arrows) {
            for (p in arrow.points) {
                totalPoints++
                arrowCentroidX += p.x
                arrowCentroidY += p.y
                val isInside = activeCells.any { (c, r) ->
                    abs(p.x - c) <= 0.95f && abs(p.y - r) <= 0.95f
                }
                if (isInside) pointsInsideOrNear++
            }
        }

        if (totalPoints > 0) {
            arrowCentroidX /= totalPoints
            arrowCentroidY /= totalPoints
        }

        val boundaryAdherence = if (totalPoints > 0) {
            (pointsInsideOrNear.toFloat() / totalPoints).coerceIn(0f, 1f)
        } else 0f

        if (boundaryAdherence < 0.65f) {
            rejectionReasons.add("Excessive points outside silhouette (${(boundaryAdherence * 100).toInt()}%)")
        }

        // 3. Density & Spacing Balance: standard deviation of nearest neighbor distances
        val neighborDistances = mutableListOf<Float>()
        for (i in arrows.indices) {
            val headI = arrows[i].points.last()
            var minNeighborDist = Float.MAX_VALUE
            for (j in arrows.indices) {
                if (i == j) continue
                val headJ = arrows[j].points.last()
                val d = headI.distanceTo(headJ)
                if (d < minNeighborDist) minNeighborDist = d
            }
            if (minNeighborDist < Float.MAX_VALUE) {
                neighborDistances.add(minNeighborDist)
            }
        }

        val meanNeighbor = if (neighborDistances.isNotEmpty()) neighborDistances.average().toFloat() else 1.5f
        var variance = 0f
        for (d in neighborDistances) {
            variance += (d - meanNeighbor).pow(2)
        }
        val stdDev = if (neighborDistances.isNotEmpty()) sqrt(variance / neighborDistances.size) else 0f
        // Well-spaced layout has low standard deviation relative to mean (stdDev < 1.0)
        val densityBalance = (1f - (stdDev / 2.0f)).coerceIn(0f, 1f)

        // 4. Path Continuity & Isolated Arrows
        var isolatedCount = 0
        for (i in arrows.indices) {
            val a = arrows[i]
            val hasNearbyNeighbor = arrows.indices.any { j ->
                if (i == j) false
                else {
                    val b = arrows[j]
                    a.points.any { p1 -> b.points.any { p2 -> p1.distanceTo(p2) <= 1.8f } }
                }
            }
            if (!hasNearbyNeighbor) isolatedCount++
        }
        val continuityScore = (1f - (isolatedCount.toFloat() / arrows.size.coerceAtLeast(1))).coerceIn(0f, 1f)
        if (isolatedCount > arrows.size * 0.45f && arrows.size > 8) {
            rejectionReasons.add("Too many isolated floating arrows ($isolatedCount of ${arrows.size})")
        }

        // 5. Negative Space Quality: ensure internal holes (inactive cells surrounded by active cells) are respected
        var negativeSpaceViolations = 0
        var internalHolesCount = 0
        for ((c, r) in inactiveCells) {
            // Check if cell is an internal hole (surrounded by active cells)
            val hasLeft = (0 until c).any { activeCells.contains(Pair(it, r)) }
            val hasRight = ((c + 1) until numCols).any { activeCells.contains(Pair(it, r)) }
            val hasTop = (0 until r).any { activeCells.contains(Pair(c, it)) }
            val hasBottom = ((r + 1) until numRows).any { activeCells.contains(Pair(c, it)) }

            if (hasLeft && hasRight && hasTop && hasBottom) {
                internalHolesCount++
                val hasArrowInHole = arrows.any { arrow ->
                    arrow.points.any { p -> abs(p.x - c) < 0.45f && abs(p.y - r) < 0.45f }
                }
                if (hasArrowInHole) negativeSpaceViolations++
            }
        }
        val negativeSpaceQuality = if (internalHolesCount > 0) {
            (1f - (negativeSpaceViolations.toFloat() / internalHolesCount)).coerceIn(0f, 1f)
        } else 1.0f

        // 6. Visual Balance: Centroid alignment
        val centroidDist = sqrt((shapeCentroidX - arrowCentroidX).pow(2) + (shapeCentroidY - arrowCentroidY).pow(2))
        val visualBalance = (1f - (centroidDist / 3.0f)).coerceIn(0f, 1f)

        // 7. Symmetry when applicable
        var isSymmetricShape = true
        for (r in 0 until numRows) {
            for (c in 0 until numCols / 2) {
                val left = activeCells.contains(Pair(c, r))
                val right = activeCells.contains(Pair(numCols - 1 - c, r))
                if (left != right) {
                    isSymmetricShape = false
                    break
                }
            }
        }

        val symmetryScore: Float
        if (isSymmetricShape) {
            var leftArrows = 0
            var rightArrows = 0
            val midX = numCols / 2f
            for (a in arrows) {
                val head = a.points.last()
                if (head.x < midX - 0.2f) leftArrows++
                else if (head.x > midX + 0.2f) rightArrows++
            }
            val totalSideArrows = max(1, leftArrows + rightArrows)
            val diff = abs(leftArrows - rightArrows).toFloat() / totalSideArrows
            symmetryScore = (1f - diff).coerceIn(0f, 1f)
        } else {
            symmetryScore = 1.0f
        }

        // 8. Solvability & Collision Depth
        val testLevel = level ?: PuzzleLevel(
            id = 1,
            name = shape.name,
            category = shape.category,
            arrows = arrows
        )
        val solveResult = PuzzleSolver.solve(testLevel)
        val solvabilityScore = if (solveResult.isSolvable) 1.0f else 0.0f
        if (!solveResult.isSolvable) {
            rejectionReasons.add("Puzzle contains deadlocks or unsolvable states")
        }

        // Collision depth score: non-trivial puzzle depth
        val depthRatio = if (arrows.isNotEmpty()) {
            (solveResult.maxDepth.toFloat() / (arrows.size * 0.7f).coerceAtLeast(1f)).coerceIn(0f, 1f)
        } else 0f
        val collisionDepthScore = (0.4f + 0.6f * depthRatio).coerceIn(0f, 1f)

        // Composite Weighted Total Score (0..100)
        val totalScore = (
            shapeCoverage * 20f +
            boundaryAdherence * 20f +
            continuityScore * 15f +
            densityBalance * 10f +
            negativeSpaceQuality * 10f +
            visualBalance * 5f +
            symmetryScore * 5f +
            solvabilityScore * 15f
        ).coerceIn(0f, 100f)

        val isPassed = rejectionReasons.isEmpty() && solvabilityScore == 1.0f && totalScore >= 65f

        return QualityScoreReport(
            totalScore = totalScore,
            isPassed = isPassed,
            shapeCoverage = shapeCoverage,
            boundaryAdherence = boundaryAdherence,
            densityBalance = densityBalance,
            continuityScore = continuityScore,
            negativeSpaceQuality = negativeSpaceQuality,
            visualBalance = visualBalance,
            symmetryScore = symmetryScore,
            solvabilityScore = solvabilityScore,
            collisionDepthScore = collisionDepthScore,
            rejectionReasons = rejectionReasons
        )
    }
}
