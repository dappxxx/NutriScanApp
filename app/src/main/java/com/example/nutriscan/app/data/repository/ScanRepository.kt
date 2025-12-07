package com.nutriscan.app.data.repository

import com.nutriscan.app.data.model.ScanSession
import com.nutriscan.app.data.model.ScanSessionInsert
import com.nutriscan.app.data.remote.SupabaseClient
import com.nutriscan.app.utils.Constants
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class ScanRepository {
    private val postgrest = SupabaseClient.client.postgrest
    private val storage = SupabaseClient.client.storage

    suspend fun uploadImage(
        userId: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<String> {
        return try {
            val path = "$userId/$fileName"
            storage.from(Constants.STORAGE_BUCKET).upload(path, imageBytes, upsert = true)
            val publicUrl = storage.from(Constants.STORAGE_BUCKET).publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createScanSession(
        userId: String,
        imageUrl: String,
        productName: String?,
        initialAnalysis: String?
    ): Result<ScanSession> {
        return try {
            val insert = ScanSessionInsert(
                userId = userId,
                imageUrl = imageUrl,
                productName = productName,
                initialAnalysis = initialAnalysis
            )

            val result = postgrest.from("scan_sessions")
                .insert(insert) {
                    select()
                }
                .decodeSingle<ScanSession>()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getScanHistory(userId: String): Result<List<ScanSession>> {
        return try {
            val sessions = postgrest.from("scan_sessions")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<ScanSession>()
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getScanSession(sessionId: String): Result<ScanSession?> {
        return try {
            val session = postgrest.from("scan_sessions")
                .select {
                    filter {
                        eq("id", sessionId)
                    }
                }
                .decodeSingleOrNull<ScanSession>()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteScanSession(sessionId: String): Result<Unit> {
        return try {
            postgrest.from("scan_sessions")
                .delete {
                    filter {
                        eq("id", sessionId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProductName(sessionId: String, productName: String): Result<Unit> {
        return try {
            postgrest.from("scan_sessions")
                .update(
                    mapOf("product_name" to productName)
                ) {
                    filter {
                        eq("id", sessionId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}