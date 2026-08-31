package com.example

import com.example.engine.CompletionScoreSystem
import com.example.engine.DifficultyCalculator
import com.example.engine.LevelRepository
import com.example.engine.LivesHeartSystem
import com.example.engine.LivesState
import com.example.engine.OfflineRewardAdProvider
import com.example.engine.PuzzleGeometry
import com.example.engine.PuzzleSolver
import com.example.engine.PuzzleValidator
import com.example.model.Arrow
import com.example.model.Direction
import com.example.model.GridPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testLevelGenerationAndValidation() {
    for (id in listOf(1, 5, 20, 50, 100)) {
      val level = LevelRepository.getLevel(id)
      assertNotNull(level)
      assertTrue("Level $id should have arrows", level.arrows.isNotEmpty())
      val report = PuzzleValidator.validateLevel(level)
      assertTrue("Level $id should be valid: ${report.errors}", report.isValid)
      assertTrue("Level $id should be solvable", report.isSolvable)
    }
  }

  @Test
  fun testDifficultyCalculator() {
    val level1 = LevelRepository.getLevel(1)
    val level100 = LevelRepository.getLevel(100)

    val diff1 = DifficultyCalculator.analyze(level1)
    val diff100 = DifficultyCalculator.analyze(level100)

    assertTrue("Difficulty score should be between 1 and 100", diff1.complexityScore in 1..100)
    assertTrue("Higher level should generally be more complex", diff100.arrowCount >= diff1.arrowCount)
  }

  @Test
  fun testLivesHeartSystem() {
    var state = LivesState(currentLives = 5, maxLives = 5)
    assertTrue(state.canPlay)
    assertFalse(state.isGameOver)

    // Consume life
    state = LivesHeartSystem.consumeLife(state)
    assertEquals(4, state.currentLives)

    // Refill
    state = LivesHeartSystem.refillFull(state)
    assertEquals(5, state.currentLives)
  }

  @Test
  fun testCompletionScoreSystem() {
    val level = LevelRepository.getLevel(1)
    val scorePerfect = CompletionScoreSystem.calculate(level, movesTaken = level.arrowCount, timeElapsedSeconds = 10)
    assertEquals(3, scorePerfect.stars)
    assertTrue(scorePerfect.totalScore > 0)
  }

  @Test
  fun testOfflineRewardAdProvider() {
    val provider = OfflineRewardAdProvider(allowSimulatedRewardsInDebug = false)
    assertFalse(provider.isAdAvailable)
  }
}

