package com.nutriscan.app.utils

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream

/**
 * OCR.space API Service - GRATIS 25,000 requests/bulan
 * Dokumentasi: https://ocr.space/OCRAPI
 */
object OCRSpaceService {

    private const val TAG = "OCRSpaceService"

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000  // 60 detik
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 60000
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Ekstrak teks dari bitmap menggunakan OCR.space API
     */
    suspend fun extractText(bitmap: Bitmap): OCRResult {
        return try {
            Log.d(TAG, "")
            Log.d(TAG, "╔═══════════════════════════════════╗")
            Log.d(TAG, "║     OCR.space - Memulai OCR       ║")
            Log.d(TAG, "╚═══════════════════════════════════╝")
            Log.d(TAG, "Image size: ${bitmap.width}x${bitmap.height}")

            // 1. Resize & compress image
            val processedBitmap = preprocessImage(bitmap)
            val base64Image = bitmapToBase64(processedBitmap)

            Log.d(TAG, "Processed size: ${processedBitmap.width}x${processedBitmap.height}")
            Log.d(TAG, "Base64 length: ${base64Image.length} chars")

            // 2. Kirim ke OCR.space API
            Log.d(TAG, "Mengirim ke OCR.space API...")

            val response = client.post(Constants.OCR_SPACE_URL) {
                header("apikey", Constants.OCR_SPACE_API_KEY)
                setBody(MultiPartFormDataContent(formData {
                    append("base64Image", "data:image/jpeg;base64,$base64Image")
                    append("language", "eng")  // English + auto-detect
                    append("isOverlayRequired", "false")
                    append("detectOrientation", "true")
                    append("scale", "true")
                    append("OCREngine", "2")  // Engine 2 lebih akurat untuk teks padat
                }))
            }

            val responseText = response.bodyAsText()
            Log.d(TAG, "Response status: ${response.status}")

            // 3. Parse response
            val result = parseResponse(responseText)

            Log.d(TAG, "")
            Log.d(TAG, "┌──────────── HASIL OCR ────────────┐")
            Log.d(TAG, "│ Status: ${if (result.isSuccess) "✓ SUCCESS" else "✗ FAILED"}")
            Log.d(TAG, "│ Text length: ${result.text.length} chars")
            if (result.error != null) {
                Log.d(TAG, "│ Error: ${result.error}")
            }
            Log.d(TAG, "├───────────────────────────────────┤")
            result.text.lines().take(20).forEach { line ->
                Log.d(TAG, "│ $line")
            }
            if (result.text.lines().size > 20) {
                Log.d(TAG, "│ ... +${result.text.lines().size - 20} baris lagi")
            }
            Log.d(TAG, "└───────────────────────────────────┘")

            result

        } catch (e: Exception) {
            Log.e(TAG, "OCR Error: ${e.message}", e)
            OCRResult(
                text = "",
                isSuccess = false,
                error = "Gagal memproses: ${e.message}"
            )
        }
    }

    /**
     * Preprocess image untuk hasil OCR lebih baik
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        // Resize ke ukuran optimal (tidak terlalu besar agar cepat)
        val maxSize = 1500
        val width = bitmap.width
        val height = bitmap.height

        return if (width > maxSize || height > maxSize) {
            val ratio = maxSize.toFloat() / maxOf(width, height)
            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else if (width < 800 || height < 800) {
            // Perbesar jika terlalu kecil
            val ratio = 1000f / minOf(width, height)
            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    /**
     * Convert bitmap ke Base64 JPEG
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Parse response dari OCR.space API
     */
    private fun parseResponse(responseText: String): OCRResult {
        return try {
            val jsonResponse = json.parseToJsonElement(responseText).jsonObject

            // Cek apakah API call berhasil
            val isErroredOnProcessing = jsonResponse["IsErroredOnProcessing"]?.jsonPrimitive?.boolean ?: false
            val ocrExitCode = jsonResponse["OCRExitCode"]?.jsonPrimitive?.intOrNull ?: 0

            if (isErroredOnProcessing || ocrExitCode != 1) {
                val errorMessage = jsonResponse["ErrorMessage"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?.joinToString(", ")
                    ?: "Unknown error"

                return OCRResult(
                    text = "",
                    isSuccess = false,
                    error = errorMessage
                )
            }

            // Ambil hasil parsing
            val parsedResults = jsonResponse["ParsedResults"]?.jsonArray

            if (parsedResults.isNullOrEmpty()) {
                return OCRResult(
                    text = "",
                    isSuccess = false,
                    error = "Tidak ada teks yang terdeteksi"
                )
            }

            // Gabungkan semua teks dari parsed results
            val allText = StringBuilder()

            for (result in parsedResults) {
                val parsedText = result.jsonObject["ParsedText"]?.jsonPrimitive?.contentOrNull
                if (!parsedText.isNullOrBlank()) {
                    allText.append(parsedText)
                }
            }

            val finalText = cleanText(allText.toString())

            OCRResult(
                text = finalText,
                isSuccess = finalText.isNotBlank(),
                error = if (finalText.isBlank()) "Tidak ada teks yang terbaca" else null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            OCRResult(
                text = "",
                isSuccess = false,
                error = "Gagal memproses hasil: ${e.message}"
            )
        }
    }

    /**
     * Bersihkan teks hasil OCR
     */
    private fun cleanText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\t", " ")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    /**
     * Data class untuk hasil OCR
     */
    data class OCRResult(
        val text: String,
        val isSuccess: Boolean,
        val error: String? = null
    )
}