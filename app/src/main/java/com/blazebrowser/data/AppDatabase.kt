package com.blazebrowser.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [Bookmark::class, HistoryEntry::class, TempProfile::class, TempMessage::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun tempProfileDao(): TempProfileDao
    abstract fun tempMessageDao(): TempMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blaze_browser_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
