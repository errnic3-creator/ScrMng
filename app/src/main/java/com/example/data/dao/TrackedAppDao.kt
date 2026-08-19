package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TrackedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedAppDao {

    @Query("SELECT * FROM tracked_apps ORDER BY appName ASC")
    fun getAllTrackedApps(): Flow<List<TrackedAppEntity>>

    @Query("SELECT * FROM tracked_apps")
    suspend fun getAllTrackedAppsList(): List<TrackedAppEntity>

    @Query("SELECT * FROM tracked_apps WHERE packageName = :packageName LIMIT 1")
    fun getTrackedAppFlow(packageName: String): Flow<TrackedAppEntity?>

    @Query("SELECT * FROM tracked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getTrackedApp(packageName: String): TrackedAppEntity?

    @Query("SELECT COUNT(*) FROM tracked_apps")
    fun getTrackedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tracked_apps WHERE isLocked = 1")
    fun getLockedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: TrackedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<TrackedAppEntity>)

    @Update
    suspend fun update(app: TrackedAppEntity)

    @Delete
    suspend fun delete(app: TrackedAppEntity)

    @Query("DELETE FROM tracked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("UPDATE tracked_apps SET isLocked = :isLocked, lockReason = :reason, lockUntilTimestamp = :lockUntil WHERE packageName = :packageName")
    suspend fun updateLockStatus(packageName: String, isLocked: Boolean, reason: String, lockUntil: Long)

    @Query("UPDATE tracked_apps SET emergencyOverrideUntilTimestamp = :overrideUntil, isLocked = 0 WHERE packageName = :packageName")
    suspend fun grantEmergencyOverride(packageName: String, overrideUntil: Long)

    @Query("UPDATE tracked_apps SET isLocked = 0, lockReason = '', lockUntilTimestamp = 0 WHERE packageName = :packageName")
    suspend fun unlockApp(packageName: String)
}
