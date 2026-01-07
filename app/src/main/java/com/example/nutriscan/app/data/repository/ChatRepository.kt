package com.nutriscan.app.data.repository

import com.nutriscan.app.data.model.ChatMessage
import com.nutriscan.app.data.model.ChatMessageInsert
import com.nutriscan.app.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class ChatRepository {
    private val postgrest = SupabaseClient.client.postgrest

    suspend fun getChatMessages(sessionId: String): Result<List<ChatMessage>> {
        return try {
            val messages = postgrest.from("chat_messages")
                .select {
                    filter {
                        eq("session_id", sessionId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<ChatMessage>()
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //menyimpan chat sebagai analisi pertama
    suspend fun insertMessage(
        sessionId: String,
        sender: String,
        message: String
    ): Result<ChatMessage> {
        return try {
            val insert = ChatMessageInsert(
                sessionId = sessionId,
                sender = sender,
                message = message
            )

            val result = postgrest.from("chat_messages")
                .insert(insert) {
                    select()
                }
                .decodeSingle<ChatMessage>()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}