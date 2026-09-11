package com.screentask.core.detection

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.screentask.core.detection.worker.ScreenshotProcessingWorker
import java.util.concurrent.TimeUnit

class ScreenshotObserver(
    private val context: Context,
    handler: Handler
) : ContentObserver(handler) {

    private var lastProcessedUri: String? = null
    private var lastProcessedTime: Long = 0L

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        uri?.let { processMediaUri(it) }
    }

    private fun processMediaUri(uri: Uri) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )

        runCatching {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return

                val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val relativePathIndex = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                val path = if (pathIndex != -1) cursor.getString(pathIndex) else ""
                val relativePath = if (relativePathIndex != -1) cursor.getString(relativePathIndex) else ""

                val isScreenshot = path.contains("Screenshot", ignoreCase = true) ||
                        relativePath.contains("Screenshot", ignoreCase = true)

                if (isScreenshot) {
                    val currentTime = System.currentTimeMillis()
                    // Debounce duplicate triggers within 1.5 seconds for the same URI
                    if (uri.toString() == lastProcessedUri && (currentTime - lastProcessedTime) < 1500) {
                        return
                    }
                    lastProcessedUri = uri.toString()
                    lastProcessedTime = currentTime

                    enqueueProcessingWork(uri)
                }
            }
        }
    }

    private fun enqueueProcessingWork(imageUri: Uri) {
        val inputData = Data.Builder()
            .putString(ScreenshotProcessingWorker.KEY_IMAGE_URI, imageUri.toString())
            .build()

        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ScreenshotProcessingWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}