package com.example.biblioscan.data_app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, BookEntity::class, FavoriteEntity::class, HistoryEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun biblioScanDao(): BiblioScanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "biblioscan_database"
                )
                    .fallbackToDestructiveMigration() // Évite crash migration version
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
