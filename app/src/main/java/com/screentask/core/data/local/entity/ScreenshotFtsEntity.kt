package com.screentask.core.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = ScreenshotEntity::class)
@Entity(tableName = "screenshots_fts")
data class ScreenshotFtsEntity(
    @PrimaryKey
    val rowid: Int,
    val summary: String,
    val extractedText: String?
)