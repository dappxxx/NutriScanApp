package com.nutriscan.app.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.nutriscan.app.utils.Constants
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream

class GeminiApiService {

    private val TAG = "GeminiApiService"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 120000
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCAN GAMBAR
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun scanAndAnalyze(
        bitmap: Bitmap,
        healthProfile: String = ""
    ): Result<String> {
        return try {
            Log.d(TAG, "╔════════════════════════════════════════╗")
            Log.d(TAG, "║   GEMINI VISION: Scan + Analisis       ║")
            Log.d(TAG, "╚════════════════════════════════════════╝")
            Log.d(TAG, "Health Profile: ${healthProfile.take(100)}")

            val base64Image = bitmapToBase64(bitmap)
            Log.d(TAG, "Image encoded, length: ${base64Image.length}")

            val prompt = buildScanPrompt(healthProfile)
            Log.d(TAG, "Prompt length: ${prompt.length} chars")

            val result = callGeminiVision(prompt, base64Image)

            result.fold(
                onSuccess = { response ->
                    Log.d(TAG, "✅ Scan berhasil!")
                    Log.d(TAG, "📝 Response length: ${response.length} chars")
                    Log.d(TAG, "Preview: ${response.take(300)}...")

                    val cleaned = cleanResponse(response)
                    Log.d(TAG, "📝 Cleaned length: ${cleaned.length} chars")

                    Result.success(cleaned)
                },
                onFailure = {
                    Log.e(TAG, "❌ Scan gagal: ${it.message}")
                    Result.failure(it)
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Scan Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun buildScanPrompt(healthProfile: String): String {
        val hasProfile = healthProfile.isNotBlank()

        return if (hasProfile) {
            """
Kamu adalah ahli gizi profesional. Baca label nutrisi pada gambar dengan AKURAT dan LENGKAP.

══════════════════════════════════════════════════════════════════════════════
🏥 KONDISI KESEHATAN PENGGUNA:
$healthProfile
══════════════════════════════════════════════════════════════════════════════

ATURAN WAJIB:
1. Baca SEMUA angka nutrisi dengan AKURAT dari gambar
2. Seluruh analisis HARUS berdasarkan kondisi di atas
3. DILARANG menyebut kondisi/penyakit yang TIDAK ada di profil
4. JANGAN sebut: ibu hamil, menyusui, lansia, anak-anak, atau penyakit lain yang tidak ada di profil
5. WAJIB memberikan output LENGKAP sesuai format

FORMAT OUTPUT (WAJIB LENGKAP):

📦 NAMA PRODUK
[Baca nama produk dari gambar]

📊 INFORMASI NILAI GIZI

Takaran saji: [baca dari gambar]
Jumlah sajian: [baca dari gambar]

| Nutrisi | Jumlah | % AKG |
|---------|--------|-------|
| Energi | [X] kkal | [X]% |
| Lemak Total | [X] g | [X]% |
| Lemak Jenuh | [X] g | [X]% |
| Protein | [X] g | [X]% |
| Karbohidrat | [X] g | [X]% |
| Gula | [X] g | [X]% |
| Natrium | [X] mg | [X]% |
| Serat | [X] g | [X]% |
[tambahkan nutrisi lain jika ada di label]

📈 ANALISIS UNTUK KONDISI ANDA ($healthProfile)

🔸 [Nutrisi relevan] ([nilai]):
   → Dampak untuk kondisi Anda: [penjelasan]
   → Status: [aman/hati-hati/hindari]

[Analisis HANYA nutrisi yang relevan dengan kondisi di profil]

⚠️ PERINGATAN UNTUK KONDISI ANDA

🔴 Yang perlu Anda waspadai:
• [Peringatan HANYA untuk kondisi yang ada di profil]

🟢 Hal positif untuk kondisi Anda:
• [Kandungan yang baik untuk kondisi di profil]

📅 REKOMENDASI UNTUK ANDA

🕐 Frekuensi: [spesifik untuk kondisi Anda]
📏 Porsi: [spesifik untuk kondisi Anda]
⏰ Waktu: [spesifik untuk kondisi Anda]

💡 TIPS UNTUK KONDISI ANDA

1. [Tips spesifik untuk kondisi di profil]
2. [Tips spesifik untuk kondisi di profil]
3. [Tips spesifik untuk kondisi di profil]

✅ KESIMPULAN

📊 Rating untuk kondisi Anda: [⭐⭐⭐⭐⭐] dari 5
📝 [Kesimpulan spesifik untuk kondisi Anda dalam 2-3 kalimat]
""".trimIndent()
        } else {
            """
Kamu adalah ahli gizi profesional. Baca label nutrisi pada gambar dengan AKURAT dan LENGKAP.

WAJIB memberikan output LENGKAP sesuai format berikut:

📦 NAMA PRODUK
[Baca nama produk dari gambar]

📊 INFORMASI NILAI GIZI

Takaran saji: [baca dari gambar]
Jumlah sajian: [baca dari gambar]

| Nutrisi | Jumlah | % AKG |
|---------|--------|-------|
| Energi | [X] kkal | [X]% |
| Lemak Total | [X] g | [X]% |
| Lemak Jenuh | [X] g | [X]% |
| Protein | [X] g | [X]% |
| Karbohidrat | [X] g | [X]% |
| Gula | [X] g | [X]% |
| Natrium | [X] mg | [X]% |
| Serat | [X] g | [X]% |
[tambahkan nutrisi lain jika ada di label]

📈 ANALISIS KANDUNGAN

🔸 Energi: [analisis kalori, apakah tinggi/sedang/rendah]
🔸 Lemak: [analisis lemak total dan jenuh]
🔸 Gula: [bandingkan dengan batas 25g/hari WHO]
🔸 Natrium: [bandingkan dengan batas 2000mg/hari]
🔸 Protein: [apakah cukup sebagai sumber protein]
🔸 Serat: [apakah mengandung serat yang baik]

⚠️ PERINGATAN UMUM

🔴 Perlu diwaspadai:
• [Kandungan yang tinggi dan perlu perhatian]

🟡 Perhatian untuk kondisi tertentu:
• Diabetes: [saran terkait gula]
• Hipertensi: [saran terkait natrium]
• Kolesterol: [saran terkait lemak jenuh]

🟢 Hal positif:
• [Kandungan yang baik untuk kesehatan]

📅 REKOMENDASI KONSUMSI

🕐 Frekuensi: [berapa kali per minggu yang aman]
📏 Porsi: [jumlah porsi yang disarankan]
⏰ Waktu terbaik: [kapan sebaiknya dikonsumsi]

💡 TIPS SEHAT

1. [Tips pertama untuk konsumsi produk ini]
2. [Tips kedua]
3. [Tips ketiga]

✅ KESIMPULAN

📊 Rating kesehatan: [⭐⭐⭐⭐⭐] dari 5
📝 [Kesimpulan keseluruhan dalam 2-3 kalimat]

💡 Lengkapi profil kesehatan Anda untuk mendapatkan analisis yang dipersonalisasi!
""".trimIndent()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHAT
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun chat(
        userMessage: String,
        productAnalysis: String,
        chatHistory: List<Pair<String, String>>,
        healthProfile: String = ""
    ): Result<String> {
        return try {
            Log.d(TAG, "💬 Chat: ${userMessage.take(50)}...")

            val systemPrompt = buildChatPrompt(productAnalysis, healthProfile)
            val result = callGeminiChat(systemPrompt, userMessage, chatHistory)

            result.fold(
                onSuccess = {
                    Log.d(TAG, "✅ Chat berhasil, length: ${it.length}")
                    Result.success(cleanResponse(it))
                },
                onFailure = {
                    Log.e(TAG, "❌ Chat gagal: ${it.message}")
                    Result.failure(it)
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Chat Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun buildChatPrompt(productAnalysis: String, healthProfile: String): String {
        val profileSection = if (healthProfile.isNotBlank()) {
            """
KONDISI KESEHATAN PENGGUNA:
$healthProfile

WAJIB pertimbangkan kondisi ini dalam setiap jawaban. JANGAN sebut kondisi lain yang tidak ada di profil.
"""
        } else {
            "Pengguna belum mengisi profil kesehatan."
        }

        return """
Kamu adalah NutriScan AI, ahli gizi profesional yang ramah dan informatif.

$profileSection

DATA PRODUK YANG SEDANG DIBAHAS:
$productAnalysis

CARA MENJAWAB:
1. Jawab dengan NATURAL seperti ahli gizi sungguhan
2. Berikan informasi yang LENGKAP dan AKURAT
3. Gunakan bahasa Indonesia yang ramah dan mudah dipahami
4. Boleh gunakan emoji untuk membuat jawaban lebih menarik 😊
5. Jika ditanya tentang nutrisi, berikan penjelasan yang edukatif

TOPIK YANG BOLEH DIBAHAS:
- Gizi dan nutrisi
- Makanan dan minuman
- Diet dan pola makan sehat
- Kesehatan terkait makanan
- Kandungan produk yang sedang dibahas

TOPIK YANG DITOLAK:
- Politik, teknologi, hiburan, atau topik di luar gizi/nutrisi

Jika ditanya di luar topik gizi/nutrisi, jawab dengan sopan:
"Maaf, saya adalah asisten khusus untuk gizi dan nutrisi 🍎 Ada yang ingin ditanyakan tentang nutrisi produk ini atau tips kesehatan lainnya?"
""".trimIndent()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // API CALLS
    // ═══════════════════════════════════════════════════════════════════════════

    private suspend fun callGeminiVision(prompt: String, base64Image: String): Result<String> {
        val requestBody = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                        add(buildJsonObject {
                            put("inline_data", buildJsonObject {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("temperature", 0.3)
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 8192)
                put("candidateCount", 1)
            })
            put("safetySettings", buildSafetySettings())
        }

        return executeGeminiRequest(requestBody)
    }

    private suspend fun callGeminiChat(
        systemPrompt: String,
        userMessage: String,
        history: List<Pair<String, String>>
    ): Result<String> {
        val contents = buildJsonArray {
            // System prompt sebagai pesan pertama
            add(buildJsonObject {
                put("role", "user")
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", "INSTRUKSI SISTEM: $systemPrompt") })
                })
            })
            add(buildJsonObject {
                put("role", "model")
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", "Baik, saya siap membantu sebagai ahli gizi! 😊 Silakan tanyakan apa saja tentang nutrisi produk ini.") })
                })
            })

            // Chat history (ambil 10 terakhir)
            history.takeLast(10).forEach { (sender, message) ->
                add(buildJsonObject {
                    put("role", if (sender == "user") "user" else "model")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", message) })
                    })
                })
            }

            // Pesan user saat ini
            add(buildJsonObject {
                put("role", "user")
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", userMessage) })
                })
            })
        }

        val requestBody = buildJsonObject {
            put("contents", contents)
            put("generationConfig", buildJsonObject {
                put("temperature", 0.7)
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 4096)
                put("candidateCount", 1)
            })
            put("safetySettings", buildSafetySettings())
        }

        return executeGeminiRequest(requestBody)
    }

    /**
     * Execute request dengan auto-retry dan fallback model
     */
    private suspend fun executeGeminiRequest(requestBody: JsonObject): Result<String> {
        // Model yang tersedia, prioritaskan 2.0 untuk output stabil
        val models = listOf(
            "gemini-2.5-flash",               // Backup - terbaru tapi output kadang beda
            "gemini-2.0-flash",              // Utama - output stabil
            "gemini-2.0-flash-001",          // Stable version
            "gemini-2.0-flash-exp",          // Experimental

        )

        var lastError: String = "Unknown error"
        var lastStatus: Int = 0

        for ((modelIndex, model) in models.withIndex()) {
            var retryCount = 0
            val maxRetries = 2

            while (retryCount < maxRetries) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${Constants.GEMINI_API_KEY}"

                    Log.d(TAG, "🔄 Model: $model | Attempt: ${retryCount + 1}/$maxRetries")

                    val response = client.post(url) {
                        contentType(ContentType.Application.Json)
                        setBody(json.encodeToString(requestBody))
                    }

                    val responseText = response.bodyAsText()
                    val status = response.status.value
                    lastStatus = status

                    Log.d(TAG, "📡 Status: $status")

                    when (status) {
                        200 -> {
                            val content = extractContent(responseText)
                            if (content.isNotBlank()) {
                                Log.d(TAG, "✅ Berhasil dengan model: $model")
                                return Result.success(content)
                            } else {
                                Log.w(TAG, "⚠️ Response kosong dari $model")
                                lastError = "Response kosong"
                                break
                            }
                        }

                        404 -> {
                            Log.w(TAG, "❌ Model $model tidak ditemukan")
                            lastError = "Model tidak ditemukan"
                            break
                        }

                        403 -> {
                            Log.e(TAG, "🔒 API Key tidak valid (403)")
                            return Result.failure(Exception("API Key tidak valid. Silakan periksa API Key Anda."))
                        }

                        429 -> {
                            retryCount++
                            Log.w(TAG, "⏳ Rate limit untuk $model (attempt $retryCount/$maxRetries)")

                            if (retryCount < maxRetries) {
                                // Delay: 20 detik, 40 detik
                                val delaySeconds = 20 * retryCount
                                Log.d(TAG, "⏰ Menunggu ${delaySeconds} detik...")
                                kotlinx.coroutines.delay(delaySeconds * 1000L)
                            } else {
                                lastError = "Rate limit - terlalu banyak request"
                                break
                            }
                        }

                        500, 502, 503, 504 -> {
                            retryCount++
                            Log.w(TAG, "🔥 Server error $status (attempt $retryCount/$maxRetries)")

                            if (retryCount < maxRetries) {
                                kotlinx.coroutines.delay(5000L)
                            } else {
                                lastError = "Server error $status"
                                break
                            }
                        }

                        else -> {
                            Log.e(TAG, "❌ Error $status: ${responseText.take(300)}")
                            lastError = "Error $status"
                            break
                        }
                    }
                } catch (e: Exception) {
                    retryCount++
                    Log.e(TAG, "💥 Exception: ${e.message}")
                    lastError = e.message ?: "Network error"

                    if (retryCount < maxRetries) {
                        kotlinx.coroutines.delay(5000L)
                    } else {
                        break
                    }
                }
            }

            // Delay sebelum coba model berikutnya
            if (modelIndex < models.size - 1) {
                Log.d(TAG, "⏳ Delay 15 detik sebelum coba model berikutnya...")
                kotlinx.coroutines.delay(15000L)
            }
        }

        // Error message yang informatif
        val errorMessage = if (lastStatus == 429) {
            "Terlalu banyak request.\n\n⏰ Tunggu 2 menit lalu coba lagi."
        } else {
            "Gagal analisis: $lastError\n\nSilakan coba lagi."
        }

        return Result.failure(Exception(errorMessage))
    }

