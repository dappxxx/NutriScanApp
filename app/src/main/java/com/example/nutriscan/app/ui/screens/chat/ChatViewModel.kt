package com.nutriscan.app.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.model.ChatMessage
import com.nutriscan.app.data.model.Profile
import com.nutriscan.app.data.model.ScanSession
import com.nutriscan.app.data.model.UiState
import com.nutriscan.app.data.remote.GeminiApiService
import com.nutriscan.app.data.repository.AuthRepository
import com.nutriscan.app.data.repository.ChatRepository
import com.nutriscan.app.data.repository.ProfileRepository
import com.nutriscan.app.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val TAG = "ChatViewModel"

    private val authRepository = AuthRepository()
    private val scanRepository = ScanRepository()
    private val chatRepository = ChatRepository()
    private val profileRepository = ProfileRepository()
    private val geminiService = GeminiApiService()

    private val _sessionState = MutableStateFlow<UiState<ScanSession>>(UiState.Idle)
    val sessionState: StateFlow<UiState<ScanSession>> = _sessionState.asStateFlow()

    private val _messagesState = MutableStateFlow<UiState<List<ChatMessage>>>(UiState.Idle)
    val messagesState: StateFlow<UiState<List<ChatMessage>>> = _messagesState.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private var currentSession: ScanSession? = null
    private var chatHistory: MutableList<Pair<String, String>> = mutableListOf()
    private var healthProfileSummary: String = ""

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _sessionState.value = UiState.Loading
            _messagesState.value = UiState.Loading

            // Load user profile
            loadUserProfile()

            // Load session
            val sessionResult = scanRepository.getScanSession(sessionId)
            sessionResult.fold(
                onSuccess = { session ->
                    if (session != null) {
                        currentSession = session
                        _sessionState.value = UiState.Success(session)
                    } else {
                        _sessionState.value = UiState.Error("Sesi tidak ditemukan")
                    }
                },
                onFailure = { error ->
                    _sessionState.value = UiState.Error(error.message ?: "Gagal memuat sesi")
                }
            )

            // Load messages
            val messagesResult = chatRepository.getChatMessages(sessionId)
            messagesResult.fold(
                onSuccess = { messages ->
                    _messagesState.value = UiState.Success(messages)
                    chatHistory.clear()
                    messages.forEach { msg ->
                        chatHistory.add(msg.sender to msg.message)
                    }
                },
                onFailure = { error ->
                    _messagesState.value = UiState.Error(error.message ?: "Gagal memuat pesan")
                }
            )
        }
    }

    private suspend fun loadUserProfile() {
        try {
            val userId = authRepository.getCurrentUserId() ?: return
            val profileResult = profileRepository.getProfile(userId)
            profileResult.fold(
                onSuccess = { profile ->
                    healthProfileSummary = profile?.getHealthProfileSummary() ?: ""
                    Log.d(TAG, "Health Profile loaded: $healthProfileSummary")
                },
                onFailure = {
                    healthProfileSummary = ""
                }
            )
        } catch (e: Exception) {
            healthProfileSummary = ""
        }
    }

    fun sendMessage(message: String) {
        val session = currentSession ?: return

        viewModelScope.launch {
            val currentMessages = (_messagesState.value as? UiState.Success)?.data?.toMutableList() ?: mutableListOf()

            // Save user message
            val userMsgResult = chatRepository.insertMessage(
                sessionId = session.id,
                sender = "user",
                message = message
            )

            userMsgResult.fold(
                onSuccess = { userMsg ->
                    currentMessages.add(userMsg)
                    _messagesState.value = UiState.Success(currentMessages.toList())
                    chatHistory.add("user" to message)
                },
                onFailure = { }
            )

            // Get AI response
            _isTyping.value = true

            val productAnalysis = session.initialAnalysis ?: ""

            // Panggil Gemini Chat
            val aiResult = geminiService.chat(
                userMessage = message,
                productAnalysis = productAnalysis,
                chatHistory = chatHistory,
                healthProfile = healthProfileSummary
            )

            _isTyping.value = false

            aiResult.fold(
                onSuccess = { aiResponse ->
                    val aiMsgResult = chatRepository.insertMessage(
                        sessionId = session.id,
                        sender = "ai",
                        message = aiResponse
                    )

                    aiMsgResult.fold(
                        onSuccess = { aiMsg ->
                            currentMessages.add(aiMsg)
                            _messagesState.value = UiState.Success(currentMessages.toList())
                            chatHistory.add("ai" to aiResponse)
                        },
                        onFailure = { }
                    )
                },
                onFailure = { error ->
                    val errorMsg = ChatMessage(
                        id = "",
                        sessionId = session.id,
                        sender = "ai",
                        message = "Maaf, terjadi kesalahan: ${error.message}"
                    )
                    currentMessages.add(errorMsg)
                    _messagesState.value = UiState.Success(currentMessages.toList())
                }
            )
        }
    }

    fun updateProductName(name: String) {
        val session = currentSession ?: return
        viewModelScope.launch {
            scanRepository.updateProductName(session.id, name)
            currentSession = session.copy(productName = name)
            _sessionState.value = UiState.Success(currentSession!!)
        }
    }

    fun hasHealthProfile(): Boolean = healthProfileSummary.isNotBlank()
}