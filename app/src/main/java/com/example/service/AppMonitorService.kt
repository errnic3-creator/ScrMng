package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ScreenTimeApplication
import com.example.data.util.UsageStatsHelper
import com.example.ui.lockdown.LockdownActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var isMonitoring = false
    private var lastHandledPackage: String? = null
    private var lastNotificationTime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isMonitoring) {
            isMonitoring = true
            startMonitoringLoop()
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, ScreenTimeApplication.CHANNEL_MONITOR_SERVICE)
            .setContentTitle("ScrMngr Limit Guard")
            .setContentText("Monitoring app launch frequency & screen time")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            val app = application as ScreenTimeApplication
            val repository = app.repository
            val settings = app.settings

            while (isActive && isMonitoring) {
                try {
                    if (settings.isMonitorServiceEnabled && UsageStatsHelper.hasUsageStatsPermission(this@AppMonitorService)) {
                        val currentPackage = UsageStatsHelper.getCurrentForegroundPackage(this@AppMonitorService)

                        if (currentPackage != null && currentPackage != packageName) {
                            // Check individual tracked app
                            val trackedApp = repository.getTrackedApp(currentPackage)
                            val allGroups = if (settings.isGroupLimitsEnabled) repository.getAllGroupsList() else emptyList()
                            val relevantGroup = allGroups.find { it.isEnabled && it.containsPackage(currentPackage) }

                            if (trackedApp != null && trackedApp.isLimitEnabled) {
                                val status = repository.evaluateAppStatus(trackedApp)

                                if (status.isUnderEmergencyOverride) {
                                    // Allowed under emergency override
                                    if (settings.isFloatingTimerEnabled) {
                                        val remainingMin = (status.overrideRemainingSeconds / 60).toInt()
                                        FloatingTimerOverlayService.update(
                                            context = this@AppMonitorService,
                                            appName = trackedApp.appName,
                                            packageName = trackedApp.packageName,
                                            remainingMinutes = remainingMin,
                                            isWarning = true
                                        )
                                    }
                                } else if (status.entity.isLocked || status.isFrequencyBreached || status.isScreenTimeBreached) {
                                    // Trigger dynamic lockdown
                                    val reason = when {
                                        status.isFrequencyBreached ->
                                            "Exceeded ${trackedApp.maxOpenCount} opens in ${trackedApp.openWindowMinutes}m window"
                                        status.isScreenTimeBreached ->
                                            "Exceeded ${trackedApp.maxScreenTimeMinutes}m daily screen time"
                                        else -> status.entity.lockReason.ifEmpty { "Screen time limit breached" }
                                    }

                                    // Dynamic lockdown duration rule:
                                    // Frequency breach -> openWindowMinutes
                                    // Screen time breach -> settings.autoLockDurationMinutes
                                    val lockDuration = if (status.isFrequencyBreached) {
                                        trackedApp.openWindowMinutes
                                    } else {
                                        settings.autoLockDurationMinutes
                                    }

                                    if (!trackedApp.isLocked) {
                                        repository.lockApp(
                                            packageName = trackedApp.packageName,
                                            reason = reason,
                                            durationMinutes = lockDuration
                                        )
                                    }

                                    FloatingTimerOverlayService.hide(this@AppMonitorService)
                                    triggerLockdownUi(trackedApp.packageName, trackedApp.appName, reason, trackedApp.lockUntilTimestamp)
                                } else {
                                    // Normal allowed usage
                                    if (settings.isFloatingTimerEnabled && trackedApp.isScreenTimeLimitEnabled) {
                                        val screenTimeMinutes = (status.usage.screenTimeMillisToday / (60 * 1000)).toInt()
                                        val remainingMinutes = maxOf(0, trackedApp.maxScreenTimeMinutes - screenTimeMinutes)
                                        FloatingTimerOverlayService.update(
                                            context = this@AppMonitorService,
                                            appName = trackedApp.appName,
                                            packageName = trackedApp.packageName,
                                            remainingMinutes = remainingMinutes,
                                            isWarning = remainingMinutes <= 5
                                        )
                                    }
                                }
                            } else if (relevantGroup != null) {
                                // Evaluate group status
                                val groupStatus = repository.evaluateGroupStatus(relevantGroup)
                                if (!groupStatus.isUnderEmergencyOverride &&
                                    (relevantGroup.isLocked || groupStatus.isFrequencyBreached || groupStatus.isScreenTimeBreached || groupStatus.isScheduleActive)) {

                                    val reason = when {
                                        groupStatus.isScheduleActive -> "Active schedule restriction for group: ${relevantGroup.name}"
                                        groupStatus.isFrequencyBreached -> "Group limit: Exceeded ${relevantGroup.maxOpenCount} opens"
                                        groupStatus.isScreenTimeBreached -> "Group limit: Exceeded ${relevantGroup.maxScreenTimeMinutes}m screen time"
                                        else -> relevantGroup.lockReason.ifEmpty { "Group limit reached" }
                                    }

                                    FloatingTimerOverlayService.hide(this@AppMonitorService)
                                    triggerLockdownUi(currentPackage, relevantGroup.name, reason, relevantGroup.lockUntilTimestamp)
                                }
                            } else {
                                // Not a monitored app
                                FloatingTimerOverlayService.hide(this@AppMonitorService)
                            }
                        } else {
                            // Home / launcher / self
                            FloatingTimerOverlayService.hide(this@AppMonitorService)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(1500L) // Polling interval
            }
        }
    }

    private fun triggerLockdownUi(
        targetPackageName: String,
        appName: String,
        reason: String,
        lockUntil: Long
    ) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime < 1000L && lastHandledPackage == targetPackageName) {
            return
        }
        lastNotificationTime = now
        lastHandledPackage = targetPackageName

        // If overlay permission granted, launch overlay
        if (UsageStatsHelper.hasOverlayPermission(this)) {
            LockdownOverlayService.show(this, targetPackageName, appName, reason, lockUntil)
        }

        // Also launch LockdownActivity
        val lockIntent = Intent(this, LockdownActivity::class.java).apply {
            putExtra(LockdownActivity.EXTRA_PACKAGE_NAME, targetPackageName)
            putExtra(LockdownActivity.EXTRA_APP_NAME, appName)
            putExtra(LockdownActivity.EXTRA_REASON, reason)
            putExtra(LockdownActivity.EXTRA_LOCK_UNTIL, lockUntil)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        try {
            startActivity(lockIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        serviceJob.cancel()
        FloatingTimerOverlayService.hide(this)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            context.stopService(intent)
            FloatingTimerOverlayService.hide(context)
        }
    }
}
