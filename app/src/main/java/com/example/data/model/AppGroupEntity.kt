package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "app_groups")
data class AppGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconEmoji: String = "📱",
    val packageNamesCsv: String = "",
    val isEnabled: Boolean = true,
    val maxOpenCount: Int = 5,
    val openWindowMinutes: Int = 30,
    val isFrequencyLimitEnabled: Boolean = true,
    val maxScreenTimeMinutes: Int = 45,
    val isScreenTimeLimitEnabled: Boolean = true,
    val isScheduleEnabled: Boolean = true,
    val daysOfWeekCsv: String = "1,2,3,4,5,6,7", // 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
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

    fun isTodayActive(): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK)
        return getDaysList().contains(today)
    }

    fun getActiveDaysSummary(): String {
        val days = getDaysList()
        if (days.size == 7) return "Active: All Days"
        if (days.containsAll(listOf(2, 3, 4, 5, 6)) && days.size == 5) return "Active: Mon–Fri"
        if (days.containsAll(listOf(1, 7)) && days.size == 2) return "Active: Weekends"

        val dayNames = mapOf(
            2 to "Mon", 3 to "Tue", 4 to "Wed",
            5 to "Thu", 6 to "Fri", 7 to "Sat", 1 to "Sun"
        )
        // Ordered Mon -> Sun
        val sortedDays = listOf(2, 3, 4, 5, 6, 7, 1).filter { days.contains(it) }
        return "Active: " + sortedDays.joinToString(", ") { dayNames[it] ?: "" }
    }
}
