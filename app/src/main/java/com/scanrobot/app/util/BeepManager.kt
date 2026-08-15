package com.scanrobot.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class BeepManager(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val beepSoundId: Int = generateBeepSound()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun generateBeepSound(): Int {
        val sampleRate = 44100
        val durationMs = 80
        val numSamples = sampleRate * durationMs / 1000
        val freq = 2500.0
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = if (i < numSamples * 0.1) {
                i.toDouble() / (numSamples * 0.1)
            } else if (i > numSamples * 0.7) {
                (numSamples - i).toDouble() / (numSamples * 0.3)
            } else {
                1.0
            }
            val value = (Short.MAX_VALUE * 0.5 * envelope * kotlin.math.sin(2.0 * Math.PI * freq * t)).toInt()
            samples[i] = value.toShort()
        }
        val audioFormat = android.media.AudioFormat.Builder()
            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val pcmBytes = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val s = samples[i]
            pcmBytes[i * 2] = (s.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = (s.toInt() shr 8 and 0xFF).toByte()
        }
        return soundPool.load(pcmBytes, 1, 1, pcmBytes.size, sampleRate, 1)
    }

    fun playBeep(alertType: String) {
        when (alertType) {
            "sound" -> {
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVol == 0) {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2,
                        0
                    )
                }
                soundPool.play(beepSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                vibrate(30)
            }
            "vibrate" -> {
                vibrate(100)
            }
            "none" -> {}
            else -> {
                soundPool.play(beepSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                vibrate(30)
            }
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
        soundPool.release()
    }
}
