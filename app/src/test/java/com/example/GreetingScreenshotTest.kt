package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.engine.LevelRepository
import com.example.model.ArrowColorPreset
import com.example.model.ArrowThickness
import com.example.model.BoardTheme
import com.example.ui.components.ArrowCanvas
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val level1 = LevelRepository.getLevel(1)
    composeTestRule.setContent {
      MyApplicationTheme {
        ArrowCanvas(
          level = level1,
          remainingArrows = level1.arrows,
          flyingArrows = emptyList(),
          blockedAnimations = emptyMap(),
          hintArrowId = null,
          boardTheme = BoardTheme.LIGHT_ZEN,
          arrowColor = androidx.compose.ui.graphics.Color(ArrowColorPreset.DEFAULT_DARK.colorValue),
          arrowThickness = ArrowThickness.MEDIUM,
          onArrowTapped = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

