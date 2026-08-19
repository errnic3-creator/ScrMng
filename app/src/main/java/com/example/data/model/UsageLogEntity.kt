package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_logs")
data class UsageLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val eventType: String, // "LOCKDOWN_TRIGGERED", "EMERGENCY_OVERRIDE", "LIMIT_WARNING", "LIMIT_CONFIG_CHANGED"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)
