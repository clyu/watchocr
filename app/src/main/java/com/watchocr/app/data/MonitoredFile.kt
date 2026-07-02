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
    @ColumnInfo(defaultValue = "0") val failedAttempts: Int = 0,
    /**
     * File size as of the last poll. Together with [lastModified] this detects
     * files that are still being written: a pending file is only processed once
     * both values are unchanged between two consecutive polls.
     */
    @ColumnInfo(defaultValue = "0") val sizeBytes: Long = 0
)
