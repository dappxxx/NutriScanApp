package com.nutriscan.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("health_condition")
    val healthCondition: String? = null,
    @SerialName("health_conditions")
    val healthConditions: List<String>? = null,
    @SerialName("dietary_preferences")
    val dietaryPreferences: List<String>? = null,
    @SerialName("food_allergies")
    val foodAllergies: List<String>? = null,
    @SerialName("birth_year")
    val birthYear: Int? = null,
    val gender: String? = null,
    @SerialName("streak_count")
    val streakCount: Int? = 0,
    @SerialName("last_scan_date")
    val lastScanDate: String? = null,
    @SerialName("longest_streak")
    val longestStreak: Int? = 0,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    /**
     * Mendapatkan semua kondisi kesehatan sebagai list
     */
    fun getAllHealthConditions(): List<String> {
        val conditions = mutableListOf<String>()
        healthConditions?.let { conditions.addAll(it) }
        healthCondition?.let { condition ->
            if (condition.isNotBlank()) {
                conditions.addAll(condition.split(",").map { it.trim() }.filter { it.isNotBlank() })
            }
        }
        return conditions.distinct()
    }

    /**
     * Cek apakah user memiliki kondisi kesehatan tertentu
     */
    fun hasCondition(condition: String): Boolean {
        return getAllHealthConditions().any {
            it.lowercase().contains(condition.lowercase())
        }
    }

    /**
     * Mendapatkan ringkasan profil kesehatan untuk AI
     */
    fun getHealthProfileSummary(): String {
        val parts = mutableListOf<String>()

        val conditions = getAllHealthConditions()
        if (conditions.isNotEmpty()) {
            parts.add("Riwayat penyakit: ${conditions.joinToString(", ")}")
        }

        foodAllergies?.takeIf { it.isNotEmpty() }?.let {
            parts.add("Alergi makanan: ${it.joinToString(", ")}")
        }

        dietaryPreferences?.takeIf { it.isNotEmpty() }?.let {
            parts.add("Preferensi diet: ${it.joinToString(", ")}")
        }

        birthYear?.let { year ->
            val age = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - year
            parts.add("Usia: $age tahun")
        }

        gender?.let {
            val genderText = when(it) {
                "male" -> "Laki-laki"
                "female" -> "Perempuan"
                else -> "Lainnya"
            }
            parts.add("Jenis kelamin: $genderText")
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString("\n")
        } else {
            "Tidak ada informasi kesehatan yang tersimpan"
        }
    }
}

@Serializable
data class ProfileUpdate(
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("health_condition")
    val healthCondition: String? = null,
    @SerialName("health_conditions")
    val healthConditions: List<String>? = null,
    @SerialName("dietary_preferences")
    val dietaryPreferences: List<String>? = null,
    @SerialName("food_allergies")
    val foodAllergies: List<String>? = null,
    @SerialName("birth_year")
    val birthYear: Int? = null,
    val gender: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Data class untuk informasi Streak
 */
data class StreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastScanDate: String? = null,
    val isActiveToday: Boolean = false,
    val streakStatus: StreakStatus = StreakStatus.INACTIVE
)

@Serializable
data class StreakUpdate(
    @SerialName("streak_count")
    val streakCount: Int,
    @SerialName("longest_streak")
    val longestStreak: Int,
    @SerialName("last_scan_date")
    val lastScanDate: String,
    @SerialName("updated_at")
    val updatedAt: String
)

enum class StreakStatus {
    ACTIVE,      // Sudah scan hari ini, streak menyala
    AT_RISK,     // Belum scan hari ini, streak bisa hilang
    INACTIVE     // Streak sudah reset (tidak scan kemarin)
}