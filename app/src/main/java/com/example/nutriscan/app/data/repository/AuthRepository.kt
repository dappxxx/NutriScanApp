package com.nutriscan.app.data.repository

import android.util.Log
import com.nutriscan.app.data.model.User
import com.nutriscan.app.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthRepository {

    private val TAG = "AuthRepository"
    private val auth = SupabaseClient.client.auth

    suspend fun signUp(email: String, password: String, fullName: String): Result<User> {
        return try {
            Log.d(TAG, "Starting signup for: $email")

            // Sign up with email
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("full_name", JsonPrimitive(fullName))
                }
            }

            Log.d(TAG, "Signup result received")

            // Cek apakah perlu verifikasi email atau langsung login
            val session = auth.currentSessionOrNull()

            if (session != null && session.user != null) {
                // User langsung login (email verification disabled di Supabase)
                Log.d(TAG, "User logged in directly: ${session.user!!.id}")
                Result.success(User(
                    id = session.user!!.id,
                    email = email,
                    fullName = fullName
                ))
            } else {
                // Registrasi berhasil tapi perlu verifikasi email
                // JANGAN auto-login, biarkan user verifikasi dulu
                Log.d(TAG, "Registration successful, email verification required")
                Result.success(User(
                    id = "pending_verification",
                    email = email,
                    fullName = fullName
                ))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Signup error: ${e.message}", e)
            val errorMessage = parseAuthError(e.message ?: "Unknown error")
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "Starting signin for: $email")

            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val session = auth.currentSessionOrNull()
            val user = session?.user

            if (user != null) {
                Log.d(TAG, "Signin successful: ${user.id}")
                Result.success(User(
                    id = user.id,
                    email = user.email ?: email,
                    fullName = user.userMetadata?.get("full_name")?.toString()?.removeSurrounding("\"")
                ))
            } else {
                Log.e(TAG, "Signin failed: No user in session")
                Result.failure(Exception("Login gagal. Silakan coba lagi."))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Signin error: ${e.message}", e)
            val errorMessage = parseAuthError(e.message ?: "Unknown error")
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Log.d(TAG, "Signout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Signout error: ${e.message}", e)
            Result.failure(Exception("Gagal logout. Silakan coba lagi."))
        }
    }

    /**
     * Get current user - dengan validasi session
     */
    suspend fun getCurrentUser(): User? {
        return try {
            // Coba refresh session dulu untuk validasi
            val session = auth.currentSessionOrNull()

            if (session == null) {
                Log.d(TAG, "No session found")
                return null
            }

            // Cek apakah session expired
            val user = session.user
            if (user != null) {
                Log.d(TAG, "Valid session for user: ${user.id}")
                User(
                    id = user.id,
                    email = user.email ?: "",
                    fullName = user.userMetadata?.get("full_name")?.toString()?.removeSurrounding("\"")
                )
            } else {
                Log.d(TAG, "Session exists but no user")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get current user error: ${e.message}", e)
            null
        }
    }

    /**
     * Validasi session - cek apakah session masih valid dengan server
     */
    suspend fun validateSession(): Boolean {
        return try {
            val session = auth.currentSessionOrNull()
            if (session == null) {
                Log.d(TAG, "No session to validate")
                return false
            }

            // Coba refresh untuk validasi dengan server
            try {
                auth.refreshCurrentSession()
                val newSession = auth.currentSessionOrNull()
                val isValid = newSession?.user != null
                Log.d(TAG, "Session validation result: $isValid")
                isValid
            } catch (refreshError: Exception) {
                // Jika refresh gagal, session tidak valid
                Log.e(TAG, "Session refresh failed: ${refreshError.message}")
                // Clear invalid session
                try {
                    auth.signOut()
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing invalid session: ${e.message}")
                }
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session validation error: ${e.message}", e)
            false
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            auth.modifyUser {
                this.password = newPassword
            }
            Log.d(TAG, "Password updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update password error: ${e.message}", e)
            val errorMessage = parseAuthError(e.message ?: "Unknown error")
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Cek apakah ada session (quick check, tanpa validasi server)
     */
    fun hasSession(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    /**
     * Cek login status dengan validasi - GUNAKAN INI untuk navigasi
     */
    suspend fun isLoggedIn(): Boolean {
        return validateSession()
    }

    fun getCurrentUserId(): String? {
        return try {
            auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUserId error: ${e.message}")
            null
        }
    }

    /**
     * Parse error message dari Supabase ke pesan yang user-friendly
     */
    private fun parseAuthError(error: String): String {
        val lowerError = error.lowercase()

        return when {
            // Login errors
            lowerError.contains("invalid login credentials") ||
                    lowerError.contains("invalid_credentials") ||
                    lowerError.contains("invalid email or password") -> {
                "Email atau password salah. Silakan periksa kembali."
            }

            // Email not confirmed
            lowerError.contains("email not confirmed") ||
                    lowerError.contains("email_not_confirmed") -> {
                "Email belum diverifikasi. Silakan cek inbox email Anda."
            }

            // User already exists
            lowerError.contains("user already registered") ||
                    lowerError.contains("already registered") ||
                    lowerError.contains("duplicate") ||
                    lowerError.contains("already exists") -> {
                "Email sudah terdaftar. Silakan gunakan email lain atau login."
            }

            // Invalid email format
            lowerError.contains("invalid email") ||
                    lowerError.contains("valid email") -> {
                "Format email tidak valid. Silakan periksa kembali."
            }

            // Password too weak
            lowerError.contains("password") && lowerError.contains("weak") ||
                    lowerError.contains("password should be") ||
                    lowerError.contains("at least") -> {
                "Password terlalu lemah. Gunakan minimal 6 karakter."
            }

            // Rate limit
            lowerError.contains("rate limit") ||
                    lowerError.contains("too many requests") ||
                    lowerError.contains("exceeded") -> {
                "Terlalu banyak percobaan. Silakan tunggu beberapa menit."
            }

            // Network error
            lowerError.contains("network") ||
                    lowerError.contains("connection") ||
                    lowerError.contains("timeout") ||
                    lowerError.contains("unable to resolve host") -> {
                "Koneksi internet bermasalah. Silakan periksa koneksi Anda."
            }

            // User not found
            lowerError.contains("user not found") -> {
                "Akun tidak ditemukan. Silakan daftar terlebih dahulu."
            }

            // Session expired
            lowerError.contains("session") && lowerError.contains("expired") ||
                    lowerError.contains("refresh_token") -> {
                "Sesi telah berakhir. Silakan login kembali."
            }

            // Default error
            else -> {
                Log.w(TAG, "Unhandled auth error: $error")
                "Terjadi kesalahan. Silakan coba lagi."
            }
        }
    }
}