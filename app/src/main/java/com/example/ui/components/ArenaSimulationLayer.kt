package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AimbotConfig
import com.example.model.CrosshairStyle
import com.example.model.OverlayUiState
import com.example.model.TargetBone
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CardBorderMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class SimulatedTarget(
    val id: String,
    val centerX: Float,
    val centerY: Float,
    val boxWidth: Float,
    val boxHeight: Float,
    val hpFraction: Float,
    val distanceMeters: Int
)

@Composable
fun ArenaSimulationLayer(
    uiState: OverlayUiState,
    showHudAndHints: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_arena_backdrop),
            contentDescription = stringResource(id = R.string.cd_arena_backdrop),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle atmospheric vignette so neon HUD vectors pop cleanly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x3306080D),
                            Color(0x9906080D),
                            Color(0xCC04060A)
                        )
                    )
                )
        )

        if (showHudAndHints) {
            ArenaHudCanvas(uiState = uiState)

            // 5. Top Live FPS & Ping Telemetry HUD
            AnimatedVisibility(
                visible = uiState.booster.showTelemetryHud,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 10.dp, start = 12.dp, end = 12.dp)
            ) {
                TopTelemetryHud(uiState = uiState)
            }

            // Subtle collapsed-state control hint
            AnimatedVisibility(
                visible = !uiState.isMenuExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 18.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    color = GlassSurface,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ElectricCyan.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.isBypassVerified) MatrixGreen else ElectricCyan
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HUD LIVE • TAP FLOATING ICON TO OPEN MENU",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArenaHudCanvas(uiState: OverlayUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "arena_sim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "enemy_patrol_phase"
    )
    val fovPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fov_ring_rotation"
    )
    val textMeasurer = rememberTextMeasurer(cacheSize = 16)

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val fovRadiusPx = uiState.aimbot.fovRadiusDp.dp.toPx().coerceAtLeast(1f)

        val primaryX = center.x + sin(phase) * (size.width * 0.24f)
        val primaryY = center.y * 0.86f + cos(phase * 1.4f) * (size.height * 0.045f)
        val primaryDist = (26 + (sin(phase * 0.7f) * 6f)).toInt()
        val primaryHp = 0.78f + sin(phase * 0.5f) * 0.14f

        val secondaryX = center.x - cos(phase * 0.8f) * (size.width * 0.28f)
        val secondaryY = center.y * 0.72f + sin(phase * 1.1f) * (size.height * 0.03f)
        val secondaryDist = (41 + (cos(phase) * 5f)).toInt()
        val secondaryHp = 0.46f + cos(phase * 0.9f) * 0.12f

        val targets = listOf(
            SimulatedTarget(
                id = "VIPER_01",
                centerX = primaryX,
                centerY = primaryY,
                boxWidth = 76.dp.toPx(),
                boxHeight = 148.dp.toPx(),
                hpFraction = primaryHp.coerceIn(0.15f, 1f),
                distanceMeters = primaryDist
            ),
            SimulatedTarget(
                id = "GHOST_02",
                centerX = secondaryX,
                centerY = secondaryY,
                boxWidth = 52.dp.toPx(),
                boxHeight = 104.dp.toPx(),
                hpFraction = secondaryHp.coerceIn(0.15f, 1f),
                distanceMeters = secondaryDist
            )
        )

        targets.forEach { target ->
            drawTargetSilhouette(target)
        }

        if (uiState.esp.enabled) {
            targets.forEach { target ->
                val boneAnchor = targetBoneOffset(target, uiState.aimbot.targetBone)
                val isInFov = uiState.aimbot.enabled &&
                    hypot(boneAnchor.x - center.x, boneAnchor.y - center.y) <= fovRadiusPx

                val boxColor = if (isInFov) NeonCrimson else ElectricCyan

                if (uiState.esp.showSnaplines) {
                    drawLine(
                        color = boxColor.copy(alpha = 0.45f),
                        start = Offset(center.x, size.height * 0.11f),
                        end = Offset(target.centerX, target.centerY - target.boxHeight / 2f),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                }

                if (uiState.esp.showSkeleton) {
                    drawSkeletonMesh(target, boxColor.copy(alpha = 0.85f))
                }

                if (uiState.esp.showBoxes) {
                    drawCornerEspBox(
                        center = Offset(target.centerX, target.centerY),
                        boxWidth = target.boxWidth,
                        boxHeight = target.boxHeight,
                        color = boxColor
                    )
                }

                if (uiState.esp.showHealthBar) {
                    drawHealthBar(target)
                }

                if (uiState.esp.showDistanceInfo) {
                    drawPlayerTag(
                        target = target,
                        isLocked = isInFov,
                        textMeasurer = textMeasurer
                    )
                }

                if (isInFov) {
                    drawCircle(
                        color = NeonCrimson.copy(alpha = 0.85f),
                        radius = 5.dp.toPx(),
                        center = boneAnchor,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                    drawLine(
                        color = NeonCrimson.copy(alpha = 0.5f),
                        start = center,
                        end = boneAnchor,
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        if (uiState.aimbot.enabled && uiState.aimbot.showFovCircle) {
            drawAimbotFovCircle(
                center = center,
                radius = fovRadiusPx,
                rotationDeg = fovPulse,
                aimbot = uiState.aimbot
            )
        }

        if (uiState.crosshair.enabled) {
            drawProCrosshair(
                center = center,
                style = uiState.crosshair.style,
                armSizePx = uiState.crosshair.sizeDp.dp.toPx()
            )
        }
    }
}

@Composable
private fun TopTelemetryHud(uiState: OverlayUiState) {
    val booster = uiState.booster
    val fpsColor = if (booster.fps120Enabled) MatrixGreen else ElectricCyan
    val pingColor = if (booster.antiLagEnabled) MatrixGreen else AmberWarn
    val bypassColor = if (uiState.isBypassVerified) MatrixGreen else NeonCrimson

    Surface(
        color = GlassSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(ElectricCyan.copy(alpha = 0.55f), NeonCrimson.copy(alpha = 0.55f))
            )
        ),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = fpsColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Column {
                    Text(
                        text = "${booster.currentFps} FPS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = fpsColor
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f ms", booster.frameTimeMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.NetworkPing,
                    contentDescription = null,
                    tint = pingColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Column {
                    Text(
                        text = "${booster.currentPingMs} ms",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = pingColor
                    )
                    Text(
                        text = if (booster.antiLagEnabled) "OPT-NET" else "STD-NET",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bypassColor.copy(alpha = 0.14f))
                    .border(0.8.dp, bypassColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = bypassColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (uiState.isBypassVerified) "BYPASS OK" else "STANDBY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = bypassColor
                )
            }
        }
    }
}

private fun DrawScope.drawTargetSilhouette(target: SimulatedTarget) {
    val left = target.centerX - target.boxWidth / 2f
    val top = target.centerY - target.boxHeight / 2f
    val headRadius = target.boxWidth * 0.16f
    val headCenter = Offset(target.centerX, top + headRadius * 1.4f)

    // Subtle dark tactical operator figure
    val bodyColor = Color(0xCC121A26)
    val rimColor = Color(0x66496285)

    // Head
    drawCircle(color = bodyColor, radius = headRadius, center = headCenter)
    drawCircle(
        color = rimColor,
        radius = headRadius,
        center = headCenter,
        style = Stroke(width = 1.dp.toPx())
    )

    // Torso
    val torsoTop = headCenter.y + headRadius * 1.05f
    val torsoWidth = target.boxWidth * 0.56f
    val torsoHeight = target.boxHeight * 0.36f
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(target.centerX - torsoWidth / 2f, torsoTop),
        size = Size(torsoWidth, torsoHeight),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )

    // Legs
    val legTop = torsoTop + torsoHeight - 2.dp.toPx()
    val legHeight = target.boxHeight * 0.36f
    val legWidth = torsoWidth * 0.36f
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(target.centerX - torsoWidth * 0.42f, legTop),
        size = Size(legWidth, legHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(target.centerX + torsoWidth * 0.06f, legTop),
        size = Size(legWidth, legHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
}

private fun DrawScope.drawCornerEspBox(
    center: Offset,
    boxWidth: Float,
    boxHeight: Float,
    color: Color
) {
    val left = center.x - boxWidth / 2f
    val right = center.x + boxWidth / 2f
    val top = center.y - boxHeight / 2f
    val bottom = center.y + boxHeight / 2f
    val cornerLen = boxWidth * 0.28f
    val stroke = 2.dp.toPx()

    // Inner fill glow
    drawRect(
        color = color.copy(alpha = 0.07f),
        topLeft = Offset(left, top),
        size = Size(boxWidth, boxHeight)
    )

    // Top-left
    drawLine(color, Offset(left, top), Offset(left + cornerLen, top), stroke, StrokeCap.Square)
    drawLine(color, Offset(left, top), Offset(left, top + cornerLen), stroke, StrokeCap.Square)
    // Top-right
    drawLine(color, Offset(right, top), Offset(right - cornerLen, top), stroke, StrokeCap.Square)
    drawLine(color, Offset(right, top), Offset(right, top + cornerLen), stroke, StrokeCap.Square)
    // Bottom-left
    drawLine(color, Offset(left, bottom), Offset(left + cornerLen, bottom), stroke, StrokeCap.Square)
    drawLine(color, Offset(left, bottom), Offset(left, bottom - cornerLen), stroke, StrokeCap.Square)
    // Bottom-right
    drawLine(color, Offset(right, bottom), Offset(right - cornerLen, bottom), stroke, StrokeCap.Square)
    drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerLen), stroke, StrokeCap.Square)
}

private fun DrawScope.drawSkeletonMesh(target: SimulatedTarget, color: Color) {
    val top = target.centerY - target.boxHeight / 2f
    val head = Offset(target.centerX, top + target.boxHeight * 0.14f)
    val neck = Offset(target.centerX, top + target.boxHeight * 0.24f)
    val pelvis = Offset(target.centerX, top + target.boxHeight * 0.58f)
    val leftShoulder = Offset(target.centerX - target.boxWidth * 0.26f, neck.y + 4.dp.toPx())
    val rightShoulder = Offset(target.centerX + target.boxWidth * 0.26f, neck.y + 4.dp.toPx())
    val leftHand = Offset(target.centerX - target.boxWidth * 0.34f, pelvis.y - 4.dp.toPx())
    val rightHand = Offset(target.centerX + target.boxWidth * 0.34f, pelvis.y - 4.dp.toPx())
    val leftFoot = Offset(target.centerX - target.boxWidth * 0.22f, top + target.boxHeight * 0.94f)
    val rightFoot = Offset(target.centerX + target.boxWidth * 0.22f, top + target.boxHeight * 0.94f)
    val w = 1.4.dp.toPx()

    drawLine(color, head, neck, w)
    drawLine(color, neck, pelvis, w)
    drawLine(color, leftShoulder, rightShoulder, w)
    drawLine(color, leftShoulder, leftHand, w)
    drawLine(color, rightShoulder, rightHand, w)
    drawLine(color, pelvis, leftFoot, w)
    drawLine(color, pelvis, rightFoot, w)
}

private fun DrawScope.drawHealthBar(target: SimulatedTarget) {
    val left = target.centerX - target.boxWidth / 2f - 8.dp.toPx()
    val top = target.centerY - target.boxHeight / 2f
    val barWidth = 4.dp.toPx()
    val barHeight = target.boxHeight

    drawRoundRect(
        color = ObsidianVoid.copy(alpha = 0.85f),
        topLeft = Offset(left, top),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(2.dp.toPx())
    )

    val fillHeight = barHeight * target.hpFraction
    val hpColor = when {
        target.hpFraction > 0.6f -> MatrixGreen
        target.hpFraction > 0.35f -> AmberWarn
        else -> NeonCrimson
    }
    drawRoundRect(
        color = hpColor,
        topLeft = Offset(left, top + (barHeight - fillHeight)),
        size = Size(barWidth, fillHeight),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
}

private fun DrawScope.drawPlayerTag(
    target: SimulatedTarget,
    isLocked: Boolean,
    textMeasurer: TextMeasurer
) {
    val hpInt = (target.hpFraction * 100).toInt()
    val label = "${target.id} [${target.distanceMeters}m] $hpInt HP"
    val accent = if (isLocked) NeonCrimson else ElectricCyan
    val style = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        color = TextPrimary
    )
    val measured = textMeasurer.measure(label, style)
    val pillW = measured.size.width + 12.dp.toPx()
    val pillH = measured.size.height + 6.dp.toPx()
    val pillLeft = target.centerX - pillW / 2f
    val pillTop = target.centerY - target.boxHeight / 2f - pillH - 6.dp.toPx()

    drawRoundRect(
        color = ObsidianVoid.copy(alpha = 0.82f),
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.75f),
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(pillLeft + 6.dp.toPx(), pillTop + 3.dp.toPx())
    )
}

private fun targetBoneOffset(target: SimulatedTarget, bone: TargetBone): Offset {
    val top = target.centerY - target.boxHeight / 2f
    return when (bone) {
        TargetBone.HEAD -> Offset(target.centerX, top + target.boxHeight * 0.14f)
        TargetBone.CHEST -> Offset(target.centerX, top + target.boxHeight * 0.36f)
        TargetBone.PELVIS -> Offset(target.centerX, top + target.boxHeight * 0.56f)
    }
}

private fun DrawScope.drawAimbotFovCircle(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    aimbot: AimbotConfig
) {
    // Outer soft glow
    drawCircle(
        color = ElectricCyan.copy(alpha = 0.12f),
        radius = radius,
        center = center,
        style = Stroke(width = 6.dp.toPx())
    )
    // Primary neon ring
    drawCircle(
        color = ElectricCyan.copy(alpha = 0.72f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.4.dp.toPx())
    )
    // Rotating crimson calibration arcs
    drawArc(
        color = NeonCrimson.copy(alpha = 0.85f),
        startAngle = rotationDeg,
        sweepAngle = 42f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
    )
    drawArc(
        color = NeonCrimson.copy(alpha = 0.85f),
        startAngle = rotationDeg + 180f,
        sweepAngle = 42f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawProCrosshair(
    center: Offset,
    style: CrosshairStyle,
    armSizePx: Float
) {
    val gap = 5.dp.toPx()
    val stroke = 2.dp.toPx()
    when (style) {
        CrosshairStyle.CYBER_CROSS -> {
            drawLine(ElectricCyan, Offset(center.x - gap - armSizePx, center.y), Offset(center.x - gap, center.y), stroke, StrokeCap.Round)
            drawLine(ElectricCyan, Offset(center.x + gap, center.y), Offset(center.x + gap + armSizePx, center.y), stroke, StrokeCap.Round)
            drawLine(ElectricCyan, Offset(center.x, center.y - gap - armSizePx), Offset(center.x, center.y - gap), stroke, StrokeCap.Round)
            drawLine(ElectricCyan, Offset(center.x, center.y + gap), Offset(center.x, center.y + gap + armSizePx), stroke, StrokeCap.Round)
            drawCircle(NeonCrimson, radius = 2.2.dp.toPx(), center = center)
        }
        CrosshairStyle.TACTICAL_DOT -> {
            drawCircle(ElectricCyan.copy(alpha = 0.3f), radius = armSizePx * 0.45f, center = center, style = Stroke(width = 1.2.dp.toPx()))
            drawCircle(NeonCrimson, radius = 3.dp.toPx(), center = center)
        }
        CrosshairStyle.DYNAMIC_BRACKET -> {
            val r = armSizePx * 0.75f
            val corner = armSizePx * 0.35f
            val left = center.x - r
            val right = center.x + r
            val top = center.y - r
            val bottom = center.y + r
            drawLine(ElectricCyan, Offset(left, top), Offset(left + corner, top), stroke)
            drawLine(ElectricCyan, Offset(left, top), Offset(left, top + corner), stroke)
            drawLine(ElectricCyan, Offset(right, top), Offset(right - corner, top), stroke)
            drawLine(ElectricCyan, Offset(right, top), Offset(right, top + corner), stroke)
            drawLine(ElectricCyan, Offset(left, bottom), Offset(left + corner, bottom), stroke)
            drawLine(ElectricCyan, Offset(left, bottom), Offset(left, bottom - corner), stroke)
            drawLine(ElectricCyan, Offset(right, bottom), Offset(right - corner, bottom), stroke)
            drawLine(ElectricCyan, Offset(right, bottom), Offset(right, bottom - corner), stroke)
            drawCircle(NeonCrimson, radius = 2.dp.toPx(), center = center)
        }
    }
}
