package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
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

@Composable
fun BottomGameControls(
    remainingCount: Int,
    totalCount: Int,
    canUndo: Boolean,
    movesCount: Int,
    elapsedSeconds: Int,
    zenMode: Boolean,
    highlightHintsCount: Int,
    autoMoveHintsCount: Int,
    boardTheme: BoardTheme,
    onUndo: () -> Unit,
    onHighlightHint: () -> Unit,
    onAutoMoveHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = Color(boardTheme.surfaceColor)
    val primaryColor = Color(boardTheme.primaryColor)
    val accentColor = Color(boardTheme.accentColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status Row: Remaining Arrows Counter + Timer / Moves
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Remaining Count Capsule
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor.copy(alpha = 0.9f),
                shadowElevation = 2.dp,
                modifier = Modifier.border(1.dp, primaryColor.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "$remainingCount / $totalCount left",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )
                }
            }

            // Stats capsule (Timer or Moves)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor.copy(alpha = 0.9f),
                shadowElevation = 2.dp,
                modifier = Modifier.border(1.dp, primaryColor.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!zenMode) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = primaryColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = formatTime(elapsedSeconds),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        )
                    } else {
                        Text(
                            text = "Moves: $movesCount",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        )
                    }
                }
            }
        }

        // Action Buttons Row: Undo | Hint 1 (Highlight) | Hint 2 (Auto Move)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Undo Button
            Surface(
                onClick = onUndo,
                enabled = canUndo,
                shape = RoundedCornerShape(16.dp),
                color = if (canUndo) surfaceColor else surfaceColor.copy(alpha = 0.5f),
                shadowElevation = if (canUndo) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .border(
                        1.dp,
                        if (canUndo) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("undo_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) primaryColor else primaryColor.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Undo",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (canUndo) primaryColor else primaryColor.copy(alpha = 0.35f)
                        )
                    )
                }
            }

            // 2. Hint 1: Highlight Move (Search/Lightbulb)
            Surface(
                onClick = onHighlightHint,
                shape = RoundedCornerShape(16.dp),
                color = surfaceColor,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .weight(1.15f)
                    .height(52.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .testTag("hint_highlight_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Highlight Hint",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Highlight",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "$highlightHintsCount left",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            // 3. Hint 2: Auto Move (Lightning / Bolt)
            Surface(
                onClick = onAutoMoveHint,
                shape = RoundedCornerShape(16.dp),
                color = accentColor,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .weight(1.15f)
                    .height(52.dp)
                    .testTag("hint_automove_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Auto Move Hint",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Auto Move",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "$autoMoveHintsCount left",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
