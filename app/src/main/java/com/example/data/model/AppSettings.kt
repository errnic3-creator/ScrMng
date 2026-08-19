package com.example.data.model

import android.content.Context
import android.content.SharedPreferences

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("screentime_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_HAS_PIN = "key_has_pin"
        private const val KEY_OVERRIDE_DURATION_MINUTES = "key_override_duration_min"
        private const val KEY_AUTO_LOCK_DURATION_MINUTES = "key_auto_lock_duration_min"
        private const val KEY_MONITOR_SERVICE_ENABLED = "key_monitor_service_enabled"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_SELECTED_THEME = "key_selected_theme"
        private const val KEY_FLOATING_TIMER_ENABLED = "key_floating_timer_enabled"
        private const val KEY_GROUP_LIMITS_ENABLED = "key_group_limits_enabled"
    }

    var pinHash: String
        get() = prefs.getString(KEY_PIN_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    var pinSalt: String
        get() = prefs.getString(KEY_PIN_SALT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PIN_SALT, value).apply()

    var hasPinConfigured: Boolean
        get() = prefs.getBoolean(KEY_HAS_PIN, false) && pinHash.isNotEmpty()
        set(value) = prefs.edit().putBoolean(KEY_HAS_PIN, value).apply()

    var emergencyOverrideDurationMinutes: Int
        get() = prefs.getInt(KEY_OVERRIDE_DURATION_MINUTES, 5) // default 5 minutes
        set(value) = prefs.edit().putInt(KEY_OVERRIDE_DURATION_MINUTES, value).apply()

    var autoLockDurationMinutes: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_DURATION_MINUTES, 30) // default 30 minutes
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_DURATION_MINUTES, value).apply()

    var isMonitorServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONITOR_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITOR_SERVICE_ENABLED, value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var selectedTheme: String
        get() = prefs.getString(KEY_SELECTED_THEME, "indigo") ?: "indigo"
        set(value) = prefs.edit().putString(KEY_SELECTED_THEME, value).apply()

    var isFloatingTimerEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_TIMER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_TIMER_ENABLED, value).apply()

    var isGroupLimitsEnabled: Boolean
        get() = prefs.getBoolean(KEY_GROUP_LIMITS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GROUP_LIMITS_ENABLED, value).apply()

    fun resetAllPreferences() {
        prefs.edit().clear().apply()
    }
}
