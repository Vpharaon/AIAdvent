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
    val type: String = "disabled"
)

@Serializable
data class ResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class ApiRequest(
    val model: String = "glm-4.5-flash",
    val messages: List<Message>,
    val thinking: Thinking = Thinking(),
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null,
    @SerialName("max_tokens")
    val maxTokens: Int = 4096,
    val temperature: Double = 0.0
)