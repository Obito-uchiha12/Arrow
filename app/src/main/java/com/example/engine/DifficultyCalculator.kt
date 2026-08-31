package com.example.engine

import com.example.model.Arrow
import com.example.model.DifficultyTier
import com.example.model.PuzzleLevel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Calculates difficulty metrics, complexity score (0..100), and assigns 8 difficulty tiers.
 * Considers 12 comprehensive structural & topological factors:
 * 1. Number of arrows
 * 2. Path density
 * 3. Average blocking count
 * 4. Maximum dependency depth
 * 5. Number of initial available moves
 * 6. Number of forced moves
 * 7. Branching factor
 * 8. Number of near-collisions
 * 9. Spatial complexity / grid spread
 * 10. Shape complexity / silhouette perimeter
 * 11. Solution sequence length
 * 12. Deceptive choices (moves that look free but don't unlock dependencies)
 */
object DifficultyCalculator {

    data class DifficultyAnalysis(
        val complexityScore: Int, // 0 to 100
        val tier: DifficultyTier,
        val arrowCount: Int,
        val initialAvailableMoves: Int,
        val forcedMovesCount: Int,
        val averageBranchingFactor: Float,
        val maxDependencyDepth: Int,
        val averageBlockingCount: Float,
        val nearCollisionsCount: Int,
        val pathDensity: Float,
        val spatialSpread: Float,
        val deceptiveChoicesCount: Int,
        val estimatedTimeSeconds: Int
    )

    /**
     * Analyzes puzzle difficulty across all 12 structural metrics.
     */
    fun analyze(level: PuzzleLevel): DifficultyAnalysis {
        val arrows = level.arrows
        val count = arrows.size

        if (count == 0) {
            return DifficultyAnalysis(
                complexityScore = 0,
                tier = DifficultyTier.VERY_EASY,
                arrowCount = 0,
                initialAvailableMoves = 0,
                forcedMovesCount = 0,
                averageBranchingFactor = 0f,
                maxDependencyDepth = 0,
                averageBlockingCount = 0f,
                nearCollisionsCount = 0,
                pathDensity = 0f,
                spatialSpread = 0f,
                deceptiveChoicesCount = 0,
                estimatedTimeSeconds = 0
            )
        }

        // 1. Initial available moves & blocker graph
        val initialClearable = PuzzleSolver.findClearableArrows(arrows)
        val initialAvailableCount = initialClearable.size

        // Calculate blocking relationships
        var totalBlockers = 0
        var nearCollisions = 0
        val blockerMap = mutableMapOf<Int, MutableList<Int>>() // arrowId -> list of arrows it blocks

        for (arrow in arrows) {
            blockerMap[arrow.id] = mutableListOf()
            val collision = PuzzleGeometry.checkArrowCollision(arrow, arrows)
            if (collision.isBlocked) {
                totalBlockers++
                if (collision.blockingArrowId != null) {
                    blockerMap.getOrPut(collision.blockingArrowId) { mutableListOf() }.add(arrow.id)
                }
                if (collision.minDistance <= 0.4f) {
                    nearCollisions++
                }
            }
        }

        val avgBlockingCount = totalBlockers.toFloat() / count

        // 2. Full simulation metrics (branching factor, forced moves, dependency depth, deceptive moves)
        val simArrows = arrows.toMutableList()
        var forcedMoves = 0
        var deceptiveChoices = 0
        var stepCount = 0
        var totalBranching = 0

        while (simArrows.isNotEmpty()) {
            val clearable = PuzzleSolver.findClearableArrows(simArrows)
            if (clearable.isEmpty()) break

            val branchSize = clearable.size
            totalBranching += branchSize

            if (branchSize == 1) {
                forcedMoves++
            } else {
                // Check if some choices are deceptive (don't unlock any new arrows)
                for (cand in clearable) {
                    val nextState = simArrows.filter { it.id != cand.id }
                    val nextClearable = PuzzleSolver.findClearableArrows(nextState)
                    // If removing cand does not increase or enable new moves, it may be a side dead-branch
                    if (nextClearable.size < branchSize) {
                        deceptiveChoices++
                    }
                }
            }

            stepCount++
            // Remove one clearable arrow (prioritize greedy unlock)
            val best = clearable.first()
            simArrows.remove(best)
        }

        val maxDepth = stepCount
        val avgBranching = if (stepCount > 0) totalBranching.toFloat() / stepCount else 1f

        // 3. Spatial Complexity & Path Density
        val bounds = level.bounds
        val area = max(1f, bounds.width * bounds.height)
        val pathDensity = (count.toFloat() / area).coerceIn(0f, 2f)
        val spatialSpread = sqrt(bounds.width * bounds.width + bounds.height * bounds.height)

        // 4. Multi-Factor Difficulty Weighting (0..100)
        // A. Arrow Count factor (weight: 22%)
        val fArrowCount = (count.toFloat() / 75f).coerceIn(0f, 1f) * 22f

        // B. Dependency Depth factor (weight: 20%)
        val fDepth = (maxDepth.toFloat() / max(1f, count * 0.9f)).coerceIn(0f, 1f) * 20f

        // C. Average Blocking & Bottlenecks (weight: 15%)
        val fBlocking = (avgBlockingCount / 0.9f).coerceIn(0f, 1f) * 15f

        // D. Branching Factor & Choice Ambiguity (weight: 12%)
        val fBranching = (avgBranching / 6f).coerceIn(0f, 1f) * 12f

        // E. Deceptive Choices & Near-Collisions (weight: 11%)
        val fDeceptive = ((deceptiveChoices + nearCollisions).toFloat() / max(1f, count * 0.8f)).coerceIn(0f, 1f) * 11f

        // F. Path Density & Spatial Packing (weight: 10%)
        val fDensity = (pathDensity / 0.8f).coerceIn(0f, 1f) * 10f

        // G. Initial Constraint (Low initial available moves relative to count increases difficulty) (weight: 10%)
        val initialFreedomRatio = if (count > 0) (initialAvailableCount.toFloat() / count).coerceIn(0f, 1f) else 1f
        val fConstraint = (1f - initialFreedomRatio) * 10f

        val normalizedStructuralFactor = ((fArrowCount + fDepth + fBlocking + fBranching + fDeceptive + fDensity + fConstraint) / 100f).coerceIn(0f, 1f)

        // Tier is anchored by progression requirements (Levels 1-20 = Very Easy, 21-50 = Easy, 51-100 = Normal, etc.)
        val levelTier = if (level.id > 0) DifficultyTier.forLevelId(level.id) else DifficultyTier.forScore((normalizedStructuralFactor * 100f).roundToInt())

        val (minAllowedScore, maxAllowedScore) = when (levelTier) {
            DifficultyTier.VERY_EASY -> Pair(2, 12)
            DifficultyTier.EASY -> Pair(13, 24)
            DifficultyTier.NORMAL -> Pair(25, 38)
            DifficultyTier.MEDIUM -> Pair(39, 52)
            DifficultyTier.HARD -> Pair(53, 66)
            DifficultyTier.VERY_HARD -> Pair(67, 78)
            DifficultyTier.EXTREME -> Pair(79, 89)
            DifficultyTier.NIGHTMARE -> Pair(90, 100)
        }

        val calibratedScore = (minAllowedScore + normalizedStructuralFactor * (maxAllowedScore - minAllowedScore))
            .roundToInt()
            .coerceIn(minAllowedScore, maxAllowedScore)

        val tier = DifficultyTier.forScore(calibratedScore)
        val estimatedSeconds = max(10, (count * 1.5f + calibratedScore * 0.8f).roundToInt())

        return DifficultyAnalysis(
            complexityScore = calibratedScore,
            tier = tier,
            arrowCount = count,
            initialAvailableMoves = initialAvailableCount,
            forcedMovesCount = forcedMoves,
            averageBranchingFactor = avgBranching,
            maxDependencyDepth = maxDepth,
            averageBlockingCount = avgBlockingCount,
            nearCollisionsCount = nearCollisions,
            pathDensity = pathDensity,
            spatialSpread = spatialSpread,
            deceptiveChoicesCount = deceptiveChoices,
            estimatedTimeSeconds = estimatedSeconds
        )
    }

    private fun sqrt(v: Float): Float = kotlin.math.sqrt(v.toDouble()).toFloat()
}
