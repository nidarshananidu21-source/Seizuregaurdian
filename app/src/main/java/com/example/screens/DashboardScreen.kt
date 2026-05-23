package com.example.screens

import android.bluetooth.BluetoothProfile
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SeizureGuardianApp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigateToScan: () -> Unit
) {
    val context = LocalContext.current
    val ble = remember { SeizureGuardianApp.bleManager }

    // Live State Collectors
    val connState by ble.connectionState.collectAsState()
    val heartRate by ble.liveHeartRate.collectAsState()
    val latitude by ble.liveLatitude.collectAsState()
    val longitude by ble.liveLongitude.collectAsState()
    val rssi by ble.rssi.collectAsState()

    val liveEcgValue by ble.liveEcgValue.collectAsState()
    val isSimulating by SeizureGuardianApp.simulationManager.isSimulating.collectAsState()

    var simulatedBattery by remember { mutableStateOf(84) }

    // Simulating minor battery depletion or device parameters
    LaunchedEffect(Unit) {
        while (true) {
            delay(45000)
            if (simulatedBattery > 10) {
                simulatedBattery -= 1
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Biomedical Core Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BIOSENSOR METRIC TERMINAL",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonTeal,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "SEIZURE GUARDIAN ACTIVE",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            IconButton(
                onClick = {
                    SeizureGuardianApp.firebaseService.logout()
                    onLogout()
                },
                modifier = Modifier.background(DarkSurfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Sign Out",
                    tint = PulseRed
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Live ECG Scrolling Card ---
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DarkBorder),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE TELEMETRY FLOW",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (connState == BluetoothProfile.STATE_CONNECTED) GridGreen else PulseRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (connState == BluetoothProfile.STATE_CONNECTED) "ONLINE" else "DISCONNECTED",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (connState == BluetoothProfile.STATE_CONNECTED) GridGreen else PulseRed
                                )
                            )
                        }
                    }

                    EcgPulseWaveform(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
                        isActive = connState == BluetoothProfile.STATE_CONNECTED
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Grid Parameters ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Heart Rate Panel
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = PulseRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "HEART RATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (connState == BluetoothProfile.STATE_CONNECTED) "$heartRate" else "--",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "bpm (Dual ICP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Connection Link Panel
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "LINK STRENGTH",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (connState == BluetoothProfile.STATE_CONNECTED) "$rssi" else "--",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonTeal,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "dBm RSSI Index",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Location Tracking Info ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DarkBorder),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GPS LOCALIZER ENGINE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        )
                    }

                    Text(
                        text = "GSM FALLBACK OK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GridGreen,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (latitude.isNotEmpty() && longitude.isNotEmpty()) {
                        "Target: $latitude, $longitude"
                    } else {
                        "Awaiting initial wearable coordinates..."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                )

                if (latitude.isNotEmpty() && longitude.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            ble.triggerDirectGoogleMapsSearch(latitude, longitude)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LAUNCH GOOGLE MAPS",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Hardware Device Diagnostic Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DarkBorder),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "WEARABLE HARDWARE STATUAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (isSimulating) "VIRTUAL_ESP32_PERIPHERAL" else (SeizureGuardianApp.storageService.pairedDeviceName ?: "Unlinked Hardware"),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold, 
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = "Battery",
                        tint = BatteryOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSimulating) "100%" else "$simulatedBattery%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BatteryOrange,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Hardware Simulation Panel ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isSimulating) NeonTeal else DarkBorder),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isSimulating) NeonTeal else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HARDWARE SIMULATION PROTOCOLS",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (isSimulating) Color(0xFF0F2624) else Color(0xFF1E293B),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSimulating) "VIRTUAL FEED" else "STANDBY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSimulating) NeonTeal else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Observe live digital values or trigger mock critical events to emulate wearable behaviors on-screen.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )

                if (isSimulating) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ECG STREAM CHANNEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${"%.3f".format(liveEcgValue)} mV",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = NeonTeal,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Activate Simulation Mode
                    Button(
                        onClick = {
                            if (isSimulating) {
                                SeizureGuardianApp.simulationManager.stopSimulation()
                            } else {
                                SeizureGuardianApp.simulationManager.startSimulation()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimulating) Color(0xFF2C1E1E) else DarkBorder
                        ),
                        modifier = Modifier.weight(1f).height(44.dp),
                        border = BorderStroke(1.dp, if (isSimulating) PulseRed else NeonBlue)
                    ) {
                        Text(
                            text = if (isSimulating) "TERMINATE FEED" else "START TEST FEED",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSimulating) PulseRed else Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    // Simulated Critical Seizure Event
                    Button(
                        onClick = {
                            SeizureGuardianApp.simulationManager.triggerSimulatedSeizure()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseRed),
                        modifier = Modifier.weight(1f).height(44.dp),
                        enabled = isSimulating
                    ) {
                        Text(
                            text = "SIMULATE SEIZURE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Manual Scan Rebound Trigger
        if (connState != BluetoothProfile.STATE_CONNECTED) {
            Button(
                onClick = { onNavigateToScan() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                border = BorderStroke(1.dp, NeonBlue),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RE-PAIR BLE DETECTOR",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

// ECG Canvas wave generator
@Composable
fun EcgPulseWaveform(modifier: Modifier = Modifier, isActive: Boolean) {
    if (!isActive) {
        Canvas(modifier = modifier) {
            val y = size.height / 2
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 3.dp.toPx()
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ecg_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val step = 8f
        val pointsList = mutableListOf<Offset>()
        val maxPoints = (width / step).toInt()

        for (i in 0..maxPoints) {
            val x = i * step
            // Phase-based cyclic offset simulation (ECG spike profile)
            val pointPhase = ((x / width) - phase + 1f) % 1f

            val yOffset = when {
                pointPhase in 0.05f..0.08f -> {
                    // P-Wave (mild atrial depolarization)
                    val pRatio = (pointPhase - 0.05f) / 0.03f
                    Math.sin(pRatio * Math.PI) * (height * 0.1f)
                }
                pointPhase in 0.12f..0.14f -> {
                    // Q-Spike (downwards)
                    val qRatio = (pointPhase - 0.12f) / 0.02f
                    -(qRatio * (height * 0.15f))
                }
                pointPhase in 0.14f..0.17f -> {
                    // R-Spike (sharp upwards depolarization)
                    val rRatio = (pointPhase - 0.14f) / 0.03f
                    Math.sin(rRatio * Math.PI) * (height * 0.85f)
                }
                pointPhase in 0.17f..0.19f -> {
                    // S-Spike (underbeat cardiac drop)
                    val sRatio = (pointPhase - 0.17f) / 0.02f
                    -(sRatio * (height * 0.25f))
                }
                pointPhase in 0.25f..0.32f -> {
                    // T-Wave (ventricular repolarization swell)
                    val tRatio = (pointPhase - 0.25f) / 0.07f
                    Math.sin(tRatio * Math.PI) * (height * 0.2f)
                }
                else -> 0.0
            }

            val finalY = (height / 2f) - yOffset.toFloat()
            pointsList.add(Offset(x, finalY))
        }

        val tracePath = Path().apply {
            if (pointsList.isNotEmpty()) {
                moveTo(pointsList.first().x, pointsList.first().y)
                for (j in 1 until pointsList.size) {
                    lineTo(pointsList[j].x, pointsList[j].y)
                }
            }
        }

        drawPath(
            path = tracePath,
            color = Color(0xFF00FFCC),
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}
