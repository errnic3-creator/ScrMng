package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScreenTimeApplication
import com.example.data.model.TrackedAppEntity
import com.example.data.model.UsageLogEntity
import com.example.data.repository.TrackedAppWithUsage
import com.example.data.util.InstalledAppInfo
import com.example.data.util.UsageStatsHelper
import com.example.service.AppMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ScreenTimeApplication).repository
    private val settings = (application as ScreenTimeApplication).settings
    private val context: Context get() = getApplication<Application>().applicationContext

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingInstalledApps = MutableStateFlow(false)
    val isLoadingInstalledApps: StateFlow<Boolean> = _isLoadingInstalledApps.asStateFlow()

    private val _isUsagePermissionGranted = MutableStateFlow(UsageStatsHelper.hasUsageStatsPermission(context))
    val isUsagePermissionGranted: StateFlow<Boolean> = _isUsagePermissionGranted.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(UsageStatsHelper.hasOverlayPermission(context))
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    private val _hasMasterPin = MutableStateFlow(settings.hasPinConfigured)
    val hasMasterPin: StateFlow<Boolean> = _hasMasterPin.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(settings.isMonitorServiceEnabled)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedAppForEdit = MutableStateFlow<TrackedAppEntity?>(null)
    val selectedAppForEdit: StateFlow<TrackedAppEntity?> = _selectedAppForEdit.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    // Real-time evaluation ticker
    private val _ticker = MutableStateFlow(System.currentTimeMillis())

    val trackedApps: StateFlow<List<TrackedAppWithUsage>> = combine(
        repository.allTrackedApps,
        _ticker
    ) { apps, _ ->
        apps.map { repository.evaluateAppStatus(it) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentLogs: StateFlow<List<UsageLogEntity>> = repository.recentLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadInstalledApps()
        startLiveRefreshLoop()
        if (settings.isMonitorServiceEnabled && UsageStatsHelper.hasUsageStatsPermission(context)) {
            AppMonitorService.start(context)
        }
    }

    private fun startLiveRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                _ticker.value = System.currentTimeMillis()
                checkPermissions()
                delay(1000L)
            }
        }
    }

    fun checkPermissions() {
        _isUsagePermissionGranted.value = UsageStatsHelper.hasUsageStatsPermission(context)
        _isOverlayPermissionGranted.value = UsageStatsHelper.hasOverlayPermission(context)
        _hasMasterPin.value = settings.hasPinConfigured
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingInstalledApps.value = true
            val apps = withContext(Dispatchers.IO) {
                UsageStatsHelper.getInstalledLaunchableApps(context)
            }
            _installedApps.value = apps
            _isLoadingInstalledApps.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedAppForEdit(app: TrackedAppEntity?) {
        _selectedAppForEdit.value = app
    }

    fun addTrackedApp(
        appInfo: InstalledAppInfo,
        maxOpenCount: Int = 3,
        openWindowMinutes: Int = 30,
        maxScreenTimeMinutes: Int = 45
    ) {
        viewModelScope.launch {
            val entity = TrackedAppEntity(
                packageName = appInfo.packageName,
                appName = appInfo.appName,
                maxOpenCount = maxOpenCount,
                openWindowMinutes = openWindowMinutes,
                maxScreenTimeMinutes = maxScreenTimeMinutes,
                category = appInfo.category
            )
            repository.addTrackedApp(entity)
            showFeedback("Added ${appInfo.appName} to monitoring list")
        }
    }

    fun updateTrackedApp(
        packageName: String,
        appName: String,
        maxOpenCount: Int,
        openWindowMinutes: Int,
        maxScreenTimeMinutes: Int,
        isLimitEnabled: Boolean,
        category: String
    ) {
        viewModelScope.launch {
            val existing = repository.getTrackedApp(packageName)
            val updated = existing?.copy(
                maxOpenCount = maxOpenCount,
                openWindowMinutes = openWindowMinutes,
                maxScreenTimeMinutes = maxScreenTimeMinutes,
                isLimitEnabled = isLimitEnabled,
                category = category
            ) ?: TrackedAppEntity(
                packageName = packageName,
                appName = appName,
                maxOpenCount = maxOpenCount,
                openWindowMinutes = openWindowMinutes,
                maxScreenTimeMinutes = maxScreenTimeMinutes,
                isLimitEnabled = isLimitEnabled,
                category = category
            )
            repository.updateTrackedApp(updated)
            showFeedback("Updated limits for $appName")
        }
    }

    fun removeTrackedApp(packageName: String) {
        viewModelScope.launch {
            repository.removeTrackedApp(packageName)
            showFeedback("Removed app from tracking")
        }
    }

    fun setMasterPin(pin: String) {
        repository.setMasterPin(pin)
        _hasMasterPin.value = true
        showFeedback("Master Security PIN configured successfully")
    }

    fun verifyMasterPin(pin: String): Boolean {
        return repository.verifyMasterPin(pin)
    }

    fun grantEmergencyOverride(packageName: String, durationMinutes: Int = settings.emergencyOverrideDurationMinutes): Boolean {
        var success = false
        viewModelScope.launch {
            success = repository.grantEmergencyOverride(packageName, durationMinutes)
            _ticker.value = System.currentTimeMillis()
            showFeedback("Emergency override granted for $durationMinutes min")
        }
        return true
    }

    fun manualLockApp(packageName: String, reason: String = "Manual Lockdown") {
        viewModelScope.launch {
            repository.lockApp(packageName, reason, settings.autoLockDurationMinutes)
            _ticker.value = System.currentTimeMillis()
            showFeedback("App placed in lockdown")
        }
    }

    fun manualUnlockApp(packageName: String) {
        viewModelScope.launch {
            repository.unlockApp(packageName)
            _ticker.value = System.currentTimeMillis()
            showFeedback("App unlocked")
        }
    }

    fun toggleMonitoringService(enabled: Boolean) {
        settings.isMonitorServiceEnabled = enabled
        _isServiceRunning.value = enabled
        if (enabled) {
            AppMonitorService.start(context)
            showFeedback("Monitoring service activated")
        } else {
            AppMonitorService.stop(context)
            showFeedback("Monitoring service paused")
        }
    }

    fun updateSettings(overrideMin: Int, autoLockMin: Int) {
        settings.emergencyOverrideDurationMinutes = overrideMin
        settings.autoLockDurationMinutes = autoLockMin
        showFeedback("Preferences updated")
    }

    fun getOverrideDuration(): Int = settings.emergencyOverrideDurationMinutes
    fun getAutoLockDuration(): Int = settings.autoLockDurationMinutes

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            showFeedback("Activity logs cleared")
        }
    }

    fun simulateTriggerLock(app: TrackedAppEntity) {
        viewModelScope.launch {
            repository.lockApp(
                packageName = app.packageName,
                reason = "Simulated Breach: Exceeded ${app.maxOpenCount} opens in ${app.openWindowMinutes} min",
                durationMinutes = settings.autoLockDurationMinutes
            )
            _ticker.value = System.currentTimeMillis()
            showFeedback("Simulated lockdown triggered for ${app.appName}")
        }
    }

    private fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
