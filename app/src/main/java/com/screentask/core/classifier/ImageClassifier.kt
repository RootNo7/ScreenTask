package com.screentask.core.classifier

import android.net.Uri
import com.screentask.core.classifier.model.ClassificationResult

sealed class ClassificationError : Exception() {
    data object Offline : ClassificationError()
    data object InvalidImage : ClassificationError()
    data object QuotaExceeded : ClassificationError()
    data class ProviderError(override val message: String) : ClassificationError()
}

interface ImageClassifier {
    suspend fun classify(imageUri: Uri): Result<ClassificationResult>
}