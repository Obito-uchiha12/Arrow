package com.example.engine

import com.example.model.Arrow
import com.example.model.DifficultyTier
import com.example.model.PuzzleLevel

/**
 * Validates structural integrity, initial moves availability, step-by-step clearance simulation,
 * and difficulty classification for puzzle levels.
 */
object PuzzleValidator {

    data class ValidationReport(
        val isValid: Boolean,
        val isSolvable: Boolean,
        val arrowCount: Int,
        val initialAvailableMoves: Int,
        val solutionLength: Int,
        val difficultyScore: Int,
        val difficultyTier: DifficultyTier,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    /**
     * Strictly validates a level for:
     * 1. Minimum arrows & non-empty grid
     * 2. Unique arrow IDs & valid point coordinates
     * 3. At least one available move at the beginning
     * 4. Step-by-step full clearance simulation (choose valid arrow -> remove -> recalculate blockers -> continue)
     * 5. No deadlock / cycle states
     * 6. Difficulty score & tier computation
     */
    fun validateLevel(level: PuzzleLevel): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val arrows = level.arrows
        if (arrows.isEmpty()) {
            errors.add("Level has no arrows")
            return ValidationReport(
                isValid = false,
                isSolvable = false,
                arrowCount = 0,
                initialAvailableMoves = 0,
                solutionLength = 0,
                difficultyScore = 0,
                difficultyTier = DifficultyTier.VERY_EASY,
                errors = errors
            )
        }

        val idSet = mutableSetOf<Int>()
        for (arrow in arrows) {
            if (!idSet.add(arrow.id)) {
                errors.add("Duplicate arrow ID: ${arrow.id}")
            }
            if (arrow.points.size < 2) {
                errors.add("Arrow ${arrow.id} has fewer than 2 points")
            }
            if (!validateArrow(arrow)) {
                errors.add("Arrow ${arrow.id} has invalid/zero-length segments")
            }
        }

        // 1. Initial available move verification
        val initialClearable = PuzzleSolver.findClearableArrows(arrows)
        val initialMovesCount = initialClearable.size
        if (initialMovesCount == 0) {
            errors.add("Deadlock at start: No arrows can be moved at the initial state")
        }

        // 2. Full simulation validation: choose valid arrow -> remove -> recalculate blockers -> continue
        val currentRemaining = arrows.toMutableList()
        val solutionSequence = mutableListOf<Int>()
        var simulatedStep = 0

        while (currentRemaining.isNotEmpty()) {
            val available = PuzzleSolver.findClearableArrows(currentRemaining)
            if (available.isEmpty()) {
                errors.add("Simulation deadlock after removing ${solutionSequence.size} of ${arrows.size} arrows")
                break
            }

            simulatedStep++
            // Remove one valid arrow and recalculate blockers for the remaining set
            val chosenArrow = available.first()
            currentRemaining.remove(chosenArrow)
            solutionSequence.add(chosenArrow.id)
        }

        val isFullyCleared = currentRemaining.isEmpty() && solutionSequence.size == arrows.size
        if (!isFullyCleared && !errors.any { it.contains("Simulation deadlock") }) {
            errors.add("Incomplete clearance: ${currentRemaining.size} arrows could not be removed")
        }

        if (arrows.size > 200) {
            warnings.add("Arrow count is very high (${arrows.size}), may affect canvas performance on low-end devices")
        }

        // 3. Difficulty calculation
        val diffAnalysis = DifficultyCalculator.analyze(level)

        val isValid = errors.isEmpty() && isFullyCleared

        return ValidationReport(
            isValid = isValid,
            isSolvable = isFullyCleared,
            arrowCount = arrows.size,
            initialAvailableMoves = initialMovesCount,
            solutionLength = solutionSequence.size,
            difficultyScore = diffAnalysis.complexityScore,
            difficultyTier = diffAnalysis.tier,
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validates visual quality and shape recognition against the corresponding shape silhouette.
     */
    fun validateQuality(level: PuzzleLevel, shape: ShapeSilhouettes.ShapeDef? = null): ShapeQualityScorer.QualityScoreReport {
        val targetShape = shape ?: ShapeSilhouettes.ALL_SHAPES.find { it.name == level.name || it.category == level.category }
        ?: ShapeSilhouettes.ALL_SHAPES.first()
        return ShapeQualityScorer.evaluate(targetShape, level.arrows, level)
    }

    /**
     * Validates a single arrow structure.
     */
    fun validateArrow(arrow: Arrow): Boolean {
        if (arrow.points.size < 2) return false
        for (i in 0 until arrow.points.size - 1) {
            val p1 = arrow.points[i]
            val p2 = arrow.points[i + 1]
            if (p1.distanceTo(p2) < 0.05f) return false
        }
        return true
    }
}
