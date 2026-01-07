package com.nutriscan.app.ui.screens.scan

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.model.ScanSession
import com.nutriscan.app.data.model.StreakInfo
import com.nutriscan.app.data.model.UiState
import com.nutriscan.app.data.remote.GeminiApiService
import com.nutriscan.app.data.repository.AuthRepository
import com.nutriscan.app.data.repository.ChatRepository
import com.nutriscan.app.data.repository.ProfileRepository
import com.nutriscan.app.data.repository.ScanRepository
import com.nutriscan.app.utils.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScanViewModel : ViewModel() {

    private val TAG = "ScanViewModel"

    private val authRepository = AuthRepository()
    private val scanRepository = ScanRepository()
    private val chatRepository = ChatRepository()
    private val profileRepository = ProfileRepository()
    private val geminiService = GeminiApiService()

    private val _scanState = MutableStateFlow<UiState<ScanSession>>(UiState.Idle)
    val scanState: StateFlow<UiState<ScanSession>> = _scanState.asStateFlow()

    private val _analysisProgress = MutableStateFlow("")
    val analysisProgress: StateFlow<String> = _analysisProgress.asStateFlow()

    // Streak info setelah scan
    private val _streakInfo = MutableStateFlow<StreakInfo?>(null)
    val streakInfo: StateFlow<StreakInfo?> = _streakInfo.asStateFlow()

    //scan di mulai
    fun processScan(bitmap: Bitmap) {
        viewModelScope.launch {
            _scanState.value = UiState.Loading

            try {
                Log.d(TAG, "╔══════════════════════════════════════╗")
                Log.d(TAG, "║         MEMULAI PROSES SCAN          ║")
                Log.d(TAG, "╚══════════════════════════════════════╝")

                // 1. Cek login untuk dapat user_id dari auth repository
                _analysisProgress.value = "Memeriksa akun..."
                val userId = authRepository.getCurrentUserId()
                    ?: throw Exception("Silakan login terlebih dahulu")

                // 2. Ambil profil kesehatan user di profile repository
                _analysisProgress.value = "Memuat profil kesehatan..."
                val profile = profileRepository.getProfile(userId).getOrNull()
                val healthProfile = profile?.getHealthProfileSummary() ?: ""

                Log.d(TAG, "Health Profile: $healthProfile")

                // 3. Upload gambar ke supabase di scan repository
                _analysisProgress.value = "Mengupload gambar..."
                val imageBytes = ImageUtils.bitmapToByteArray(bitmap, 85)
                val fileName = ImageUtils.generateUniqueFileName()

                val imageUrl = scanRepository.uploadImage(userId, imageBytes, fileName)
                    .getOrElse { throw Exception("Gagal upload: ${it.message}") }

                // 4. Scan & Analisis dengan Gemini api service
                _analysisProgress.value = if (healthProfile.isNotBlank()) {
                    "🔍 Menganalisis untuk kondisi Anda..."
                } else {
                    "🔍 Membaca label nutrisi..."
                }

                val analysis = geminiService.scanAndAnalyze(bitmap, healthProfile)
                    .getOrElse { throw Exception("Gagal analisis: ${it.message}") }

                // 5. Ekstrak nama produk
                val productName = extractProductName(analysis)

                // 6. Simpan ke database dan membuat session baru di scan repository
                _analysisProgress.value = "Menyimpan hasil..."
                val session = scanRepository.createScanSession(
                    userId = userId,
                    imageUrl = imageUrl,
                    productName = productName,
                    initialAnalysis = analysis
                ).getOrElse { throw Exception("Gagal menyimpan") }

                // 7. Simpan analisis sebagai chat pertama dengan chat repository
                chatRepository.insertMessage(session.id, "ai", analysis)

                // 8. UPDATE STREAK 🔥
                _analysisProgress.value = "🔥 Updating streak..."
                val streakResult = profileRepository.updateStreakAfterScan(userId)
                streakResult.fold(
                    onSuccess = { info ->
                        _streakInfo.value = info
                        Log.d(TAG, "🔥 Streak updated: ${info.currentStreak} days")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to update streak: ${error.message}")
                    }
                )

                Log.d(TAG, "✓ Scan berhasil!")
                _analysisProgress.value = ""
                _scanState.value = UiState.Success(session)

            } catch (e: Exception) {
                Log.e(TAG, "Scan Error: ${e.message}", e)
                _analysisProgress.value = ""
                _scanState.value = UiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    //untuk extract nama product
    private fun extractProductName(analysis: String): String {
        val patterns = listOf(
            Regex("""📦\s*NAMA PRODUK\s*\n+([^\n]+)""", RegexOption.IGNORE_CASE),
            Regex("""NAMA PRODUK[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(analysis)?.groupValues?.getOrNull(1)?.let { name ->
                val cleaned = name.replace(Regex("[*\\[\\]\"📦]"), "").trim()
                if (cleaned.length in 2..50 &&
                    !cleaned.lowercase().contains("tidak") &&
                    !cleaned.lowercase().contains("teridentifikasi")) {
                    return cleaned
                }
            }
        }
        return "Produk Scan"
    }

    fun resetState() {
        _scanState.value = UiState.Idle
        _analysisProgress.value = ""
        _streakInfo.value = null
    }
}