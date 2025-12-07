package com.nutriscan.app.data.repository

import com.nutriscan.app.data.model.Profile
import com.nutriscan.app.data.model.ProfileUpdate
import com.nutriscan.app.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.Clock

class ProfileRepository {
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
            // Simpan sebagai comma-separated string di kolom health_condition
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
}