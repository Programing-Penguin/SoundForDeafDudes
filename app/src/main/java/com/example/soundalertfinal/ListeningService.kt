package com.example.soundalertfinal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlin.math.max

class ListeningService : Service() {

    private lateinit var classifier: YamnetClassifier

    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private var running = false

    private var flashEnabled = true
    private var vibeEnabled = true

    // YAMNet expects ~0.975s at 16kHz => 15600 samples
    private val sampleRate = 16000
    private val windowSize = 15600
    private val chunkSize = 1600 // 0.1s chunks read into ring buffer

    private val ring = FloatRingBuffer(windowSize)

    // Debounce alerts
    private var lastAlert = 0L
    private val cooldownMs = 2500L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        classifier = YamnetClassifier(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        flashEnabled = intent?.getBooleanExtra("flash", true) ?: true
        vibeEnabled = intent?.getBooleanExtra("vibe", true) ?: true

        startForeground(1, buildNotification("Listening for: siren, alarm, doorbell, baby…"))

        startListening()
        return START_STICKY
    }

    private fun startListening() {
        if (running) return
        running = true

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBuf, chunkSize * 4)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()

        thread = Thread {
            val pcm = ShortArray(chunkSize)
            while (running) {
                val read = audioRecord?.read(pcm, 0, pcm.size) ?: 0
                if (read > 0) {
                    // Convert to float [-1,1] and push to ring
                    for (i in 0 until read) {
                        ring.push(pcm[i] / 32768f)
                    }

                    // When ring is “full” enough, run inference periodically
                    if (ring.isReady()) {
                        val window = ring.snapshot()

                        val result = classifier.classify(window)
                        val mapped = SoundMapper.map(result)

                        if (mapped != null) maybeAlert(mapped.type, mapped.confidence)
                    }
                }
            }
        }
        thread?.start()
    }

    private fun maybeAlert(type: SoundType, confidence: Float) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAlert < cooldownMs) return
        lastAlert = now

        // Update foreground notif text (judge-friendly)
        val msg = "Detected: ${type.display} (${String.format("%.2f", confidence)})"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, buildNotification(msg))

        val alerts = AlertManager(this)
        if (vibeEnabled) alerts.vibrate(type)
        if (flashEnabled) alerts.flash(type)
    }

    private fun buildNotification(content: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "sound_alert_channel")
            .setContentTitle("SoundAlert is running")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "sound_alert_channel",
                "SoundAlert Background Listening",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        running = false
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        audioRecord?.release()
        audioRecord = null
        thread = null
        classifier.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
