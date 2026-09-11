package com.screentask.core.classifier.gemini

import android.content.Context
import android.net.Uri
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.screentask.BuildConfig
import com.screentask.core.classifier.ClassificationError
import com.screentask.core.classifier.ImageClassifier
import com.screentask.core.classifier.model.ClassificationCategory
import com.screentask.core.classifier.model.ClassificationResult
import com.screentask.core.classifier.model.SuggestedAction
import com.screentask.core.classifier.util.BitmapScaler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

class GeminiClassifierImpl(
    private val context: Context
) : ImageClassifier {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.1f
            }
        )
    }

    override suspend fun classify(imageUri: Uri): Result<ClassificationResult> = withContext(Dispatchers.IO) {
        val scaledBitmap = BitmapScaler.scaleImageForClassification(context, imageUri)
            ?: return@withContext Result.failure(ClassificationError.InvalidImage)

        val prompt = """
            Analyze the attached screenshot and output ONLY a single raw JSON object conforming strictly to this schema:
            {
              "category": "SCHEDULE_OR_CALENDAR | RECEIPT_OR_FINANCIAL | TEXT_DOCUMENT_OR_NOTE | CODE_OR_TECHNICAL | CHAT_OR_SOCIAL | MEDIA_OR_MEME | UNKNOWN",
              "suggestedAction": "KEEP | REMIND | DISCARD",
              "summary": "<Concise 1-sentence description>",
              "extractedText": "<Key visible textual content if actionable, otherwise null>",
              "reminderTimestamp": <Unix epoch timestamp in ms if schedule/deadline found, otherwise null>,
              "confidenceScore": <Float between 0.0 and 1.0>
            }
        """.trimIndent()

        runCatching {
            val response = generativeModel.generateContent(
                content {
                    image(scaledBitmap)
                    text(prompt)
                }
            )

            val jsonText = response.text ?: throw ClassificationError.ProviderError("Empty response body")
            parseAndValidateResponse(jsonText)
        }.recoverCatching { throwable ->
            throw when (throwable) {
                is IOException -> ClassificationError.Offline
                is ClassificationError -> throwable
                else -> {
                    val message = throwable.localizedMessage ?: ""
                    if (message.contains("429") || message.contains("quota", ignoreCase = true)) {
                        ClassificationError.QuotaExceeded
                    } else {
                        ClassificationError.ProviderError(message)
                    }
                }
            }
        }
    }

    private fun parseAndValidateResponse(jsonText: String): ClassificationResult {
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