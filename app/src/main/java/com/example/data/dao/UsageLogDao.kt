package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {

    @Query("SELECT * FROM usage_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<UsageLogEntity>>

    @Query("SELECT * FROM usage_logs WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getLogsForPackage(packageName: String): Flow<List<UsageLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: UsageLogEntity)

    @Query("DELETE FROM usage_logs")
    suspend fun clearAllLogs()
}
