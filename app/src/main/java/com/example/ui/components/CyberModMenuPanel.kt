package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.R
import com.example.model.CrosshairStyle
import com.example.model.InjectionState
import com.example.model.LogLevel
import com.example.model.OverlayTab
import com.example.model.OverlayUiState
import com.example.model.SessionsUiState
import com.example.model.TargetBone
import com.example.model.TerminalLine
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CardBorderMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CyberModMenuPanel(
    uiState: OverlayUiState,
    sessionsState: SessionsUiState,
    onCollapseMenu: () -> Unit,
    onLockSession: () -> Unit,
    onSelectTab: (OverlayTab) -> Unit,
    onToggleEspMaster: (Boolean) -> Unit,
    onToggleEspBoxes: (Boolean) -> Unit,
    onToggleEspHp: (Boolean) -> Unit,
    onToggleEspSkeleton: (Boolean) -> Unit,
    onToggleEspSnaplines: (Boolean) -> Unit,
    onToggleAimbotMaster: (Boolean) -> Unit,
    onToggleFovCircle: (Boolean) -> Unit,
    onFovRadiusChange: (Float) -> Unit,
    onSmoothnessChange: (Float) -> Unit,
    onTargetBoneSelected: (TargetBone) -> Unit,
    onToggleCrosshair: (Boolean) -> Unit,
    onCrosshairStyleSelected: (CrosshairStyle) -> Unit,
    onToggle120Fps: (Boolean) -> Unit,
    onToggleAntiLag: (Boolean) -> Unit,
    onToggleTelemetryHud: (Boolean) -> Unit,
    onDeleteSession: (String) -> Unit,
    onInjectPythonBypass: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = if (uiState.booster.showTelemetryHud) 52.dp else 10.dp,
                bottom = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        val isCompactHeight = maxHeight < 480.dp
        val isUltraCompact = maxHeight < 340.dp
        val panelMaxHeight = (maxHeight - 4.dp).coerceAtLeast(200.dp)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .heightIn(max = panelMaxHeight),
            color = GlassSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    listOf(NeonCrimson, ElectricCyan)
                )
            ),
            shadowElevation = 24.dp
        ) {
            val outerScrollModifier = if (isUltraCompact) {
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxWidth()
            }
            Column(modifier = outerScrollModifier) {
                // 1. ImGui-style Header Bar
                MenuHeaderBar(
                    isBypassVerified = uiState.isBypassVerified,
                    onCollapseMenu = onCollapseMenu,
                    onLockSession = onLockSession
                )

                // Consent status banner
                if (uiState.consentNoticeMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (uiState.ipConsentGranted == true) MatrixGreen.copy(alpha = 0.12f)
                                else AmberWarn.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.consentNoticeMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.ipConsentGranted == true) MatrixGreen else AmberWarn
                        )
                    }
                }

                HorizontalDivider(color = CardBorderMuted, thickness = 1.dp)

                // 2. Category Tabs (including ADMIN / SESSIONS)
                CyberTabBar(
                    selectedTab = uiState.selectedTab,
                    isAdminUser = uiState.isAdminUser,
                    onSelectTab = onSelectTab
                )

                HorizontalDivider(color = CardBorderMuted, thickness = 1.dp)

                // 3. Scrollable Feature Controls
                val controlsModifier = if (isUltraCompact) {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                } else {
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                }
                Column(
                    modifier = controlsModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (uiState.selectedTab) {
                        OverlayTab.ESP -> {
                            EspControlsSection(
                                uiState = uiState,
                                onToggleEspMaster = onToggleEspMaster,
                                onToggleEspBoxes = onToggleEspBoxes,
                                onToggleEspHp = onToggleEspHp,
                                onToggleEspSkeleton = onToggleEspSkeleton,
                                onToggleEspSnaplines = onToggleEspSnaplines
                            )
                        }
                        OverlayTab.AIMBOT -> {
                            AimbotControlsSection(
                                uiState = uiState,
                                onToggleAimbotMaster = onToggleAimbotMaster,
                                onToggleFovCircle = onToggleFovCircle,
                                onFovRadiusChange = onFovRadiusChange,
                                onSmoothnessChange = onSmoothnessChange,
                                onTargetBoneSelected = onTargetBoneSelected,
                                onToggleCrosshair = onToggleCrosshair,
                                onCrosshairStyleSelected = onCrosshairStyleSelected
                            )
                        }
                        OverlayTab.BOOSTER -> {
                            BoosterControlsSection(
                                uiState = uiState,
                                onToggle120Fps = onToggle120Fps,
                                onToggleAntiLag = onToggleAntiLag,
                                onToggleTelemetryHud = onToggleTelemetryHud
                            )
                        }
                        OverlayTab.ADMIN -> {
                            AdminPanelSection(
                                isAdminUser = uiState.isAdminUser,
                                sessionsState = sessionsState,
                                onDeleteSession = onDeleteSession
                            )
                        }
                    }
                }

                HorizontalDivider(color = CardBorderMuted, thickness = 1.dp)

                // 4. Persistent Bottom Python & Memory Injection Terminal Dock
                PythonTerminalDock(
                    logs = uiState.terminalLogs,
                    injectionState = uiState.injectionState,
                    isCompact = isCompactHeight,
                    onInjectPythonBypass = onInjectPythonBypass
                )
            }
        }
    }
}

