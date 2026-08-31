package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.components.ArrowCanvas
import com.example.ui.components.BottomGameControls
import com.example.ui.components.OutOfHeartsDialog
import com.example.ui.components.SettingsSheet
import com.example.ui.components.TopGameBar
import com.example.ui.components.TutorialOverlayDialog
import com.example.ui.components.VictoryDialog
import com.example.ui.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val isLevelSelect by viewModel.isLevelSelectScreen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsSheetOpen.collectAsState()
    val isTutorialOpen by viewModel.isTutorialOpen.collectAsState()
    val isVictory by viewModel.isVictory.collectAsState()
    val isOutOfHearts by viewModel.isOutOfHearts.collectAsState()
    val currentHearts by viewModel.currentHearts.collectAsState()

    val level by viewModel.currentLevel.collectAsState()
    val remainingArrows by viewModel.remainingArrows.collectAsState()
    val flyingArrows by viewModel.flyingAnimations.collectAsState()
    val blockedDisplacements by viewModel.blockedDisplacements.collectAsState()
    val hintArrowId by viewModel.hintArrowId.collectAsState()
    val movesCount by viewModel.movesCount.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val starsEarned by viewModel.starsEarned.collectAsState()

    val allProgress by viewModel.allProgress.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val totalStars by viewModel.totalStars.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val boardTheme = userSettings.boardTheme
    val arrowColor = Color(userSettings.arrowColor.getColorForTheme(boardTheme.isDark))

    // Handle system back button
    BackHandler(enabled = isLevelSelect) {
        viewModel.hideLevelSelect()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(boardTheme.backgroundColor))
    ) {
        if (isLevelSelect) {
            LevelSelectScreen(
                currentSelectedCategory = selectedCategory,
                onSelectCategory = { viewModel.selectCategory(it) },
                progressList = allProgress,
                totalCompleted = completedCount,
                totalStars = totalStars,
                boardTheme = boardTheme,
                onLevelSelected = {
                    viewModel.loadLevel(it)
                    viewModel.hideLevelSelect()
                },
                onOpenSettings = { viewModel.openSettings() },
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar
                TopGameBar(
                    level = level,
                    hearts = currentHearts,
                    maxHearts = viewModel.maxHearts,
                    remainingCount = remainingArrows.size,
                    totalCount = level.arrowCount,
                    elapsedSeconds = elapsedSeconds,
                    zenMode = userSettings.zenMode,
                    boardTheme = boardTheme,
                    onBackToLevelSelect = { viewModel.showLevelSelect() },
                    onRestartLevel = { viewModel.onRestartLevel() },
                    onOpenSettings = { viewModel.openSettings() }
                )

                // Main Puzzle Canvas (Directional arrow silhouette tap zone)
                ArrowCanvas(
                    level = level,
                    remainingArrows = remainingArrows,
                    flyingArrows = flyingArrows,
                    blockedAnimations = blockedDisplacements,
                    hintArrowId = hintArrowId,
                    boardTheme = boardTheme,
                    arrowColor = arrowColor,
                    arrowThickness = userSettings.arrowThickness,
                    onArrowTapped = { arrow -> viewModel.onArrowTapped(arrow) },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )

                // Bottom Controls (Undo, Dual Hints, Remaining Counter)
                BottomGameControls(
                    remainingCount = remainingArrows.size,
                    totalCount = level.arrowCount,
                    canUndo = remainingArrows.size < level.arrowCount,
                    movesCount = movesCount,
                    elapsedSeconds = elapsedSeconds,
                    zenMode = userSettings.zenMode,
                    highlightHintsCount = userSettings.highlightHintsCount,
                    autoMoveHintsCount = userSettings.autoMoveHintsCount,
                    boardTheme = boardTheme,
                    onUndo = { viewModel.onUndo() },
                    onHighlightHint = { viewModel.onHighlightHint() },
                    onAutoMoveHint = { viewModel.onAutoMoveHint() }
                )
            }
        }

        // Out of Hearts Dialog
        if (isOutOfHearts) {
            OutOfHeartsDialog(
                canUndo = remainingArrows.size < level.arrowCount,
                boardTheme = boardTheme,
                onRetry = { viewModel.onRestartLevel() },
                onUndo = { viewModel.onUndo() },
                onEnableZenMode = { viewModel.enableZenModeFromDialog() },
                onDismiss = { viewModel.dismissOutOfHeartsDialog() }
            )
        }

        // Victory Dialog
        if (isVictory) {
            VictoryDialog(
                level = level,
                starsEarned = starsEarned,
                elapsedSeconds = elapsedSeconds,
                movesCount = movesCount,
                boardTheme = boardTheme,
                onNextLevel = {
                    viewModel.onNextLevel()
                },
                onLevelSelect = {
                    viewModel.showLevelSelect()
                },
                onReplay = {
                    viewModel.onRestartLevel()
                }
            )
        }

        // Settings Bottom Sheet
        if (isSettingsOpen) {
            SettingsSheet(
                settings = userSettings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onOpenTutorial = { viewModel.openTutorial() },
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // First-Time / Interactive Tutorial Overlay Dialog
        if (isTutorialOpen) {
            TutorialOverlayDialog(
                boardTheme = boardTheme,
                onDismiss = { viewModel.closeTutorial() }
            )
        }
    }
}
