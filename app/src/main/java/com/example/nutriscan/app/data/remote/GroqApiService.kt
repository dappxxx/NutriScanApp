//package com.nutriscan.app.data.remote
//
//import android.util.Log
//import com.nutriscan.app.utils.Constants
//import io.ktor.client.*
//import io.ktor.client.engine.android.*
//import io.ktor.client.plugins.*
//import io.ktor.client.request.*
//import io.ktor.client.statement.*
//import io.ktor.http.*
//import kotlinx.serialization.encodeToString
//import kotlinx.serialization.json.*
//
//class GroqApiService {
//
//    private val TAG = "GroqApiService"
//
//    private val json = Json {
//        ignoreUnknownKeys = true
//        isLenient = true
//    }
//
//    private val client = HttpClient(Android) {
//        install(HttpTimeout) {
//            requestTimeoutMillis = 90000
//            connectTimeoutMillis = 30000
//        }
//    }
//
//    /**
//     * Analisis teks label nutrisi
//     */
//    suspend fun analyzeNutritionLabel(ocrText: String, healthProfile: String = ""): Result<String> {
//        return try {
//            Log.d(TAG, "")
//            Log.d(TAG, "╔════════════════════════════════════╗")
//            Log.d(TAG, "║    Mengirim ke AI untuk analisis   ║")
//            Log.d(TAG, "╚════════════════════════════════════╝")
//            Log.d(TAG, "OCR Text length: ${ocrText.length}")
//            Log.d(TAG, "OCR Text preview:\n${ocrText.take(300)}...")
//            Log.d(TAG, "Health Profile: $healthProfile")
//
//            val prompt = buildAnalysisPrompt(ocrText, healthProfile)
//
//            val requestBody = buildJsonObject {
//                put("model", "llama-3.1-8b-instant")
//                put("messages", buildJsonArray {
//                    add(buildJsonObject {
//                        put("role", "user")
//                        put("content", prompt)
//                    })
//                })
//                put("max_tokens", 2000)
//                put("temperature", 0.2)
//            }
//
//            Log.d(TAG, "Sending request to Groq API...")
//
//            val response = client.post(Constants.GROQ_API_URL) {
//                contentType(ContentType.Application.Json)
//                header("Authorization", "Bearer ${Constants.GROQ_API_KEY}")
//                setBody(json.encodeToString(requestBody))
//            }
//
//            val responseText = response.bodyAsText()
//            Log.d(TAG, "Response status: ${response.status}")
//
//            if (response.status.value !in 200..299) {
//                Log.e(TAG, "API Error Response: $responseText")
//                return Result.failure(Exception("Server error: ${response.status}"))
//            }
//
//            val content = extractContent(responseText)
//
//            if (content.isBlank() || content.startsWith("Error")) {
//                Log.e(TAG, "Empty or error content: $content")
//                return Result.failure(Exception("AI tidak memberikan respons valid"))
//            }
//
//            val cleanedContent = cleanResponse(content)
//
//            Log.d(TAG, "")
//            Log.d(TAG, "┌────────── HASIL ANALISIS AI ──────────┐")
//            cleanedContent.lines().take(20).forEach { line ->
//                Log.d(TAG, "│ $line")
//            }
//            Log.d(TAG, "└────────────────────────────────────────┘")
//
//            Result.success(cleanedContent)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "GroqApiService Error: ${e.message}", e)
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * Build prompt untuk analisis nutrisi (initial scan)
//     */
//    private fun buildAnalysisPrompt(ocrText: String, healthProfile: String = ""): String {
//        val healthSection = if (healthProfile.isNotBlank()) {
//            """
//
//PROFIL KESEHATAN PENGGUNA:
//$healthProfile
//
//⚠️ PENTING: Berikan peringatan dan saran yang SPESIFIK berdasarkan kondisi kesehatan pengguna di atas!
//"""
//        } else {
//            ""
//        }
//
//        return """
//Anda adalah AHLI GIZI PROFESIONAL dengan pengalaman 15 tahun. Analisis label INFORMASI NILAI GIZI berikut dengan SANGAT DETAIL dan berikan saran yang PRAKTIS serta BERMANFAAT.
//$healthSection
//
//TEKS HASIL SCAN:
//$ocrText
//
//TUGAS:
//Susun informasi nutrisi dari teks di atas dengan format yang RAPI. Cocokkan setiap NAMA nutrisi dengan NILAI dan SATUAN yang tepat.
//
//ATURAN PENTING:
//1. Baca teks dengan teliti, cocokkan nama nutrisi dengan angka nilai (g/mg) dan % AKG dengan BENAR
//2. HANYA tulis informasi yang ADA di teks
//3. Jangan mengarang atau menambah data yang tidak ada
//4. Gunakan satuan yang benar (g, mg, kkal, %)
//5. Berikan analisis yang MENDALAM dan EDUKATIF
//6. Saran harus SPESIFIK dan ACTIONABLE
//${if (healthProfile.isNotBlank()) "7. SELALU pertimbangkan kondisi kesehatan pengguna dalam setiap saran!" else ""}
//
//FORMAT OUTPUT:
//
//📦 NAMA PRODUK
//[Tulis nama produk jika ada, atau "Tidak teridentifikasi"]
//
//📊 INFORMASI NILAI GIZI
//Takaran saji: [nilai dari teks]
//Jumlah sajian per kemasan: [nilai dari teks]
//
//Per sajian mengandung:
//• Energi: [nilai] kkal
//• Lemak total: [nilai]g ([%]% AKG)
//• Lemak jenuh: [nilai]g ([%]% AKG)
//• Kolesterol: [nilai]mg ([%]% AKG)
//• Protein: [nilai]g ([%]% AKG)
//• Karbohidrat total: [nilai]g ([%]% AKG)
//• Gula: [nilai]g
//• Serat: [nilai]g ([%]% AKG)
//• Natrium: [nilai]mg ([%]% AKG)
//[Tambahkan nutrisi lain jika ada]
//
//📈 ANALISIS KANDUNGAN
//[Berikan analisis DETAIL untuk setiap nutrisi utama:]
//
//🔸 Energi/Kalori:
//[Jelaskan apakah kalori tinggi/sedang/rendah, cocok untuk siapa]
//
//🔸 Lemak:
//[Analisis lemak total, jenuh, trans. Jelaskan dampaknya]
//
//🔸 Gula:
//[Bandingkan dengan batas harian WHO 25g]
//
//🔸 Natrium/Garam:
//[Bandingkan dengan batas harian 2000mg]
//
//🔸 Protein:
//[Jelaskan kecukupan protein]
//
//🔸 Serat:
//[Jelaskan kecukupan serat (kebutuhan 25-30g)]
//
//⚠️ PERINGATAN KESEHATAN${if (healthProfile.isNotBlank()) " (DIPERSONALISASI)" else ""}
//
//🔴 PERLU DIWASPADAI:
//[List hal-hal yang perlu diwaspadai]
//${if (healthProfile.isNotBlank()) "\n🎯 PERINGATAN KHUSUS UNTUK KONDISI ANDA:\n[Berikan peringatan SPESIFIK berdasarkan kondisi kesehatan pengguna]" else ""}
//
//🟡 PERHATIAN KHUSUS UNTUK:
//• Penderita diabetes: [saran spesifik]
//• Penderita hipertensi: [saran spesifik]
//• Penderita kolesterol tinggi: [saran spesifik]
//• Anak-anak: [saran spesifik]
//• Ibu hamil/menyusui: [saran spesifik]
//• Lansia: [saran spesifik]
//
//🟢 HAL POSITIF:
//[Sebutkan kandungan yang BAIK dari produk ini]
//
//📅 REKOMENDASI KONSUMSI${if (healthProfile.isNotBlank()) " (UNTUK KONDISI ANDA)" else ""}
//
//🕐 Frekuensi Aman:
//${if (healthProfile.isNotBlank()) "[Frekuensi Aman KHUSUS untuk kondisi kesehatan pengguna]" else "[Frekuensi aman untuk orang sehat]"}
//
//📏 Porsi yang Disarankan:
//[Jelaskan porsi ideal${if (healthProfile.isNotBlank()) " untuk kondisi pengguna" else ""}]
//
//⏰ Waktu Terbaik Mengonsumsi:
//[Kapan dan alasannya]
//
//🚫 Hindari Mengonsumsi Jika:
//• [Kondisi tertentu]
//
//✅ Aman Dikonsumsi Oleh:
//• [Kelompok yang aman]
//
//💡 TIPS KESEHATAN${if (healthProfile.isNotBlank()) " UNTUK KONDISI ANDA" else ""}
//
//${if (healthProfile.isNotBlank()) """
//Tips Khusus Berdasarkan Kondisi Kesehatan Anda:
//1. [Tips spesifik #1 - SESUAIKAN dengan kondisi pengguna]
//2. [Tips spesifik #2 - SESUAIKAN dengan kondisi pengguna]
//3. [Tips spesifik #3 - SESUAIKAN dengan kondisi pengguna]
//""" else """
//Tips Umum:
//1. [Tips #1]
//2. [Tips #2]
//3. [Tips #3]
//"""}
//
//Tips Menyeimbangkan Nutrisi:
//• [Saran makanan pendamping${if (healthProfile.isNotBlank()) " yang AMAN untuk kondisi pengguna" else ""}]
//• [Saran aktivitas fisik${if (healthProfile.isNotBlank()) " yang SESUAI" else ""}]
//• [Saran lainnya]
//
//🍽️ ALTERNATIF LEBIH SEHAT
//1. [Alternatif #1]: [Alasan]
//2. [Alternatif #2]: [Alasan]
//3. [Alternatif #3]: [Alasan]
//
//✅ KESIMPULAN
//
//📊 Rating Kesehatan: [⭐⭐⭐⭐⭐ / ⭐⭐⭐⭐ / ⭐⭐⭐ / ⭐⭐ / ⭐] dari 5
//${if (healthProfile.isNotBlank()) "📊 Rating untuk Kondisi Anda: [⭐⭐⭐⭐⭐ / ⭐⭐⭐⭐ / ⭐⭐⭐ / ⭐⭐ / ⭐] dari 5" else ""}
//
//📝 Ringkasan:
//[2-3 kalimat kesimpulan${if (healthProfile.isNotBlank()) " yang mempertimbangkan kondisi kesehatan Anda" else ""}]
//
//💬 Saran Akhir${if (healthProfile.isNotBlank()) " untuk Anda" else ""}:
//[1-2 kalimat saran praktis${if (healthProfile.isNotBlank()) " yang disesuaikan dengan kondisi kesehatan Anda" else ""}]
//
//═══════════════════════════════════════════════════════════════
//""".trimIndent()
//    }
//
//    /**
//     * Chat dengan AI - UPDATED: Dengan personalisasi kesehatan
//     */
//    suspend fun chat(
//        message: String,
//        context: String,
//        history: List<Pair<String, String>>,
//        healthProfile: String = "" // ← PARAMETER BARU
//    ): Result<String> {
//        return try {
//            Log.d(TAG, "Chat: $message")
//            Log.d(TAG, "Health Profile for chat: ${healthProfile.take(100)}...")
//
//            val systemPrompt = buildChatSystemPrompt(context, healthProfile)
//
//            val messages = buildJsonArray {
//                add(buildJsonObject {
//                    put("role", "system")
//                    put("content", systemPrompt)
//                })
//
//                history.takeLast(6).forEach { (sender, msg) ->
//                    add(buildJsonObject {
//                        put("role", if (sender == "user") "user" else "assistant")
//                        put("content", msg)
//                    })
//                }
//
//                add(buildJsonObject {
//                    put("role", "user")
//                    put("content", message)
//                })
//            }
//
//            val requestBody = buildJsonObject {
//                put("model", "llama-3.1-8b-instant")
//                put("messages", messages)
//                put("max_tokens", 3500)
//                put("temperature", 0.3)
//            }
//
//            val response = client.post(Constants.GROQ_API_URL) {
//                contentType(ContentType.Application.Json)
//                header("Authorization", "Bearer ${Constants.GROQ_API_KEY}")
//                setBody(json.encodeToString(requestBody))
//            }
//
//            val responseText = response.bodyAsText()
//
//            if (response.status.value !in 200..299) {
//                return Result.failure(Exception("Server tidak merespons"))
//            }
//
//            val content = extractContent(responseText)
//            Result.success(cleanResponse(content))
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Chat error: ${e.message}")
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * Build system prompt untuk chat - DENGAN PERSONALISASI
//     */
//    private fun buildChatSystemPrompt(context: String, healthProfile: String): String {
//        val healthSection = if (healthProfile.isNotBlank()) {
//            """
//
//═══════════════════════════════════════════════════════════════
//📋 PROFIL KESEHATAN PENGGUNA (SANGAT PENTING!)
//═══════════════════════════════════════════════════════════════
//$healthProfile
//═══════════════════════════════════════════════════════════════
//
//⚠️ INSTRUKSI KRITIS:
//- SELALU pertimbangkan kondisi kesehatan pengguna dalam SETIAP jawaban
//- Jika pengguna punya diabetes → waspadai gula & karbohidrat
//- Jika pengguna punya hipertensi → waspadai natrium/garam
//- Jika pengguna punya kolesterol tinggi → waspadai lemak jenuh
//- Jika pengguna punya alergi → PERINGATKAN jika produk mengandung alergen
//- Berikan saran yang AMAN dan SESUAI untuk kondisi mereka
//- Jika ragu, sarankan untuk konsultasi dengan dokter/ahli gizi
//"""
//        } else {
//            """
//
//📋 PROFIL KESEHATAN: Tidak ada data kondisi kesehatan yang tersimpan.
//💡 Tip: Sarankan pengguna untuk melengkapi profil kesehatan mereka agar mendapat saran yang lebih personal.
//"""
//        }
//
//        return """
//Kamu adalah NutriScan AI, asisten ahli gizi profesional yang KHUSUS membahas nutrisi dan kesehatan makanan.
//
//$healthSection
//
//═══════════════════════════════════════════════════════════════
//📦 DATA PRODUK YANG SEDANG DIBAHAS:
//═══════════════════════════════════════════════════════════════
//$context
//═══════════════════════════════════════════════════════════════
//
//🎯 PERAN KAMU:
//- Ahli gizi dan nutrisi yang ramah, profesional, dan PEDULI dengan kondisi kesehatan user
//- Membantu user memahami kandungan nutrisi produk
//- Memberikan saran kesehatan yang DIPERSONALISASI berdasarkan kondisi user
//- SELALU mempertimbangkan riwayat penyakit, alergi, dan preferensi diet user
//
//${if (healthProfile.isNotBlank()) """
//🏥 CARA MENJAWAB DENGAN PERSONALISASI:
//1. Jika ditanya "apakah ini aman untuk saya?" → Jawab berdasarkan kondisi kesehatan mereka
//2. Jika ditanya tentang porsi/frekuensi → Sesuaikan dengan kondisi mereka
//3. Jika produk mengandung sesuatu yang berbahaya untuk kondisi mereka → PERINGATKAN!
//4. Selalu akhiri dengan saran yang relevan dengan kondisi mereka
//5. Gunakan bahasa yang empati dan supportive
//""" else """
//💡 USER BELUM MELENGKAPI PROFIL KESEHATAN:
//- Berikan saran umum yang aman
//- Sesekali ingatkan: "Untuk saran yang lebih personal, Anda bisa melengkapi profil kesehatan di menu Profile"
//"""}
//
//❌ TOPIK YANG HARUS DITOLAK (dengan sopan):
//- Pertanyaan di luar nutrisi/gizi/makanan (politik, teknologi, hiburan, dll)
//- Permintaan menulis kode program
//- Pertanyaan tentang hal-hal tidak terkait kesehatan makanan
//- Diagnosis medis (sarankan ke dokter)
//
//📝 CARA MENOLAK PERTANYAAN DI LUAR TOPIK:
//Jika user bertanya di luar topik nutrisi, jawab dengan sopan:
//"Maaf, saya adalah asisten khusus nutrisi dan gizi. Saya hanya bisa membantu Anda dengan pertanyaan seputar:
//• Kandungan gizi produk yang Anda scan
//• Rekomendasi konsumsi dan diet
//• Tips kesehatan terkait makanan
//${if (healthProfile.isNotBlank()) "• Saran berdasarkan kondisi kesehatan Anda" else ""}
//
//Silakan tanyakan hal-hal terkait nutrisi produk ini! 😊"
//
//📋 ATURAN MENJAWAB:
//1. Jawab berdasarkan data produk di atas
//2. ${if (healthProfile.isNotBlank()) "SELALU pertimbangkan kondisi kesehatan user" else "Berikan saran umum yang aman"}
//3. Gunakan bahasa Indonesia yang mudah dipahami
//4. Berikan saran praktis dan actionable
//5. Gunakan emoji untuk membuat jawaban lebih friendly
//6. ${if (healthProfile.isNotBlank()) "Jika ada risiko untuk kondisi user, PERINGATKAN dengan jelas" else "Ingatkan untuk cek profil kesehatan"}
//7. Jika ditanya hal di luar nutrisi, TOLAK dengan sopan
//8. Jangan pernah memberikan diagnosis medis - sarankan ke dokter jika perlu
//
//🚫 PENTING: JANGAN PERNAH menjawab pertanyaan yang tidak berhubungan dengan nutrisi, gizi, makanan, atau kesehatan!
//""".trimIndent()
//    }
//
//    /**
//     * Extract content dari response JSON
//     */
//    private fun extractContent(responseText: String): String {
//        return try {
//            val jsonObj = json.parseToJsonElement(responseText).jsonObject
//
//            // Check for error
//            jsonObj["error"]?.let { error ->
//                val msg = error.jsonObject["message"]?.jsonPrimitive?.contentOrNull
//                Log.e(TAG, "API returned error: $msg")
//                return "Error: $msg"
//            }
//
//            val choices = jsonObj["choices"]?.jsonArray
//            if (choices.isNullOrEmpty()) {
//                Log.e(TAG, "No choices in response")
//                return ""
//            }
//
//            val content = choices[0].jsonObject
//                .get("message")?.jsonObject
//                ?.get("content")?.jsonPrimitive?.contentOrNull
//
//            content ?: ""
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Extract content error: ${e.message}")
//            ""
//        }
//    }
//
//    /**
//     * Bersihkan response dari formatting markdown
//     */
//    private fun cleanResponse(text: String): String {
//        return text
//            .replace("**", "")
//            .replace("__", "")
//            .replace("```", "")
//            .replace("\\n", "\n")
//            .lines()
//            .joinToString("\n") { it.trimEnd() }
//            .replace(Regex("\n{3,}"), "\n\n")
//            .trim()
//    }
//}