package com.nutriscan.app.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.model.Profile
import com.nutriscan.app.data.model.ScanSession
import com.nutriscan.app.data.model.StreakInfo
import com.nutriscan.app.data.model.StreakStatus
import com.nutriscan.app.data.model.UiState
import com.nutriscan.app.data.repository.AuthRepository
import com.nutriscan.app.data.repository.ProfileRepository
import com.nutriscan.app.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val profileRepository = ProfileRepository()
    private val scanRepository = ScanRepository()

    private val _profileState = MutableStateFlow<UiState<Profile?>>(UiState.Idle)
    val profileState: StateFlow<UiState<Profile?>> = _profileState.asStateFlow()

    private val _recentScans = MutableStateFlow<UiState<List<ScanSession>>>(UiState.Idle)
    val recentScans: StateFlow<UiState<List<ScanSession>>> = _recentScans.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _healthConditions = MutableStateFlow<List<String>>(emptyList())
    val healthConditions: StateFlow<List<String>> = _healthConditions.asStateFlow()

    private val _personalizedTips = MutableStateFlow<List<HealthTip>>(emptyList())
    val personalizedTips: StateFlow<List<HealthTip>> = _personalizedTips.asStateFlow()

    private val _updateHealthState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateHealthState: StateFlow<UiState<Unit>> = _updateHealthState.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════════
    // STREAK STATE 🔥
    // ═══════════════════════════════════════════════════════════════════════════

    private val _streakInfo = MutableStateFlow(StreakInfo())
    val streakInfo: StateFlow<StreakInfo> = _streakInfo.asStateFlow()

    init {
        loadUserData()
        loadRecentScans()
        loadStreakInfo()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading

            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val result = profileRepository.getProfile(userId)
                result.fold(
                    onSuccess = { profile ->
                        _profileState.value = UiState.Success(profile)

                        val nameFromProfile = profile?.fullName
                        val nameFromAuth = authRepository.getCurrentUser()?.fullName

                        _userName.value = when {
                            !nameFromProfile.isNullOrBlank() -> nameFromProfile
                            !nameFromAuth.isNullOrBlank() -> nameFromAuth
                            else -> "User"
                        }

                        _healthConditions.value = profile?.getAllHealthConditions() ?: emptyList()
                        generatePersonalizedTips(profile)
                    },
                    onFailure = { error ->
                        _profileState.value = UiState.Error(error.message ?: "Gagal memuat profil")
                        viewModelScope.launch {
                            val user = authRepository.getCurrentUser()
                            _userName.value = user?.fullName?.takeIf { it.isNotBlank() } ?: "User"
                        }
                        generatePersonalizedTips(null)
                    }
                )
            } else {
                _profileState.value = UiState.Error("User tidak ditemukan")
                _userName.value = "User"
                generatePersonalizedTips(null)
            }
        }
    }

    fun loadRecentScans() {
        viewModelScope.launch {
            _recentScans.value = UiState.Loading

            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val result = scanRepository.getScanHistory(userId)
                result.fold(
                    onSuccess = { sessions ->
                        _recentScans.value = UiState.Success(sessions.take(5))
                    },
                    onFailure = { error ->
                        _recentScans.value = UiState.Error(error.message ?: "Gagal memuat riwayat")
                    }
                )
            } else {
                _recentScans.value = UiState.Error("User tidak ditemukan")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STREAK FUNCTIONS 🔥
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Load streak info dari database
     */
    fun loadStreakInfo() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val result = profileRepository.getStreakInfo(userId)
                result.fold(
                    onSuccess = { info ->
                        _streakInfo.value = info
                        Log.d("HomeViewModel", "Streak loaded: $info")
                    },
                    onFailure = { error ->
                        Log.e("HomeViewModel", "Failed to load streak: ${error.message}")
                        _streakInfo.value = StreakInfo()
                    }
                )
            }
        }
    }

    /**
     * Refresh streak (dipanggil setelah scan)
     */
    fun refreshStreak() {
        loadStreakInfo()
    }

    /**
     * Update riwayat penyakit user
     */
    fun updateHealthConditions(conditions: List<String>) {
        viewModelScope.launch {
            _updateHealthState.value = UiState.Loading

            val userId = authRepository.getCurrentUserId()

            Log.d("HomeViewModel", "updateHealthConditions - userId: $userId")
            Log.d("HomeViewModel", "updateHealthConditions - conditions: $conditions")

            if (userId != null) {
                val result = profileRepository.updateHealthConditions(userId, conditions)
                result.fold(
                    onSuccess = {
                        Log.d("HomeViewModel", "updateHealthConditions - SUCCESS")
                        _healthConditions.value = conditions
                        _updateHealthState.value = UiState.Success(Unit)
                        loadUserData()
                    },
                    onFailure = { error ->
                        Log.e("HomeViewModel", "updateHealthConditions - FAILED: ${error.message}")
                        _updateHealthState.value = UiState.Error(error.message ?: "Gagal menyimpan")
                    }
                )
            } else {
                Log.e("HomeViewModel", "updateHealthConditions - userId is NULL!")
                _updateHealthState.value = UiState.Error("User tidak ditemukan")
            }
        }
    }

    /**
     * Generate tips kesehatan berdasarkan kondisi kesehatan user
     */
    private fun generatePersonalizedTips(profile: Profile?) {
        val tips = mutableListOf<HealthTip>()
        val conditions = profile?.getAllHealthConditions() ?: emptyList()

        if (conditions.isEmpty()) {
            tips.addAll(getGeneralHealthTips())
        } else {
            conditions.forEach { condition ->
                tips.addAll(getTipsForCondition(condition))
            }
            tips.addAll(getGeneralHealthTips().take(2))
        }

        _personalizedTips.value = tips.distinctBy { it.title }.shuffled().take(6)
    }

    private fun getTipsForCondition(condition: String): List<HealthTip> {
        val conditionLower = condition.lowercase()

        return when {
            conditionLower.contains("diabetes") || conditionLower.contains("gula darah") -> {
                listOf(
                    HealthTip("🍬", "Batasi Gula", "Hindari makanan dengan gula >10g per sajian.", HealthTipPriority.HIGH),
                    HealthTip("🥗", "Perbanyak Serat", "Konsumsi sayuran hijau dan biji-bijian utuh.", HealthTipPriority.HIGH),
                    HealthTip("⏰", "Makan Teratur", "Jaga jadwal makan yang teratur.", HealthTipPriority.MEDIUM)
                )
            }
            conditionLower.contains("hipertensi") || conditionLower.contains("darah tinggi") -> {
                listOf(
                    HealthTip("🧂", "Kurangi Garam", "Batasi natrium <1500mg/hari.", HealthTipPriority.HIGH),
                    HealthTip("🍌", "Perbanyak Kalium", "Konsumsi pisang, alpukat, dan bayam.", HealthTipPriority.HIGH)
                )
            }
            conditionLower.contains("kolesterol") -> {
                listOf(
                    HealthTip("🥑", "Lemak Sehat", "Pilih alpukat dan minyak zaitun.", HealthTipPriority.HIGH),
                    HealthTip("🐟", "Konsumsi Ikan", "Makan ikan berlemak 2x seminggu.", HealthTipPriority.HIGH)
                )
            }
            conditionLower.contains("maag") || conditionLower.contains("gerd") -> {
                listOf(
                    HealthTip("🍽️", "Makan Porsi Kecil", "Makan porsi kecil tapi sering.", HealthTipPriority.HIGH),
                    HealthTip("🌶️", "Hindari Pedas", "Kurangi makanan pedas dan asam.", HealthTipPriority.HIGH)
                )
            }
            else -> {
                listOf(
                    HealthTip("⚕️", "Konsultasi Dokter", "Konsultasikan kondisi $condition dengan dokter.", HealthTipPriority.HIGH)
                )
            }
        }
    }

    private fun getGeneralHealthTips(): List<HealthTip> {
        return listOf(
            HealthTip("💧", "Minum Air Putih", "Konsumsi minimal 8 gelas air putih setiap hari.", HealthTipPriority.MEDIUM),
            HealthTip("🥗", "Perbanyak Sayur & Buah", "Konsumsi 5 porsi sayur dan buah setiap hari.", HealthTipPriority.MEDIUM),
            HealthTip("🚶", "Aktif Bergerak", "Minimal 30 menit aktivitas fisik setiap hari.", HealthTipPriority.MEDIUM),
            HealthTip("😴", "Tidur Cukup", "Tidur 7-9 jam setiap malam.", HealthTipPriority.LOW),
            HealthTip("📖", "Baca Label Nutrisi", "Selalu periksa label nutrisi sebelum membeli.", HealthTipPriority.LOW)
        )
    }

    fun refresh() {
        loadUserData()
        loadRecentScans()
        loadStreakInfo()
    }

    fun resetUpdateState() {
        _updateHealthState.value = UiState.Idle
    }
}

data class HealthTip(
    val emoji: String,
    val title: String,
    val description: String,
    val priority: HealthTipPriority = HealthTipPriority.MEDIUM
)

enum class HealthTipPriority {
    HIGH, MEDIUM, LOW
}