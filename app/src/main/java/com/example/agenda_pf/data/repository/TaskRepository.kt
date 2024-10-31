package com.example.agenda_pf.data.repository

import com.example.agenda_pf.data.entities.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTaskById(taskId: Int): Flow<Task?>
    suspend fun insert(task: Task):Long
    suspend fun update(task: Task): Int
    suspend fun delete(task: Task): Int
}