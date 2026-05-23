package com.example.ble

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SeizureTelemetryProvider {
    data class TelemetryState(
        val heartRate: Int,
        val ecgVoltage: Float,
        val latitude: Double,
        val longitude: Double,
        val isAlert: Boolean = false
    )

    // extraBufferCapacity keeps recent updates in memory without locking or creating GC pressures
    private val _telemetryFlow = MutableSharedFlow<TelemetryState>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val telemetryFlow: SharedFlow<TelemetryState> = _telemetryFlow.asSharedFlow()

    fun emitTelemetry(state: TelemetryState) {
        _telemetryFlow.tryEmit(state)
    }
}
