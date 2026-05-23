package com.example.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SeizureGuardianApp
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.PulseRed
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToNext: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "heart_pulse")
    
    // Smooth heart scale animation (Pulsing bio-signal)
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )

    // Pulse color oscillation between Neon Blue and Alert Red
    val heartColor by infiniteTransition.animateColor(
        initialValue = NeonBlue,
        targetValue = PulseRed,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_color"
    )

    var loadingStatus by remember { mutableStateOf("Initializing Medical Core...") }

    LaunchedEffect(Unit) {
        delay(1000)
        loadingStatus = "Scanning Secure Protocols..."
        delay(1000)
        loadingStatus = "Synchronizing Cloud Feed..."
        delay(800)
        
        // Auto routing audit
        val nextRoute = if (SeizureGuardianApp.firebaseService.isUserSignedIn()) {
            if (SeizureGuardianApp.storageService.pairedDeviceAddress != null) {
                "dashboard"
            } else {
                "device_scan"
            }
        } else {
            "login"
        }
        onNavigateToNext(nextRoute)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Glowing tech bio-grid background simulator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Shield Heart Beat",
                modifier = Modifier
                    .size(110.dp)
                    .scale(heartScale),
                tint = heartColor
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "SEIZURE GUARDIAN",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            )

            Text(
                text = "IOT LIFE-SAVING COMPANION",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.sp,
                    color = NeonBlue
                ),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Dynamic textual ticker
            Text(
                text = loadingStatus,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
