package com.example.models

data class Alert(
    val id: String = "",
    val timestamp: String = "",
    val heartRate: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val deviceId: String = "",
    val alertType: String = "SEIZURE",
    val status: String = "TRIGGERED"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "timestamp" to timestamp,
            "heartRate" to heartRate,
            "latitude" to latitude,
            "longitude" to longitude,
            "deviceId" to deviceId,
            "alertType" to alertType,
            "status" to status
        )
    }
}
