package com.example.agenda_pf.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val dueDate: Long,
    val reminderDate: Long? = null,
    val isCompleted: Boolean = false // Campo para marcar la tarea como completada
)