    private fun buildSafetySettings(): JsonArray {
        return buildJsonArray {
            listOf(
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT"
            ).forEach { category ->
                add(buildJsonObject {
                    put("category", category)
                    put("threshold", "BLOCK_NONE")
                })
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()

        // Kompres gambar untuk mengurangi ukuran request
        val maxSize = 1024
        val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
            val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else bitmap

        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        Log.d(TAG, "📷 Image compressed: ${base64.length} chars")

        return base64
    }

    private fun extractContent(responseText: String): String {
        return try {
            val jsonObj = json.parseToJsonElement(responseText).jsonObject

            // Cek error
            jsonObj["error"]?.let { error ->
                val message = error.jsonObject["message"]?.jsonPrimitive?.contentOrNull
                Log.e(TAG, "API Error: $message")
                return ""
            }

            val candidates = jsonObj["candidates"]?.jsonArray
            if (candidates.isNullOrEmpty()) {
                Log.e(TAG, "No candidates in response")
                Log.d(TAG, "Response: ${responseText.take(500)}")
                return ""
            }

            // Cek finish reason
            val finishReason = candidates[0].jsonObject["finishReason"]?.jsonPrimitive?.contentOrNull
            Log.d(TAG, "Finish reason: $finishReason")

            if (finishReason == "SAFETY") {
                Log.w(TAG, "⚠️ Response diblokir karena safety filter")
                return "Maaf, konten tidak dapat ditampilkan karena filter keamanan."
            }

            val content = candidates[0].jsonObject["content"]?.jsonObject
            val parts = content?.get("parts")?.jsonArray

            if (parts.isNullOrEmpty()) {
                Log.e(TAG, "No parts in content")
                return ""
            }

            // Gabungkan semua parts
            val fullText = StringBuilder()
            for (part in parts) {
                val text = part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                if (!text.isNullOrBlank()) {
                    fullText.append(text)
                }
            }

            val result = fullText.toString()
            Log.d(TAG, "📝 Extracted content: ${result.length} chars")

            result

        } catch (e: Exception) {
            Log.e(TAG, "Extract error: ${e.message}")
            Log.d(TAG, "Raw response: ${responseText.take(500)}")
            ""
        }
    }

    private fun cleanResponse(text: String): String {
        return text
            .replace("\\n", "\n")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}