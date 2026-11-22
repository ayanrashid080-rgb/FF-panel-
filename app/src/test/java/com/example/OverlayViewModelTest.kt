package com.example

import com.example.data.IpAddressResolver
import com.example.data.LoginSessionRepository
import com.example.model.AuthStage
import com.example.model.InjectionState
import com.example.model.LoginSession
import com.example.viewmodel.OverlayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeLoginSessionRepository : LoginSessionRepository {
    val recordedSessions = mutableListOf<LoginSession>()
    private val flow = MutableStateFlow<List<LoginSession>>(emptyList())

    override fun observeSessions(currentUserId: String, isAdmin: Boolean): Flow<List<LoginSession>> = flow

    override suspend fun recordConsentedSession(
        activationKey: String,
        ipAddress: String,
        hardwareId: String
    ): Result<Unit> {
        val session = LoginSession(
            id = "sess_${recordedSessions.size + 1}",
            userId = "user_1",
            userEmail = "client@example.com",
            activationKey = activationKey,
            ipAddress = ipAddress,
            hardwareId = hardwareId,
            consentStatus = "ALLOWED"
        )
        recordedSessions.add(session)
        flow.value = recordedSessions.toList()
        return Result.success(Unit)
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        recordedSessions.removeAll { it.id == sessionId }
        flow.value = recordedSessions.toList()
        return Result.success(Unit)
    }
}

private class FakeIpResolver(private val ip: String = "203.0.113.77") : IpAddressResolver {
    var called = false
    override suspend fun fetchDeviceIp(): String {
        called = true
        return ip
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OverlayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun verifyKeyShowsIpConsentDialogAndAllowRecordsSessionToAdminPanel() = runTest {
        val fakeRepo = FakeLoginSessionRepository()
        val fakeIp = FakeIpResolver("203.0.113.77")
        val viewModel = OverlayViewModel(
            sessionRepository = fakeRepo,
            ipResolver = fakeIp,
            currentUserId = "user_1",
            currentUserEmail = "ayanrashid080@gmail.com",
            isEmailVerified = true,
            startTelemetry = false
        )

        viewModel.onGetKeyClicked()
        val key = viewModel.uiState.value.keyInput
        assertTrue(key.startsWith("NXS-"))

        viewModel.onVerifyKeyClicked()
        assertTrue(viewModel.uiState.value.showIpConsentDialog)
        assertFalse(fakeIp.called)
        assertTrue(fakeRepo.recordedSessions.isEmpty())

        viewModel.onAllowIpSharingClicked()
        advanceUntilIdle()

        assertTrue(fakeIp.called)
        assertEquals(AuthStage.UNLOCKED, viewModel.uiState.value.authStage)
        assertEquals(true, viewModel.uiState.value.ipConsentGranted)
        assertEquals("203.0.113.77", viewModel.uiState.value.lastSharedIp)
        assertTrue(viewModel.uiState.value.isAdminUser)
        assertEquals(1, fakeRepo.recordedSessions.size)
        assertEquals("203.0.113.77", fakeRepo.recordedSessions.first().ipAddress)
    }

    @Test
    fun declineIpSharingUnlocksPrivatelyWithoutFetchingOrRecordingIp() = runTest {
        val fakeRepo = FakeLoginSessionRepository()
        val fakeIp = FakeIpResolver("203.0.113.77")
        val viewModel = OverlayViewModel(
            sessionRepository = fakeRepo,
            ipResolver = fakeIp,
            currentUserId = "user_2",
            currentUserEmail = "client@example.com",
            isEmailVerified = true,
            startTelemetry = false
        )

        viewModel.onGetKeyClicked()
        viewModel.onVerifyKeyClicked()
        assertTrue(viewModel.uiState.value.showIpConsentDialog)

        viewModel.onDeclineIpSharingClicked()
        advanceUntilIdle()

        assertFalse(fakeIp.called)
        assertTrue(fakeRepo.recordedSessions.isEmpty())
        assertEquals(AuthStage.UNLOCKED, viewModel.uiState.value.authStage)
        assertEquals(false, viewModel.uiState.value.ipConsentGranted)
        assertNull(viewModel.uiState.value.lastSharedIp)
        assertFalse(viewModel.uiState.value.isAdminUser)
    }

    @Test
    fun pythonBypassInjectionCompletesWithSuccessLog() = runTest {
        val viewModel = OverlayViewModel(startTelemetry = false)
        viewModel.runPythonBypassInjection()
        advanceTimeBy(4000L)

        val state = viewModel.uiState.value
        assertEquals(InjectionState.INJECTED, state.injectionState)
        assertTrue(state.isBypassVerified)
        assertTrue(state.terminalLogs.any { it.message.contains("SUCCESS!") })
        assertTrue(state.terminalLogs.any { it.message.contains("core_hook.py --arch arm64-v8a") })
    }
}
