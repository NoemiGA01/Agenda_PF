package com.example.agenda_pf.data.repository

import com.example.agenda_pf.data.dao.TaskDao
import com.example.agenda_pf.data.entities.Task
import kotlinx.coroutines.flow.Flow

class OfflineTaskRepository (private val taskDao: TaskDao) : TaskRepository {
    override fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    override fun getTaskById(taskId: Int): Flow<Task?> = taskDao.getTaskById(taskId)
    override suspend fun insert(task: Task) = taskDao.insert(task)
    override suspend fun update(task: Task) = taskDao.update(task)
    override suspend fun delete(task: Task) = taskDao.delete(task)
}