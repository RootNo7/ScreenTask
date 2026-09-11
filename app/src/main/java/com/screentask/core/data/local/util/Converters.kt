package com.screentask.core.data.local.util

import androidx.room.TypeConverter
import com.screentask.core.classifier.model.ClassificationCategory
import com.screentask.core.classifier.model.SuggestedAction

class Converters {

    @TypeConverter
    fun fromCategory(category: ClassificationCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): ClassificationCategory = runCatching {
        ClassificationCategory.valueOf(value)
    }.getOrDefault(ClassificationCategory.UNKNOWN)

    @TypeConverter
    fun fromSuggestedAction(action: SuggestedAction): String = action.name

    @TypeConverter
    fun toSuggestedAction(value: String): SuggestedAction = runCatching {
        SuggestedAction.valueOf(value)
    }.getOrDefault(SuggestedAction.KEEP)
}