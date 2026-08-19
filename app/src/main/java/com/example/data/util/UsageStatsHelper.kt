package com.example.data.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean = false,
    val category: String = "Other"
)

data class RealtimeAppUsage(
    val packageName: String,
    val opensInWindow: Int,
    val screenTimeMillisToday: Long,
    val screenTimeMillisInWindow: Long,
    val lastForegroundTimestamp: Long
)

object UsageStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun getUsageStatsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getOverlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getInstalledLaunchableApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val appList = mutableListOf<InstalledAppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) continue // Skip self
            if (seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val appName = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val category = categorizeApp(pkg, appName)

            appList.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = appName,
                    icon = icon,
                    isSystemApp = isSystem,
                    category = category
                )
            )
        }

        return appList.sortedBy { it.appName.lowercase() }
    }

    private fun categorizeApp(packageName: String, appName: String): String {
        val lowerPkg = packageName.lowercase()
        val lowerName = appName.lowercase()

        return when {
            lowerPkg.contains("instagram") || lowerPkg.contains("twitter") || lowerPkg.contains("x.corp") ||
            lowerPkg.contains("tiktok") || lowerPkg.contains("facebook") || lowerPkg.contains("reddit") ||
            lowerPkg.contains("snapchat") || lowerPkg.contains("social") || lowerName.contains("social") -> "Social"

            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("spotify") ||
            lowerPkg.contains("twitch") || lowerPkg.contains("media") || lowerPkg.contains("video") ||
            lowerPkg.contains("music") -> "Entertainment"

            lowerPkg.contains("game") || lowerPkg.contains("play") || lowerName.contains("game") -> "Games"

            lowerPkg.contains("mail") || lowerPkg.contains("slack") || lowerPkg.contains("notion") ||
            lowerPkg.contains("docs") || lowerPkg.contains("office") || lowerPkg.contains("trello") ||
            lowerPkg.contains("chrome") || lowerPkg.contains("browser") -> "Productivity"

            else -> "Other"
        }
    }

    /**
     * Queries UsageEvents to determine the current active foreground package and event counts.
     */
    fun getCurrentForegroundPackage(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 5 // Check last 5 minutes

        val usageEvents = usm.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null
        var lastTimestamp = 0L

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > lastTimestamp) {
                    lastTimestamp = event.timeStamp
                    lastForegroundPackage = event.packageName
                }
            }
        }

        return lastForegroundPackage
    }

    /**
     * Calculates open frequency in the past `windowMinutes` and screen time today for a package.
     */
    fun getUsageForPackage(
        context: Context,
        packageName: String,
        windowMinutes: Int
    ): RealtimeAppUsage {
        if (!hasUsageStatsPermission(context)) {
            return RealtimeAppUsage(
                packageName = packageName,
                opensInWindow = 0,
                screenTimeMillisToday = 0L,
                screenTimeMillisInWindow = 0L,
                lastForegroundTimestamp = 0L
            )
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return RealtimeAppUsage(packageName, 0, 0L, 0L, 0L)

        val now = System.currentTimeMillis()
        val windowStart = now - (windowMinutes * 60 * 1000L)

        // Today start (midnight)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis

        var opensInWindow = 0
        var lastForeground = 0L

        // Query events from earliest start
        val queryStart = minOf(windowStart, todayStart)
        val usageEvents = usm.queryEvents(queryStart, now)
        val event = UsageEvents.Event()

        var currentForegroundStart: Long? = null
        var totalTodayScreenTime = 0L
        var totalWindowScreenTime = 0L

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.packageName == packageName) {
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        lastForeground = event.timeStamp
                        if (event.timeStamp >= windowStart) {
                            opensInWindow++
                        }
                        currentForegroundStart = event.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        currentForegroundStart?.let { start ->
                            val duration = event.timeStamp - start
                            if (start >= todayStart) {
                                totalTodayScreenTime += duration
                            }
                            if (start >= windowStart) {
                                totalWindowScreenTime += duration
                            }
                        }
                        currentForegroundStart = null
                    }
                }
            }
        }

        // If currently in foreground
        currentForegroundStart?.let { start ->
            val duration = now - start
            if (start >= todayStart) {
                totalTodayScreenTime += duration
            }
            if (start >= windowStart) {
                totalWindowScreenTime += duration
            }
        }

        // Fallback or augment with queryUsageStats for daily total if events are truncated
        if (totalTodayScreenTime == 0L) {
            val statsList = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                todayStart,
                now
            )
            val appStats = statsList?.find { it.packageName == packageName }
            if (appStats != null) {
                totalTodayScreenTime = appStats.totalTimeInForeground
            }
        }

        return RealtimeAppUsage(
            packageName = packageName,
            opensInWindow = opensInWindow,
            screenTimeMillisToday = totalTodayScreenTime,
            screenTimeMillisInWindow = totalWindowScreenTime,
            lastForegroundTimestamp = lastForeground
        )
    }
}
