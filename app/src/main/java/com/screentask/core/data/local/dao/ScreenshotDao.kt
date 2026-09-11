package com.screentask.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.screentask.core.data.local.entity.ScreenshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenshot(screenshot: ScreenshotEntity): Long

    @Query("SELECT * FROM screenshots WHERE uri = :uri LIMIT 1")
    suspend fun getScreenshotByUri(uri: String): ScreenshotEntity?

    @Query("SELECT * FROM screenshots ORDER BY createdAt DESC")
    fun getAllScreenshots(): Flow<List<ScreenshotEntity>>

    @Query(
        """
        SELECT screenshots.* FROM screenshots
        JOIN screenshots_fts ON screenshots.rowid = screenshots_fts.rowid
        WHERE screenshots_fts MATCH :query
        ORDER BY screenshots.createdAt DESC
        """
    )
    fun searchScreenshots(query: String): Flow<List<ScreenshotEntity>>

    @Query("DELETE FROM screenshots WHERE uri = :uri")
    suspend fun deleteByUri(uri: String): Int

    @Query("DELETE FROM screenshots WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
