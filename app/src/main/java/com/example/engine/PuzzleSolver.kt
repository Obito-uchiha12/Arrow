package com.example.engine

import com.example.model.Arrow
import com.example.model.PuzzleLevel

object PuzzleSolver {

    data class SolveResult(
        val isSolvable: Boolean,
        val solutionSequence: List<Int> = emptyList(),
        val branchingFactorAvg: Float = 0f,
        val maxDepth: Int = 0
    )

    /**
     * Finds all arrow IDs that are currently free to escape.
     */
    fun findClearableArrows(remainingArrows: List<Arrow>): List<Arrow> {
        val clearable = mutableListOf<Arrow>()
        for (arrow in remainingArrows) {
            val collision = PuzzleGeometry.checkArrowCollision(arrow, remainingArrows)
            if (!collision.isBlocked) {
                clearable.add(arrow)
            }
        }
        return clearable
    }

    /**
     * Checks if a level can be cleared from current state to empty.
     */
    fun solve(level: PuzzleLevel): SolveResult {
        return solveFromState(level.arrows)
    }

    fun solveFromState(initialArrows: List<Arrow>): SolveResult {
        val current = initialArrows.toMutableList()
        val solution = mutableListOf<Int>()
        var totalChoices = 0
        var stepCount = 0

        while (current.isNotEmpty()) {
            val clearable = findClearableArrows(current)
            if (clearable.isEmpty()) {
                return SolveResult(isSolvable = false, solutionSequence = solution)
            }
            totalChoices += clearable.size
            stepCount++

            // Pick the first clearable arrow to simulate player step
            val next = clearable.first()
            current.remove(next)
            solution.add(next.id)
        }

        val avgBranching = if (stepCount > 0) totalChoices.toFloat() / stepCount else 0f
        return SolveResult(
            isSolvable = true,
            solutionSequence = solution,
            branchingFactorAvg = avgBranching,
            maxDepth = stepCount
        )
    }

    /**
     * Returns the optimal hint arrow ID (or null if stuck/empty).
     */
    fun getHint(remainingArrows: List<Arrow>): Arrow? {
        val clearable = findClearableArrows(remainingArrows)
        if (clearable.isEmpty()) return null
        // Prioritize arrows that unlock the most downstream arrows
        var bestArrow = clearable.first()
        var maxFreed = -1

        for (candidate in clearable) {
            val simulated = remainingArrows.filter { it.id != candidate.id }
            val nextClearable = findClearableArrows(simulated).size
            if (nextClearable > maxFreed) {
                maxFreed = nextClearable
                bestArrow = candidate
            }
        }
        return bestArrow
    }
}
