package com.example.services

import android.content.Context
import android.content.SharedPreferences
import com.example.models.EmergencyContact
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class StorageService(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("seizure_guardian_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val listMyContactsType = Types.newParameterizedType(List::class.java, EmergencyContact::class.java)
    private val contactsAdapter = moshi.adapter<List<EmergencyContact>>(listMyContactsType)

    companion object {
        private const val KEY_PAIRED_DEVICE_ADDRESS = "paired_device_address"
        private const val KEY_PAIRED_DEVICE_NAME = "paired_device_name"
        private const val KEY_CONTACTS = "emergency_contacts"
    }

    var pairedDeviceAddress: String?
        get() = prefs.getString(KEY_PAIRED_DEVICE_ADDRESS, null)
        set(value) = prefs.edit().putString(KEY_PAIRED_DEVICE_ADDRESS, value).apply()

    var pairedDeviceName: String?
        get() = prefs.getString(KEY_PAIRED_DEVICE_NAME, null)
        set(value) = prefs.edit().putString(KEY_PAIRED_DEVICE_NAME, value).apply()

    fun getContacts(): List<EmergencyContact> {
        val json = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        return try {
            contactsAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveContacts(contacts: List<EmergencyContact>) {
        val json = contactsAdapter.toJson(contacts)
        prefs.edit().putString(KEY_CONTACTS, json).apply()
    }
}
