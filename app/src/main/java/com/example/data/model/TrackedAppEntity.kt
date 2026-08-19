package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_apps")
data class TrackedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val maxOpenCount: Int = 3,
    val openWindowMinutes: Int = 30,
    val maxScreenTimeMinutes: Int = 45,
    val isLimitEnabled: Boolean = true,
    val isLocked: Boolean = false,
    val lockReason: String = "",
    val lockUntilTimestamp: Long = 0L,
    val emergencyOverrideUntilTimestamp: Long = 0L,
    val category: String = "Other",
    val addedTimestamp: Long = System.currentTimeMillis()
)
