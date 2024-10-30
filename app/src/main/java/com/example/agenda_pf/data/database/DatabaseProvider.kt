package com.example.agenda_pf.data.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "agenda_db"
            ).fallbackToDestructiveMigration() // Agrega esto solo si quieres eliminar la base de datos al cambiar la versión en desarrollo.
                .build()
        }
        return instance!!
    }
}
