package com.farionik.aiadvent.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class Thinking(
    val type: String = "enabled"
)

@Serializable
data class ApiRequest(
    val model: String = "glm-4.6",
    val messages: List<Message>,
    val thinking: Thinking = Thinking(),
    @SerialName("max_tokens")
    val maxTokens: Int = 4096,
    val temperature: Double = 1.0
)