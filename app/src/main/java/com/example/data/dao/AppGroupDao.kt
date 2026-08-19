package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppGroupDao {

    @Query("SELECT * FROM app_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<AppGroupEntity>>

    @Query("SELECT * FROM app_groups ORDER BY name ASC")
    suspend fun getAllGroupsList(): List<AppGroupEntity>

    @Query("SELECT * FROM app_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): AppGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: AppGroupEntity): Long

    @Update
    suspend fun updateGroup(group: AppGroupEntity)

    @Delete
    suspend fun deleteGroup(group: AppGroupEntity)

    @Query("DELETE FROM app_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    @Query("DELETE FROM app_groups")
    suspend fun clearAllGroups()
}
