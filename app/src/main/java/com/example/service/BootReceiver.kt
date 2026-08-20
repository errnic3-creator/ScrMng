package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ScreenTimeApplication

class BootReceiver : BroadcastReceiver {
    constructor() : super()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? ScreenTimeApplication
            if (app?.settings?.isMonitorServiceEnabled == true) {
                AppMonitorService.start(context)
            }
        }
    }
}
