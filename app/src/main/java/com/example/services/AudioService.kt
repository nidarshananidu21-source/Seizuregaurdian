package com.example.services

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AudioService(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var syntheticTrack: AudioTrack? = null
    private var isSirenActive = false
    private var originalVolume: Int = 0

    // Vibrator access with extremely safe fallback to avoid NullPointerExceptions in virtual/headless/emulator environments
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.e("AudioService", "Failed to retrieve Vibrator service safely", e)
        null
    }

    fun playEmergencySiren() {
        if (isSirenActive) return
        isSirenActive = true

        // 1. Force Ringer/Audio Settings to Maximum Volume
        try {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
            Log.d("AudioService", "Audio stream volumes forced to maximum: $maxVolume")
        } catch (e: Exception) {
            Log.e("AudioService", "Failed to force volume settings", e)
        }

        // 2. Start Pulsing Vibration Pattern
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 800, 200, 800, 200)
                    val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 800, 200, 800, 200), 0)
                }
            } ?: Log.d("AudioService", "Vibration starting skipped: Vibrator service is unavailable on this device/emulator")
        } catch (e: Exception) {
            Log.e("AudioService", "Vibration starting failed", e)
        }

        // 3. Try Raw Asset first, fallback to Synthetic AudioTrack Siren
        try {
            // Check if R.raw.alert_siren exists in resource file system
            val rawId = context.resources.getIdentifier("alert_siren", "raw", context.packageName)
            if (rawId > 0) {
                mediaPlayer = MediaPlayer.create(context, rawId).apply {
                    isLooping = true
                    start()
                }
                Log.d("AudioService", "Playing alert_siren.mp3 from Raw assets")
            } else {
                // Fall back to highly sophisticated real-time digital synthesizer
                startSyntheticSiren()
                Log.d("AudioService", "Raw alert_siren.mp3 missing; fallback to Synthetic Medical Siren Track")
            }
        } catch (e: Exception) {
            Log.e("AudioService", "Error during media player setup, starting synthesizer fallback", e)
            startSyntheticSiren()
        }
    }

    private fun startSyntheticSiren() {
        Thread {
            try {
                val sampleRate = 44100
                val numSamples = sampleRate // 1 second buffer
                val buffer = ShortArray(numSamples)
                
                // Dynamic AudioTrack configuration
                syntheticTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    numSamples * 2,
                    AudioTrack.MODE_STATIC
                )

                // High-vis warble math: oscillating frequency sweeps from 900Hz to 1600Hz
                var phase = 0.0
                val lowFreq = 900.0
                val highFreq = 1600.0
                val sweepFreq = 3.5 // 3.5 sweeps per second (extreme alertness)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val modulation = Math.sin(2.0 * Math.PI * sweepFreq * t)
                    val currentFreq = lowFreq + (highFreq - lowFreq) * (0.5 * (modulation + 1.0))

                    phase += 2.0 * Math.PI * currentFreq / sampleRate
                    buffer[i] = (Math.sin(phase) * 32767.0).toInt().toShort()
                }

                syntheticTrack?.let { track ->
                    track.write(buffer, 0, numSamples)
                    track.setLoopPoints(0, numSamples, -1) // loop infinitely
                    track.play()
                }
            } catch (e: Exception) {
                Log.e("AudioService", "Synthetic siren synthesis failed", e)
            }
        }.start()
    }

    fun stopEmergencySiren() {
        if (!isSirenActive) return
        isSirenActive = false

        // Stop vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {}

        // Stop MediaPlayer
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {}

        // Stop Synthetic AudioTrack
        try {
            syntheticTrack?.apply {
                stop()
                release()
            }
            syntheticTrack = null
        } catch (e: Exception) {}

        // Restore original ringer volumes gracefully
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
        } catch (e: Exception) {}
        
        Log.d("AudioService", "Emergency siren silenced; original audio state restored.")
    }
}
