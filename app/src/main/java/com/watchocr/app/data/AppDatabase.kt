package com.watchocr.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [OcrRecord::class, MonitoredFile::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ocrRecordDao(): OcrRecordDao
    abstract fun monitoredFileDao(): MonitoredFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v2 adds OCR retry tracking to monitored_files. Existing rows were either
        // baselined or already handled under the old semantics, so they migrate as
        // processed with no failed attempts.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE monitored_files ADD COLUMN processed INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE monitored_files ADD COLUMN failedAttempts INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "watchocr.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}
