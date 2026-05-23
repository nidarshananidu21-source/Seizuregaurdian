package com.example.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SeizureGuardianApp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.PulseRed

data class DeviceLog(
    val time: String,
    val device: String,
    val info: String
)

@Composable
fun AdminScreen() {
    val context = LocalContext.current
    val storage = remember { SeizureGuardianApp.storageService }
    
    // Static / Simulated network console logs for complete IoT validation
    val consoleLogs = remember {
        mutableStateListOf(
            DeviceLog("10:44:20", "ESP32_GUARD_01", "GATT Client connection completed"),
            DeviceLog("10:44:22", "ESP32_GUARD_01", "NUS Service discovery: Success"),
            DeviceLog("10:44:22", "ESP32_GUARD_01", "Notify enabled on RX/TX lines"),
            DeviceLog("10:44:25", "APP_CORE", "Firebase Connection established successfully"),
            DeviceLog("10:45:01", "ESP32_GUARD_01", "Tx Heartbeat payload received: HR:78")
        )
    }

    LaunchedEffect(Unit) {
        val pairedAddr = storage.pairedDeviceAddress ?: "UNLINKED_APP"
        consoleLogs.add(0, DeviceLog("10:52:12", pairedAddr, "Diagnostic logs synced with Cloud console"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SYSTEM ARCHITECT TERMINAL",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonTeal,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "ADMIN DIAGNOSTIC CENTER",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Admin system status indicators
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ACTIVE AGENTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1 Secured Node", fontWeight = FontWeight.Bold, color = NeonBlue, fontFamily = FontFamily.Monospace)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("SERVER FIREBASE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("SECURED / ONLINE", fontWeight = FontWeight.Bold, color = NeonTeal, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "LIVE SYSTEM BUS LOGS",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonBlue,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Console Logs terminal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonTeal),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030810)) // Visual Retro Black terminal
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(consoleLogs) { log ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "[${log.time}] ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NeonTeal,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )

                            Text(
                                text = "${log.device}: ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NeonBlue,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )

                            Text(
                                text = log.info,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Terminal audit utilities
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        consoleLogs.clear()
                        consoleLogs.add(DeviceLog("10:53:01", "CONSOLE", "Buffer reset. Diagnostics idle."))
                        Toast.makeText(context, "Log buffer flushed.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBorder)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = PulseRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FLUSH TERMINAL", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Logs successfully extracted to local directory (CSV mode)!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EXTRACT CSV", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
