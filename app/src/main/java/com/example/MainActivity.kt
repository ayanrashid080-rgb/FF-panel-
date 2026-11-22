package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FirestoreLoginSessionRepository
import com.example.model.AuthStage
import com.example.model.OverlayUiState
import com.example.ui.components.AdminPanelModalDialog
import com.example.ui.components.ArenaSimulationLayer
import com.example.ui.components.AuthScreen
import com.example.ui.components.CyberModMenuPanel
import com.example.ui.components.DraggableFloatingBubbleOverlay
import com.example.ui.components.IpConsentNotificationDialog
import com.example.ui.components.VipLoginScreen
import com.example.ui.components.signOutNexus
import com.example.ui.theme.NexusOverlayTheme
import com.example.viewmodel.OverlayViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            NexusOverlayTheme {
                AppNavigation()
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            ArenaSimulationLayer(
                uiState = OverlayUiState(),
                showHudAndHints = false
            )
            AuthScreen(
                onAuthSuccess = { }
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedNexusApp(
                currentUserId = user.uid,
                currentUserEmail = user.email.orEmpty(),
                isEmailVerified = user.isEmailVerified
            )
        }
    }
}

@Composable
fun AuthenticatedNexusApp(
    currentUserId: String,
    currentUserEmail: String,
    isEmailVerified: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: OverlayViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                OverlayViewModel(
                    sessionRepository = FirestoreLoginSessionRepository(db),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    isEmailVerified = isEmailVerified
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val isUnlocked = uiState.authStage == AuthStage.UNLOCKED

    BackHandler(enabled = isUnlocked && uiState.isMenuExpanded) {
        viewModel.setMenuExpanded(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Tactical Arena Viewport + ESP / FOV / Crosshair / Telemetry HUD
        ArenaSimulationLayer(
            uiState = uiState,
            showHudAndHints = isUnlocked
        )

        // 2. VIP Activation Key Gate
        AnimatedVisibility(
            visible = !isUnlocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VipLoginScreen(
                uiState = uiState,
                onKeyInputChanged = viewModel::onKeyInputChanged,
                onGetKeyClicked = viewModel::onGetKeyClicked,
                onVerifyClicked = viewModel::onVerifyKeyClicked,
                onOpenAdminPanel = { viewModel.setGateAdminSheetVisible(true) },
                onSignOutClicked = {
                    signOutNexus(credentialManager = credentialManager, scope = scope)
                }
            )
        }

        // 3. Explicit IP Consent Notification Dialog before unlocking VIP menu
        if (uiState.showIpConsentDialog) {
            IpConsentNotificationDialog(
                activationKey = uiState.keyInput.trim(),
                hardwareId = uiState.hardwareId,
                onAllow = viewModel::onAllowIpSharingClicked,
                onDecline = viewModel::onDeclineIpSharingClicked
            )
        }

        // 4. Gate-level Admin / Sessions Audit Dialog
        if (uiState.showGateAdminSheet && !isUnlocked) {
            AdminPanelModalDialog(
                isAdminUser = uiState.isAdminUser,
                sessionsState = sessionsState,
                onDeleteSession = viewModel::onDeleteSessionClicked,
                onDismiss = { viewModel.setGateAdminSheetVisible(false) }
            )
        }

        // 5. Unlocked Mod Menu Overlay + Draggable Floating Bubble
        if (isUnlocked) {
            AnimatedVisibility(
                visible = uiState.isMenuExpanded,
                enter = fadeIn() + scaleIn(initialScale = 0.94f),
                exit = fadeOut() + scaleOut(targetScale = 0.94f)
            ) {
                CyberModMenuPanel(
                    uiState = uiState,
                    sessionsState = sessionsState,
                    onCollapseMenu = { viewModel.setMenuExpanded(false) },
                    onLockSession = viewModel::onLockSessionClicked,
                    onSelectTab = viewModel::selectTab,
                    onToggleEspMaster = { enabled ->
                        viewModel.updateEsp { it.copy(enabled = enabled) }
                    },
                    onToggleEspBoxes = { enabled ->
                        viewModel.updateEsp { it.copy(showBoxes = enabled) }
                    },
                    onToggleEspHp = { enabled ->
                        viewModel.updateEsp { it.copy(showHealthBar = enabled) }
                    },
                    onToggleEspSkeleton = { enabled ->
                        viewModel.updateEsp { it.copy(showSkeleton = enabled) }
                    },
                    onToggleEspSnaplines = { enabled ->
                        viewModel.updateEsp { it.copy(showSnaplines = enabled) }
                    },
                    onToggleAimbotMaster = { enabled ->
                        viewModel.updateAimbot { it.copy(enabled = enabled) }
                    },
                    onToggleFovCircle = { enabled ->
                        viewModel.updateAimbot { it.copy(showFovCircle = enabled) }
                    },
                    onFovRadiusChange = { radius ->
                        viewModel.updateAimbot { it.copy(fovRadiusDp = radius) }
                    },
                    onSmoothnessChange = { smooth ->
                        viewModel.updateAimbot { it.copy(smoothness = smooth) }
                    },
                    onTargetBoneSelected = viewModel::setTargetBone,
                    onToggleCrosshair = { enabled ->
                        viewModel.updateCrosshair { it.copy(enabled = enabled) }
                    },
                    onCrosshairStyleSelected = viewModel::setCrosshairStyle,
                    onToggle120Fps = viewModel::toggle120Fps,
                    onToggleAntiLag = viewModel::toggleAntiLag,
                    onToggleTelemetryHud = { enabled ->
                        viewModel.updateBooster { it.copy(showTelemetryHud = enabled) }
                    },
                    onDeleteSession = viewModel::onDeleteSessionClicked,
                    onInjectPythonBypass = viewModel::runPythonBypassInjection
                )
            }

            DraggableFloatingBubbleOverlay(
                isMenuExpanded = uiState.isMenuExpanded,
                isBypassVerified = uiState.isBypassVerified,
                onToggleMenu = viewModel::toggleMenuExpanded
            )
        }
    }
}
