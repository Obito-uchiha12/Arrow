package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.LevelRepository
import com.example.engine.PuzzleSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Arrow Puzzle", appName)
  }

  @Test
  fun `test level 1 is valid and solvable`() {
    val level1 = LevelRepository.getLevel(1)
    assertTrue("Level 1 should have arrows", level1.arrows.isNotEmpty())
    val solveResult = PuzzleSolver.solve(level1)
    assertTrue("Level 1 must be solvable", solveResult.isSolvable)
  }
}

