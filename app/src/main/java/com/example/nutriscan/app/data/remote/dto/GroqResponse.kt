//package com.nutriscan.app.data.remote.dto
//
//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//
//@Serializable
//data class GroqResponse(
//    val id: String? = null,
//    val choices: List<GroqChoice>? = null,
//    val error: GroqError? = null
//)
//
//@Serializable
//data class GroqChoice(
//    val index: Int,
//    val message: GroqResponseMessage,
//    @SerialName("finish_reason")
//    val finishReason: String? = null
//)
//
//@Serializable
//data class GroqResponseMessage(
//    val role: String,
//    val content: String
//)
//
//@Serializable
//data class GroqError(
//    val message: String,
//    val type: String? = null
//)