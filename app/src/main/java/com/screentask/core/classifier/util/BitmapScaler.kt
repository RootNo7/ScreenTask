package com.screentask.core.classifier.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

object BitmapScaler {

    private const val MAX_DIMENSION_PX = 1024

    suspend fun scaleImageForClassification(context: Context, imageUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return@runCatching null

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) return@runCatching null

            var sampleSize = 1
            val maxOriginalDimension = max(originalWidth, originalHeight)
            
            while (maxOriginalDimension / sampleSize > MAX_DIMENSION_PX) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            }
        }.getOrNull()
    }
}