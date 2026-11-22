package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AuthStage
import com.example.model.OverlayUiState
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
fun VipLoginScreen(
    uiState: OverlayUiState,
    onKeyInputChanged: (String) -> Unit,
    onGetKeyClicked: () -> Unit,
    onVerifyClicked: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVerifying = uiState.authStage == AuthStage.VERIFYING

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            color = GlassSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    listOf(NeonCrimson, ElectricCyan)
                )
            ),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Signed-in account + Admin Panel / Sign Out bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.currentUserEmail.ifBlank { "Google Session" },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onSignOutClicked,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = NeonCrimson,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.action_sign_out),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCrimson
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Cyber emblem header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(NeonCrimson.copy(alpha = 0.28f), ObsidianVoid)
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(ElectricCyan, NeonCrimson)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(id = R.string.vip_gate_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(id = R.string.vip_gate_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )

                Spacer(modifier = Modifier.height(14.dp))

                // HWID badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedPanel)
                        .border(1.dp, CardBorderMuted, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = MatrixGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HWID",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = uiState.hardwareId,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.keyInput,
                    onValueChange = onKeyInputChanged,
                    enabled = !isVerifying,
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(id = R.string.activation_key_label),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.activation_key_placeholder),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                    },
                    isError = uiState.keyErrorResId != null,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onVerifyClicked() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = CardBorderMuted,
                        errorBorderColor = NeonCrimson,
                        focusedContainerColor = ObsidianVoid.copy(alpha = 0.7f),
                        unfocusedContainerColor = ObsidianVoid.copy(alpha = 0.7f),
                        cursorColor = ElectricCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activation_key_input")
                )

                if (uiState.keyErrorResId != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = uiState.keyErrorResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCrimson,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onGetKeyClicked,
                        enabled = !isVerifying,
                        border = BorderStroke(1.2.dp, ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(0.42f)
                            .height(50.dp)
                            .testTag("get_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoFixHigh,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.action_get_key),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                            color = ElectricCyan,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onVerifyClicked,
                        enabled = !isVerifying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCrimson,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(0.58f)
                            .height(50.dp)
                            .testTag("verify_key_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = TextPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.verifying_key_status),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                                maxLines = 1
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.action_verify_unlock),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenAdminPanel,
                    border = BorderStroke(1.dp, CardBorderMuted),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_admin_panel_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = if (uiState.isAdminUser) NeonCrimson else ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            id = if (uiState.isAdminUser) R.string.admin_panel_title else R.string.client_sessions_title
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
