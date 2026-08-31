package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundEffectsManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    /**
     * Plays a cheerful crisp pop / whoosh when an arrow is cleared.
     */
    fun playClearSound(pitchMultiplier: Float = 1.0f) {
        if (!soundEnabled) return
        scope.launch {
            generateTone(
                startFreq = 520f * pitchMultiplier,
                endFreq = 880f * pitchMultiplier,
                durationMs = 90,
                volume = 0.55f,
                waveType = WaveType.SINE
            )
        }
    }

    /**
     * Plays a soft wooden thud / bounce when an arrow is blocked.
     */
    fun playBlockedSound() {
        if (!soundEnabled) return
        scope.launch {
            generateTone(
                startFreq = 180f,
                endFreq = 110f,
                durationMs = 80,
                volume = 0.4f,
                waveType = WaveType.TRIANGLE
            )
        }
    }

    /**
     * Plays a gentle undo chime.
     */
    fun playUndoSound() {
        if (!soundEnabled) return
        scope.launch {
            generateTone(
                startFreq = 440f,
                endFreq = 330f,
                durationMs = 70,
                volume = 0.4f,
                waveType = WaveType.SINE
            )
        }
    }

    /**
     * Plays a sparkling hint chime.
     */
    fun playHintSound() {
        if (!soundEnabled) return
        scope.launch {
            generateTone(startFreq = 660f, endFreq = 990f, durationMs = 60, volume = 0.4f, waveType = WaveType.SINE)
            kotlinx.coroutines.delay(65)
            generateTone(startFreq = 990f, endFreq = 1320f, durationMs = 80, volume = 0.45f, waveType = WaveType.SINE)
        }
    }

    /**
     * Plays a 3-note victory arpeggio on puzzle complete.
     */
    fun playVictoryFanfare() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
            for (freq in notes) {
                generateTone(startFreq = freq, endFreq = freq * 1.02f, durationMs = 120, volume = 0.5f, waveType = WaveType.SINE)
                kotlinx.coroutines.delay(100)
            }
        }
    }

    fun vibrateSuccess(view: View? = null) {
        if (!hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.let { v ->
                    if (v.hasVibrator()) {
                        v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } catch (_: Throwable) {
            // Graceful fallback if device lacks hardware vibrator permission or capability
        }
    }

    fun vibrateBlocked(view: View? = null) {
        if (!hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.let { v ->
                    if (v.hasVibrator()) {
                        val pattern = longArrayOf(0, 20, 30, 20)
                        v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    }
                }
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
            }
        } catch (_: Throwable) {
            // Graceful fallback
        }
    }

    fun vibrateVictory() {
        if (!hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.let { v ->
                    if (v.hasVibrator()) {
                        val pattern = longArrayOf(0, 40, 60, 40, 60, 80)
                        v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    }
                }
            }
        } catch (_: Throwable) {
            // Graceful fallback
        }
    }

    private enum class WaveType { SINE, TRIANGLE }

    private fun generateTone(
        startFreq: Float,
        endFreq: Float,
        durationMs: Int,
        volume: Float,
        waveType: WaveType
    ) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            var currentPhase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                // Smooth envelope (attack + decay)
                val envelope = when {
                    progress < 0.1f -> progress / 0.1f
                    else -> (1.0f - progress)
                }

                val currentFreq = startFreq + (endFreq - startFreq) * progress
                val phaseIncrement = (2.0 * Math.PI * currentFreq) / sampleRate
                currentPhase += phaseIncrement

                val sampleVal = when (waveType) {
                    WaveType.SINE -> sin(currentPhase)
                    WaveType.TRIANGLE -> {
                        val p = (currentPhase % (2.0 * Math.PI)) / (2.0 * Math.PI)
                        if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)
                    }
                }

                val shortVal = (sampleVal * Short.MAX_VALUE * volume * envelope).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                buffer[i] = shortVal
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            scope.launch {
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            // Audio generation failure fallback gracefully
        }
    }
}
