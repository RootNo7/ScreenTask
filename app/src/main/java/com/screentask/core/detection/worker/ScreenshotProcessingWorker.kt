package com.screentask.core.detection.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screentask.core.classifier.ClassificationError
import com.screentask.core.classifier.gemini.GeminiClassifierImpl
import com.screentask.core.data.local.AppDatabase
import com.screentask.core.data.local.entity.ScreenshotEntity
import com.screentask.core.notification.NotificationDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenshotProcessingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val classifier by lazy { GeminiClassifierImpl(applicationContext) }
    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private val notificationDispatcher by lazy { NotificationDispatcher(applicationContext) }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriString = inputData.getString(KEY_IMAGE_URI) ?: return@withContext Result.failure()
        val imageUri = Uri.parse(uriString)

        val classificationResult = classifier.classify(imageUri)

        classificationResult.fold(
            onSuccess = { result ->
                val entity = ScreenshotEntity(
                    uri = uriString,
                    category = result.category,
                    suggestedAction = result.suggestedAction,
                    summary = result.summary,
                    extractedText = result.extractedText,
                    reminderTimestamp = result.reminderTimestamp,
                    confidenceScore = result.rawConfidenceScore
                )

                database.screenshotDao().insertScreenshot(entity)
                notificationDispatcher.dispatchClassificationNotification(imageUri, result)

                Result.success()
            },
            onFailure = { error ->
                when (error) {
                    is ClassificationError.Offline -> Result.retry()
                    is ClassificationError.QuotaExceeded -> Result.retry()
                    is ClassificationError.InvalidImage -> Result.failure()
                    else -> Result.failure()
                }
            }
        )
    }

    companion object {
        const val KEY_IMAGE_URI = "key_image_uri"
    }
}