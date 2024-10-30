package com.example.agenda_pf.data.dao

import androidx.room.*
import com.example.agenda_pf.data.entities.Multimedia

@Dao
interface MultimediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(multimedia: Multimedia): Long

    @Update
    suspend fun update(multimedia: Multimedia): Int

    @Delete
    suspend fun delete(multimedia: Multimedia): Int

    @Query("SELECT * FROM multimedia WHERE noteId = :noteId")
    suspend fun getMultimediaForNote(noteId: Int): List<Multimedia>
}
