package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.MainActivity
import com.example.services.AudioService
import com.example.services.FirebaseService
import com.example.services.LocalNotificationService
import com.example.services.StorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import java.util.UUID

@SuppressLint("MissingPermission")
class BLEManager(
    private val context: Context,
    private val storageService: StorageService,
    private val firebaseService: FirebaseService,
    private val audioService: AudioService,
    private val notificationService: LocalNotificationService
) {
    companion object {
        const val SERVICE_NUS_STRING = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        const val CHAR_RX_STRING = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
        const val CHAR_TX_STRING = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
        
        val UUID_NUS: UUID = UUID.fromString(SERVICE_NUS_STRING)
        val UUID_RX_CHAR: UUID = UUID.fromString(CHAR_RX_STRING)
        val UUID_TX_CHAR: UUID = UUID.fromString(CHAR_TX_STRING)
        
        // Standard Client Characteristic Configuration Descriptor UUID for enabling notifications
        val UUID_CCC_DESC: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isConnecting = false
    private var retryCount = 0

    // Live observable states for UI Compositions
    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState

    private val _liveHeartRate = MutableStateFlow("--")
    val liveHeartRate: StateFlow<String> = _liveHeartRate

    private val _liveLatitude = MutableStateFlow("")
    val liveLatitude: StateFlow<String> = _liveLatitude

    private val _liveLongitude = MutableStateFlow("")
    val liveLongitude: StateFlow<String> = _liveLongitude

    private val _rssi = MutableStateFlow(-100)
    val rssi: StateFlow<Int> = _rssi

    // Active state tracker for the full-screen alert popup
    var isEmergencyTriggered = MutableStateFlow(false)

    // Simulation/Countdown State Nodes
    val liveEcgValue = MutableStateFlow(0f)
    val isCountdownActive = MutableStateFlow(false)
    val countdownSeconds = MutableStateFlow(15)
    val pendingAlertData = MutableStateFlow<PendingAlert?>(null)

    private val mainScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var countdownJob: kotlinx.coroutines.Job? = null

    data class PendingAlert(
        val heartRate: String,
        val latitude: String,
        val longitude: String,
        val deviceId: String
    )

    fun setConnectionState(state: Int) {
        _connectionState.value = state
    }
    fun setHeartRate(rate: String) {
        _liveHeartRate.value = rate
    }
    fun setLatitude(lat: String) {
        _liveLatitude.value = lat
    }
    fun setLongitude(lon: String) {
        _liveLongitude.value = lon
    }
    fun setRssi(rssiVal: Int) {
        _rssi.value = rssiVal
    }
    fun setLiveEcgValue(value: Float) {
        liveEcgValue.value = value
    }

    fun triggerSeizureDetection(heartRate: String, lat: String, lon: String, isMock: Boolean) {
        countdownJob?.cancel()
        
        val deviceId = if (isMock) "MOCK_ESP32" else (storageService.pairedDeviceAddress ?: "CONNECTED_ESP32")
        pendingAlertData.value = PendingAlert(heartRate, lat, lon, deviceId)
        
        _liveHeartRate.value = heartRate
        _liveLatitude.value = lat
        _liveLongitude.value = lon
        
        countdownSeconds.value = 15
        isCountdownActive.value = true

        countdownJob = mainScope.launch {
            while (countdownSeconds.value > 0) {
                kotlinx.coroutines.delay(1000)
                countdownSeconds.value -= 1
            }
            // 15 seconds passed -> Raise alarm sequence
            executeFullAlertEvent()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        isCountdownActive.value = false
        pendingAlertData.value = null
        isEmergencyTriggered.value = false
        Log.d("BLEManager", "Alert countdown aborted. Disarmed.")
    }

    fun executeFullAlertEvent() {
        val alert = pendingAlertData.value ?: return
        isCountdownActive.value = false
        
        // 1. Force Ringer level to Max and trigger audio siren using AudioService
        audioService.playEmergencySiren()

        // 2. Upload Alert dynamically to Firebase
        firebaseService.addAlert(
            heartRate = alert.heartRate,
            latitude = alert.latitude,
            longitude = alert.longitude,
            deviceId = alert.deviceId
        )

        // 3. Fulfill standard user local banner notification
        notificationService.triggerEmergencyAlertNotification(alert.heartRate, "${alert.latitude}, ${alert.longitude}")

        // 4. Request full overlay state change to active emergency disarm Screen (Phase 2 UI overlay taking over)
        isEmergencyTriggered.value = true

        // 5. Instantly broadcast Google Maps search navigation intent
        triggerDirectGoogleMapsSearch(alert.latitude, alert.longitude)
    }

    // Reconnection runnable block
    private val reconnectRunnable = object : Runnable {
        override fun run() {
            val savedAddress = storageService.pairedDeviceAddress
            if (savedAddress != null && _connectionState.value == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLEManager", "Auto-reconnection run initiated to MAC: $savedAddress")
                connectToDevice(savedAddress)
                mainHandler.postDelayed(this, 10000) // retry every 10s if it fails
            }
        }
    }

    fun startAutoReconnectCycle() {
        mainHandler.removeCallbacks(reconnectRunnable)
        mainHandler.post(reconnectRunnable)
    }

    fun stopAutoReconnectCycle() {
        mainHandler.removeCallbacks(reconnectRunnable)
    }

    fun connectToDevice(address: String) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("BLEManager", "Bluetooth is disabled or unsupported on this device!")
            return
        }
        if (isConnecting) return
        isConnecting = true
        _connectionState.value = BluetoothProfile.STATE_CONNECTING

        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            Log.d("BLEManager", "Initiating Gatt connection request to ${device.name ?: "Unknown"} [${device.address}]")
            
            // Connect to GATT Server
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            isConnecting = false
            _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
            Log.e("BLEManager", "Error connecting GATT", e)
        }
    }

    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
            Log.d("BLEManager", "Gatt disconnect requested manually")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            isConnecting = false
            _connectionState.value = newState

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                retryCount = 0
                mainHandler.removeCallbacks(reconnectRunnable)
                Log.d("BLEManager", "GATT connected! Initiating service discovery...")
                gatt.discoverServices()
                
                // Read signal strength RSSI periodically
                readRssiLoop()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                rxCharacteristic = null
                _rssi.value = -100
                Log.d("BLEManager", "GATT disconnected! Triggering auto-reconnect cycle.")
                startAutoReconnectCycle()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val nusService = gatt.getService(UUID_NUS)
                if (nusService != null) {
                    val txChar = nusService.getCharacteristic(UUID_TX_CHAR)
                    rxCharacteristic = nusService.getCharacteristic(UUID_RX_CHAR)

                    if (txChar != null) {
                        // Enable local notification listen for characteristic TX change
                        gatt.setCharacteristicNotification(txChar, true)

                        // Enable remote notification by writing the Descriptor
                        val descriptor = txChar.getDescriptor(UUID_CCC_DESC)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                            Log.d("BLEManager", "Nordic NUS Service characteristics matching and notification enabled!")
                            firebaseService.addDeviceLog(gatt.device.address, "NUS Handshake Successful. Monitoring live feed.")
                        }
                    }
                } else {
                    Log.e("BLEManager", "Nordic UART Service not found on target peripheral device!")
                    gatt.disconnect()
                }
            } else {
                Log.e("BLEManager", "Service discovery failed with status status Code: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            super.onCharacteristicChanged(gatt, characteristic)
            val rawBytes = characteristic.value
            if (rawBytes != null && characteristic.uuid == UUID_TX_CHAR) {
                val incomingString = String(rawBytes).trim()
                Log.d("BLEManager", "Data Received over NUS TX: $incomingString")
                parseIncomingMessage(gatt.device.address, incomingString)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (characteristic.uuid == UUID_TX_CHAR) {
                val incomingString = String(value).trim()
                Log.d("BLEManager", "Data Received (New API) NUS TX: $incomingString")
                parseIncomingMessage(gatt.device.address, incomingString)
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssiValue: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssiValue, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _rssi.value = rssiValue
            }
        }
    }

    private fun readRssiLoop() {
        if (_connectionState.value == BluetoothProfile.STATE_CONNECTED) {
            bluetoothGatt?.readRemoteRssi()
            mainHandler.postDelayed({ readRssiLoop() }, 4000)
        }
    }

    // Custom parsing logic:
    // Raw Format: ALERT|HR:130|LAT:12.97|LON:77.59
    private fun parseIncomingMessage(deviceId: String, message: String) {
        try {
            if (message.contains("V:") || message.contains(",")) {
                // High-performance direct parser pipeline
                BleDataParser.parseAndEmit(message)
                return
            }

            if (message.startsWith("ALERT")) {
                val tokens = message.split("|")
                var hr = "130"
                var lat = "12.97"
                var lon = "77.59"

                for (token in tokens) {
                    when {
                        token.startsWith("HR:") -> hr = token.substring(3).trim()
                        token.startsWith("LAT:") -> lat = token.substring(4).trim()
                        token.startsWith("LON:") -> lon = token.substring(4).trim()
                    }
                }

                // Divert to 15-second false-alarm countdown sequence
                triggerSeizureDetection(
                    heartRate = hr,
                    lat = lat,
                    lon = lon,
                    isMock = false
                )
            } else if (message.startsWith("HEARTBEAT|") || message.startsWith("HR:")) {
                // Heartbeat packet parser (e.g., HR:82)
                val cleanHr = message.replace("HEARTBEAT|", "").replace("HR:", "").trim()
                _liveHeartRate.value = cleanHr
                Log.d("BLEManager", "Heartbeat received. Local HR Updated: $cleanHr")
            }
        } catch (e: Exception) {
            Log.e("BLEManager", "Critical parsing failed for NUS: $message", e)
        }
    }

    fun triggerDirectGoogleMapsSearch(latitude: String, longitude: String) {
        mainHandler.post {
            try {
                val uri = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                val mapIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(mapIntent)
                Log.d("BLEManager", "Auto-Opening Google Maps for locations $latitude, $longitude")
            } catch (e: Exception) {
                Log.e("BLEManager", "Failed to auto-launch maps intent", e)
            }
        }
    }

    // Sends contact configuration payload CONFIG|+919876543210
    fun sendContactConfigToESP32(phoneNumber: String) {
        val payload = "CONFIG|$phoneNumber\n"
        val characteristic = rxCharacteristic
        val gatt = bluetoothGatt

        if (gatt != null && characteristic != null) {
            try {
                characteristic.value = payload.toByteArray()
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                gatt.writeCharacteristic(characteristic)
                Log.d("BLEManager", "Successfully sent configuration command to ESP32: $payload")
                firebaseService.addDeviceLog(gatt.device.address, "Transmitted Phone configuration command: $payload")
            } catch (e: Exception) {
                Log.e("BLEManager", "Failed writing characteristic config", e)
            }
        } else {
            Log.w("BLEManager", "Cannot write CONFIG. BLE Device is not fully initialized/connected.")
        }
    }
}
