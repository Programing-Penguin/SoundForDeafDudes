package com.example.soundalertfinal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var toggleBtn: Button
    private lateinit var flashSwitch: Switch
    private lateinit var vibeSwitch: Switch

    private var running = false

    private val req = 2001
    private val perms = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        toggleBtn = findViewById(R.id.toggleBtn)
        flashSwitch = findViewById(R.id.flashSwitch)
        vibeSwitch = findViewById(R.id.vibeSwitch)

        toggleBtn.setOnClickListener {
            if (!hasPerms()) {
                ActivityCompat.requestPermissions(this, perms, req)
                return@setOnClickListener
            }
            if (!running) startSvc() else stopSvc()
        }
    }

    private fun hasPerms(): Boolean =
        perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun startSvc() {
        running = true
        toggleBtn.text = "Stop"
        statusText.text = "Status: Listening (background)"
        val i = Intent(this, ListeningService::class.java).apply {
            putExtra("flash", flashSwitch.isChecked)
            putExtra("vibe", vibeSwitch.isChecked)
        }
        ContextCompat.startForegroundService(this, i)
    }

    private fun stopSvc() {
        running = false
        toggleBtn.text = "Start Background Listening"
        statusText.text = "Status: Idle"
        stopService(Intent(this, ListeningService::class.java))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (
            requestCode == req &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            startSvc()
        } else {
            statusText.text = "Status: Permissions denied"
        }
    }
}
