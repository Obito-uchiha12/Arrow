package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LevelProgressEntity
import com.example.engine.LevelRepository
import com.example.model.BoardTheme
import com.example.model.DifficultyTier
import com.example.model.LevelCategory

@Composable
fun LevelSelectScreen(
    currentSelectedCategory: LevelCategory,
    onSelectCategory: (LevelCategory) -> Unit,
    progressList: List<LevelProgressEntity>,
    totalCompleted: Int,
    totalStars: Int,
    boardTheme: BoardTheme,
    onLevelSelected: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = Color(boardTheme.primaryColor)
    val accentColor = Color(boardTheme.accentColor)
    val surfaceColor = Color(boardTheme.surfaceColor)

    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpInputText by remember { mutableStateOf("") }

    val progressMap = remember(progressList) {
        progressList.associateBy { it.levelId }
    }

    // Filter level IDs for selected category
    val levelIds = remember(currentSelectedCategory) {
        LevelRepository.getLevelIdsForCategory(currentSelectedCategory)
    }

    val gridState = rememberLazyGridState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(boardTheme.backgroundColor))
            .padding(top = 12.dp)
            .testTag("level_select_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Arrow Puzzle",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                )
                Text(
                    text = "1000+ Directional Silhouette Levels",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = textColor.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Jump to Level Button
                IconButton(
                    onClick = { showJumpDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(surfaceColor.copy(alpha = 0.9f))
                        .testTag("jump_to_level_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Jump to Level",
                        tint = textColor
                    )
                }

                // Settings Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(surfaceColor.copy(alpha = 0.9f))
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = textColor
                    )
                }
            }
        }

        // Stats Overview Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor.copy(alpha = 0.9f),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .border(1.dp, textColor.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completed Levels
                Column {
                    Text(
                        text = "CLEARED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        text = "$totalCompleted / ${LevelRepository.TOTAL_LEVELS}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                    )
                }

                // Total Stars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "$totalStars",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                    )
                }
            }
        }

        // Category Filter Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(LevelCategory.values()) { category ->
                val isSelected = category == currentSelectedCategory
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) accentColor else surfaceColor,
                    shadowElevation = if (isSelected) 3.dp else 1.dp,
                    modifier = Modifier
                        .clickable { onSelectCategory(category) }
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else textColor.copy(alpha = 0.08f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textColor
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Levels Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            state = gridState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            items(levelIds, key = { it }) { levelId ->
                val progress = progressMap[levelId]
                val isUnlocked = progress?.isUnlocked ?: (levelId == 1)
                val isCompleted = progress?.isCompleted ?: false
                val stars = progress?.stars ?: 0

                val levelPreview = remember(levelId) { LevelRepository.getLevel(levelId) }
                val tier = levelPreview.difficulty

                LevelCardItem(
                    levelId = levelId,
                    levelName = levelPreview.name,
                    arrowCount = levelPreview.arrowCount,
                    tier = tier,
                    isUnlocked = isUnlocked,
                    isCompleted = isCompleted,
                    stars = stars,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    accentColor = accentColor,
                    onClick = {
                        if (isUnlocked) {
                            onLevelSelected(levelId)
                        }
                    }
                )
            }
        }
    }

    // Jump To Level Dialog
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = {
                Text("Jump to Level", fontWeight = FontWeight.Bold, color = textColor)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter level number (1 to ${LevelRepository.TOTAL_LEVELS}):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    OutlinedTextField(
                        value = jumpInputText,
                        onValueChange = { jumpInputText = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        placeholder = { Text("e.g. 42") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = jumpInputText.toIntOrNull()
                        if (num != null && num in 1..LevelRepository.TOTAL_LEVELS) {
                            showJumpDialog = false
                            onLevelSelected(num)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Play", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel", color = textColor)
                }
            }
        )
    }
}

@Composable
private fun LevelCardItem(
    levelId: Int,
    levelName: String,
    arrowCount: Int,
    tier: DifficultyTier,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    stars: Int,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) surfaceColor else surfaceColor.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked, onClick = onClick)
            .border(
                1.dp,
                if (isCompleted) accentColor.copy(alpha = 0.4f) else textColor.copy(alpha = 0.06f),
                RoundedCornerShape(18.dp)
            )
            .testTag("level_card_$levelId")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Level Number and status icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$levelId",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isUnlocked) textColor else textColor.copy(alpha = 0.4f)
                    )
                )

                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                } else if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = textColor.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Silhouette name
            Text(
                text = levelName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) textColor.copy(alpha = 0.8f) else textColor.copy(alpha = 0.35f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Arrow count + Tier pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(tier.badgeColor).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${tier.displayName} • $arrowCount",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(tier.badgeColor)
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Stars
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                for (s in 1..3) {
                    val isLit = s <= stars
                    Icon(
                        imageVector = if (isLit) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (isLit) Color(0xFFFFB703) else textColor.copy(alpha = 0.18f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