@Composable
private fun MenuHeaderBar(
    isBypassVerified: Boolean,
    onCollapseMenu: () -> Unit,
    onLockSession: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(NeonCrimson.copy(alpha = 0.16f), ElectricCyan.copy(alpha = 0.12f))
                )
            )
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isBypassVerified) MatrixGreen else NeonCrimson)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.menu_brand_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonCrimson.copy(alpha = 0.24f))
                    .border(0.8.dp, NeonCrimson, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.menu_vip_badge),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeonCrimson
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onLockSession,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("lock_session_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = stringResource(id = R.string.cd_lock_session),
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onCollapseMenu,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("collapse_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Minimize,
                    contentDescription = stringResource(id = R.string.cd_collapse_menu),
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CyberTabBar(
    selectedTab: OverlayTab,
    isAdminUser: Boolean,
    onSelectTab: (OverlayTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianVoid.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CyberTabButton(
            label = stringResource(id = R.string.tab_esp),
            icon = Icons.Filled.RemoveRedEye,
            selected = selectedTab == OverlayTab.ESP,
            accent = ElectricCyan,
            onClick = { onSelectTab(OverlayTab.ESP) },
            modifier = Modifier
                .weight(1f)
                .testTag("tab_esp")
        )
        CyberTabButton(
            label = stringResource(id = R.string.tab_aimbot),
            icon = Icons.Filled.MyLocation,
            selected = selectedTab == OverlayTab.AIMBOT,
            accent = NeonCrimson,
            onClick = { onSelectTab(OverlayTab.AIMBOT) },
            modifier = Modifier
                .weight(1f)
                .testTag("tab_aimbot")
        )
        CyberTabButton(
            label = stringResource(id = R.string.tab_booster),
            icon = Icons.Filled.Bolt,
            selected = selectedTab == OverlayTab.BOOSTER,
            accent = MatrixGreen,
            onClick = { onSelectTab(OverlayTab.BOOSTER) },
            modifier = Modifier
                .weight(1f)
                .testTag("tab_booster")
        )
        CyberTabButton(
            label = stringResource(id = if (isAdminUser) R.string.tab_admin else R.string.tab_my_sessions),
            icon = Icons.Filled.AdminPanelSettings,
            selected = selectedTab == OverlayTab.ADMIN,
            accent = if (isAdminUser) NeonCrimson else ElectricCyan,
            onClick = { onSelectTab(OverlayTab.ADMIN) },
            modifier = Modifier
                .weight(1f)
                .testTag("tab_admin")
        )
    }
}

@Composable
private fun CyberTabButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.16f) else ElevatedPanel.copy(alpha = 0.65f),
        label = "tab_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else CardBorderMuted,
        label = "tab_border"
    )

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EspControlsSection(
    uiState: OverlayUiState,
    onToggleEspMaster: (Boolean) -> Unit,
    onToggleEspBoxes: (Boolean) -> Unit,
    onToggleEspHp: (Boolean) -> Unit,
    onToggleEspSkeleton: (Boolean) -> Unit,
    onToggleEspSnaplines: (Boolean) -> Unit
) {
    val esp = uiState.esp
    CyberToggleRow(
        label = stringResource(id = R.string.esp_master_label),
        subtitle = "Draws live moving enemy hitboxes & tags",
        checked = esp.enabled,
        accent = ElectricCyan,
        testTag = "esp_master_switch",
        onCheckedChange = onToggleEspMaster
    )
    CyberToggleRow(
        label = stringResource(id = R.string.esp_boxes_label),
        subtitle = "Dynamic corner brackets (Crimson on FOV lock)",
        checked = esp.showBoxes,
        enabled = esp.enabled,
        accent = ElectricCyan,
        testTag = "esp_boxes_switch",
        onCheckedChange = onToggleEspBoxes
    )
    CyberToggleRow(
        label = stringResource(id = R.string.esp_hp_label),
        subtitle = "Real-time vertical HP bar & distance meter",
        checked = esp.showHealthBar,
        enabled = esp.enabled,
        accent = MatrixGreen,
        testTag = "esp_hp_switch",
        onCheckedChange = onToggleEspHp
    )
    CyberToggleRow(
        label = stringResource(id = R.string.esp_skeleton_label),
        subtitle = "Biped joint bone overlay mesh",
        checked = esp.showSkeleton,
        enabled = esp.enabled,
        accent = ElectricCyan,
        testTag = "esp_skeleton_switch",
        onCheckedChange = onToggleEspSkeleton
    )
    CyberToggleRow(
        label = stringResource(id = R.string.esp_snapline_label),
        subtitle = "Top-HUD vector tracers to enemy targets",
        checked = esp.showSnaplines,
        enabled = esp.enabled,
        accent = NeonCrimson,
        testTag = "esp_snaplines_switch",
        onCheckedChange = onToggleEspSnaplines
    )
}

