package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.IpAddressResolver
import com.example.data.LoginSessionRepository
import com.example.data.NetworkIpAddressResolver
import com.example.model.AimbotConfig
import com.example.model.AuthStage
import com.example.model.BoosterConfig
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.model.EspConfig
import com.example.model.InjectionState
import com.example.model.LogLevel
import com.example.model.LoginSession
import com.example.model.OverlayTab
import com.example.model.OverlayUiState
import com.example.model.SessionsUiState
import com.example.model.TargetBone
import com.example.model.TerminalLine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class OverlayViewModel @JvmOverloads constructor(
    private val sessionRepository: LoginSessionRepository? = null,
    private val ipResolver: IpAddressResolver = NetworkIpAddressResolver(),
    private val currentUserId: String = "",
    currentUserEmail: String = "",
    isEmailVerified: Boolean = false,
    startTelemetry: Boolean = true
) : ViewModel() {

    private val isAdmin = isEmailVerified &&
        currentUserEmail.equals(ADMIN_EMAIL, ignoreCase = true)

    private val _uiState = MutableStateFlow(
        OverlayUiState(
            currentUserEmail = currentUserEmail,
            isAdminUser = isAdmin,
            terminalLogs = initialBootLogs()
        )
    )
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    val sessionsState: StateFlow<SessionsUiState> =
        if (sessionRepository != null && currentUserId.isNotBlank()) {
            sessionRepository.observeSessions(currentUserId, isAdmin)
                .map<List<LoginSession>, SessionsUiState> { SessionsUiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing login sessions", rootError)
                    emit(SessionsUiState.Error(rootError.message ?: "Failed to load login sessions"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = SessionsUiState.Loading
                )
        } else {
            MutableStateFlow<SessionsUiState>(SessionsUiState.Success(emptyList())).asStateFlow()
        }

    private var nextLogId = 100L
    private var injectionJob: Job? = null
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        if (startTelemetry) {
            startTelemetryLoop()
        }
    }

    private fun startTelemetryLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(800L)
                if (_uiState.value.authStage != AuthStage.UNLOCKED) continue
                _uiState.update { state ->
                    val booster = state.booster
                    val fps = if (booster.fps120Enabled) {
                        Random.nextInt(117, 121)
                    } else {
                        Random.nextInt(57, 62)
                    }
                    val ping = if (booster.antiLagEnabled) {
                        Random.nextInt(13, 21)
                    } else {
                        Random.nextInt(42, 66)
                    }
                    val frameTime = 1000f / fps.toFloat()
                    val memSaved = if (booster.antiLagEnabled && booster.fps120Enabled) {
                        486
                    } else if (booster.antiLagEnabled || booster.fps120Enabled) {
                        312
                    } else {
                        128
                    }
                    state.copy(
                        booster = booster.copy(
                            currentFps = fps,
                            currentPingMs = ping,
                            frameTimeMs = frameTime,
                            memoryFreedMb = memSaved
                        )
                    )
                }
            }
        }
    }

    fun onKeyInputChanged(newValue: String) {
        _uiState.update {
            it.copy(
                keyInput = newValue.uppercase(Locale.US),
                keyErrorResId = null
            )
        }
    }

    fun onGetKeyClicked() {
        val hexPart = (1000..9999).random()
        val tierCode = listOf("VIP9", "PRO7", "ELTE", "APEX").random()
        val generated = "NXS-$tierCode-ARM64-$hexPart"
        _uiState.update {
            it.copy(
                keyInput = generated,
                keyErrorResId = null
            )
        }
    }

    fun onVerifyKeyClicked() {
        val trimmed = _uiState.value.keyInput.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(keyErrorResId = R.string.error_empty_key) }
            return
        }
        val isValidFormat = trimmed.startsWith("NXS-") && trimmed.length >= 14
        if (!isValidFormat) {
            _uiState.update { it.copy(keyErrorResId = R.string.error_invalid_key) }
            return
        }

        // Prompt the client with an explicit IP sharing consent notification before unlocking
        _uiState.update {
            it.copy(
                keyErrorResId = null,
                showIpConsentDialog = true
            )
        }
    }

    fun onAllowIpSharingClicked() {
        val trimmedKey = _uiState.value.keyInput.trim()
        val hardwareId = _uiState.value.hardwareId
        _uiState.update {
            it.copy(
                showIpConsentDialog = false,
                authStage = AuthStage.VERIFYING
            )
        }

        viewModelScope.launch {
            val resolvedIp = ipResolver.fetchDeviceIp()
            var notice = "IP shared with Admin Panel ($resolvedIp)"
            if (sessionRepository != null && currentUserId.isNotBlank()) {
                val result = sessionRepository.recordConsentedSession(
                    activationKey = trimmedKey,
                    ipAddress = resolvedIp,
                    hardwareId = hardwareId
                )
                result.onFailure { err ->
                    notice = "Unlocked locally (Cloud log failed: ${err.localizedMessage ?: "error"})"
                }
            }

            _uiState.update {
                it.copy(
                    authStage = AuthStage.UNLOCKED,
                    isMenuExpanded = true,
                    ipConsentGranted = true,
                    lastSharedIp = resolvedIp,
                    consentNoticeMessage = notice
                )
            }
            appendLog("AUTH", "VIP key [$trimmedKey] verified against HWID $hardwareId", LogLevel.SUCCESS)
            appendLog("CONSENT", "User allowed IP sharing -> $resolvedIp logged to Admin Panel", LogLevel.INFO)
        }
    }

    fun onDeclineIpSharingClicked() {
        val trimmedKey = _uiState.value.keyInput.trim()
        val hardwareId = _uiState.value.hardwareId
        _uiState.update {
            it.copy(
                showIpConsentDialog = false,
                authStage = AuthStage.VERIFYING
            )
        }

        viewModelScope.launch {
            delay(450L)
            _uiState.update {
                it.copy(
                    authStage = AuthStage.UNLOCKED,
                    isMenuExpanded = true,
                    ipConsentGranted = false,
                    lastSharedIp = null,
                    consentNoticeMessage = "Private session: IP sharing declined"
                )
            }
            appendLog("AUTH", "VIP key [$trimmedKey] verified against HWID $hardwareId", LogLevel.SUCCESS)
            appendLog("PRIVACY", "User declined IP sharing. No IP address transmitted.", LogLevel.WARN)
        }
    }

    fun setGateAdminSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showGateAdminSheet = visible) }
    }

    fun onDeleteSessionClicked(sessionId: String) {
        val repo = sessionRepository ?: return
        viewModelScope.launch {
            repo.deleteSession(sessionId)
        }
    }

    fun onLockSessionClicked() {
        injectionJob?.cancel()
        _uiState.update {
            it.copy(
                authStage = AuthStage.LOCKED,
                isMenuExpanded = true,
                showIpConsentDialog = false
            )
        }
    }

    fun setMenuExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isMenuExpanded = expanded) }
    }

    fun toggleMenuExpanded() {
        _uiState.update { it.copy(isMenuExpanded = !it.isMenuExpanded) }
    }

    fun selectTab(tab: OverlayTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateEsp(transform: (EspConfig) -> EspConfig) {
        _uiState.update { it.copy(esp = transform(it.esp)) }
    }

    fun updateAimbot(transform: (AimbotConfig) -> AimbotConfig) {
        _uiState.update { it.copy(aimbot = transform(it.aimbot)) }
    }

    fun setTargetBone(bone: TargetBone) {
        _uiState.update { it.copy(aimbot = it.aimbot.copy(targetBone = bone)) }
        appendLog("AIM", "Target bone priority switched -> ${bone.label}", LogLevel.INFO)
    }

    fun updateCrosshair(transform: (CrosshairConfig) -> CrosshairConfig) {
        _uiState.update { it.copy(crosshair = transform(it.crosshair)) }
    }

    fun setCrosshairStyle(style: CrosshairStyle) {
        _uiState.update { it.copy(crosshair = it.crosshair.copy(style = style)) }
    }

    fun updateBooster(transform: (BoosterConfig) -> BoosterConfig) {
        _uiState.update { state ->
            val updated = transform(state.booster)
            state.copy(booster = updated)
        }
    }

    fun toggle120Fps(enabled: Boolean) {
        updateBooster { it.copy(fps120Enabled = enabled) }
        appendLog(
            "BOOST",
            if (enabled) "Unlocked swap-chain vsync -> 120Hz frame target" else "Restored default 60Hz frame cap",
            if (enabled) LogLevel.SUCCESS else LogLevel.INFO
        )
    }

    fun toggleAntiLag(enabled: Boolean) {
        updateBooster { it.copy(antiLagEnabled = enabled) }
        appendLog(
            "NET",
            if (enabled) "TCP_NODELAY & UDP priority queue active" else "Network scheduler reverted to default",
            if (enabled) LogLevel.SUCCESS else LogLevel.INFO
        )
    }

    fun runPythonBypassInjection() {
        if (_uiState.value.injectionState == InjectionState.RUNNING) return
        injectionJob?.cancel()
        injectionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    injectionState = InjectionState.RUNNING,
                    isBypassVerified = false
                )
            }

            val scriptSteps = listOf(
                Triple("EXEC", "$ python3 core_hook.py --arch arm64-v8a --pid 14892", LogLevel.PYTHON) to 260L,
                Triple("PY", "Importing pymem==1.13.1, frida==16.4.8, capstone-arm64", LogLevel.PYTHON) to 380L,
                Triple("HOOK", "Attaching process_vm_readv -> target.arena.client", LogLevel.HOOK) to 420L,
                Triple("SCAN", "Resolved ELF module libil2cpp.so base @ 0x7B4F200000", LogLevel.INFO) to 390L,
                Triple("SCAN", "AOB match [48 89 5C 24 ? 57] -> WorldToScreen @ +0x01F4B880", LogLevel.HOOK) to 440L,
                Triple("SCAN", "Resolved EntityList offset -> +0x02A8C410 (32-byte stride)", LogLevel.HOOK) to 360L,
                Triple("PATCH", "Patching CRC32 checksum table in libanogs.so +0x004D12A0", LogLevel.PATCH) to 480L,
                Triple("PATCH", "Nop-ing anti-cheat heartbeat timer (0x0092E114 -> 1F 20 03 D5)", LogLevel.PATCH) to 410L,
                Triple("SUCCESS", "SUCCESS! Python memory bridge active. Anti-cheat bypassed.", LogLevel.SUCCESS) to 200L
            )

            for ((entry, pauseMs) in scriptSteps) {
                appendLog(entry.first, entry.second, entry.third)
                delay(pauseMs)
            }

            _uiState.update {
                it.copy(
                    injectionState = InjectionState.INJECTED,
                    isBypassVerified = true
                )
            }
        }
    }

    private fun appendLog(tag: String, message: String, level: LogLevel) {
        val now = timeFormatter.format(Date())
        val line = TerminalLine(
            id = nextLogId++,
            timestamp = now,
            tag = tag,
            message = message,
            level = level
        )
        _uiState.update { state ->
            val trimmed = (state.terminalLogs + line).takeLast(60)
            state.copy(terminalLogs = trimmed)
        }
    }

    private fun initialBootLogs(): List<TerminalLine> {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        return listOf(
            TerminalLine(1L, ts, "SYS", "Nexus Overlay Engine v4.2 initialized (ARM64)", LogLevel.INFO),
            TerminalLine(2L, ts, "PY", "Python 3.11.8 runtime sandbox ready [core_hook.py]", LogLevel.PYTHON),
            TerminalLine(3L, ts, "WARN", "Memory hooks in standby. Tap INJECT PYTHON BYPASS.", LogLevel.WARN)
        )
    }

    private companion object {
        const val TAG = "OverlayViewModel"
        const val ADMIN_EMAIL = "ayanrashid080@gmail.com"
    }
}
