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
        }
    }

    // SCAN GAMBAR
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

            val result = callGeminiVision(prompt, base64Image)

            result.fold(
                onSuccess = {
                    Log.d(TAG, "✅ Scan berhasil!")
                    Result.success(cleanResponse(it))
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
Kamu adalah ahli gizi profesional. Baca label nutrisi pada gambar dengan AKURAT.

══════════════════════════════════════════════════════════════════════════════
🏥 KONDISI KESEHATAN PENGGUNA:
$healthProfile
══════════════════════════════════════════════════════════════════════════════

ATURAN WAJIB:
1. Baca SEMUA angka nutrisi dengan AKURAT dari gambar
2. Seluruh analisis HARUS berdasarkan kondisi di atas
3. DILARANG menyebut kondisi/penyakit yang TIDAK ada di profil
4. JANGAN sebut: ibu hamil, menyusui, lansia, anak-anak, atau penyakit lain yang tidak ada di profil

FORMAT OUTPUT:

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
[tambahkan nutrisi lain jika ada]

📈 ANALISIS UNTUK KONDISI ANDA ($healthProfile)

🔸 [Nutrisi relevan] ([nilai]):
   → Dampak untuk kondisi Anda: [penjelasan]
   → Status: [aman/hati-hati/hindari]

[Analisis HANYA nutrisi yang relevan dengan kondisi di profil]

⚠️ PERINGATAN UNTUK KONDISI ANDA ($healthProfile)

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
📝 [Kesimpulan spesifik untuk kondisi Anda]
""".trimIndent()
        } else {
            """
Kamu adalah ahli gizi profesional. Baca label nutrisi pada gambar dengan AKURAT.

FORMAT OUTPUT:

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

📈 ANALISIS KANDUNGAN

🔸 Energi: [analisis]
🔸 Lemak: [analisis]
🔸 Gula: [bandingkan dengan 25g/hari]
🔸 Natrium: [bandingkan dengan 2000mg/hari]

⚠️ PERINGATAN UMUM

🔴 Perlu diwaspadai: [kandungan tinggi]
🟡 Perhatian untuk:
• Diabetes: [saran gula]
• Hipertensi: [saran natrium]
• Kolesterol: [saran lemak]
🟢 Hal positif: [kandungan baik]

📅 REKOMENDASI

• Frekuensi: [X kali/minggu]
• Porsi: [jumlah]
• Waktu: [kapan]

💡 TIPS

1. [Tips #1]
2. [Tips #2]
3. [Tips #3]

✅ KESIMPULAN

📊 Rating: [⭐⭐⭐⭐⭐] dari 5
📝 [Kesimpulan]

💡 Lengkapi profil kesehatan untuk analisis personal!
""".trimIndent()
        }
    }

    // CHAT
    suspend fun chat(
        userMessage: String,
        productAnalysis: String,
        chatHistory: List<Pair<String, String>>,
        healthProfile: String = ""
    ): Result<String> {
        return try {
            Log.d(TAG, "Chat: $userMessage")

            val systemPrompt = buildChatPrompt(productAnalysis, healthProfile)
            val result = callGeminiChat(systemPrompt, userMessage, chatHistory)

            result.fold(
                onSuccess = { Result.success(cleanResponse(it)) },
                onFailure = { Result.failure(it) }
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
Kamu adalah NutriScan AI, ahli gizi profesional.

$profileSection

DATA PRODUK:
$productAnalysis

CARA MENJAWAB:
1. Jawab NATURAL seperti ahli gizi sungguhan
2. Jangan pakai template kaku
3. Bahasa Indonesia yang ramah
4. Boleh pakai emoji 😊

TOPIK BOLEH: Gizi, nutrisi, makanan, diet, kesehatan makanan
TOPIK DITOLAK: Politik, teknologi, hiburan, dll

Jika ditanya di luar topik:
"Maaf, saya asisten khusus gizi dan nutrisi 🍎 Ada yang ingin ditanyakan tentang nutrisi produk ini?"
""".trimIndent()
    }

    // API CALLS - DENGAN AUTO-RETRY MODELS
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
                put("temperature", 0.1)
                put("topK", 32)
                put("topP", 1.0)
                put("maxOutputTokens", 4096)
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
            // System as first user message
            add(buildJsonObject {
                put("role", "user")
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", "INSTRUKSI: $systemPrompt") })
                })
            })
            add(buildJsonObject {
                put("role", "model")
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", "Baik, saya siap membantu! 😊") })
                })
            })

            // Chat history
            history.takeLast(10).forEach { (sender, message) ->
                add(buildJsonObject {
                    put("role", if (sender == "user") "user" else "model")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", message) })
                    })
                })
            }

            // Current message
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
                put("maxOutputTokens", 2048)
            })
            put("safetySettings", buildSafetySettings())
        }

        return executeGeminiRequest(requestBody)
    }

    /**
     * Execute request dengan auto-retry berbagai model
     */
    private suspend fun executeGeminiRequest(requestBody: JsonObject): Result<String> {
        // Daftar model yang akan dicoba secara berurutan
        val models = listOf(
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-flash-latest",
            "gemini-1.5-pro",
            "gemini-pro"
        )

        var lastError: String = "Unknown error"

        for (model in models) {
            try {
                // Build URL dengan model saat ini
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${Constants.GEMINI_API_KEY}"

                Log.d(TAG, "Trying model: $model")

                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(requestBody))
                }

                val responseText = response.bodyAsText()
                val status = response.status.value

                Log.d(TAG, "Response status: $status")

                when (status) {
                    200 -> {
                        val content = extractContent(responseText)
                        if (content.isNotBlank()) {
                            Log.d(TAG, "✅ Success with model: $model")
                            return Result.success(content)
                        } else {
                            Log.w(TAG, "Empty content from $model")
                            lastError = "Empty response"
                        }
                    }
                    404 -> {
                        Log.w(TAG, "Model $model not found (404)")
                        lastError = "Model not found"
                        // Lanjut ke model berikutnya
                    }
                    403 -> {
                        Log.e(TAG, "API Key invalid or no access (403)")
                        return Result.failure(Exception("API Key tidak valid. Silakan buat API Key baru di https://aistudio.google.com/app/apikey"))
                    }
                    429 -> {
                        Log.e(TAG, "Rate limit exceeded (429)")
                        return Result.failure(Exception("Terlalu banyak request. Coba lagi dalam beberapa menit."))
                    }
                    else -> {
                        Log.e(TAG, "Error $status: ${responseText.take(200)}")
                        lastError = "Error $status"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception for $model: ${e.message}")
                lastError = e.message ?: "Unknown error"
            }
        }

        // Semua model gagal
        return Result.failure(Exception("Gagal menghubungi AI. $lastError\n\nSolusi: Buat API Key baru di https://aistudio.google.com/app/apikey"))
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

        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun extractContent(responseText: String): String {
        return try {
            val jsonObj = json.parseToJsonElement(responseText).jsonObject

            jsonObj["error"]?.let { error ->
                val message = error.jsonObject["message"]?.jsonPrimitive?.contentOrNull
                Log.e(TAG, "API Error: $message")
                return ""
            }

            val candidates = jsonObj["candidates"]?.jsonArray
            if (candidates.isNullOrEmpty()) {
                Log.e(TAG, "No candidates")
                return ""
            }

            candidates[0].jsonObject
                .get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.get(0)?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: ""

        } catch (e: Exception) {
            Log.e(TAG, "Extract error: ${e.message}")
            ""
        }
    }

    private fun cleanResponse(text: String): String {
        return text
            .replace("**", "")
            .replace("__", "")
            .replace("```", "")
            .replace("\\n", "\n")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}