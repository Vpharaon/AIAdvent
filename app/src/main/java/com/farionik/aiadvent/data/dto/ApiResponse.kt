package com.farionik.aiadvent.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ResponseMessage(
    val content: String = "",
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
    val role: String = ""
)

@Serializable
data class Choice(
    @SerialName("finish_reason")
    val finishReason: String = "",
    val index: Int = 0,
    val message: ResponseMessage = ResponseMessage()
)

@Serializable
data class PromptTokensDetails(
    @SerialName("cached_tokens")
    val cachedTokens: Int = 0
)

@Serializable
data class Usage(
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("prompt_tokens_details")
    val promptTokensDetails: PromptTokensDetails? = null,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

@Serializable
data class ApiResponse(
    val choices: List<Choice> = emptyList(),
    val created: Long = 0,
    val id: String = "",
    val model: String = "",
    @SerialName("request_id")
    val requestId: String = "",
    val usage: Usage = Usage(),
    val error: ErrorResponse? = null
)

@Serializable
data class ErrorResponse(
    val message: String = "",
    val type: String = "",
    val code: String? = null
)

/**
 * Extension функция для парсинга CountryInfo из JSON ответа
 */
fun ApiResponse.parseCountryInfo(): CountryInfo? {
    return try {
        val content = choices.firstOrNull()?.message?.content ?: return null
        val countryInfo = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }.decodeFromString<CountryInfo>(content)
        // Сохраняем raw JSON для отображения
        countryInfo.copy(rawJson = content)
    } catch (e: Exception) {
        null
    }
}