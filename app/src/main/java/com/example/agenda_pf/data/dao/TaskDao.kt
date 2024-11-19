package com.example.agenda_pf.data.dao

import androidx.room.*
import com.example.agenda_pf.data.entities.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Update
    suspend fun updateTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task): Int

    @Delete
    suspend fun delete(task: Task): Int

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<Task>>


    // Método para obtener una tarea específica por ID
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Int): Flow<Task?>
}