@Composable
private fun AimbotControlsSection(
    uiState: OverlayUiState,
    onToggleAimbotMaster: (Boolean) -> Unit,
    onToggleFovCircle: (Boolean) -> Unit,
    onFovRadiusChange: (Float) -> Unit,
    onSmoothnessChange: (Float) -> Unit,
    onTargetBoneSelected: (TargetBone) -> Unit,
    onToggleCrosshair: (Boolean) -> Unit,
    onCrosshairStyleSelected: (CrosshairStyle) -> Unit
) {
    val aimbot = uiState.aimbot
    val crosshair = uiState.crosshair

    CyberToggleRow(
        label = stringResource(id = R.string.aimbot_master_label),
        subtitle = "Target acquisition & lock preview",
        checked = aimbot.enabled,
        accent = NeonCrimson,
        testTag = "aimbot_master_switch",
        onCheckedChange = onToggleAimbotMaster
    )
    CyberToggleRow(
        label = stringResource(id = R.string.aimbot_fov_toggle),
        subtitle = "Live glowing neon center-screen ring",
        checked = aimbot.showFovCircle,
        enabled = aimbot.enabled,
        accent = ElectricCyan,
        testTag = "aimbot_fov_switch",
        onCheckedChange = onToggleFovCircle
    )

    CyberSliderCard(
        label = stringResource(id = R.string.aimbot_fov_radius),
        valueText = "${aimbot.fovRadiusDp.toInt()} px",
        value = aimbot.fovRadiusDp,
        valueRange = 42f..170f,
        accent = ElectricCyan,
        testTag = "fov_radius_slider",
        onValueChange = onFovRadiusChange
    )

    CyberSliderCard(
        label = stringResource(id = R.string.aimbot_smoothness),
        valueText = String.format(Locale.US, "%.1f", aimbot.smoothness),
        value = aimbot.smoothness,
        valueRange = 1f..10f,
        accent = NeonCrimson,
        testTag = "smoothness_slider",
        onValueChange = onSmoothnessChange
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedPanel.copy(alpha = 0.8f))
            .border(1.dp, CardBorderMuted, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.aimbot_bone_label),
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TargetBone.entries.forEach { bone ->
                val selected = aimbot.targetBone == bone
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) NeonCrimson.copy(alpha = 0.22f)
                            else ObsidianVoid.copy(alpha = 0.6f)
                        )
                        .border(
                            1.dp,
                            if (selected) NeonCrimson else CardBorderMuted,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onTargetBoneSelected(bone) }
                        .testTag("bone_${bone.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bone.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }

    CyberToggleRow(
        label = stringResource(id = R.string.crosshair_master_label),
        subtitle = "Sharp center-screen tactical reticle",
        checked = crosshair.enabled,
        accent = ElectricCyan,
        testTag = "crosshair_switch",
        onCheckedChange = onToggleCrosshair
    )

    if (crosshair.enabled) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrosshairStyle.entries.forEach { style ->
                val selected = crosshair.style == style
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) ElectricCyan.copy(alpha = 0.18f)
                            else ElevatedPanel
                        )
                        .border(
                            1.dp,
                            if (selected) ElectricCyan else CardBorderMuted,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onCrosshairStyleSelected(style) }
                        .testTag("crosshair_${style.name.lowercase()}"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusStrong,
                        contentDescription = null,
                        tint = if (selected) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun BoosterControlsSection(
    uiState: OverlayUiState,
    onToggle120Fps: (Boolean) -> Unit,
    onToggleAntiLag: (Boolean) -> Unit,
    onToggleTelemetryHud: (Boolean) -> Unit
) {
    val booster = uiState.booster
    CyberToggleRow(
        label = stringResource(id = R.string.booster_fps_label),
        subtitle = "Unlocks 120Hz frame pacing in telemetry HUD",
        checked = booster.fps120Enabled,
        accent = MatrixGreen,
        testTag = "fps_booster_switch",
        onCheckedChange = onToggle120Fps
    )
    CyberToggleRow(
        label = stringResource(id = R.string.booster_antilag_label),
        subtitle = "Prioritizes game UDP sockets & reduces jitter",
        checked = booster.antiLagEnabled,
        accent = ElectricCyan,
        testTag = "antilag_switch",
        onCheckedChange = onToggleAntiLag
    )
    CyberToggleRow(
        label = stringResource(id = R.string.booster_hud_label),
        subtitle = "Display live FPS & Ping pill at top of screen",
        checked = booster.showTelemetryHud,
        accent = ElectricCyan,
        testTag = "telemetry_hud_switch",
        onCheckedChange = onToggleTelemetryHud
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedPanel)
            .border(1.dp, CardBorderMuted, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Memory,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "LIVE ENGINE TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "${booster.currentFps} FPS • ${booster.currentPingMs} ms • ${booster.memoryFreedMb} MB freed",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MatrixGreen
                )
            }
        }
    }
}

