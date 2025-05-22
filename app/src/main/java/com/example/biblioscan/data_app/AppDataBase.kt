package com.example.biblioscan.data_app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Liste des entités et version de la base de données
@Database(
    entities = [BookEntity::class, UserEntity::class, FavoriteEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun biblioScanDao(): BiblioScanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "biblioscan_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
