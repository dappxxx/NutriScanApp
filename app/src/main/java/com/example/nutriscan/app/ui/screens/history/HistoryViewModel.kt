package com.nutriscan.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.model.ScanSession
import com.nutriscan.app.data.model.UiState
import com.nutriscan.app.data.repository.AuthRepository
import com.nutriscan.app.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val scanRepository = ScanRepository()

    private val _historyState = MutableStateFlow<UiState<List<ScanSession>>>(UiState.Idle)
    val historyState: StateFlow<UiState<List<ScanSession>>> = _historyState.asStateFlow()

    private val _deleteState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteState: StateFlow<UiState<Unit>> = _deleteState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = UiState.Loading

            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val result = scanRepository.getScanHistory(userId)
                result.fold(
                    onSuccess = { sessions ->
                        _historyState.value = UiState.Success(sessions)
                    },
                    onFailure = { error ->
                        _historyState.value = UiState.Error(error.message ?: "Gagal memuat riwayat")
                    }
                )
            } else {
                _historyState.value = UiState.Error("User tidak ditemukan")
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading

            val result = scanRepository.deleteScanSession(sessionId)
            result.fold(
                onSuccess = {
                    _deleteState.value = UiState.Success(Unit)
                    // Refresh history
                    loadHistory()
                },
                onFailure = { error ->
                    _deleteState.value = UiState.Error(error.message ?: "Gagal menghapus")
                }
            )
        }
    }

    fun resetDeleteState() {
        _deleteState.value = UiState.Idle
    }
}