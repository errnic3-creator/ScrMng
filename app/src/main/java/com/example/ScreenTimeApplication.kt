package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.repository.AppLimitRepository

class ScreenTimeApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settings: AppSettings
        private set

    lateinit var repository: AppLimitRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        settings = AppSettings(this)
        repository = AppLimitRepository(
            context = this,
            trackedAppDao = database.trackedAppDao(),
            appGroupDao = database.appGroupDao(),
            usageLogDao = database.usageLogDao(),
            settings = settings
        )

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_SERVICE,
                "ScrMngr Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active background app limit tracking status"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_LOCKDOWN_ALERTS,
                "Lockdown & Limit Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when an app breaches open or screen time limits"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    companion object {
        const val CHANNEL_MONITOR_SERVICE = "channel_monitor_service"
        const val CHANNEL_LOCKDOWN_ALERTS = "channel_lockdown_alerts"

        lateinit var instance: ScreenTimeApplication
            private set
    }
}
