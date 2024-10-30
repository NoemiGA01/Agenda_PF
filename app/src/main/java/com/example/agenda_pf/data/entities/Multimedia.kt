package com.example.agenda_pf.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "multimedia")
data class Multimedia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int, // Relación con la nota
    val filePath: String,
    val description: String
)
