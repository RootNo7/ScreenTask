package com.screentask.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.screentask.core.classifier.model.ClassificationCategory
import com.screentask.core.classifier.model.SuggestedAction

@Entity(
    tableName = "screenshots",
    indices = [Index(value = ["uri"], unique = true)]
)
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val category: ClassificationCategory,
    val suggestedAction: SuggestedAction,
    val summary: String,
    val extractedText: String?,
    val reminderTimestamp: Long?,
    val confidenceScore: Float,
    val createdAt: Long = System.currentTimeMillis()
)