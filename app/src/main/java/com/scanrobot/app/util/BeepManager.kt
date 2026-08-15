package com.scanrobot.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class BeepManager(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val beepPcm: ByteArray = generateBeepPcm()

    private fun generateBeepPcm(): ByteArray {
        val sampleRate = 44100
        val durationMs = 80
        val numSamples = sampleRate * durationMs / 1000
        val freq = 2500.0
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = when {
                i < numSamples * 0.1 -> i.toDouble() / (numSamples * 0.1)
                i > numSamples * 0.7 -> (numSamples - i).toDouble() / (numSamples * 0.3)
                else -> 1.0
            }
            val value = (Short.MAX_VALUE * 0.5 * envelope * kotlin.math.sin(2.0 * Math.PI * freq * t)).toInt()
            samples[i] = value.toShort()
        }
        val pcmBytes = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val s = samples[i]
            pcmBytes[i * 2] = (s.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = (s.toInt() shr 8 and 0xFF).toByte()
        }
        return pcmBytes
    }

    fun playBeep(alertType: String) {
        when (alertType) {
            "sound" -> {
                playBeepSound()
                vibrate(30)
            }
            "vibrate" -> {
                vibrate(100)
            }
            "none" -> {}
            else -> {
                playBeepSound()
                vibrate(30)
            }
        }
    }

    private fun playBeepSound() {
        try {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
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
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            audioTrack.write(beepPcm, 0, beepPcm.size)
            audioTrack.play()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                audioTrack.release()
            }, 200)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate(durationMs: Long) {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durationMs)
        }
    }

    fun release() {
    }
}
