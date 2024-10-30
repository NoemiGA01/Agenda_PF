package com.example.agenda_pf.data.dao

import androidx.room.*
import com.example.agenda_pf.data.entities.Notification

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: Notification): Long

    @Update
    suspend fun update(notification: Notification): Int

    @Delete
    suspend fun delete(notification: Notification): Int

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    suspend fun getAllNotifications(): List<Notification>
}
