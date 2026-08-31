package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundEffectsManager
import com.example.engine.LevelRepository
import com.example.engine.PuzzleGeometry
import com.example.engine.PuzzleSolver
import com.example.model.Arrow
import com.example.model.ArrowState
import com.example.model.Direction
import com.example.model.GridPoint
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RuntimeArrowInteractionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        viewModel = GameViewModel(app)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test01_TapActiveClearableArrow() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialArrows = viewModel.remainingArrows.value
        val initialCount = initialArrows.size
        assertTrue("Level 1 must have arrows", initialCount > 0)

        // Find clearable arrow
        val clearableIds = viewModel.clearableArrowIds.value
        assertTrue("Clearable arrows must exist in Level 1", clearableIds.isNotEmpty())
        val clearableId = clearableIds.first()
        val targetArrow = initialArrows.first { it.id == clearableId }

        assertEquals(ArrowState.ACTIVE, viewModel.arrowStates.value[clearableId])

        // Tap the clearable arrow
        viewModel.onArrowTapped(targetArrow)

        // Arrow must immediately enter MOVING state and be removed from remaining active board list
        assertEquals(ArrowState.MOVING, viewModel.arrowStates.value[clearableId])
        assertEquals(initialCount - 1, viewModel.remainingArrows.value.size)
        assertFalse(viewModel.remainingArrows.value.any { it.id == clearableId })

        // Flight animation active
        assertTrue(viewModel.flyingAnimations.value.any { it.arrow.id == clearableId })

        // Advance animation to completion
        testDispatcher.scheduler.advanceUntilIdle()

        // After flight completes, arrow state becomes REMOVED and flying animation is removed
        assertEquals(ArrowState.REMOVED, viewModel.arrowStates.value[clearableId])
        assertFalse(viewModel.flyingAnimations.value.any { it.arrow.id == clearableId })
    }

    @Test
    fun test02_TapBlockedArrow() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialArrows = viewModel.remainingArrows.value
        val clearableIds = viewModel.clearableArrowIds.value
        val blockedArrow = initialArrows.firstOrNull { !clearableIds.contains(it.id) }

        if (blockedArrow != null) {
            val initialHearts = viewModel.currentHearts.value

            // Tap blocked arrow
            viewModel.onArrowTapped(blockedArrow)

            // Must enter BLOCKED state during nudge animation
            assertEquals(ArrowState.BLOCKED, viewModel.arrowStates.value[blockedArrow.id])
            // Count must not decrease
            assertEquals(initialArrows.size, viewModel.remainingArrows.value.size)
            // Heart penalty applied
            assertEquals(initialHearts - 1, viewModel.currentHearts.value)

            // Advance through spring bounce
            testDispatcher.scheduler.advanceUntilIdle()

            // State returns to ACTIVE after recoil completes
            assertEquals(ArrowState.ACTIVE, viewModel.arrowStates.value[blockedArrow.id])
        }
    }

    @Test
    fun test03_TapRemovedArrowIgnored() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val clearableId = viewModel.clearableArrowIds.value.first()
        val targetArrow = viewModel.remainingArrows.value.first { it.id == clearableId }

        // Clear it
        viewModel.onArrowTapped(targetArrow)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ArrowState.REMOVED, viewModel.arrowStates.value[clearableId])
        val movesAfterClear = viewModel.movesCount.value
        val remainingAfterClear = viewModel.remainingArrows.value.size

        // Tap the removed arrow again
        viewModel.onArrowTapped(targetArrow)
        testDispatcher.scheduler.advanceUntilIdle()

        // State remains REMOVED, no move incremented, no crash
        assertEquals(ArrowState.REMOVED, viewModel.arrowStates.value[clearableId])
        assertEquals(movesAfterClear, viewModel.movesCount.value)
        assertEquals(remainingAfterClear, viewModel.remainingArrows.value.size)
    }

    @Test
    fun test04_DoubleTapArrowProtection() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val clearableId = viewModel.clearableArrowIds.value.first()
        val targetArrow = viewModel.remainingArrows.value.first { it.id == clearableId }
        val initialCount = viewModel.remainingArrows.value.size

        // Double tap rapidly in succession
        viewModel.onArrowTapped(targetArrow)
        viewModel.onArrowTapped(targetArrow)

        // First tap initiated MOVING state; second tap is ignored
        assertEquals(ArrowState.MOVING, viewModel.arrowStates.value[clearableId])
        assertEquals(initialCount - 1, viewModel.remainingArrows.value.size)
        // Exactly one flying animation spawned
        assertEquals(1, viewModel.flyingAnimations.value.count { it.arrow.id == clearableId })

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ArrowState.REMOVED, viewModel.arrowStates.value[clearableId])
        assertEquals(0, viewModel.flyingAnimations.value.count { it.arrow.id == clearableId })
    }

    @Test
    fun test05_RapidTapMultipleArrows() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val clearableList = viewModel.clearableArrowIds.value.toList()
        assertTrue("At least 2 clearable arrows in Level 1", clearableList.size >= 2)

        val arrow1 = viewModel.remainingArrows.value.first { it.id == clearableList[0] }
        val arrow2 = viewModel.remainingArrows.value.first { it.id == clearableList[1] }

        // Rapidly tap both without waiting
        viewModel.onArrowTapped(arrow1)
        viewModel.onArrowTapped(arrow2)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ArrowState.REMOVED, viewModel.arrowStates.value[arrow1.id])
        assertEquals(ArrowState.REMOVED, viewModel.arrowStates.value[arrow2.id])
    }

    @Test
    fun test06_RemoveArrowExactlyOnce() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val clearableId = viewModel.clearableArrowIds.value.first()
        val targetArrow = viewModel.remainingArrows.value.first { it.id == clearableId }

        viewModel.onArrowTapped(targetArrow)
        viewModel.onArrowTapped(targetArrow)
        viewModel.onArrowTapped(targetArrow)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ArrowState.REMOVED, viewModel.arrowStates.value[clearableId])
        val occurrences = viewModel.remainingArrows.value.count { it.id == clearableId }
        assertEquals(0, occurrences)
    }

    @Test
    fun test07_RecalculateAfterRemoval() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialClearable = viewModel.clearableArrowIds.value.toSet()
        val arrowToClear = viewModel.remainingArrows.value.first { initialClearable.contains(it.id) }

        viewModel.onArrowTapped(arrowToClear)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedClearable = viewModel.clearableArrowIds.value
        assertFalse(updatedClearable.contains(arrowToClear.id))
        assertTrue("Next clearable arrows must be computed", updatedClearable.isNotEmpty() || viewModel.remainingArrows.value.isEmpty())
    }

    @Test
    fun test08_RestartDuringAnimation() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val clearableId = viewModel.clearableArrowIds.value.first()
        val targetArrow = viewModel.remainingArrows.value.first { it.id == clearableId }

        // Start animation
        viewModel.onArrowTapped(targetArrow)
        assertEquals(1, viewModel.flyingAnimations.value.size)

        // Immediately restart level while animation is in flight
        viewModel.onRestartLevel()
        testDispatcher.scheduler.advanceUntilIdle()

        // All arrows restored to active, flying animations cleared
        val level1 = LevelRepository.getLevel(1)
        assertEquals(level1.arrowCount, viewModel.remainingArrows.value.size)
        assertEquals(0, viewModel.flyingAnimations.value.size)
        assertFalse(viewModel.isVictory.value)
        assertEquals(ArrowState.ACTIVE, viewModel.arrowStates.value[clearableId])
    }

    @Test
    fun test09_NextLevelAfterCompletion() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Solve Level 1 completely
        val solveSequence = PuzzleSolver.solve(viewModel.currentLevel.value).solutionSequence
        assertTrue(solveSequence.isNotEmpty())

        for (id in solveSequence) {
            val arrow = viewModel.remainingArrows.value.firstOrNull { it.id == id }
            assertNotNull(arrow)
            viewModel.onArrowTapped(arrow!!)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        assertTrue("Victory reached", viewModel.isVictory.value)
        assertEquals(0, viewModel.remainingArrows.value.size)

        // Load next level
        viewModel.onNextLevel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.currentLevelId.value)
        assertFalse(viewModel.isVictory.value)
        val level2 = LevelRepository.getLevel(2)
        assertEquals(level2.arrowCount, viewModel.remainingArrows.value.size)
    }

    @Test
    fun test10_EmptyArrowListSafety() {
        val tapped = PuzzleGeometry.findTappedArrow(
            tapX = 5.0f,
            tapY = 5.0f,
            arrows = emptyList(),
            hitRadius = 0.75f
        )
        assertNull("Empty arrow list must safely return null", tapped)
    }

    @Test
    fun test11_InvalidArrowIdSafety() {
        viewModel.loadLevel(1)
        // Should ignore and not crash
        viewModel.onArrowTappedById(999999)
        viewModel.onArrowTappedById(-1)
    }

    @Test
    fun test12_LevelCompletionExactlyOnce() = runTest {
        viewModel.loadLevel(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val solveSequence = PuzzleSolver.solve(viewModel.currentLevel.value).solutionSequence
        for (id in solveSequence) {
            val arrow = viewModel.remainingArrows.value.first { it.id == id }
            viewModel.onArrowTapped(arrow)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        assertTrue(viewModel.isVictory.value)
        val initialStars = viewModel.starsEarned.value

        // Subsequent taps or callbacks do not re-trigger or corrupt victory
        viewModel.onArrowTappedById(solveSequence.last())
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isVictory.value)
        assertEquals(initialStars, viewModel.starsEarned.value)
    }

    @Test
    fun test13_EndToEndPlayLevels1Through10() = runTest {
        for (levelId in 1..10) {
            viewModel.loadLevel(levelId)
            testDispatcher.scheduler.advanceUntilIdle()

            val level = viewModel.currentLevel.value
            assertEquals(levelId, level.id)
            assertTrue("Level $levelId must have arrows", level.arrows.isNotEmpty())
            assertEquals(level.arrowCount, viewModel.remainingArrows.value.size)
            assertFalse(viewModel.isVictory.value)

            val solver = PuzzleSolver.solve(level)
            assertTrue("Level $levelId must be solvable", solver.isSolvable)

            // Play all arrows in order
            for (arrowId in solver.solutionSequence) {
                val arrow = viewModel.remainingArrows.value.firstOrNull { it.id == arrowId }
                assertNotNull("Arrow $arrowId must be on board", arrow)
                assertEquals(ArrowState.ACTIVE, viewModel.arrowStates.value[arrowId])

                viewModel.onArrowTapped(arrow!!)
                testDispatcher.scheduler.advanceUntilIdle()

                assertEquals(ArrowState.REMOVED, viewModel.arrowStates.value[arrowId])
            }

            assertTrue("Level $levelId solved successfully", viewModel.isVictory.value)
            assertEquals(0, viewModel.remainingArrows.value.size)
        }
    }

    @Test
    fun test14_HitTestingPrecisionAndEdgeCases() {
        val arrow = Arrow(
            id = 101,
            points = listOf(GridPoint(2f, 2f), GridPoint(6f, 2f)),
            facing = Direction.RIGHT
        )
        val arrows = listOf(arrow)

        // 1. Direct hit on body
        val directHit = PuzzleGeometry.findTappedArrow(4f, 2.1f, arrows, hitRadius = 0.75f)
        assertNotNull(directHit)
        assertEquals(101, directHit?.id)

        // 2. Direct hit on tip
        val tipHit = PuzzleGeometry.findTappedArrow(6.1f, 2.0f, arrows, hitRadius = 0.75f)
        assertNotNull(tipHit)
        assertEquals(101, tipHit?.id)

        // 3. Far away tap (empty space)
        val miss = PuzzleGeometry.findTappedArrow(15f, 15f, arrows, hitRadius = 0.75f)
        assertNull(miss)

        // 4. Overlapping region deterministic selection
        val arrow2 = Arrow(
            id = 102,
            points = listOf(GridPoint(2f, 3f), GridPoint(6f, 3f)),
            facing = Direction.RIGHT
        )
        val closeTo102 = PuzzleGeometry.findTappedArrow(4f, 2.9f, listOf(arrow, arrow2), hitRadius = 0.75f)
        assertEquals(102, closeTo102?.id)
    }

    @Test
    fun test15_SoundEffectsHapticSafety() {
        val soundManager = SoundEffectsManager(app)
        // Sound and haptic methods must never crash even if hardware is absent
        soundManager.soundEnabled = true
        soundManager.hapticsEnabled = true

        soundManager.playClearSound(1.0f)
        soundManager.playBlockedSound()
        soundManager.playUndoSound()
        soundManager.playHintSound()
        soundManager.playVictoryFanfare()

        soundManager.vibrateSuccess()
        soundManager.vibrateBlocked()
        soundManager.vibrateVictory()
    }
}
