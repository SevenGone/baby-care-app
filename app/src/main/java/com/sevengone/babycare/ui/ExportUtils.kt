package com.sevengone.babycare.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.core.view.drawToBitmap
import java.io.OutputStream

object ExportUtils {
    fun exportCurrentView(context: Context, view: View): Result<String> {
        return runCatching {
            val bitmap = view.drawToBitmap(Bitmap.Config.ARGB_8888)
            saveBitmap(context = context, bitmap = bitmap)
        }
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap): String {
        val fileName = "baby-care-overview-${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/BabyCare"
                )
            }
        }

        val uri = requireNotNull(
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        )

        val stream: OutputStream = requireNotNull(resolver.openOutputStream(uri))
        stream.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        return fileName
    }
}
