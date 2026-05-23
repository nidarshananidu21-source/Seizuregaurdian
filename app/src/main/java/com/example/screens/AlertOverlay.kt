package com.example.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SeizureGuardianApp
import com.example.ble.BLEManager
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.PulseRed
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AlertOverlay(
    bleManager: BLEManager,
    onDismissAlert: () -> Unit
) {
    val activeState by bleManager.isEmergencyTriggered.collectAsState(initial = false)
    val isCountdownActive by bleManager.isCountdownActive.collectAsState(initial = false)
    val countdownSeconds by bleManager.countdownSeconds.collectAsState(initial = 15)

    // Absorb clicks and block user inputs on any of these high-priority overlays
    if (!activeState && !isCountdownActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "warning_glow")
    
    // Pulsing danger background animation
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "danger_glow"
    )

    // Alarm Icon scale pulse
    val rawScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarm_icon_scale"
    )

    if (isCountdownActive) {
        // ==========================================
        // PHASE 1: FALSE-ALARM COUNTDOWN OVERLAY
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1219).copy(alpha = 0.98f))
                .pointerInput(Unit) {
                    detectTapGestures {}
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Circular yellow-orange warning glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .scale(rawScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFB923C), Color(0x00FB923C))
                            )
                        )
                ) {
                    Text(
                        text = "$countdownSeconds",
                        style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFB923C),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 48.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "⚠️ ANOMALY DETECTED",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFFB923C),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "A potential seizure pattern has occurred.\nWe are initiating caretaker alerts.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unless disarmed, a loud distress siren will trigger on max speaker output, and real-time GPS coordinates will automatically transmit to your caregivers in:",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Countdown progress line indicator
                val ratio = countdownSeconds / 15f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .background(Color(0xFFFB923C))
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Massive Accessible False Alarm button (height 72dp)
                Button(
                    onClick = {
                        bleManager.cancelCountdown()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(3.dp, NeonBlue)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "FALSE ALARM - CANCEL",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = NeonBlue,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    } else {
        // ==========================================
        // PHASE 2: ALARM ACTIVATED SCREEN
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF200406).copy(alpha = alphaAnim))
                .pointerInput(Unit) {
                    detectTapGestures {}
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(rawScale)
                        .clip(CircleShape)
                        .background(PulseRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.HeartBroken,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "🚨 SEIZURE DETECTED 🚨",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "EMERGENCY OUTREACH TRANSMITTED",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Loud emergency siren is looping on max volume.\nGoogle Maps directions pushed to caregivers.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFFFA0A0),
                        letterSpacing = 0.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Massive Accessible Dismiss key (height 72dp)
                Button(
                    onClick = {
                        // 1. Silencing standard hardware beep sweeps
                        SeizureGuardianApp.audioService.stopEmergencySiren()
                        
                        // 2. Clear state flows
                        onDismissAlert()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = BorderStroke(4.dp, PulseRed)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = PulseRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "I AM SAFE (DISARM ALARM)",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}
