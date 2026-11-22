package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun DraggableFloatingBubbleOverlay(
    isMenuExpanded: Boolean,
    isBypassVerified: Boolean,
    onToggleMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val density = LocalDensity.current
        val touchSlop = LocalViewConfiguration.current.touchSlop
        val bubbleSize = 54.dp
        val bubbleSizePx = with(density) { bubbleSize.toPx() }
        val edgeMarginPx = with(density) { 10.dp.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        val minX = edgeMarginPx
        val maxX = (maxWidthPx - bubbleSizePx - edgeMarginPx).coerceAtLeast(minX)
        val minY = with(density) { 58.dp.toPx() }.coerceAtMost((maxHeightPx - bubbleSizePx).coerceAtLeast(0f))
        val maxY = (maxHeightPx - bubbleSizePx - edgeMarginPx).coerceAtLeast(minY)
        val defaultY = (maxHeightPx * 0.42f).coerceIn(minY, maxY)

        var initialized by remember { mutableStateOf(false) }
        var offsetX by remember { mutableFloatStateOf(maxX) }
        var offsetY by remember { mutableFloatStateOf(defaultY) }

        LaunchedEffect(maxWidthPx, maxHeightPx) {
            if (!initialized && maxWidthPx > 0f && maxHeightPx > 0f) {
                offsetX = maxX
                offsetY = defaultY
                initialized = true
            } else {
                offsetX = offsetX.coerceIn(minX, maxX)
                offsetY = offsetY.coerceIn(minY, maxY)
            }
        }

        val currentOnToggle by rememberUpdatedState(onToggleMenu)
        val bubbleDescription = stringResource(id = R.string.cd_floating_bubble)

        val infiniteTransition = rememberInfiniteTransition(label = "bubble_ring")
        val sweepRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "bubble_spin"
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(bubbleSize)
                .shadow(elevation = 14.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(GlassSurface, ObsidianVoid)
                    )
                )
                .semantics {
                    contentDescription = bubbleDescription
                    role = Role.Button
                    onClick {
                        currentOnToggle()
                        true
                    }
                }
                .pointerInput(minX, maxX, minY, maxY, touchSlop) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalMoved = 0f
                        var isDragging = false
                        var completedCleanly = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                completedCleanly = true
                                break
                            }
                            val delta = change.positionChange()
                            totalMoved += hypot(delta.x, delta.y)
                            if (totalMoved > touchSlop) {
                                isDragging = true
                            }
                            if (isDragging && (delta.x != 0f || delta.y != 0f)) {
                                change.consume()
                                offsetX = (offsetX + delta.x).coerceIn(minX, maxX)
                                offsetY = (offsetY + delta.y).coerceIn(minY, maxY)
                            }
                        }

                        if (completedCleanly && !isDragging) {
                            currentOnToggle()
                        }
                    }
                }
                .testTag("floating_bubble_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 2.4.dp.toPx()
                drawArc(
                    color = ElectricCyan,
                    startAngle = sweepRotation,
                    sweepAngle = 125f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = NeonCrimson,
                    startAngle = sweepRotation + 180f,
                    sweepAngle = 125f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Icon(
                imageVector = if (isMenuExpanded) Icons.Filled.Close else Icons.Filled.MyLocation,
                contentDescription = null,
                tint = if (isMenuExpanded) NeonCrimson else ElectricCyan,
                modifier = Modifier.size(24.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isBypassVerified) MatrixGreen else TextPrimary)
                    .border(1.2.dp, ObsidianVoid, CircleShape)
            )
        }
    }
}
