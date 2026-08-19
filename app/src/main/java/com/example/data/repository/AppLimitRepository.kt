package com.example.data.repository

import android.content.Context
import com.example.data.dao.TrackedAppDao
import com.example.data.dao.UsageLogDao
import com.example.data.model.AppSettings
import com.example.data.model.TrackedAppEntity
import com.example.data.model.UsageLogEntity
import com.example.data.util.RealtimeAppUsage
import com.example.data.util.SecurityHelper
import com.example.data.util.UsageStatsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TrackedAppWithUsage(
    val entity: TrackedAppEntity,
    val usage: RealtimeAppUsage,
    val isFrequencyBreached: Boolean,
    val isScreenTimeBreached: Boolean,
    val isUnderEmergencyOverride: Boolean,
    val overrideRemainingSeconds: Long,
    val lockRemainingSeconds: Long
)

class AppLimitRepository(
    private val context: Context,
    private val trackedAppDao: TrackedAppDao,
    private val usageLogDao: UsageLogDao,
    val settings: AppSettings
) {
    val allTrackedApps: Flow<List<TrackedAppEntity>> = trackedAppDao.getAllTrackedApps()
    val trackedCount: Flow<Int> = trackedAppDao.getTrackedCount()
    val lockedCount: Flow<Int> = trackedAppDao.getLockedCount()
    val recentLogs: Flow<List<UsageLogEntity>> = usageLogDao.getRecentLogs(50)

    fun getTrackedAppFlow(packageName: String): Flow<TrackedAppEntity?> =
        trackedAppDao.getTrackedAppFlow(packageName)

    suspend fun getTrackedApp(packageName: String): TrackedAppEntity? =
        trackedAppDao.getTrackedApp(packageName)

    suspend fun addTrackedApp(app: TrackedAppEntity) {
        trackedAppDao.insert(app)
        logEvent(
            packageName = app.packageName,
            appName = app.appName,
            eventType = "LIMIT_CONFIG_CHANGED",
            details = "Added to tracking list with limit: ${app.maxOpenCount} opens / ${app.openWindowMinutes}m, Screen time: ${app.maxScreenTimeMinutes}m"
        )
    }

    suspend fun updateTrackedApp(app: TrackedAppEntity) {
        trackedAppDao.update(app)
        logEvent(
            packageName = app.packageName,
            appName = app.appName,
            eventType = "LIMIT_CONFIG_CHANGED",
            details = "Limits updated: ${app.maxOpenCount} opens / ${app.openWindowMinutes}m, Screen time: ${app.maxScreenTimeMinutes}m"
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

    suspend fun lockApp(packageName: String, reason: String, durationMinutes: Int = settings.autoLockDurationMinutes) {
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

        val isFrequencyBreached = app.isLimitEnabled && (usage.opensInWindow >= app.maxOpenCount)
        val screenTimeMinutesToday = usage.screenTimeMillisToday / (60 * 1000)
        val isScreenTimeBreached = app.isLimitEnabled && (screenTimeMinutesToday >= app.maxScreenTimeMinutes)

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
}
