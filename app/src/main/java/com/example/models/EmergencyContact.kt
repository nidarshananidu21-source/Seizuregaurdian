package com.example.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EmergencyContact(
    val name: String = "",
    val phoneNumber: String = ""
)
