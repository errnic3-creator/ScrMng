package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ScreenTimeApplication

class BootReceiver : BroadcastReceiver {
    constructor() : super()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? ScreenTimeApplication
            if (app?.settings?.isMonitorServiceEnabled == true) {
                AppMonitorService.start(context)
            }
        }
    }
}
