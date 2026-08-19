package com.example.data.repository

import android.content.Context
import com.example.data.dao.AppGroupDao
import com.example.data.dao.TrackedAppDao
import com.example.data.dao.UsageLogDao
import com.example.data.model.AppGroupEntity
import com.example.data.model.AppSettings
import com.example.data.model.TrackedAppEntity
import com.example.data.model.UsageLogEntity
import com.example.data.util.GroupUsageSummary
import com.example.data.util.RealtimeAppUsage
import com.example.data.util.SecurityHelper
import com.example.data.util.UsageStatsHelper
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

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
    val usage: GroupUsageSummary,
    val isFrequencyBreached: Boolean,
    val isScreenTimeBreached: Boolean,
    val isScheduleActive: Boolean,
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
            details = "Added to tracking list. Launch limit: ${if (app.isFrequencyLimitEnabled) "${app.maxOpenCount} opens / ${app.openWindowMinutes}m" else "Disabled"}, Screen time: ${if (app.isScreenTimeLimitEnabled) "${app.maxScreenTimeMinutes}m" else "Disabled"}"
        )
    }

    suspend fun updateTrackedApp(app: TrackedAppEntity) {
        trackedAppDao.update(app)
        logEvent(
            packageName = app.packageName,
            appName = app.appName,
            eventType = "LIMIT_CONFIG_CHANGED",
            details = "Limits updated. Launch limit: ${if (app.isFrequencyLimitEnabled) "${app.maxOpenCount} opens / ${app.openWindowMinutes}m" else "Disabled"}, Screen time: ${if (app.isScreenTimeLimitEnabled) "${app.maxScreenTimeMinutes}m" else "Disabled"}"
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
        logEvent(
            packageName = "group_${group.name}",
            appName = group.name,
            eventType = "GROUP_UPDATED",
            details = "Updated app group limits and schedules"
        )
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
     * Uses fixed-interval window-based reset logic.
     */
    fun evaluateAppStatus(app: TrackedAppEntity): TrackedAppWithUsage {
        val now = System.currentTimeMillis()
        val usage = UsageStatsHelper.getUsageForPackage(context, app.packageName, app.openWindowMinutes)

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
     * Checks an app group's live usage and scheduled active window.
     */
    fun evaluateGroupStatus(group: AppGroupEntity): AppGroupWithUsage {
        val now = System.currentTimeMillis()
        val packageList = group.getPackageList()
        val usage = UsageStatsHelper.getGroupUsage(
            context = context,
            groupId = group.id,
            groupName = group.name,
            packageList = packageList,
            windowMinutes = group.openWindowMinutes
        )

        val isUnderOverride = group.emergencyOverrideUntilTimestamp > now
        val overrideRemainingSeconds = if (isUnderOverride) {
            (group.emergencyOverrideUntilTimestamp - now) / 1000L
        } else 0L

        val lockRemainingSeconds = if (group.isLocked && group.lockUntilTimestamp > now) {
            (group.lockUntilTimestamp - now) / 1000L
        } else 0L

        val isFrequencyBreached = group.isEnabled &&
                group.isFrequencyLimitEnabled &&
                (usage.combinedOpensInWindow >= group.maxOpenCount)

        val combinedScreenMinutes = (usage.combinedScreenTimeMillisToday / (60 * 1000)).toInt()
        val isScreenTimeBreached = group.isEnabled &&
                group.isScreenTimeLimitEnabled &&
                (combinedScreenMinutes >= group.maxScreenTimeMinutes)

        // Check if schedule is currently active
        val isScheduleActive = if (group.isEnabled && group.isScheduleEnabled) {
            isTimeInGroupSchedule(group, now)
        } else false

        return AppGroupWithUsage(
            group = group,
            usage = usage,
            isFrequencyBreached = isFrequencyBreached,
            isScreenTimeBreached = isScreenTimeBreached,
            isScheduleActive = isScheduleActive,
            isUnderEmergencyOverride = isUnderOverride,
            overrideRemainingSeconds = overrideRemainingSeconds,
            lockRemainingSeconds = lockRemainingSeconds
        )
    }

    private fun isTimeInGroupSchedule(group: AppGroupEntity, now: Long): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        if (!group.getDaysList().contains(dayOfWeek)) return false

        val currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinuteOfDay = group.startHour * 60 + group.startMinute
        val endMinuteOfDay = group.endHour * 60 + group.endMinute

        return if (startMinuteOfDay <= endMinuteOfDay) {
            currentMinuteOfDay in startMinuteOfDay..endMinuteOfDay
        } else {
            // Over midnight
            currentMinuteOfDay >= startMinuteOfDay || currentMinuteOfDay <= endMinuteOfDay
        }
    }
}
