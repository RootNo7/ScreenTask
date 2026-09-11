package com.screentask.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.screentask.core.data.local.dao.ScreenshotDao
import com.screentask.core.data.local.entity.ScreenshotEntity
import com.screentask.core.data.local.entity.ScreenshotFtsEntity
import com.screentask.core.data.local.util.Converters

@Database(
    entities = [ScreenshotEntity::class, ScreenshotFtsEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screentask_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
