package com.watchocr.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_files")
data class MonitoredFile(
    @PrimaryKey val documentUri: String,
    val lastModified: Long,
    /** False while the file still needs (re)processing; baseline entries start true. */
    @ColumnInfo(defaultValue = "1") val processed: Boolean = true,
    /** Number of failed OCR attempts so far. */
    @ColumnInfo(defaultValue = "0") val failedAttempts: Int = 0
)
