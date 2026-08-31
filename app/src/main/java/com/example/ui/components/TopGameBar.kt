package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoardTheme
import com.example.model.DifficultyTier
import com.example.model.PuzzleLevel

@Composable
fun TopGameBar(
    level: PuzzleLevel,
    hearts: Int,
    maxHearts: Int,
    remainingCount: Int,
    totalCount: Int,
    elapsedSeconds: Int,
    zenMode: Boolean,
    boardTheme: BoardTheme,
    onBackToLevelSelect: () -> Unit,
    onRestartLevel: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = Color(boardTheme.primaryColor)
    val subTextColor = textColor.copy(alpha = 0.65f)
    val tier = level.difficulty

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back / Level select button
        IconButton(
            onClick = onBackToLevelSelect,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(boardTheme.surfaceColor).copy(alpha = 0.85f))
                .testTag("back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Select Level",
                tint = textColor
            )
        }

        // Center: Level Name, Tier, and Hearts Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "LEVEL ${level.id}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = textColor
                    )
                )

                // Difficulty tier badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(tier.badgeColor).copy(alpha = 0.18f),
                    contentColor = Color(tier.badgeColor)
                ) {
                    Text(
                        text = tier.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Hearts / Lives Row
            if (!zenMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.testTag("hearts_container")
                ) {
                    for (i in 1..maxHearts) {
                        val isAlive = i <= hearts
                        Text(
                            text = if (isAlive) "❤️" else "🖤",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 1.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(boardTheme.accentColor).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Zen Mode • ∞",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(boardTheme.accentColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Right Action Buttons (Restart & Settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onRestartLevel,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(boardTheme.surfaceColor).copy(alpha = 0.85f))
                    .testTag("restart_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restart Level",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(boardTheme.surfaceColor).copy(alpha = 0.85f))
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
