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
import com.example.models.EmergencyContact
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.PulseRed

@Composable
fun ContactScreen() {
    val context = LocalContext.current
    val storage = remember { SeizureGuardianApp.storageService }
    val firebase = remember { SeizureGuardianApp.firebaseService }
    val ble = remember { SeizureGuardianApp.bleManager }

    val contacts = remember { mutableStateListOf<EmergencyContact>() }
    
    var showDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputPhone by remember { mutableStateOf("") }

    fun reloadContacts() {
        contacts.clear()
        contacts.addAll(storage.getContacts())
    }

    LaunchedEffect(Unit) {
        reloadContacts()
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
                text = "EMERGENCY PROTOCOL MANAGEMENT",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonTeal,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "CAREGIVER REGISTRY",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            )

            Text(
                text = "When saved, contacts are uploaded safely. If paired, their numbers are flashed to the ESP32 band via CONFIG command.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = DarkBorder
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO CAREGIVERS RECOGNIZED",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(contacts) { contact ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = null,
                                    tint = NeonBlue,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name.uppercase(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                Row {
                                    // Hardware push trigger button
                                    IconButton(
                                        onClick = {
                                            ble.sendContactConfigToESP32(contact.phoneNumber)
                                            Toast.makeText(context, "Config command pushed to band!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Upload,
                                            contentDescription = "Sync to ESP32",
                                            tint = NeonTeal
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Delete caregiver contact
                                    IconButton(
                                        onClick = {
                                            val index = contacts.indexOf(contact)
                                            if (index >= 0) {
                                                contacts.removeAt(index)
                                                // Save locally
                                                storage.saveContacts(contacts.toList())
                                                // Sync cloud
                                                firebase.syncContactsToFirebase(contacts.toList())
                                                Toast.makeText(context, "Caregiver erased.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = PulseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Configure New Caregiver Contact button
            Button(
                onClick = {
                    inputName = ""
                    inputPhone = ""
                    showDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REGISTER CAREGIVER",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Add Dialog box
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(
                        "ADD EMERGENCY CONTACT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Caregiver Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = inputPhone,
                            onValueChange = { inputPhone = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                        onClick = {
                            if (inputName.isBlank() || inputPhone.isBlank()) {
                                Toast.makeText(context, "Fields must be complete!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val freshContact = EmergencyContact(inputName.trim(), inputPhone.trim())
                            val currentList = storage.getContacts().toMutableList()
                            currentList.add(freshContact)

                            // Apply storage & Firebase sync commands
                            storage.saveContacts(currentList)
                            firebase.syncContactsToFirebase(currentList)
                            
                            // Send Bluetooth Command CONFIG payload to wearable directly
                            ble.sendContactConfigToESP32(freshContact.phoneNumber)

                            showDialog = false
                            reloadContacts()
                            Toast.makeText(context, "Caregiver synchronized & flashed to band!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("SYNC & SAVE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
