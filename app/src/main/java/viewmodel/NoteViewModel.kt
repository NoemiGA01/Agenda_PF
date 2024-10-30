package com.example.agenda_pf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agenda_pf.data.dao.NoteDao
import com.example.agenda_pf.data.entities.Note
import com.example.agenda_pf.data.entities.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NoteViewModel(private val NoteDao: NoteDao) : ViewModel() {

    val allNotes: Flow<List<Note>> = NoteDao.getAllNotes()

    fun addNote(note: Note) {
        viewModelScope.launch {
            NoteDao.insert(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            NoteDao.update(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            NoteDao.delete(note)
        }
    }


    //Método para obtener una tarea específica por ID
    fun getNoteById(noteId: Int): Flow<Note?> {
        return NoteDao.getNoteById(noteId)
    }
}