@Composable
private fun CyberToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    testTag: String,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedPanel.copy(alpha = if (enabled) 0.85f else 0.45f))
            .border(
                1.dp,
                if (checked && enabled) accent.copy(alpha = 0.55f) else CardBorderMuted,
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) TextPrimary else TextMuted
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = if (enabled) TextSecondary else TextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = accent,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ObsidianVoid,
                uncheckedBorderColor = CardBorderMuted
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun CyberSliderCard(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: Color,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedPanel.copy(alpha = 0.85f))
            .border(1.dp, CardBorderMuted, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = ObsidianVoid
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PythonTerminalDock(
    logs: List<TerminalLine>,
    injectionState: InjectionState,
    isCompact: Boolean = false,
    onInjectPythonBypass: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianVoid.copy(alpha = 0.9f))
            .padding(if (isCompact) 10.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.terminal_header),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricCyan
                )
            }
            Text(
                text = when (injectionState) {
                    InjectionState.IDLE -> "core_hook.py [IDLE]"
                    InjectionState.RUNNING -> "EXECUTING..."
                    InjectionState.INJECTED -> "HOOKS VERIFIED"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (injectionState) {
                    InjectionState.IDLE -> TextMuted
                    InjectionState.RUNNING -> AmberWarn
                    InjectionState.INJECTED -> MatrixGreen
                }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 58.dp else 96.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF04060A))
                .border(1.dp, CardBorderMuted, RoundedCornerShape(8.dp))
                .padding(8.dp)
                .testTag("python_terminal_log"),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(items = logs, key = { it.id }) { line ->
                val tagColor = when (line.level) {
                    LogLevel.INFO -> TextSecondary
                    LogLevel.PYTHON -> ElectricCyan
                    LogLevel.HOOK -> Color(0xFF8B5CF6)
                    LogLevel.PATCH -> NeonCrimson
                    LogLevel.SUCCESS -> MatrixGreen
                    LogLevel.WARN -> AmberWarn
                }
                Text(
                    text = "[${line.timestamp}] [${line.tag}] ${line.message}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    ),
                    color = tagColor
                )
            }
        }

        val buttonColor by animateColorAsState(
            targetValue = when (injectionState) {
                InjectionState.IDLE -> NeonCrimson
                InjectionState.RUNNING -> ElevatedPanel
                InjectionState.INJECTED -> MatrixGreen
            },
            label = "inject_btn_color"
        )
        val buttonTextColor = if (injectionState == InjectionState.INJECTED) ObsidianVoid else TextPrimary

        Button(
            onClick = onInjectPythonBypass,
            enabled = injectionState != InjectionState.RUNNING,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = buttonTextColor,
                disabledContainerColor = ElevatedPanel,
                disabledContentColor = ElectricCyan
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("inject_python_button")
        ) {
            if (injectionState == InjectionState.RUNNING) {
                CircularProgressIndicator(
                    color = ElectricCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.action_injecting),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (injectionState == InjectionState.INJECTED) {
                        stringResource(id = R.string.action_injected)
                    } else {
                        stringResource(id = R.string.action_inject_python)
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp)
                )
            }
        }
    }
}
