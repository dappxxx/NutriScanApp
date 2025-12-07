//package com.nutriscan.app.data.remote.dto
//
//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//
//@Serializable
//data class GroqRequest(
//    val model: String = "llama-3.2-11b-vision-preview",
//    val messages: List<GroqMessage>,
//    @SerialName("max_tokens")
//    val maxTokens: Int = 2048,
//    val temperature: Double = 0.7
//)
//
//@Serializable
//data class GroqMessage(
//    val role: String,
//    val content: List<GroqContent>
//)
//
//@Serializable
//data class GroqContent(
//    val type: String,
//    val text: String? = null,
//    @SerialName("image_url")
//    val imageUrl: GroqImageUrl? = null
//)
//
//@Serializable
//data class GroqImageUrl(
//    val url: String
//)
//
//// Request untuk chat text saja (tanpa image)
//@Serializable
//data class GroqTextRequest(
//    val model: String = "llama-3.1-8b-instant",
//    val messages: List<GroqTextMessage>,
//    @SerialName("max_tokens")
//    val maxTokens: Int = 2048,
//    val temperature: Double = 0.7
//)
//
//@Serializable
//data class GroqTextMessage(
//    val role: String,
//    val content: String
//)