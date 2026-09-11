package com.screentask.core.classifier.gemini

import com.screentask.core.classifier.model.ClassificationCategory
import com.screentask.core.classifier.model.ClassificationResult
import com.screentask.core.classifier.model.SuggestedAction
import org.json.JSONObject

object GeminiResponseParser {

    fun parse(jsonText: String): ClassificationResult {
        val json = JSONObject(jsonText)

        val rawCategory = json.optString("category", "UNKNOWN")
        val category = runCatching { ClassificationCategory.valueOf(rawCategory) }
            .getOrDefault(ClassificationCategory.UNKNOWN)

        val rawAction = json.optString("suggestedAction", "KEEP")
        val action = runCatching { SuggestedAction.valueOf(rawAction) }
            .getOrDefault(SuggestedAction.KEEP)

        val summary = json.optString("summary", "No summary available.").ifBlank { "No summary available." }
        val extractedText = json.optString("extractedText").takeIf { it.isNotBlank() && it != "null" }
        val reminderTimestamp = if (json.has("reminderTimestamp") && !json.isNull("reminderTimestamp")) {
            json.optLong("reminderTimestamp").takeIf { it > 0L }
        } else null

        val confidenceScore = json.optDouble("confidenceScore", 0.0).toFloat().coerceIn(0.0f, 1.0f)

        return ClassificationResult(
            category = category,
            suggestedAction = action,
            summary = summary,
            extractedText = extractedText,
            reminderTimestamp = reminderTimestamp,
            rawConfidenceScore = confidenceScore
        )
    }
}