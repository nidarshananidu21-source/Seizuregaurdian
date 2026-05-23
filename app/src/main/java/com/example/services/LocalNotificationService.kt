package com.example.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class LocalNotificationService(private val context: Context) {
    companion object {
        const val EMERGENCY_CHANNEL_ID = "seizure_guardian_emergency_channel"
        private const val EMERGENCY_NOTIFICATION_ID = 911
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Emergency Alerts"
            val descriptionText = "Urgent hardware alerts for seizure detection events"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(EMERGENCY_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun triggerEmergencyAlertNotification(heartRate: String, coordinates: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Signal to MainActivity to load the full-screen alert automatically
            putExtra("LAUNCHED_FOR_EMERGENCY", true)
            putExtra("EMERGENCY_HR", heartRate)
            putExtra("EMERGENCY_COORDS", coordinates)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, EMERGENCY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ SEIZURE ALERT DETECTED")
            .setContentText("Emergency! Heart Rate: $heartRate. Coordinates: $coordinates")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true) // prevent being swiped away easily during crisis

        with(NotificationManagerCompat.from(context)) {
            // Check for posture permission in Android 13 is done at UI layer, but we invoke standard notify here
            notify(EMERGENCY_NOTIFICATION_ID, builder.build())
        }
    }
}
