package com.example.agenda_pf.data.repository

import com.example.agenda_pf.data.dao.NoteDao
import com.example.agenda_pf.data.entities.Note
import kotlinx.coroutines.flow.Flow

class OfflineNoteRepository (private val noteDao: NoteDao) : NoteRepository {
    override fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()
    override fun getNoteById(noteId: Int): Flow<Note?> = noteDao.getNoteById(noteId)
    override suspend fun insert(note: Note) = noteDao.insert(note)
    override suspend fun update(note: Note) = noteDao.update(note)
    override suspend fun delete(note: Note) = noteDao.delete(note)
}