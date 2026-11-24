package com.example.mayuranddhyeyinternshipproject.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mayuranddhyeyinternshipproject.ui.components.LoadingOverlay
import com.example.mayuranddhyeyinternshipproject.ui.theme.MintAccent
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusError
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusErrorBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealPrimary
import kotlinx.coroutines.delay

/**
 * Screen presented to users whose email address has not yet been verified.
 * Complies with requirement 2B:
 * - "Resend email" button with 60-second cooldown timer
 * - "I have verified" button checking user.reload()
 * - "Sign out" button
 * Prevents reaching Home until isEmailVerified is true.
 */
@Composable
fun VerifyEmailScreen(
    authViewModel: AuthViewModel,
    onVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var resendCooldown by remember { mutableIntStateOf(0) }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    LaunchedEffect(uiState.isSuccess, uiState.isEmailVerificationPending) {
        if (authViewModel.isEmailVerified()) {
            onVerified()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Email Verification Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(MintAccent.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Verify Your Email",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "A verification link has been sent to your email address. Please click the link in your email inbox or spam folder, then tap below.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error banner
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusErrorBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium.copy(color = StatusError),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Info banner
            AnimatedVisibility(visible = uiState.infoMessage != null) {
                uiState.infoMessage?.let { info ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MintAccent.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TealPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // "I have verified" button
            Button(
                onClick = {
                    authViewModel.checkEmailVerification()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = "I have verified",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "Resend email" button
            OutlinedButton(
                onClick = {
                    resendCooldown = 60
                    authViewModel.resendVerificationEmail()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = resendCooldown == 0 && !uiState.isLoading
            ) {
                Text(
                    text = if (resendCooldown > 0) "Resend email in ${resendCooldown}s" else "Resend verification email",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Sign out" button
            TextButton(
                onClick = {
                    authViewModel.signOut()
                    onSignOut()
                }
            ) {
                Text(
                    text = "Sign out",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        LoadingOverlay(isLoading = uiState.isLoading)
    }
}
