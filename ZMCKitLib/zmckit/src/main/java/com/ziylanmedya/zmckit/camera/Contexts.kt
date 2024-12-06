package com.ziylanmedya.zmckit.camera

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.util.UUID

/**
 * Saves the provided [bitmap] as a jpeg file to application's cache directory.
 */
internal fun Context.cacheJpegOf(bitmap: Bitmap): File {
   return File(cacheDir, "${UUID.randomUUID()}.jpg").also {
      it.outputStream().use { outputStream ->
              bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
      }
   }
}