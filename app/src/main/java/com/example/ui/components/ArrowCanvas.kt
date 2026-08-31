package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.PuzzleGeometry
import com.example.model.Arrow
import com.example.model.ArrowThickness
import com.example.model.BoardTheme
import com.example.model.Direction
import com.example.model.GridPoint
import com.example.model.PuzzleLevel
import com.example.model.RectF
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class FlyingArrowAnimation(
    val arrow: Arrow,
    val startTimeMs: Long = System.currentTimeMillis(),
    val durationMs: Long = 300L
)

data class BlockedArrowAnimation(
    val arrowId: Int,
    val maxNudge: Float = 0.35f,
    val startTimeMs: Long = System.currentTimeMillis(),
    val durationMs: Long = 200L
)

@Composable
fun ArrowCanvas(
    level: PuzzleLevel,
    remainingArrows: List<Arrow>,
    flyingArrows: List<FlyingArrowAnimation>,
    blockedAnimations: Map<Int, BlockedArrowAnimation>,
    hintArrowId: Int?,
    boardTheme: BoardTheme,
    arrowColor: Color,
    arrowThickness: ArrowThickness,
    onArrowTapped: (Arrow) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { arrowThickness.strokeDp.dp.toPx() }

    var zoomScale by remember(level.id) { mutableFloatStateOf(1.0f) }
    var panOffset by remember(level.id) { mutableStateOf(Offset.Zero) }

    // Pulsing animation for hint using infinite transition
    val infiniteTransition = rememberInfiniteTransition(label = "hintTransition")
    val hintPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintPulse"
    )

    // Subtle tap ripple feedback
    var tapRippleOffset by remember { mutableStateOf<Offset?>(null) }
    val tapRippleProgress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Capture latest state safely without restarting pointer input
    val currentRemainingArrows by rememberUpdatedState(remainingArrows)
    val currentOnArrowTapped by rememberUpdatedState(onArrowTapped)
    val currentBounds by rememberUpdatedState(level.bounds)

    // Animation frame driver for smooth 60/120fps flying & nudge rendering
    var animationFrameTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(flyingArrows.isNotEmpty() || blockedAnimations.isNotEmpty()) {
        if (flyingArrows.isNotEmpty() || blockedAnimations.isNotEmpty()) {
            while (true) {
                withFrameNanos { frameNanos ->
                    animationFrameTime = frameNanos
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("puzzle_canvas")
            .pointerInput(level.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.7f, 3.0f)
                    panOffset += pan
                }
            }
            .pointerInput(level.id) {
                detectTapGestures { tapOffset ->
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val gridTransform = calculateGridTransform(
                        canvasWidth = canvasWidth.toFloat(),
                        canvasHeight = canvasHeight.toFloat(),
                        bounds = currentBounds,
                        zoom = zoomScale,
                        pan = panOffset
                    )

                    if (gridTransform.cellSize > 0.001f) {
                        // Convert screen tap to grid coordinates
                        val gridX = (tapOffset.x - gridTransform.originX) / gridTransform.cellSize
                        val gridY = (tapOffset.y - gridTransform.originY) / gridTransform.cellSize

                        val tapped = PuzzleGeometry.findTappedArrow(
                            tapX = gridX,
                            tapY = gridY,
                            arrows = currentRemainingArrows,
                            hitRadius = 0.75f
                        )
                        if (tapped != null) {
                            coroutineScope.launch {
                                try {
                                    tapRippleOffset = tapOffset
                                    tapRippleProgress.snapTo(0f)
                                    tapRippleProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                    )
                                } catch (_: Exception) {
                                } finally {
                                    tapRippleOffset = null
                                }
                            }
                            currentOnArrowTapped(tapped)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val frameTime = animationFrameTime
            val currentTime = System.currentTimeMillis()

            val bounds = currentBounds
            val gridTransform = calculateGridTransform(
                canvasWidth = size.width,
                canvasHeight = size.height,
                bounds = bounds,
                zoom = zoomScale,
                pan = panOffset
            )

            // Draw remaining arrows
            for (arrow in remainingArrows) {
                val isHint = (arrow.id == hintArrowId)
                val blocked = blockedAnimations[arrow.id]
                val nudge = if (blocked != null) {
                    val elapsed = (currentTime - blocked.startTimeMs).coerceAtLeast(0L)
                    val totalMs = blocked.durationMs.coerceAtLeast(1L)
                    val halfMs = totalMs / 2f
                    if (elapsed < halfMs) {
                        val forwardP = FastOutSlowInEasing.transform((elapsed / halfMs).coerceIn(0f, 1f))
                        blocked.maxNudge * forwardP
                    } else {
                        val recoilP = ((elapsed - halfMs) / halfMs).coerceIn(0f, 1f)
                        val decayed = (1f - recoilP) * cos(recoilP * PI.toFloat() * 1.5f)
                        (blocked.maxNudge * decayed).coerceAtLeast(0f)
                    }
                } else 0f

                drawSingleArrow(
                    arrow = arrow,
                    gridTransform = gridTransform,
                    strokeWidthPx = strokeWidthPx,
                    baseColor = arrowColor,
                    isHint = isHint,
                    hintPulse = if (isHint) hintPulse else 0f,
                    nudgeDisplacement = nudge
                )
            }

            // Draw flying arrows (exiting animation cleanly beyond viewport)
            val maxSpan = max(bounds.width, bounds.height) + 12f
            for (flying in flyingArrows) {
                val elapsed = (currentTime - flying.startTimeMs).coerceAtLeast(0L)
                val rawP = if (flying.durationMs > 0) (elapsed.toFloat() / flying.durationMs).coerceIn(0f, 1f) else 1f
                val p = FastOutSlowInEasing.transform(rawP)
                // Accelerate outward beyond the visible canvas area
                val flightDistance = (maxSpan * 1.5f) * (p * p + 0.3f * p)
                // Maintain crisp solid visibility throughout majority of flight, fading only right as it exits perimeter
                val alpha = when {
                    p < 0.75f -> 1.0f
                    else -> (1.0f - ((p - 0.75f) / 0.25f)).coerceIn(0f, 1f)
                }

                drawSingleArrow(
                    arrow = flying.arrow,
                    gridTransform = gridTransform,
                    strokeWidthPx = strokeWidthPx,
                    baseColor = arrowColor.copy(alpha = alpha),
                    isHint = false,
                    hintPulse = 0f,
                    nudgeDisplacement = flightDistance
                )
            }

            // Draw subtle, elegant tap ripple indicator (no excessive clutter)
            val tapPos = tapRippleOffset
            if (tapPos != null && tapRippleProgress.value > 0f) {
                val progress = tapRippleProgress.value
                val rippleRadius = (20.dp.toPx()) * (0.4f + 0.6f * progress)
                val rippleAlpha = (1f - progress).coerceIn(0f, 0.45f)
                drawCircle(
                    color = Color(boardTheme.accentColor).copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = tapPos,
                    style = Stroke(width = 2.dp.toPx() * (1f - progress * 0.5f))
                )
            }
        }
    }
}

private data class GridTransform(
    val cellSize: Float,
    val originX: Float,
    val originY: Float
)

private fun calculateGridTransform(
    canvasWidth: Float,
    canvasHeight: Float,
    bounds: RectF,
    zoom: Float,
    pan: Offset
): GridTransform {
    val padding = 48f
    val availW = (canvasWidth - padding * 2).coerceAtLeast(100f)
    val availH = (canvasHeight - padding * 2).coerceAtLeast(100f)

    val gridW = max(1f, bounds.width + 1.5f)
    val gridH = max(1f, bounds.height + 1.5f)

    val scale = min(availW / gridW, availH / gridH) * zoom

    val contentW = gridW * scale
    val contentH = gridH * scale

    val centerX = canvasWidth / 2f + pan.x
    val centerY = canvasHeight / 2f + pan.y

    val originX = centerX - (bounds.centerX * scale)
    val originY = centerY - (bounds.centerY * scale)

    return GridTransform(cellSize = scale, originX = originX, originY = originY)
}

private fun DrawScope.drawSingleArrow(
    arrow: Arrow,
    gridTransform: GridTransform,
    strokeWidthPx: Float,
    baseColor: Color,
    isHint: Boolean,
    hintPulse: Float,
    nudgeDisplacement: Float
) {
    if (arrow.points.isEmpty()) return

    val dir = arrow.facing
    val nudgeX = dir.dx * nudgeDisplacement * gridTransform.cellSize
    val nudgeY = dir.dy * nudgeDisplacement * gridTransform.cellSize

    // Transform points to screen pixels
    val screenPoints = arrow.points.map { pt ->
        Offset(
            x = gridTransform.originX + pt.x * gridTransform.cellSize + nudgeX,
            y = gridTransform.originY + pt.y * gridTransform.cellSize + nudgeY
        )
    }

    val finalColor = if (isHint) {
        val pulseIntensity = (sin(hintPulse * PI.toFloat() * 4f) * 0.5f + 0.5f)
        Color(0xFFFFB703).copy(alpha = 0.7f + 0.3f * pulseIntensity)
    } else {
        baseColor
    }

    // If hint is active, draw a subtle glowing backdrop halo
    if (isHint) {
        val haloPath = Path().apply {
            moveTo(screenPoints.first().x, screenPoints.first().y)
            for (i in 1 until screenPoints.size) {
                lineTo(screenPoints[i].x, screenPoints[i].y)
            }
        }
        drawPath(
            path = haloPath,
            color = Color(0xFFFFD166).copy(alpha = 0.45f),
            style = Stroke(
                width = strokeWidthPx * 3.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // 1. Draw arrow body line / polyline
    if (screenPoints.size >= 2) {
        val bodyPath = Path().apply {
            moveTo(screenPoints.first().x, screenPoints.first().y)
            for (i in 1 until screenPoints.size) {
                lineTo(screenPoints[i].x, screenPoints[i].y)
            }
        }

        drawPath(
            path = bodyPath,
            color = finalColor,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // 2. Draw sharp, crisp, minimalist triangular arrowhead at the tip
    val tip = screenPoints.last()
    val headLength = strokeWidthPx * 3.2f
    val headWidthHalf = strokeWidthPx * 1.8f

    if (isHint) {
        rotate(degrees = dir.angleDegrees, pivot = tip) {
            val headHaloPath = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - headLength * 1.15f, tip.y - headWidthHalf * 1.35f)
                lineTo(tip.x - headLength * 0.85f, tip.y)
                lineTo(tip.x - headLength * 1.15f, tip.y + headWidthHalf * 1.35f)
                close()
            }
            drawPath(
                path = headHaloPath,
                color = Color(0xFFFFD166).copy(alpha = 0.5f)
            )
        }
    }

    rotate(degrees = dir.angleDegrees, pivot = tip) {
        val headPath = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(tip.x - headLength, tip.y - headWidthHalf)
            lineTo(tip.x - headLength * 0.75f, tip.y) // slight chevron indent for modern aerodynamic profile
            lineTo(tip.x - headLength, tip.y + headWidthHalf)
            close()
        }

        drawPath(
            path = headPath,
            color = finalColor
        )
    }
}
