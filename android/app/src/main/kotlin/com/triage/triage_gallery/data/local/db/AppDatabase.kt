package com.triage.triage_gallery.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
// Importaciones de tus Entidades y DAO
import com.triage.triage_gallery.data.local.db.entities.PhotoEntity
import com.triage.triage_gallery.data.local.db.entities.CategoryEntity
import com.triage.triage_gallery.data.local.db.entities.PhotoCategoryEntity
import com.triage.triage_gallery.data.local.db.dao.TriageDao

@Database(
    entities = [
        PhotoEntity::class,
        CategoryEntity::class,
        PhotoCategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Exponemos el DAO para que el Repositorio lo use
    abstract fun triageDao(): TriageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Patrón Singleton para no abrir múltiples conexiones a la DB
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "triage_gallery.db"
                )
                    // .fallbackToDestructiveMigration() // Descomentar si cambias el esquema y quieres borrar datos viejos
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}