package com.example.agenda_pf.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.agenda_pf.data.entities.Note
import com.example.agenda_pf.data.entities.Task
import com.example.agenda_pf.data.entities.Notification
import com.example.agenda_pf.data.entities.Multimedia
import com.example.agenda_pf.data.dao.NoteDao
import com.example.agenda_pf.data.dao.TaskDao
import com.example.agenda_pf.data.dao.NotificationDao
import com.example.agenda_pf.data.dao.MultimediaDao

@Database(
    entities = [Note::class, Task::class, Notification::class, Multimedia::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao
    abstract fun multimediaDao(): MultimediaDao
}
