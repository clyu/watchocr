package com.watchocr.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MonitoredFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: MonitoredFile)

    @Query("SELECT * FROM monitored_files")
    suspend fun getAll(): List<MonitoredFile>

    @Query("UPDATE monitored_files SET processed = 1 WHERE documentUri = :uri")
    suspend fun markProcessed(uri: String)

    @Query("UPDATE monitored_files SET failedAttempts = failedAttempts + 1 WHERE documentUri = :uri")
    suspend fun incrementFailedAttempts(uri: String)

    @Query("DELETE FROM monitored_files")
    suspend fun clear()
}
