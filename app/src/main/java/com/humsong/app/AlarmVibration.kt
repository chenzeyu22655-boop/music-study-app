package com.humsong.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

fun Context.alarmVibrate(repeat: Boolean = false) {
    val timings = longArrayOf(0, 420, 180, 420, 180, 420, 220, 760, 500)
    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0)
    val repeatIndex = if (repeat) 1 else -1
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)
            ?.defaultVibrator
            ?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, repeatIndex))
    } else {
        @Suppress("DEPRECATION")
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, repeatIndex))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(timings, repeatIndex)
        }
    }
}

fun Context.cancelAlarmVibration() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator?.cancel()
    } else {
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
    }
}
