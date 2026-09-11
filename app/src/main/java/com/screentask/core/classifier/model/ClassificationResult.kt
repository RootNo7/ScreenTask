package com.screentask.core.classifier.model

enum class ClassificationCategory {
    SCHEDULE_OR_CALENDAR,
    RECEIPT_OR_FINANCIAL,
    TEXT_DOCUMENT_OR_NOTE,
    CODE_OR_TECHNICAL,
    CHAT_OR_SOCIAL,
    MEDIA_OR_MEME,
    UNKNOWN
}

enum class SuggestedAction {
    KEEP,
    REMIND,
    DISCARD
}

data class ClassificationResult(
    val category: ClassificationCategory,
    val suggestedAction: SuggestedAction,
    val summary: String,
    val extractedText: String?,
    val reminderTimestamp: Long? = null,
    val rawConfidenceScore: Float = 0.0f
)