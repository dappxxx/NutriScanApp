package com.nutriscan.app.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.model.AuthState
import com.nutriscan.app.data.repository.AuthRepository
import com.nutriscan.app.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val TAG = "AuthViewModel"
    private val authRepository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _confirmPasswordError = MutableStateFlow<String?>(null)
    val confirmPasswordError: StateFlow<String?> = _confirmPasswordError.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    // Flag untuk menandai registrasi berhasil tapi perlu verifikasi email
    private val _needsEmailVerification = MutableStateFlow(false)
    val needsEmailVerification: StateFlow<Boolean> = _needsEmailVerification.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                // Gunakan validasi yang proper
                val isLoggedIn = authRepository.isLoggedIn()
                _authState.value = if (isLoggedIn) {
                    AuthState.Authenticated
                } else {
                    AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                Log.e(TAG, "Check auth status error: ${e.message}")
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signIn(email: String, password: String) {
        // Reset errors
        clearErrors()

        // Validate input
        var hasError = false

        if (email.isBlank()) {
            _emailError.value = "Email tidak boleh kosong"
            hasError = true
        } else if (!ValidationUtils.isValidEmail(email)) {
            _emailError.value = "Format email tidak valid"
            hasError = true
        }

        if (password.isBlank()) {
            _passwordError.value = "Password tidak boleh kosong"
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "Attempting sign in for: $email")

            val result = authRepository.signIn(email.trim(), password)

            result.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Sign in successful: ${user.id}")
                    _authState.value = AuthState.Authenticated
                },
                onFailure = { error ->
                    Log.e(TAG, "Sign in failed: ${error.message}")
                    _authState.value = AuthState.Error(error.message ?: "Login gagal")
                }
            )
        }
    }

    fun signUp(fullName: String, email: String, password: String, confirmPassword: String) {
        // Reset errors
        clearErrors()

        // Validate input
        var hasError = false

        if (fullName.isBlank()) {
            _nameError.value = "Nama tidak boleh kosong"
            hasError = true
        } else if (fullName.length < 2) {
            _nameError.value = "Nama terlalu pendek"
            hasError = true
        }

        if (email.isBlank()) {
            _emailError.value = "Email tidak boleh kosong"
            hasError = true
        } else if (!ValidationUtils.isValidEmail(email)) {
            _emailError.value = "Format email tidak valid"
            hasError = true
        }

        if (password.isBlank()) {
            _passwordError.value = "Password tidak boleh kosong"
            hasError = true
        } else if (!ValidationUtils.isValidPassword(password)) {
            _passwordError.value = "Password minimal 6 karakter"
            hasError = true
        }

        if (confirmPassword.isBlank()) {
            _confirmPasswordError.value = "Konfirmasi password tidak boleh kosong"
            hasError = true
        } else if (!ValidationUtils.isPasswordMatch(password, confirmPassword)) {
            _confirmPasswordError.value = "Password tidak cocok"
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "Attempting sign up for: $email")

            val result = authRepository.signUp(email.trim(), password, fullName.trim())

            result.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Sign up successful: ${user.id}")

                    if (user.id == "pending_verification") {
                        // Perlu verifikasi email - JANGAN langsung ke home
                        _needsEmailVerification.value = true
                        _authState.value = AuthState.Error(
                            "Registrasi berhasil! Silakan cek email Anda untuk verifikasi, lalu login."
                        )
                    } else {
                        // Langsung authenticated (jika email verification disabled di Supabase)
                        _authState.value = AuthState.Authenticated
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Sign up failed: ${error.message}")
                    _authState.value = AuthState.Error(error.message ?: "Registrasi gagal")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signOut()

            result.fold(
                onSuccess = {
                    _authState.value = AuthState.Unauthenticated
                    _needsEmailVerification.value = false
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Logout gagal")
                }
            )
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
        clearErrors()
    }

    private fun clearErrors() {
        _emailError.value = null
        _passwordError.value = null
        _confirmPasswordError.value = null
        _nameError.value = null
    }

    suspend fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    fun clearEmailVerificationFlag() {
        _needsEmailVerification.value = false
    }
}