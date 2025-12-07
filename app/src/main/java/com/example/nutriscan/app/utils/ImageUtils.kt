package com.nutriscan.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

object ImageUtils {

    fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let { bitmapToByteArray(it, 80) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    fun byteArrayToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun createTempImageFile(context: Context): File {
        val fileName = "SCAN_${System.currentTimeMillis()}"
        return File.createTempFile(fileName, ".jpg", context.cacheDir)
    }

    fun generateUniqueFileName(): String {
        return "scan_${System.currentTimeMillis()}.jpg"
    }
}