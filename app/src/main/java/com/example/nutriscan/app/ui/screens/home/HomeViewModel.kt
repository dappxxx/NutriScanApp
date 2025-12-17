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
                    ),
                    HealthTip(
                        emoji = "🚶",
                        title = "Olahraga Rutin",
                        description = "Aktivitas fisik membantu tubuh menggunakan insulin lebih efektif.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "📊",
                        title = "Pantau Karbohidrat",
                        description = "Hitung asupan karbohidrat harian, idealnya 45-60g per makan.",
                        priority = HealthTipPriority.HIGH
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
                    ),
                    HealthTip(
                        emoji = "🍷",
                        title = "Hindari Alkohol",
                        description = "Alkohol dapat meningkatkan tekanan darah secara signifikan.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🧘",
                        title = "Kelola Stres",
                        description = "Stres dapat meningkatkan tekanan darah. Coba meditasi atau yoga.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🥬",
                        title = "Diet DASH",
                        description = "Ikuti pola makan DASH: banyak sayur, buah, dan produk susu rendah lemak.",
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
                    ),
                    HealthTip(
                        emoji = "🥚",
                        title = "Batasi Telur",
                        description = "Maksimal 3-4 kuning telur per minggu untuk jaga kolesterol.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🍖",
                        title = "Pilih Daging Tanpa Lemak",
                        description = "Pilih daging ayam tanpa kulit atau ikan daripada daging merah.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🫘",
                        title = "Konsumsi Kacang-kacangan",
                        description = "Kacang almond, walnut membantu menurunkan kolesterol jahat (LDL).",
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
                    ),
                    HealthTip(
                        emoji = "🥤",
                        title = "Hindari Minuman Manis",
                        description = "Fruktosa dalam minuman manis dapat meningkatkan asam urat.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🥛",
                        title = "Konsumsi Susu Rendah Lemak",
                        description = "Produk susu rendah lemak dapat membantu menurunkan asam urat.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "☕",
                        title = "Kopi Boleh Diminum",
                        description = "Kopi (tanpa gula berlebih) dapat membantu menurunkan asam urat.",
                        priority = HealthTipPriority.LOW
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
                    ),
                    HealthTip(
                        emoji = "🍫",
                        title = "Hindari Cokelat",
                        description = "Cokelat dapat merelaksasi katup lambung dan memicu GERD.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🧅",
                        title = "Batasi Bawang",
                        description = "Bawang merah dan bawang putih mentah dapat memicu asam lambung.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🚭",
                        title = "Hindari Rokok",
                        description = "Merokok melemahkan katup lambung dan memperburuk GERD.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "👔",
                        title = "Pakaian Longgar",
                        description = "Hindari pakaian ketat di perut yang menekan lambung.",
                        priority = HealthTipPriority.LOW
                    )
                )
            }

            conditionLower.contains("obesitas") || conditionLower.contains("kegemukan") || conditionLower.contains("diet") || conditionLower.contains("berat badan") -> {
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
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "💧",
                        title = "Minum Air Sebelum Makan",
                        description = "Minum air putih 30 menit sebelum makan membantu kontrol porsi.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🍽️",
                        title = "Makan Perlahan",
                        description = "Kunyah makanan dengan baik, makan selama 20 menit agar otak register kenyang.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🥤",
                        title = "Hindari Minuman Kalori",
                        description = "Ganti soda dan jus kemasan dengan air putih atau teh tanpa gula.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🌙",
                        title = "Hindari Makan Malam Larut",
                        description = "Usahakan tidak makan berat setelah jam 7 malam.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("alergi kacang") -> {
                listOf(
                    HealthTip(
                        emoji = "🥜",
                        title = "Hindari Semua Produk Kacang",
                        description = "Termasuk selai kacang, minyak kacang, dan makanan yang mengandung kacang.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "📋",
                        title = "Baca Label dengan Teliti",
                        description = "Cek label 'may contain nuts' atau 'processed in facility with nuts'.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🏥",
                        title = "Siapkan Obat Darurat",
                        description = "Selalu bawa antihistamin atau EpiPen jika alergi parah.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍳",
                        title = "Masak Sendiri",
                        description = "Masak di rumah lebih aman karena Anda kontrol semua bahan.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🗣️",
                        title = "Informasikan Alergi",
                        description = "Selalu beritahu pelayan restoran tentang alergi Anda sebelum memesan.",
                        priority = HealthTipPriority.HIGH
                    )
                )
            }

            conditionLower.contains("alergi susu") || conditionLower.contains("laktosa") -> {
                listOf(
                    HealthTip(
                        emoji = "🥛",
                        title = "Pilih Susu Alternatif",
                        description = "Ganti dengan susu almond, oat, atau kedelai yang bebas laktosa.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🧀",
                        title = "Hindari Produk Susu",
                        description = "Termasuk keju, yogurt, mentega, dan es krim dari susu sapi.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "📋",
                        title = "Cek Kandungan Whey/Kasein",
                        description = "Banyak makanan olahan mengandung whey atau kasein dari susu.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "💊",
                        title = "Suplemen Kalsium",
                        description = "Konsumsi suplemen kalsium untuk menjaga kesehatan tulang.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🥬",
                        title = "Sumber Kalsium Alternatif",
                        description = "Dapatkan kalsium dari brokoli, bayam, dan ikan teri.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("alergi seafood") || conditionLower.contains("alergi laut") -> {
                listOf(
                    HealthTip(
                        emoji = "🦐",
                        title = "Hindari Semua Seafood",
                        description = "Termasuk udang, kepiting, cumi, kerang, dan ikan laut.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍜",
                        title = "Waspadai Saus",
                        description = "Saus tiram, terasi, dan kecap ikan mengandung seafood.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🏥",
                        title = "Siapkan Obat Darurat",
                        description = "Alergi seafood bisa parah. Selalu siap dengan antihistamin.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍣",
                        title = "Hati-hati di Restoran Asia",
                        description = "Banyak masakan Asia menggunakan produk laut sebagai bumbu.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "💊",
                        title = "Suplemen Omega-3",
                        description = "Dapatkan omega-3 dari suplemen alga atau minyak flaxseed.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("alergi gluten") || conditionLower.contains("celiac") -> {
                listOf(
                    HealthTip(
                        emoji = "🌾",
                        title = "Hindari Gluten",
                        description = "Hindari gandum, barley, rye, dan semua produk turunannya.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍚",
                        title = "Pilih Karbohidrat Alternatif",
                        description = "Pilih nasi, kentang, quinoa, atau tepung bebas gluten.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "📋",
                        title = "Cek Label Gluten-Free",
                        description = "Pastikan produk memiliki label 'gluten-free' resmi.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍺",
                        title = "Hindari Bir",
                        description = "Bir mengandung gluten. Pilih wine atau cider sebagai alternatif.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🥣",
                        title = "Waspadai Cross-Contamination",
                        description = "Gunakan peralatan masak terpisah untuk makanan bebas gluten.",
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
                    ),
                    HealthTip(
                        emoji = "🥬",
                        title = "Konsumsi Sayuran Hijau",
                        description = "Bayam, kangkung, dan brokoli kaya akan zat besi.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🫘",
                        title = "Makan Kacang-kacangan",
                        description = "Kacang merah, kedelai, dan lentil adalah sumber zat besi nabati.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🥚",
                        title = "Konsumsi Telur",
                        description = "Telur mengandung zat besi dan vitamin B12 untuk produksi sel darah.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "💊",
                        title = "Suplemen Jika Perlu",
                        description = "Konsultasikan dengan dokter untuk suplemen zat besi jika diperlukan.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("jantung") || conditionLower.contains("kardio") -> {
                listOf(
                    HealthTip(
                        emoji = "🫀",
                        title = "Jaga Kesehatan Jantung",
                        description = "Batasi lemak jenuh dan trans untuk kesehatan jantung optimal.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🧂",
                        title = "Kurangi Garam",
                        description = "Batasi natrium untuk menjaga tekanan darah tetap normal.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🐟",
                        title = "Makan Ikan Berlemak",
                        description = "Omega-3 dari ikan salmon, makarel membantu kesehatan jantung.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🥑",
                        title = "Lemak Sehat",
                        description = "Pilih minyak zaitun, alpukat, dan kacang-kacangan.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🚶",
                        title = "Olahraga Teratur",
                        description = "Jalan kaki atau aktivitas ringan 30 menit sehari.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🚭",
                        title = "Hindari Rokok",
                        description = "Merokok sangat berbahaya untuk kesehatan jantung.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍷",
                        title = "Batasi Alkohol",
                        description = "Konsumsi alkohol berlebihan merusak otot jantung.",
                        priority = HealthTipPriority.HIGH
                    )
                )
            }

            conditionLower.contains("ginjal") -> {
                listOf(
                    HealthTip(
                        emoji = "🧂",
                        title = "Batasi Natrium",
                        description = "Kurangi garam untuk mengurangi beban kerja ginjal.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🥩",
                        title = "Kontrol Protein",
                        description = "Batasi konsumsi protein sesuai anjuran dokter.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "💧",
                        title = "Perhatikan Cairan",
                        description = "Ikuti anjuran dokter tentang asupan cairan harian.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🍌",
                        title = "Batasi Kalium",
                        description = "Hindari pisang, jeruk, dan tomat jika kalium harus dibatasi.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🥛",
                        title = "Batasi Fosfor",
                        description = "Kurangi produk susu dan makanan olahan tinggi fosfor.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "📋",
                        title = "Pantau Asupan",
                        description = "Catat makanan harian untuk kontrol nutrisi yang lebih baik.",
                        priority = HealthTipPriority.MEDIUM
                    )
                )
            }

            conditionLower.contains("hamil") || conditionLower.contains("kehamilan") -> {
                listOf(
                    HealthTip(
                        emoji = "🥬",
                        title = "Konsumsi Asam Folat",
                        description = "Penting untuk perkembangan janin. Makan sayuran hijau dan suplemen.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🐟",
                        title = "Hindari Ikan Mentah",
                        description = "Hindari sushi dan ikan mentah yang berisiko kontaminasi.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "☕",
                        title = "Batasi Kafein",
                        description = "Maksimal 200mg kafein per hari (sekitar 1 cangkir kopi).",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🧀",
                        title = "Hindari Keju Lunak",
                        description = "Keju brie, camembert berisiko listeria. Pilih keju keras.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🥛",
                        title = "Perbanyak Kalsium",
                        description = "Penting untuk tulang bayi. Konsumsi susu dan produk susu.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🥩",
                        title = "Pastikan Daging Matang",
                        description = "Hindari daging setengah matang untuk cegah toksoplasmosis.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "💊",
                        title = "Konsumsi Zat Besi",
                        description = "Kebutuhan zat besi meningkat saat hamil untuk cegah anemia.",
                        priority = HealthTipPriority.HIGH
                    )
                )
            }

            conditionLower.contains("menyusui") -> {
                listOf(
                    HealthTip(
                        emoji = "💧",
                        title = "Minum Banyak Air",
                        description = "Produksi ASI membutuhkan banyak cairan. Minum 10-12 gelas sehari.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🥗",
                        title = "Makan Bergizi Seimbang",
                        description = "Kebutuhan kalori meningkat 500 kkal/hari saat menyusui.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🐟",
                        title = "Konsumsi Omega-3",
                        description = "Penting untuk perkembangan otak bayi. Makan ikan 2-3x seminggu.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "☕",
                        title = "Batasi Kafein",
                        description = "Kafein bisa masuk ke ASI. Maksimal 2 cangkir kopi/hari.",
                        priority = HealthTipPriority.MEDIUM
                    ),
                    HealthTip(
                        emoji = "🍷",
                        title = "Hindari Alkohol",
                        description = "Alkohol masuk ke ASI dan berbahaya untuk bayi.",
                        priority = HealthTipPriority.HIGH
                    ),
                    HealthTip(
                        emoji = "🥛",
                        title = "Perbanyak Kalsium",
                        description = "Kalsium penting untuk kualitas ASI dan kesehatan tulang ibu.",
                        priority = HealthTipPriority.HIGH
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
                    ),
                    HealthTip(
                        emoji = "📖",
                        title = "Baca Label Nutrisi",
                        description = "Perhatikan kandungan nutrisi sebelum mengonsumsi makanan kemasan.",
                        priority = HealthTipPriority.MEDIUM
                    )
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