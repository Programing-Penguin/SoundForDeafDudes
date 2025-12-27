package com.example.soundalertfinal

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

class AlertManager(private val ctx: Context) {

    fun vibrate(type: SoundType) {
        val vib = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val pattern = when (type) {
            SoundType.DOORBELL -> longArrayOf(0, 120, 80, 120) // short-short
            SoundType.SIREN -> longArrayOf(0, 400, 150, 400, 150, 400) // long repeats
            SoundType.ALARM -> longArrayOf(0, 120, 70, 120, 70, 250, 70, 250)
            SoundType.BABY -> longArrayOf(0, 200, 120, 200, 120, 200)
            SoundType.HORN -> longArrayOf(0, 180, 80, 180, 80, 180)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, -1)
        }
    }

    fun flash(type: SoundType) {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camId = cm.cameraIdList.firstOrNull() ?: return

        val handler = Handler(Looper.getMainLooper())
        val onOff = when (type) {
            SoundType.SIREN, SoundType.ALARM -> 120L
            else -> 220L
        }

        val blinks = when (type) {
            SoundType.DOORBELL -> 4
            SoundType.BABY -> 6
            SoundType.HORN -> 6
            SoundType.SIREN, SoundType.ALARM -> 8
        }

        for (i in 0 until blinks * 2) {
            val on = i % 2 == 0
            handler.postDelayed({
                try {
                    cm.setTorchMode(camId, on)
                } catch (_: Exception) {
                }
            }, i * onOff)
        }
    }
}
