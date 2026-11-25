package com.example.mayuranddhyeyinternshipproject.ui.screens.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mayuranddhyeyinternshipproject.R
import com.example.mayuranddhyeyinternshipproject.ui.theme.MintAccent
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealPrimary
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealSecondary
import kotlinx.coroutines.delay

/**
 * Splash screen shown on app launch.
 * Shows MedMate logo animation, motivational quote, and a vector illustration of person taking medicine at a table.
 * After ~2.5 seconds, automatically navigates to Home if signed in, else Login.
 */
@Composable
fun SplashScreen(
    isUserLoggedIn: Boolean,
    isEmailVerified: Boolean = false,
    onNavigateToHome: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit = {},
    onNavigateToLogin: () -> Unit
) {
    val authRepository = androidx.compose.runtime.remember { com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository() }

    LaunchedEffect(Unit) {
        if (authRepository.isUserLoggedIn()) {
            try {
                authRepository.reloadUser()
            } catch (ignored: Exception) {}
        }
        delay(2200)
        if (authRepository.isUserLoggedIn()) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_scale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_anim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // MedMate Animated Logo
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale)
                    .background(
                        color = TealPrimary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = TealPrimary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = "MedMate Logo",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Vector illustration placeholder: Person taking medicine at a table
            PersonTakingMedicineIllustration(
                modifier = Modifier
                    .size(width = 240.dp, height = 150.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Motivational quote
            Text(
                text = "“${stringResource(R.string.quote_splash)}”",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Clean vector illustration of a person at a table with a glass of water and medicine pill.
 * Designed using Compose Canvas so it renders crisply with zero asset loading issues.
 */
@Composable
fun PersonTakingMedicineIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Table top
        val tableY = height * 0.72f
        drawRoundRect(
            color = Color(0xFFD7E5E5),
            topLeft = Offset(width * 0.15f, tableY),
            size = Size(width * 0.7f, 12.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        // Table legs
        drawRoundRect(
            color = Color(0xFFB5C9C8),
            topLeft = Offset(width * 0.22f, tableY + 10.dp.toPx()),
            size = Size(8.dp.toPx(), height * 0.22f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFFB5C9C8),
            topLeft = Offset(width * 0.74f, tableY + 10.dp.toPx()),
            size = Size(8.dp.toPx(), height * 0.22f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Person Head (behind table)
        val personCenterX = width * 0.42f
        val headCenterY = height * 0.28f
        drawCircle(
            color = Color(0xFFFFCC80), // Warm skin tone
            radius = 24.dp.toPx(),
            center = Offset(personCenterX, headCenterY)
        )

        // Person Hair
        drawArc(
            color = Color(0xFF4E342E),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(personCenterX - 25.dp.toPx(), headCenterY - 26.dp.toPx()),
            size = Size(50.dp.toPx(), 30.dp.toPx())
        )

        // Person Body / Torso
        drawRoundRect(
            color = TealSecondary,
            topLeft = Offset(personCenterX - 28.dp.toPx(), headCenterY + 20.dp.toPx()),
            size = Size(56.dp.toPx(), tableY - (headCenterY + 16.dp.toPx())),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )

        // Glass of water on the table
        val glassX = width * 0.62f
        val glassY = tableY - 32.dp.toPx()
        drawRoundRect(
            color = Color(0xFF90CAF9).copy(alpha = 0.5f),
            topLeft = Offset(glassX, glassY),
            size = Size(20.dp.toPx(), 30.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Water inside
        drawRoundRect(
            color = Color(0xFF42A5F5).copy(alpha = 0.7f),
            topLeft = Offset(glassX + 2.dp.toPx(), glassY + 10.dp.toPx()),
            size = Size(16.dp.toPx(), 18.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Medicine Pill / Capsule on the table
        val pillX = width * 0.52f
        val pillY = tableY - 10.dp.toPx()
        // Half 1
        drawRoundRect(
            color = Color(0xFFE53935),
            topLeft = Offset(pillX, pillY),
            size = Size(10.dp.toPx(), 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Half 2
        drawRoundRect(
            color = Color(0xFFFFFFFF),
            topLeft = Offset(pillX + 8.dp.toPx(), pillY),
            size = Size(10.dp.toPx(), 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}
