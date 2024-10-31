package com.example.agenda_pf.data.repository

import com.example.agenda_pf.data.entities.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getNoteById(noteId: Int): Flow<Note?>
    suspend fun insert(note: Note): Long
    suspend fun update(note: Note): Int
    suspend fun delete(note: Note): Int
}