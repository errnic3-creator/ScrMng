package com.example.data.repository

import android.content.Context
import com.example.data.dao.AppGroupDao
import com.example.data.dao.TrackedAppDao
import com.example.data.dao.UsageLogDao
import com.example.data.model.AppGroupEntity
import com.example.data.model.AppSettings
import com.example.data.model.TrackedAppEntity
import com.example.data.model.UsageLogEntity
import com.example.data.util.RealtimeAppUsage
import com.example.data.util.SecurityHelper
import com.example.data.util.UsageStatsHelper
import com.example.service.FloatingTimerOverlayService
import com.example.service.LockdownOverlayService
import kotlinx.coroutines.flow.Flow

data class TrackedAppWithUsage(
    val entity: TrackedAppEntity,
    val usage: RealtimeAppUsage,
    val isFrequencyBreached: Boolean,
    val isScreenTimeBreached: Boolean,
    val isUnderEmergencyOverride: Boolean,
    val overrideRemainingSeconds: Long,
    val lockRemainingSeconds: Long
)

data class AppGroupWithUsage(
    val group: AppGroupEntity,
    val memberAppUsages: List<TrackedAppWithUsage>,
    val isTodayScheduleActive: Boolean,
    val isAnyMemberLocked: Boolean,
    val isUnderEmergencyOverride: Boolean,
    val overrideRemainingSeconds: Long,
    val lockRemainingSeconds: Long
)

