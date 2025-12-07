package com.nutriscan.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.model.Profile
import com.nutriscan.app.data.model.UiState
import com.nutriscan.app.data.model.User
import com.nutriscan.app.data.repository.AuthRepository
import com.nutriscan.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val profileRepository = ProfileRepository()

    private val _userState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val userState: StateFlow<UiState<User>> = _userState.asStateFlow()

    private val _profileState = MutableStateFlow<UiState<Profile?>>(UiState.Idle)
    val profileState: StateFlow<UiState<Profile?>> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateState: StateFlow<UiState<Unit>> = _updateState.asStateFlow()

    private val _passwordState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val passwordState: StateFlow<UiState<Unit>> = _passwordState.asStateFlow()

    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoutState: StateFlow<UiState<Unit>> = _logoutState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _userState.value = UiState.Loading
            _profileState.value = UiState.Loading

            val user = authRepository.getCurrentUser()
            if (user != null) {
                _userState.value = UiState.Success(user)

                // Load profile
                val profileResult = profileRepository.getProfile(user.id)
                profileResult.fold(
                    onSuccess = { profile ->
                        _profileState.value = UiState.Success(profile)
                    },
                    onFailure = { error ->
                        _profileState.value = UiState.Error(error.message ?: "Gagal memuat profil")
                    }
                )
            } else {
                _userState.value = UiState.Error("User tidak ditemukan")
            }
        }
    }

    fun updateProfile(fullName: String, healthCondition: String) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading

            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val result = profileRepository.updateProfile(
                    userId = userId,
                    fullName = fullName,
                    healthCondition = healthCondition
                )

                result.fold(
                    onSuccess = {
                        _updateState.value = UiState.Success(Unit)
                        loadUserData() // Refresh data
                    },
                    onFailure = { error ->
                        _updateState.value = UiState.Error(error.message ?: "Gagal mengupdate profil")
                    }
                )
            } else {
                _updateState.value = UiState.Error("User tidak ditemukan")
            }
        }
    }

    fun changePassword(newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _passwordState.value = UiState.Error("Password minimal 6 karakter")
            return
        }

        if (newPassword != confirmPassword) {
            _passwordState.value = UiState.Error("Password tidak cocok")
            return
        }

        viewModelScope.launch {
            _passwordState.value = UiState.Loading

            val result = authRepository.updatePassword(newPassword)
            result.fold(
                onSuccess = {
                    _passwordState.value = UiState.Success(Unit)
                },
                onFailure = { error ->
                    _passwordState.value = UiState.Error(error.message ?: "Gagal mengubah password")
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = UiState.Loading

            val result = authRepository.signOut()
            result.fold(
                onSuccess = {
                    _logoutState.value = UiState.Success(Unit)
                },
                onFailure = { error ->
                    _logoutState.value = UiState.Error(error.message ?: "Gagal logout")
                }
            )
        }
    }

    fun resetUpdateState() {
        _updateState.value = UiState.Idle
    }

    fun resetPasswordState() {
        _passwordState.value = UiState.Idle
    }
}