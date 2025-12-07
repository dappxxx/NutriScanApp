package com.nutriscan.app.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.model.Profile
import com.nutriscan.app.data.model.ScanSession
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

    init {
        loadUserData()
        loadRecentScans()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading

            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                // Coba ambil dari profile database dulu
                val result = profileRepository.getProfile(userId)
                result.fold(
                    onSuccess = { profile ->
                        _profileState.value = UiState.Success(profile)

                        // Prioritas: Profile DB > Auth Metadata > Default
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

                        // Fallback ke auth metadata jika profile gagal
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

    /**
     * Update riwayat penyakit user
     */
    fun updateHealthConditions(conditions: List<String>) {
        viewModelScope.launch {
            _updateHealthState.value = UiState.Loading

            val userId = authRepository.getCurrentUserId()

            // DEBUG LOG
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
            // Tips umum jika tidak ada kondisi kesehatan
            tips.addAll(getGeneralHealthTips())
        } else {
            // Tips berdasarkan kondisi kesehatan
            conditions.forEach { condition ->
                tips.addAll(getTipsForCondition(condition))
            }

            // Tambah beberapa tips umum
            tips.addAll(getGeneralHealthTips().take(2))
        }

        // Remove duplicates dan shuffle
        _personalizedTips.value = tips.distinctBy { it.title }.shuffled().take(6)
    }

    private fun getTipsForCondition(condition: String): List<HealthTip> {
        val conditionLower = condition.lowercase()

        return when {
            conditionLower.contains("diabetes") || conditionLower.contains("gula darah") -> {
                listOf(
                    HealthTip(
                        emoji = "🍬",
                        title = "Batasi Gula",
                        description = "Hindari makanan dengan gula >10g per sajian. Pilih makanan dengan indeks glikemik rendah.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🥗",
                        title = "Perbanyak Serat",
                        description = "Konsumsi sayuran hijau dan biji-bijian utuh untuk mengontrol gula darah.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "⏰",
                        title = "Makan Teratur",
                        description = "Jaga jadwal makan yang teratur untuk stabilkan kadar gula darah.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🍎",
                        title = "Pilih Buah Rendah GI",
                        description = "Pilih apel, pir, jeruk daripada semangka atau nanas yang tinggi gula.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("hipertensi") || conditionLower.contains("darah tinggi") || conditionLower.contains("tekanan darah") -> {
                listOf(
                    HealthTip(
                        emoji = "🧂",
                        title = "Kurangi Garam",
                        description = "Batasi natrium <1500mg/hari. Hindari makanan kemasan tinggi natrium.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍌",
                        title = "Perbanyak Kalium",
                        description = "Konsumsi pisang, alpukat, dan bayam untuk menyeimbangkan tekanan darah.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🚶",
                        title = "Olahraga Teratur",
                        description = "Jalan kaki 30 menit sehari membantu menurunkan tekanan darah.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "☕",
                        title = "Batasi Kafein",
                        description = "Kurangi kopi dan teh. Maksimal 2 cangkir kopi per hari.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("kolesterol") -> {
                listOf(
                    HealthTip(
                        emoji = "🥑",
                        title = "Lemak Sehat",
                        description = "Pilih alpukat, kacang-kacangan, dan minyak zaitun daripada lemak jenuh.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🐟",
                        title = "Konsumsi Ikan",
                        description = "Makan ikan berlemak (salmon, tuna) 2x seminggu untuk omega-3.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🚫",
                        title = "Hindari Gorengan",
                        description = "Kurangi makanan yang digoreng dan lemak trans dari makanan olahan.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🌾",
                        title = "Pilih Whole Grain",
                        description = "Ganti nasi putih dengan nasi merah atau oatmeal untuk serat lebih.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("asam urat") || conditionLower.contains("gout") -> {
                listOf(
                    HealthTip(
                        emoji = "💧",
                        title = "Minum Air Putih",
                        description = "Minimal 8-10 gelas per hari untuk membantu mengeluarkan asam urat.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🦐",
                        title = "Hindari Purin Tinggi",
                        description = "Batasi jeroan, seafood, dan daging merah yang tinggi purin.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍒",
                        title = "Konsumsi Ceri",
                        description = "Ceri dan buah beri dapat membantu menurunkan kadar asam urat.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🍺",
                        title = "Hindari Alkohol",
                        description = "Alkohol terutama bir dapat meningkatkan kadar asam urat.",
                        priority = HealthTipPriority.HIGH
                    )
                )
            }

            conditionLower.contains("maag") || conditionLower.contains("gerd") || conditionLower.contains("lambung") -> {
                listOf(
                    HealthTip(
                        emoji = "🍽️",
                        title = "Makan Porsi Kecil",
                        description = "Makan porsi kecil tapi sering (5-6x sehari) untuk kurangi beban lambung.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🌶️",
                        title = "Hindari Makanan Pedas",
                        description = "Kurangi makanan pedas, asam, dan berminyak yang memicu asam lambung.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🛏️",
                        title = "Jangan Langsung Tidur",
                        description = "Tunggu 2-3 jam setelah makan sebelum berbaring atau tidur.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "☕",
                        title = "Batasi Kafein",
                        description = "Kurangi kopi dan minuman berkarbonasi yang merangsang asam lambung.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("obesitas") || conditionLower.contains("kegemukan") || conditionLower.contains("diet") -> {
                listOf(
                    HealthTip(
                        emoji = "🥦",
                        title = "Perbanyak Sayuran",
                        description = "Isi setengah piring dengan sayuran untuk kenyang lebih lama dengan kalori rendah.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "📊",
                        title = "Perhatikan Kalori",
                        description = "Batasi 1500-1800 kkal/hari untuk penurunan berat badan yang sehat.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🏃",
                        title = "Aktivitas Fisik",
                        description = "Minimal 150 menit olahraga sedang per minggu untuk bakar kalori.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍬",
                        title = "Hindari Gula Tambahan",
                        description = "Skip minuman manis dan makanan tinggi gula yang padat kalori.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("alergi") -> {
                listOf(
                    HealthTip(
                        emoji = "📋",
                        title = "Baca Label",
                        description = "Selalu baca label makanan untuk mengidentifikasi alergen tersembunyi.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🏥",
                        title = "Siapkan Obat",
                        description = "Selalu bawa obat antihistamin atau EpiPen jika alergi parah.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍳",
                        title = "Masak Sendiri",
                        description = "Masak di rumah lebih aman karena Anda kontrol semua bahan.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("anemia") || conditionLower.contains("kurang darah") -> {
                listOf(
                    HealthTip(
                        emoji = "🥩",
                        title = "Konsumsi Zat Besi",
                        description = "Makan daging merah, bayam, dan kacang-kacangan yang kaya zat besi.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍊",
                        title = "Vitamin C",
                        description = "Kombinasikan makanan kaya besi dengan vitamin C untuk penyerapan optimal.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "☕",
                        title = "Hindari Teh Saat Makan",
                        description = "Tannin dalam teh dapat menghambat penyerapan zat besi.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            else -> {
                // Kondisi tidak dikenal, return tips umum
                listOf(
                    HealthTip(
                        emoji = "⚕️",
                        title = "Konsultasi Dokter",
                        description = "Selalu konsultasikan kondisi $condition dengan dokter untuk saran nutrisi spesifik.",
                        priority = HealthTipPriority.HIGH
                    )
                )
            }
        }
    }

    private fun getGeneralHealthTips(): List<HealthTip> {
        return listOf(
            HealthTip(
                emoji = "💧",
                title = "Minum Air Putih",
                description = "Konsumsi minimal 8 gelas (2 liter) air putih setiap hari untuk hidrasi optimal.",
                priority = HealthTipPriority.MEDIUM
            ),
            HealthTip(
                emoji = "🥗",
                title = "Perbanyak Sayur & Buah",
                description = "Konsumsi 5 porsi sayur dan buah setiap hari untuk vitamin dan serat.",
                priority = HealthTipPriority.MEDIUM
            ),
            HealthTip(
                emoji = "🚶",
                title = "Aktif Bergerak",
                description = "Minimal 30 menit aktivitas fisik setiap hari untuk kesehatan jantung.",
                priority = HealthTipPriority.MEDIUM
            ),
            HealthTip(
                emoji = "😴",
                title = "Tidur Cukup",
                description = "Tidur 7-9 jam setiap malam untuk pemulihan tubuh yang optimal.",
                priority = HealthTipPriority.LOW
            ),
            HealthTip(
                emoji = "📖",
                title = "Baca Label Nutrisi",
                description = "Selalu periksa label nutrisi sebelum membeli makanan kemasan.",
                priority = HealthTipPriority.LOW
            ),
            HealthTip(
                emoji = "🍽️",
                title = "Makan Teratur",
                description = "Jaga pola makan 3x sehari dengan porsi seimbang.",
                priority = HealthTipPriority.LOW
            )
        )
    }

    fun refresh() {
        loadUserData()
        loadRecentScans()
    }

    fun resetUpdateState() {
        _updateHealthState.value = UiState.Idle
    }
}

/**
 * Data class untuk Health Tips
 */
data class HealthTip(
    val emoji: String,
    val title: String,
    val description: String,
    val priority: HealthTipPriority = HealthTipPriority.MEDIUM
)

enum class HealthTipPriority {
    HIGH, MEDIUM, LOW
}