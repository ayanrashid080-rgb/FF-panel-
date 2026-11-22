package com.example.model

enum class AuthStage {
    LOCKED,
    VERIFYING,
    UNLOCKED
}

enum class OverlayTab {
    ESP,
    AIMBOT,
    BOOSTER,
    ADMIN
}

enum class TargetBone(val label: String) {
    HEAD("HEAD"),
    CHEST("CHEST"),
    PELVIS("PELVIS")
}

enum class CrosshairStyle(val label: String) {
    CYBER_CROSS("CYBER"),
    TACTICAL_DOT("DOT"),
    DYNAMIC_BRACKET("BRACKET")
}

enum class InjectionState {
    IDLE,
    RUNNING,
    INJECTED
}

enum class LogLevel {
    INFO,
    PYTHON,
    HOOK,
    PATCH,
    SUCCESS,
    WARN
}

sealed interface SessionsUiState {
    data object Loading : SessionsUiState
    data class Success(val sessions: List<LoginSession>) : SessionsUiState
    data class Error(val message: String) : SessionsUiState
}

data class TerminalLine(
    val id: Long,
    val timestamp: String,
    val tag: String,
    val message: String,
    val level: LogLevel
)

data class EspConfig(
    val enabled: Boolean = true,
    val showBoxes: Boolean = true,
    val showHealthBar: Boolean = true,
    val showSkeleton: Boolean = true,
    val showSnaplines: Boolean = true,
    val showDistanceInfo: Boolean = true
)

data class AimbotConfig(
    val enabled: Boolean = true,
    val showFovCircle: Boolean = true,
    val fovRadiusDp: Float = 96f,
    val smoothness: Float = 4.5f,
    val targetBone: TargetBone = TargetBone.HEAD
)

data class CrosshairConfig(
    val enabled: Boolean = true,
    val style: CrosshairStyle = CrosshairStyle.CYBER_CROSS,
    val sizeDp: Float = 18f
)

data class BoosterConfig(
    val fps120Enabled: Boolean = false,
    val antiLagEnabled: Boolean = false,
    val showTelemetryHud: Boolean = true,
    val currentFps: Int = 60,
    val currentPingMs: Int = 48,
    val frameTimeMs: Float = 16.6f,
    val memoryFreedMb: Int = 128
)

data class OverlayUiState(
    val authStage: AuthStage = AuthStage.LOCKED,
    val keyInput: String = "",
    val keyErrorResId: Int? = null,
    val hardwareId: String = "ARM64-NX-9F42-88A1",
    val currentUserEmail: String = "",
    val isAdminUser: Boolean = false,
    val showIpConsentDialog: Boolean = false,
    val ipConsentGranted: Boolean? = null,
    val lastSharedIp: String? = null,
    val consentNoticeMessage: String? = null,
    val showGateAdminSheet: Boolean = false,
    val isMenuExpanded: Boolean = true,
    val selectedTab: OverlayTab = OverlayTab.ESP,
    val esp: EspConfig = EspConfig(),
    val aimbot: AimbotConfig = AimbotConfig(),
    val crosshair: CrosshairConfig = CrosshairConfig(),
    val booster: BoosterConfig = BoosterConfig(),
    val injectionState: InjectionState = InjectionState.IDLE,
    val isBypassVerified: Boolean = false,
    val terminalLogs: List<TerminalLine> = emptyList()
)
