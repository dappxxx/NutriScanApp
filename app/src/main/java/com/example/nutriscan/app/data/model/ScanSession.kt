package com.nutriscan.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanSession(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("image_url")
    val imageUrl: String = "",
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("initial_analysis")
    val initialAnalysis: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class ScanSessionInsert(
    @SerialName("user_id")
    val userId: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("initial_analysis")
    val initialAnalysis: String? = null
)