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
import com.example.data.model.TrackedAppEntity
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
                            val trackedApp = repository.getTrackedApp(currentPackage)
                            val allGroups = if (settings.isGroupLimitsEnabled) repository.getAllGroupsList() else emptyList()
                            val relevantGroup = allGroups.find { it.isEnabled && it.containsPackage(currentPackage) }

                            var appToEvaluate: TrackedAppEntity? = trackedApp
                            if (appToEvaluate == null && relevantGroup != null && relevantGroup.isTodayActive()) {
                                appToEvaluate = TrackedAppEntity(
                                    packageName = currentPackage,
                                    appName = currentPackage.substringAfterLast('.'),
                                    maxOpenCount = relevantGroup.maxOpenCount,
                                    openWindowMinutes = relevantGroup.openWindowMinutes,
                                    isFrequencyLimitEnabled = relevantGroup.isFrequencyLimitEnabled,
                                    maxScreenTimeMinutes = relevantGroup.maxScreenTimeMinutes,
                                    isScreenTimeLimitEnabled = relevantGroup.isScreenTimeLimitEnabled,
                                    isLimitEnabled = true,
                                    category = relevantGroup.name
                                )
                            }

                            if (appToEvaluate != null && appToEvaluate.isLimitEnabled) {
                                val status = repository.evaluateAppStatus(appToEvaluate)

                                if (status.isUnderEmergencyOverride) {
                                    // Override Session active: bypass limit enforcement and do NOT trigger lockdown
                                    if (settings.isFloatingTimerEnabled) {
                                        FloatingTimerOverlayService.update(
                                            context = this@AppMonitorService,
                                            appName = appToEvaluate.appName,
                                            packageName = appToEvaluate.packageName,
                                            remainingSeconds = status.overrideRemainingSeconds,
                                            isWarning = true
                                        )
                                    }
                                } else if (status.entity.isLocked || status.isFrequencyBreached || status.isScreenTimeBreached) {
                                    val reason = when {
                                        status.isFrequencyBreached ->
                                            "Exceeded ${appToEvaluate.maxOpenCount} opens in ${appToEvaluate.openWindowMinutes}m window"
                                        status.isScreenTimeBreached ->
                                            "Exceeded ${appToEvaluate.maxScreenTimeMinutes}m daily screen time"
                                        else -> status.entity.lockReason.ifEmpty { "Screen time limit breached" }
                                    }

                                    // Preserve fixed window countdown timer without resetting on blocked attempts
                                    val now = System.currentTimeMillis()
                                    val lockUntil = if (appToEvaluate.isLocked && appToEvaluate.lockUntilTimestamp > now) {
                                        appToEvaluate.lockUntilTimestamp
                                    } else {
                                        now + (maxOf(1, appToEvaluate.openWindowMinutes) * 60 * 1000L)
                                    }

                                    if (!appToEvaluate.isLocked || appToEvaluate.lockUntilTimestamp <= now) {
                                        repository.lockAppUntil(
                                            packageName = appToEvaluate.packageName,
                                            reason = reason,
                                            lockUntilTimestamp = lockUntil
                                        )
                                    }

                                    FloatingTimerOverlayService.hide(this@AppMonitorService)
                                    triggerLockdownUi(appToEvaluate.packageName, appToEvaluate.appName, reason, lockUntil)
                                } else {
                                    if (settings.isFloatingTimerEnabled && appToEvaluate.isScreenTimeLimitEnabled) {
                                        val remainingMillis = maxOf(0L, (appToEvaluate.maxScreenTimeMinutes * 60 * 1000L) - status.usage.screenTimeMillisToday)
                                        val remainingSeconds = remainingMillis / 1000L
                                        FloatingTimerOverlayService.update(
                                            context = this@AppMonitorService,
                                            appName = appToEvaluate.appName,
                                            packageName = appToEvaluate.packageName,
                                            remainingSeconds = remainingSeconds,
                                            isWarning = remainingSeconds <= 300L
                                        )
                                    }
                                }
                            } else {
                                FloatingTimerOverlayService.hide(this@AppMonitorService)
                            }
                        } else {
                            FloatingTimerOverlayService.hide(this@AppMonitorService)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(800L) // Responsive polling interval
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

        if (UsageStatsHelper.hasOverlayPermission(this)) {
            LockdownOverlayService.show(this, targetPackageName, appName, reason, lockUntil)
        }

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
