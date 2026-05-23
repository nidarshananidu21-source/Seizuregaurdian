package com.example.ble

import android.util.Log

object BleDataParser {

    /**
     * Parse bytes directly by converting to string, avoiding multiple temporary object creations.
     */
    fun parseAndEmit(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val rawString = String(bytes, Charsets.UTF_8).trim()
        parseAndEmit(rawString)
    }

    /**
     * Lightweight string parsing routine. Uses linear index-of scans to slice and parse key-values.
     */
    fun parseAndEmit(payload: String) {
        if (payload.isEmpty()) return

        try {
            var heartRate = 80
            var voltage = 0.0f
            var latitude = 0.0
            var longitude = 0.0
            var isAlertPayload = false

            var index = 0
            val len = payload.length

            while (index < len) {
                val nextComma = payload.indexOf(',', index)
                val segmentEnd = if (nextComma != -1) nextComma else len

                val colon = payload.indexOf(':', index)
                if (colon != -1 && colon < segmentEnd) {
                    // Avoid full substring creation unless matching key
                    val key = payload.substring(index, colon).trim()
                    val valueStr = payload.substring(colon + 1, segmentEnd).trim()

                    when (key) {
                        "HR" -> {
                            heartRate = valueStr.toIntOrNull() ?: 80
                        }
                        "V" -> {
                            voltage = valueStr.toFloatOrNull() ?: 0.0f
                        }
                        "LAT" -> {
                            latitude = valueStr.toDoubleOrNull() ?: 0.0
                        }
                        "LNG", "LON" -> {
                            longitude = valueStr.toDoubleOrNull() ?: 0.0
                        }
                    }
                }

                if (nextComma == -1) break
                index = nextComma + 1
            }

            // Raise alert state if heart rate is dangerously elevated or payload triggers emergency levels
            isAlertPayload = (heartRate > 140)

            val telemetryState = SeizureTelemetryProvider.TelemetryState(
                heartRate = heartRate,
                ecgVoltage = voltage,
                latitude = latitude,
                longitude = longitude,
                isAlert = isAlertPayload
            )

            // Dynamic stream flow dispatch
            SeizureTelemetryProvider.emitTelemetry(telemetryState)

            // Bind parsed coordinates and metrics directly to BLEManager for visual update
            try {
                val bleManager = com.example.SeizureGuardianApp.bleManager
                bleManager.setLiveEcgValue(voltage)
                bleManager.setHeartRate(heartRate.toString())
                if (latitude != 0.0) {
                    bleManager.setLatitude(latitude.toString())
                }
                if (longitude != 0.0) {
                    bleManager.setLongitude(longitude.toString())
                }

                // If heartRate is critically elevated (> 140), automatically trigger active crisis countdown
                if (isAlertPayload) {
                    bleManager.triggerSeizureDetection(
                        heartRate = heartRate.toString(),
                        lat = latitude.toString(),
                        lon = longitude.toString(),
                        isMock = false
                    )
                }
            } catch (uninitialized: Exception) {
                // Ignore if app startup singleton sequence not yet complete
            }

        } catch (e: Exception) {
            Log.e("BleDataParser", "High-frequency parser failed gracefully on malformed payload: $payload", e)
        }
    }
}
