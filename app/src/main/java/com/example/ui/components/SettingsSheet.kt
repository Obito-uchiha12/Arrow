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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArrowColorPreset
import com.example.model.ArrowThickness
import com.example.model.BoardTheme
import com.example.model.UserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: UserSettings,
    onSettingsChanged: (UserSettings) -> Unit,
    onOpenTutorial: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val theme = settings.boardTheme
    val textColor = Color(theme.primaryColor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(theme.surfaceColor),
        modifier = Modifier.testTag("settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Puzzle Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textColor
                    )
                }
            }

            // 1. Arrow Color Palette
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Arrow Color",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ArrowColorPreset.values()) { preset ->
                        val isSelected = preset == settings.arrowColor
                        val color = Color(preset.getColorForTheme(theme.isDark))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    onSettingsChanged(settings.copy(arrowColor = preset))
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color(theme.accentColor) else textColor.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    val checkTint = if (color.luminance() > 0.5f) Color.Black else Color.White
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = checkTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Color(theme.accentColor) else textColor.copy(alpha = 0.7f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            // 2. Board Background Theme
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Board Canvas Theme",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BoardTheme.values()) { bTheme ->
                        val isSelected = bTheme == settings.boardTheme
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(bTheme.backgroundColor),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(theme.accentColor) else Color.Gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .clickable {
                                    onSettingsChanged(settings.copy(boardTheme = bTheme))
                                }
                                .width(108.dp)
                                .height(60.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Text(
                                    text = bTheme.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(bTheme.primaryColor)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Arrow Stroke Thickness
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Stroke Style",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArrowThickness.values().forEach { thickness ->
                        val isSelected = thickness == settings.arrowThickness
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(theme.accentColor).copy(alpha = 0.15f) else Color(theme.backgroundColor),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(theme.accentColor) else Color.Gray.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    onSettingsChanged(settings.copy(arrowThickness = thickness))
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = thickness.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(theme.accentColor) else textColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. Animation Speed / Timing
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Animation Speed",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.model.AnimationSpeed.values().forEach { speed ->
                        val isSelected = speed == settings.animationSpeed
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(theme.accentColor).copy(alpha = 0.15f) else Color(theme.backgroundColor),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(theme.accentColor) else Color.Gray.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    onSettingsChanged(settings.copy(animationSpeed = speed))
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = speed.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(theme.accentColor) else textColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5. Sound & Vibration Toggles
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SettingToggleRow(
                    label = "Sound Effects",
                    description = "Crisp audio pops & victory chimes",
                    checked = settings.soundEnabled,
                    textColor = textColor,
                    accentColor = Color(theme.accentColor),
                    onCheckedChange = { onSettingsChanged(settings.copy(soundEnabled = it)) }
                )

                SettingToggleRow(
                    label = "Haptic Vibration",
                    description = "Tactile feedback on taps & obstacles",
                    checked = settings.hapticsEnabled,
                    textColor = textColor,
                    accentColor = Color(theme.accentColor),
                    onCheckedChange = { onSettingsChanged(settings.copy(hapticsEnabled = it)) }
                )

                SettingToggleRow(
                    label = "Zen Mode",
                    description = "Unlimited hearts for relaxed, unhurried play",
                    checked = settings.zenMode,
                    textColor = textColor,
                    accentColor = Color(theme.accentColor),
                    onCheckedChange = { onSettingsChanged(settings.copy(zenMode = it)) }
                )

                // How to Play Tutorial button
                Surface(
                    onClick = {
                        onDismiss()
                        onOpenTutorial()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(theme.backgroundColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("how_to_play_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "💡 How to Play (Tutorial)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    textColor: Color,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = textColor.copy(alpha = 0.6f)
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor
            )
        )
    }
}
