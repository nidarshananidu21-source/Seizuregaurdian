package com.example.ble

import android.bluetooth.BluetoothProfile
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

class SimulationManager(private val bleManager: BLEManager) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating

    private val _liveEcgSignal = MutableStateFlow(0f)
    val liveEcgSignal: StateFlow<Float> = _liveEcgSignal

    fun startSimulation() {
        if (_isSimulating.value) return
        _isSimulating.value = true
        Log.d("SimulationManager", "Hardware simulation started.")

        // Forge connection state to connected in BLEManager and set RSSI
        bleManager.setConnectionState(BluetoothProfile.STATE_CONNECTED)
        bleManager.setRssi(-45)

        job = scope.launch {
            var tick = 0f
            var lastHr = 72
            while (isActive) {
                // Generate raw ECG numeric voltage (0 - 1.5 mV pattern)
                val ecgVal = generateSimulatedEcgPoint(tick)
                _liveEcgSignal.value = ecgVal

                // Periodically adjust simulated heart rate
                if ((tick * 10).toInt() % 15 == 0) {
                    lastHr = Random.nextInt(70, 84)
                }

                // Construct a realistic, compressed serial packet string
                // Example format: "HR:75,V:0.24,LAT:12.9234,LNG:77.5012"
                val mockPacket = "HR:$lastHr,V:${"%.2f".format(ecgVal)},LAT:12.9234,LNG:77.5012"
                
                // Pipeline the raw simulation data directly through our physical UART parsing engine!
                BleDataParser.parseAndEmit(mockPacket)

                tick += 0.1f
                delay(100)
            }
        }
    }

    fun stopSimulation() {
        job?.cancel()
        _isSimulating.value = false
        bleManager.setConnectionState(BluetoothProfile.STATE_DISCONNECTED)
        bleManager.setHeartRate("--")
        _liveEcgSignal.value = 0f
        bleManager.setLiveEcgValue(0f)
        Log.d("SimulationManager", "Hardware simulation stopped.")
    }

    private fun generateSimulatedEcgPoint(tick: Float): Float {
        // High fidelity synthetic wave: P-Q-R-S-T sequence with low-level random sensory noise
        val normalizedTick = tick % 1.0f
        return when {
            normalizedTick in 0.05f..0.08f -> {
                0.15f * Math.sin(((normalizedTick - 0.05f) / 0.03f) * Math.PI).toFloat()
            }
            normalizedTick in 0.11f..0.13f -> {
                -0.1f * ((normalizedTick - 0.11f) / 0.02f)
            }
            normalizedTick in 0.13f..0.16f -> {
                1.35f * Math.sin(((normalizedTick - 0.13f) / 0.03f) * Math.PI).toFloat()
            }
            normalizedTick in 0.16f..0.18f -> {
                -0.2f * ((normalizedTick - 0.16f) / 0.02f)
            }
            normalizedTick in 0.22f..0.28f -> {
                0.25f * Math.sin(((normalizedTick - 0.22f) / 0.06f) * Math.PI).toFloat()
            }
            else -> {
                Random.nextFloat() * 0.03f // physiological micro-fluctuations
            }
        }
    }

    fun triggerSimulatedSeizure() {
        Log.d("SimulationManager", "Manual seizure event simulated!")
        // High spike in ECG and Pulse parameters
        bleManager.triggerSeizureDetection(
            heartRate = "154",
            lat = "12.9234",
            lon = "77.5012",
            isMock = true
        )
    }
}
