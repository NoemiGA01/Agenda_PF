package com.example.agenda_pf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agenda_pf.data.entities.Task
import com.example.agenda_pf.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    val allTasks: Flow<List<Task>> = repository.getAllTasks()

    fun addTask(task: Task) {
        viewModelScope.launch {
            repository.insert(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.update(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }



    fun getTaskById(taskId: Int): Flow<Task?> = repository.getTaskById(taskId)
}