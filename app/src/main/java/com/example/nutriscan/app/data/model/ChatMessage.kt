package com.nutriscan.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = "",
    @SerialName("session_id")
    val sessionId: String = "",
    val sender: String = "",
    val message: String = "",
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class ChatMessageInsert(
    @SerialName("session_id")
    val sessionId: String,
    val sender: String,
    val message: String
)