package com.example

import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.screens.*
import com.example.ui.theme.*

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DEVICE_SCAN = "device_scan"
    
    // Bottom navigation nodes
    const val DASHBOARD = "dashboard"
    const val CARE_CONTACTS = "care_contacts"
    const val INCIDENT_HISTORY = "incident_history"
    const val ADMIN_AUDIT = "admin_audit"
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle quick alerts triggered directly by push notification clicks
        parseEmergencyIntent(intent)

        setContent {
            MyApplicationTheme {
                PermissionGuard {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route

                    // Manage clean global alerting overlay state flows
                    val bleManager = remember { SeizureGuardianApp.bleManager }

                    // Nested Navigation Scaffold
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            // Display bottom bar navigation solely on active core screens
                            val showBottomBar = currentRoute in listOf(
                                Routes.DASHBOARD,
                                Routes.CARE_CONTACTS,
                                Routes.INCIDENT_HISTORY,
                                Routes.ADMIN_AUDIT
                            )
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = DarkSurface,
                                    contentColor = TextSecondary
                                ) {
                                    NavigationBarItem(
                                        selected = currentRoute == Routes.DASHBOARD,
                                        onClick = { navController.navigate(Routes.DASHBOARD) { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.MonitorHeart, contentDescription = "Dashboard") },
                                        label = { Text("ECG Feed", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NeonBlue,
                                            selectedTextColor = NeonBlue,
                                            indicatorColor = DarkSurfaceVariant
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == Routes.CARE_CONTACTS,
                                        onClick = { navController.navigate(Routes.CARE_CONTACTS) { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.ContactPhone, contentDescription = "Caregivers") },
                                        label = { Text("Guardians", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NeonTeal,
                                            selectedTextColor = NeonTeal,
                                            indicatorColor = DarkSurfaceVariant
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == Routes.INCIDENT_HISTORY,
                                        onClick = { navController.navigate(Routes.INCIDENT_HISTORY) { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.History, contentDescription = "Incidents") },
                                        label = { Text("Incident Log", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PulseRed,
                                            selectedTextColor = PulseRed,
                                            indicatorColor = DarkSurfaceVariant
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == Routes.ADMIN_AUDIT,
                                        onClick = { navController.navigate(Routes.ADMIN_AUDIT) { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Console") },
                                        label = { Text("Terminal", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NeonBlue,
                                            selectedTextColor = NeonBlue,
                                            indicatorColor = DarkSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            // Navigation controller matching routes cleanly
                            NavHost(
                                navController = navController,
                                startDestination = Routes.SPLASH
                            ) {
                                composable(Routes.SPLASH) {
                                    SplashScreen(onNavigateToNext = { route ->
                                        navController.navigate(route) {
                                            popUpTo(Routes.SPLASH) { inclusive = true }
                                        }
                                    })
                                }

                                composable(Routes.LOGIN) {
                                    AuthScreen(onAuthSuccess = {
                                        val nextRoute = if (SeizureGuardianApp.storageService.pairedDeviceAddress != null) {
                                            Routes.DASHBOARD
                                        } else {
                                            Routes.DEVICE_SCAN
                                        }
                                        navController.navigate(nextRoute) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    })
                                }

                                composable(Routes.DEVICE_SCAN) {
                                    ScanScreen(onDeviceConnected = {
                                        navController.navigate(Routes.DASHBOARD) {
                                            popUpTo(Routes.DEVICE_SCAN) { inclusive = true }
                                        }
                                    })
                                }

                                composable(Routes.DASHBOARD) {
                                    DashboardScreen(
                                        onLogout = {
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        },
                                        onNavigateToScan = {
                                            navController.navigate(Routes.DEVICE_SCAN)
                                        }
                                    )
                                }

                                composable(Routes.CARE_CONTACTS) {
                                    ContactScreen()
                                }

                                composable(Routes.INCIDENT_HISTORY) {
                                    HistoryScreen()
                                }

                                composable(Routes.ADMIN_AUDIT) {
                                    AdminScreen()
                                }
                            }

                            // --- Global Alert Dialog overlay ---
                            AlertOverlay(
                                bleManager = bleManager,
                                onDismissAlert = {
                                    bleManager.isEmergencyTriggered.value = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseEmergencyIntent(intent)
    }

    private fun parseEmergencyIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("LAUNCHED_FOR_EMERGENCY", false)) {
            Log.d("MainActivity", "Urgent Deep Link Alarm Intent triggered from notification action click")
            try {
                // Instantly pop open the global Alert screen layout
                SeizureGuardianApp.bleManager.isEmergencyTriggered.value = true
                
                // Keep the loud siren loop active
                SeizureGuardianApp.audioService.playEmergencySiren()
            } catch (e: Exception) {
                Log.e("MainActivity", "Bypassed emergency UI flash startup", e)
            }
        }
    }
}
