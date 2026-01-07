package com.nutriscan.app.data.repository

import android.util.Log
import com.nutriscan.app.data.model.Profile
import com.nutriscan.app.data.model.ProfileUpdate
import com.nutriscan.app.data.model.StreakInfo
import com.nutriscan.app.data.model.StreakStatus
import com.nutriscan.app.data.model.StreakUpdate
import com.nutriscan.app.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class ProfileRepository {
    private val TAG = "ProfileRepository"
    private val postgrest = SupabaseClient.client.postgrest

    suspend fun getProfile(userId: String): Result<Profile?> {
        return try {
            val profile = postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<Profile>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        fullName: String? = null,
        healthCondition: String? = null,
        healthConditions: List<String>? = null,
        foodAllergies: List<String>? = null,
        dietaryPreferences: List<String>? = null,
        birthYear: Int? = null,
        gender: String? = null
    ): Result<Unit> {
        return try {
            postgrest.from("profiles")
                .update(
                    ProfileUpdate(
                        fullName = fullName,
                        healthCondition = healthCondition,
                        healthConditions = healthConditions,
                        foodAllergies = foodAllergies,
                        dietaryPreferences = dietaryPreferences,
                        birthYear = birthYear,
                        gender = gender,
                        updatedAt = Clock.System.now().toString()
                    )
                ) {
                    filter {
                        eq("id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHealthConditions(
        userId: String,
        healthConditions: List<String>
    ): Result<Unit> {
        return try {
            val conditionString = healthConditions.joinToString(", ")

            postgrest.from("profiles")
                .update(
                    mapOf(
                        "health_condition" to conditionString,
                        "updated_at" to Clock.System.now().toString()
                    )
                ) {
                    filter {
                        eq("id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // STREAK FUNCTIONS
    /**
     * Mendapatkan informasi streak user
     */
    suspend fun getStreakInfo(userId: String): Result<StreakInfo> {
        return try {
            val profile = postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<Profile>()

            if (profile == null) {
                return Result.success(StreakInfo())
            }

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val lastScanDate = profile.lastScanDate?.let {
                try {
                    LocalDate.parse(it.take(10)) // Format: YYYY-MM-DD
                } catch (e: Exception) {
                    null
                }
            }

            val streakInfo = calculateStreakStatus(
                currentStreak = profile.streakCount ?: 0,
                longestStreak = profile.longestStreak ?: 0,
                lastScanDate = lastScanDate,
                today = today
            )

            Log.d(TAG, "StreakInfo: $streakInfo")
            Result.success(streakInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting streak: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update streak setelah user melakukan scan
     */
    suspend fun updateStreakAfterScan(userId: String): Result<StreakInfo> {
        return try {
            Log.d(TAG, "🔥 updateStreakAfterScan called for user: $userId")

            val profile = postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<Profile>()

            Log.d(TAG, "Current profile: streakCount=${profile?.streakCount}, lastScanDate=${profile?.lastScanDate}")

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val todayString = today.toString()

            Log.d(TAG, "Today: $todayString")

            val lastScanDate = profile?.lastScanDate?.let {
                try {
                    LocalDate.parse(it.take(10))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing lastScanDate: $it")
                    null
                }
            }

            Log.d(TAG, "Parsed lastScanDate: $lastScanDate")

            val currentStreak = profile?.streakCount ?: 0
            val longestStreak = profile?.longestStreak ?: 0

            // Hitung streak baru
            val (newStreak, newLongest) = when {
                lastScanDate == today -> {
                    Log.d(TAG, "Already scanned today, keeping streak: $currentStreak")
                    Pair(currentStreak, longestStreak)
                }
                lastScanDate != null && daysBetween(lastScanDate, today) == 1 -> {
                    val newS = currentStreak + 1
                    val newL = maxOf(longestStreak, newS)
                    Log.d(TAG, "Continuing streak: $currentStreak -> $newS")
                    Pair(newS, newL)
                }
                else -> {
                    Log.d(TAG, "Streak reset! Starting new streak at 1")
                    Pair(1, maxOf(longestStreak, 1))
                }
            }

            Log.d(TAG, "New values: streak=$newStreak, longest=$newLongest, date=$todayString")

            // Update ke database dengan data class yang @Serializable
            val streakUpdate = StreakUpdate(
                streakCount = newStreak,
                longestStreak = newLongest,
                lastScanDate = todayString,
                updatedAt = Clock.System.now().toString()
            )

            postgrest.from("profiles")
                .update(streakUpdate) {
                    filter {
                        eq("id", userId)
                    }
                }

            Log.d(TAG, "✅ Database updated successfully!")

            val streakInfo = StreakInfo(
                currentStreak = newStreak,
                longestStreak = newLongest,
                lastScanDate = todayString,
                isActiveToday = true,
                streakStatus = StreakStatus.ACTIVE
            )

            Log.d(TAG, "Returning StreakInfo: $streakInfo")
            Result.success(streakInfo)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating streak: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Hitung status streak berdasarkan tanggal(logika penentuan status streak)
     */
    private fun calculateStreakStatus(
        currentStreak: Int,
        longestStreak: Int,
        lastScanDate: LocalDate?,
        today: LocalDate
    ): StreakInfo {
        if (lastScanDate == null) {
            return StreakInfo(
                currentStreak = 0,
                longestStreak = longestStreak,
                isActiveToday = false,
                streakStatus = StreakStatus.INACTIVE
            )
        }

        val daysDiff = daysBetween(lastScanDate, today)

        return when {
            // Sudah scan hari ini
            daysDiff == 0 -> StreakInfo(
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                lastScanDate = lastScanDate.toString(),
                isActiveToday = true,
                streakStatus = StreakStatus.ACTIVE
            )
            // Scan kemarin, streak masih bisa dilanjutkan
            daysDiff == 1 -> StreakInfo(
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                lastScanDate = lastScanDate.toString(),
                isActiveToday = false,
                streakStatus = StreakStatus.AT_RISK
            )
            // Lebih dari 1 hari, streak reset
            else -> StreakInfo(
                currentStreak = 0,
                longestStreak = longestStreak,
                lastScanDate = lastScanDate.toString(),
                isActiveToday = false,
                streakStatus = StreakStatus.INACTIVE
            )
        }
    }

    /**
     * Hitung selisih hari antara dua tanggal
     */
    private fun daysBetween(from: LocalDate, to: LocalDate): Int {
        return (to.toEpochDays() - from.toEpochDays()).toInt()
    }
}