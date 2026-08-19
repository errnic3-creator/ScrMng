package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null

    private val repository by lazy { (application as ScreenTimeApplication).repository }
    private val settings by lazy { (application as ScreenTimeApplication).settings }

    private var lastObservedForegroundPackage: String? = null
    private var lastLockTriggerTime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CHECK_NOW -> {
                serviceScope.launch {
                    checkForegroundAppLimits()
                }
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                if (settings.isMonitorServiceEnabled && UsageStatsHelper.hasUsageStatsPermission(this@AppMonitorService)) {
                    checkForegroundAppLimits()
                }
                delay(1500L) // Polling interval
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun checkForegroundAppLimits() {
        val currentPackage = UsageStatsHelper.getCurrentForegroundPackage(this) ?: return
        if (currentPackage == packageName) {
            // Our app is in foreground, no action needed
            lastObservedForegroundPackage = currentPackage
            return
        }

        val trackedApp = repository.getTrackedApp(currentPackage) ?: run {
            lastObservedForegroundPackage = currentPackage
            return
        }

        if (!trackedApp.isLimitEnabled) {
            lastObservedForegroundPackage = currentPackage
            return
        }

        val now = System.currentTimeMillis()

        // 1. Check if under emergency override
        if (trackedApp.emergencyOverrideUntilTimestamp > now) {
            // Override active, do not block
            lastObservedForegroundPackage = currentPackage
            return
        }

        // 2. Check if already in active locked state
        if (trackedApp.isLocked && (trackedApp.lockUntilTimestamp > now || trackedApp.lockUntilTimestamp == 0L)) {
            triggerLockdown(trackedApp, trackedApp.lockReason.ifEmpty { "This app is in active lockdown." })
            return
        }

        // 3. Evaluate real-time usage stats
        val usage = UsageStatsHelper.getUsageForPackage(this, currentPackage, trackedApp.openWindowMinutes)
        val screenTimeMinutes = (usage.screenTimeMillisToday / (60 * 1000)).toInt()

        // Check Open Frequency Limit
        if (usage.opensInWindow >= trackedApp.maxOpenCount) {
            val reason = "Open frequency limit reached (${usage.opensInWindow}/${trackedApp.maxOpenCount} opens in ${trackedApp.openWindowMinutes} min)"
            repository.lockApp(currentPackage, reason, settings.autoLockDurationMinutes)
            triggerLockdown(trackedApp, reason)
            return
        }

        // Check Screen Time Limit
        if (screenTimeMinutes >= trackedApp.maxScreenTimeMinutes) {
            val reason = "Daily screen time limit reached (${screenTimeMinutes}/${trackedApp.maxScreenTimeMinutes} min)"
            repository.lockApp(currentPackage, reason, settings.autoLockDurationMinutes)
            triggerLockdown(trackedApp, reason)
            return
        }

        lastObservedForegroundPackage = currentPackage
    }

    private fun triggerLockdown(app: TrackedAppEntity, reason: String) {
        val now = System.currentTimeMillis()
        // Debounce triggers so we don't spam starts
        if (now - lastLockTriggerTime < 2000L && lastObservedForegroundPackage == app.packageName) {
            return
        }
        lastLockTriggerTime = now
        lastObservedForegroundPackage = app.packageName

        // Try launching overlay service if permission is available, or launch fullscreen lockdown activity
        if (UsageStatsHelper.hasOverlayPermission(this)) {
            val overlayIntent = Intent(this, LockdownOverlayService::class.java).apply {
                putExtra(LockdownOverlayService.EXTRA_PACKAGE_NAME, app.packageName)
                putExtra(LockdownOverlayService.EXTRA_APP_NAME, app.appName)
                putExtra(LockdownOverlayService.EXTRA_REASON, reason)
                putExtra(LockdownOverlayService.EXTRA_LOCK_UNTIL, app.lockUntilTimestamp)
            }
            startService(overlayIntent)
        }

        // Also launch LockdownActivity to ensure reliable full-screen capture
        val activityIntent = Intent(this, LockdownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LockdownActivity.EXTRA_PACKAGE_NAME, app.packageName)
            putExtra(LockdownActivity.EXTRA_APP_NAME, app.appName)
            putExtra(LockdownActivity.EXTRA_REASON, reason)
            putExtra(LockdownActivity.EXTRA_LOCK_UNTIL, app.lockUntilTimestamp)
        }
        startActivity(activityIntent)
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ScreenTimeApplication.CHANNEL_MONITOR_SERVICE)
            .setContentTitle("ScreenTime Lock Active")
            .setContentText("Monitoring app open frequency and screen time limits")
            .setSmallIcon(R.drawable.app_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "com.example.action.START_MONITOR"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_MONITOR"
        const val ACTION_CHECK_NOW = "com.example.action.CHECK_NOW"

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun checkNow(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java).apply {
                action = ACTION_CHECK_NOW
            }
            context.startService(intent)
        }
    }
}
