package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.ble.BLEManager
import com.example.ble.SimulationManager
import com.example.services.AudioService
import com.example.services.FirebaseService
import com.example.services.LocalNotificationService
import com.example.services.StorageService

class SeizureGuardianApp : Application() {

    companion object {
        lateinit var instance: SeizureGuardianApp
            private set

        lateinit var storageService: StorageService
            private set

        lateinit var firebaseService: FirebaseService
            private set

        lateinit var audioService: AudioService
            private set

        lateinit var notificationService: LocalNotificationService
            private set

        lateinit var bleManager: BLEManager
            private set

        lateinit var simulationManager: SimulationManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d("SeizureGuardianApp", "Initializing application layers...")

        // Direct, solid Constructor-based dependency structure:
        storageService = StorageService(this)
        firebaseService = FirebaseService(this)
        audioService = AudioService(this)
        notificationService = LocalNotificationService(this)

        bleManager = BLEManager(
            context = this,
            storageService = storageService,
            firebaseService = firebaseService,
            audioService = audioService,
            notificationService = notificationService
        )

        simulationManager = SimulationManager(bleManager)

        Log.d("SeizureGuardianApp", "All architectural singletons loaded successfully!")
    }
}
