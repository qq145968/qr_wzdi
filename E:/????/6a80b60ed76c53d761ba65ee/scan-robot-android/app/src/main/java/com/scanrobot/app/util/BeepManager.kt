package com.scanrobot.app.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context

class BeepManager(context: Context) {

    private val toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    } catch (e: Exception) {
        null
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun playBeep(alertType: String) {
        when (alertType) {
            "sound" -> {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                vibrate(30)
            }
            "vibrate" -> {
                vibrate(100)
            }
            "none" -> {}
            else -> {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
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
        toneGenerator?.release()
    }
}
