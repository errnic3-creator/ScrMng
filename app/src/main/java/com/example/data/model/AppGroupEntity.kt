package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_groups")
data class AppGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconEmoji: String = "📱",
    val packageNamesCsv: String = "",
    val isEnabled: Boolean = false,
    val maxOpenCount: Int = 5,
    val openWindowMinutes: Int = 30,
    val isFrequencyLimitEnabled: Boolean = true,
    val maxScreenTimeMinutes: Int = 60,
    val isScreenTimeLimitEnabled: Boolean = true,
    val isScheduleEnabled: Boolean = false,
    val daysOfWeekCsv: String = "1,2,3,4,5,6,7", // 1=Sun, 2=Mon... 7=Sat
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0,
    val isLocked: Boolean = false,
    val lockReason: String = "",
    val lockUntilTimestamp: Long = 0L,
    val emergencyOverrideUntilTimestamp: Long = 0L
) {
    fun getPackageList(): List<String> {
        return if (packageNamesCsv.isBlank()) emptyList()
        else packageNamesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun containsPackage(packageName: String): Boolean {
        return getPackageList().contains(packageName)
    }

    fun getDaysList(): List<Int> {
        return if (daysOfWeekCsv.isBlank()) listOf(1, 2, 3, 4, 5, 6, 7)
        else daysOfWeekCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
