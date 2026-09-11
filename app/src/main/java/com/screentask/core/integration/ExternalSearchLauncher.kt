package com.screentask.core.integration

import android.content.Context
import android.content.Intent
import android.net.Uri

object ExternalSearchLauncher {

    fun launchImageSearch(context: Context, imageUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Search image using").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            context.startActivity(chooserIntent)
        }
    }
}