class AppLimitRepository(
    private val context: Context,
    private val trackedAppDao: TrackedAppDao,
    private val appGroupDao: AppGroupDao,
    private val usageLogDao: UsageLogDao,
    val settings: AppSettings
) {
    val allTrackedApps: Flow<List<TrackedAppEntity>> = trackedAppDao.getAllTrackedApps()
    val allGroups: Flow<List<AppGroupEntity>> = appGroupDao.getAllGroups()
    val trackedCount: Flow<Int> = trackedAppDao.getTrackedCount()
    val lockedCount: Flow<Int> = trackedAppDao.getLockedCount()
    val recentLogs: Flow<List<UsageLogEntity>> = usageLogDao.getRecentLogs(60)

    fun getTrackedAppFlow(packageName: String): Flow<TrackedAppEntity?> =
        trackedAppDao.getTrackedAppFlow(packageName)

    suspend fun getTrackedApp(packageName: String): TrackedAppEntity? =
        trackedAppDao.getTrackedApp(packageName)

    suspend fun getAllTrackedAppsList(): List<TrackedAppEntity> =
        trackedAppDao.getAllTrackedAppsList()

    suspend fun getAllGroupsList(): List<AppGroupEntity> =
        appGroupDao.getAllGroupsList()

    suspend fun addTrackedApp(app: TrackedAppEntity) {
        trackedAppDao.insert(app)
        logEvent(
            packageName = app.packageName,
            appName = app.appName,
            eventType = "LIMIT_CONFIG_CHANGED",
            details = "Added to tracking list. Frequency limit: ${if (app.isFrequencyLimitEnabled) "${app.maxOpenCount} opens / ${app.openWindowMinutes}m" else "Disabled"}, Screen time: ${if (app.isScreenTimeLimitEnabled) "${app.maxScreenTimeMinutes}m" else "Disabled"}"
        )
    }

    suspend fun updateTrackedApp(app: TrackedAppEntity) {
        trackedAppDao.update(app)
        logEvent(
            packageName = app.packageName,
            appName = app.appName,
            eventType = "LIMIT_CONFIG_CHANGED",
            details = "Limits updated. Frequency limit: ${if (app.isFrequencyLimitEnabled) "${app.maxOpenCount} opens / ${app.openWindowMinutes}m" else "Disabled"}, Screen time: ${if (app.isScreenTimeLimitEnabled) "${app.maxScreenTimeMinutes}m" else "Disabled"}"
        )
    }

    suspend fun removeTrackedApp(packageName: String) {
        val existing = trackedAppDao.getTrackedApp(packageName)
        trackedAppDao.deleteByPackageName(packageName)
        if (existing != null) {
            logEvent(
                packageName = packageName,
                appName = existing.appName,
                eventType = "LIMIT_CONFIG_CHANGED",
                details = "Removed from tracking list"
            )
        }
    }

    // App Group Operations
    suspend fun addAppGroup(group: AppGroupEntity): Long {
        val id = appGroupDao.insertGroup(group)
        // Ensure each app in the group exists in tracked_apps with individual limits
        syncGroupMemberApps(group)
        logEvent(
            packageName = "group_${group.name}",
            appName = group.name,
            eventType = "GROUP_CREATED",
            details = "Created app group with ${group.getPackageList().size} apps"
        )
        return id
    }

    suspend fun updateAppGroup(group: AppGroupEntity) {
        appGroupDao.updateGroup(group)
        syncGroupMemberApps(group)
        logEvent(
            packageName = "group_${group.name}",
            appName = group.name,
            eventType = "GROUP_UPDATED",
            details = "Updated app group rules"
        )
    }

    private suspend fun syncGroupMemberApps(group: AppGroupEntity) {
        val packages = group.getPackageList()
        val pm = context.packageManager
        for (pkg in packages) {
            val existing = trackedAppDao.getTrackedApp(pkg)
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.substringAfterLast('.')
            }

            if (existing != null) {
                val updated = existing.copy(
                    maxOpenCount = group.maxOpenCount,
                    openWindowMinutes = group.openWindowMinutes,
                    isFrequencyLimitEnabled = group.isFrequencyLimitEnabled,
                    maxScreenTimeMinutes = group.maxScreenTimeMinutes,
                    isScreenTimeLimitEnabled = group.isScreenTimeLimitEnabled
                )
                trackedAppDao.update(updated)
            } else {
                val newApp = TrackedAppEntity(
                    packageName = pkg,
                    appName = appLabel,
                    maxOpenCount = group.maxOpenCount,
                    openWindowMinutes = group.openWindowMinutes,
                    isFrequencyLimitEnabled = group.isFrequencyLimitEnabled,
                    maxScreenTimeMinutes = group.maxScreenTimeMinutes,
                    isScreenTimeLimitEnabled = group.isScreenTimeLimitEnabled,
                    category = group.name,
                    addedTimestamp = System.currentTimeMillis()
                )
                trackedAppDao.insert(newApp)
            }
        }
    }

    suspend fun deleteAppGroup(group: AppGroupEntity) {
        appGroupDao.deleteGroup(group)
        logEvent(
            packageName = "group_${group.name}",
            appName = group.name,
            eventType = "GROUP_DELETED",
            details = "Deleted app group"
        )
    }

    suspend fun lockApp(packageName: String, reason: String, durationMinutes: Int) {
        val existing = trackedAppDao.getTrackedApp(packageName) ?: return
        val lockUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        trackedAppDao.updateLockStatus(
            packageName = packageName,
            isLocked = true,
            reason = reason,
            lockUntil = lockUntil
        )
        logEvent(
            packageName = packageName,
            appName = existing.appName,
            eventType = "LOCKDOWN_TRIGGERED",
            details = "Locked for $durationMinutes min. Reason: $reason"
        )
    }

    suspend fun unlockApp(packageName: String) {
        val existing = trackedAppDao.getTrackedApp(packageName) ?: return
        trackedAppDao.unlockApp(packageName)
        logEvent(
            packageName = packageName,
            appName = existing.appName,
            eventType = "LOCKDOWN_CLEARED",
            details = "Manual unlock granted"
        )
    }

    suspend fun grantEmergencyOverride(packageName: String, durationMinutes: Int = settings.emergencyOverrideDurationMinutes): Boolean {
        val existing = trackedAppDao.getTrackedApp(packageName) ?: return false
        val overrideUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        trackedAppDao.grantEmergencyOverride(packageName, overrideUntil)
        logEvent(
            packageName = packageName,
            appName = existing.appName,
            eventType = "EMERGENCY_OVERRIDE",
            details = "Emergency override granted for $durationMinutes minutes"
        )
        return true
    }

    fun verifyMasterPin(pin: String): Boolean {
        return SecurityHelper.verifyPin(pin, settings.pinHash, settings.pinSalt)
    }

    fun setMasterPin(newPin: String) {
        val salt = SecurityHelper.generateSalt()
        val hash = SecurityHelper.hashPin(newPin, salt)
        settings.pinSalt = salt
        settings.pinHash = hash
        settings.hasPinConfigured = true
    }

    suspend fun wipeAllData() {
        trackedAppDao.clearAll()
        appGroupDao.clearAllGroups()
        usageLogDao.clearAllLogs()
        settings.resetAllPreferences()
        try {
            LockdownOverlayService.dismiss(context)
            FloatingTimerOverlayService.hide(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun logEvent(packageName: String, appName: String, eventType: String, details: String) {
        usageLogDao.insert(
            UsageLogEntity(
                packageName = packageName,
                appName = appName,
                eventType = eventType,
                details = details
            )
        )
    }

    suspend fun clearLogs() {
        usageLogDao.clearAllLogs()
    }

    /**
     * Checks an individual app's live usage against configured limits.
     * Uses track-on-add baseline and fixed-interval window-based reset logic.
     */
    fun evaluateAppStatus(app: TrackedAppEntity): TrackedAppWithUsage {
        val now = System.currentTimeMillis()
        val usage = UsageStatsHelper.getUsageForPackage(
            context = context,
            packageName = app.packageName,
            windowMinutes = app.openWindowMinutes,
            trackedFromTimestamp = app.addedTimestamp
        )

        val isUnderOverride = app.emergencyOverrideUntilTimestamp > now
        val overrideRemainingSeconds = if (isUnderOverride) {
            (app.emergencyOverrideUntilTimestamp - now) / 1000L
        } else 0L

        val isLockedByTimer = app.isLocked && (app.lockUntilTimestamp > now || app.lockUntilTimestamp == 0L)
        val lockRemainingSeconds = if (app.isLocked && app.lockUntilTimestamp > now) {
            (app.lockUntilTimestamp - now) / 1000L
        } else 0L

        // Independent Limit checks
        val isFrequencyBreached = app.isLimitEnabled &&
                app.isFrequencyLimitEnabled &&
                (usage.opensInWindow >= app.maxOpenCount)

        val screenTimeMinutesToday = (usage.screenTimeMillisToday / (60 * 1000)).toInt()
        val isScreenTimeBreached = app.isLimitEnabled &&
                app.isScreenTimeLimitEnabled &&
                (screenTimeMinutesToday >= app.maxScreenTimeMinutes)

        return TrackedAppWithUsage(
            entity = app,
            usage = usage,
            isFrequencyBreached = isFrequencyBreached,
            isScreenTimeBreached = isScreenTimeBreached,
            isUnderEmergencyOverride = isUnderOverride,
            overrideRemainingSeconds = overrideRemainingSeconds,
            lockRemainingSeconds = lockRemainingSeconds
        )
    }

    /**
     * Checks an app group's individual member apps (NO POOLED LIMITS).
     * Rules configured at the group level apply individually to each member app.
     */
    fun evaluateGroupStatus(group: AppGroupEntity): AppGroupWithUsage {
        val now = System.currentTimeMillis()
        val packageList = group.getPackageList()
        val isTodayActive = group.isTodayActive() && group.isEnabled

        val pm = context.packageManager
        val memberUsages = mutableListOf<TrackedAppWithUsage>()

        for (pkg in packageList) {
            val appLabel = try {
                val info = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                pkg.substringAfterLast('.')
            }

            val appEntity = TrackedAppEntity(
                packageName = pkg,
                appName = appLabel,
                maxOpenCount = group.maxOpenCount,
                openWindowMinutes = group.openWindowMinutes,
                isFrequencyLimitEnabled = group.isFrequencyLimitEnabled && isTodayActive,
                maxScreenTimeMinutes = group.maxScreenTimeMinutes,
                isScreenTimeLimitEnabled = group.isScreenTimeLimitEnabled && isTodayActive,
                isLimitEnabled = group.isEnabled && isTodayActive,
                category = group.name
            )
            val usage = evaluateAppStatus(appEntity)
            memberUsages.add(usage)
        }

        val isAnyMemberLocked = memberUsages.any { it.isFrequencyBreached || it.isScreenTimeBreached || it.entity.isLocked }

        val isUnderOverride = group.emergencyOverrideUntilTimestamp > now
        val overrideRemainingSeconds = if (isUnderOverride) {
            (group.emergencyOverrideUntilTimestamp - now) / 1000L
        } else 0L

        val lockRemainingSeconds = if (group.isLocked && group.lockUntilTimestamp > now) {
            (group.lockUntilTimestamp - now) / 1000L
        } else 0L

        return AppGroupWithUsage(
            group = group,
            memberAppUsages = memberUsages,
            isTodayScheduleActive = isTodayActive,
            isAnyMemberLocked = isAnyMemberLocked,
            isUnderEmergencyOverride = isUnderOverride,
            overrideRemainingSeconds = overrideRemainingSeconds,
            lockRemainingSeconds = lockRemainingSeconds
        )
    }
}
