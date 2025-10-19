package com.farionik.aiadvent.data.repository

import android.util.Log
import com.farionik.aiadvent.data.dto.ConsultantResponse
import com.farionik.aiadvent.data.dto.ConsultantResponseRaw
import com.farionik.aiadvent.data.dto.toConsultantResponse
import com.farionik.aiadvent.data.network.ApiClient
import com.farionik.aiadvent.domain.model.ChatMessage
import com.farionik.aiadvent.domain.repository.ChatRepository
import kotlinx.serialization.json.Json

class ChatRepositoryImpl(
    private val apiClient: ApiClient,
) : ChatRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun sendMessage(messageHistory: List<ChatMessage>, apiKey: String): Result<ChatMessage> {
        return try {
            val response = apiClient.sendMessage(messageHistory, apiKey)
            val content = response.choices.firstOrNull()?.message?.content ?: "Нет ответа"

            // Пытаемся распарсить JSON ответ
            var isParsingError = false
            val consultantResponse = try {
                // Сначала пробуем обычный парсинг
                try {
                    json.decodeFromString<ConsultantResponse>(content)
                } catch (e: Exception) {
                    // Если не удалось, используем гибкий парсинг с JsonElement
                    Log.w("ChatRepository", "Обычный парсинг не сработал, использую гибкий парсинг...")
                    val rawResponse = json.decodeFromString<ConsultantResponseRaw>(content)
                    rawResponse.toConsultantResponse()
                }
            } catch (e: Exception) {
                // Если даже гибкий парсинг не помог, помечаем как ошибку и логируем
                isParsingError = true
                Log.e("ChatRepository", "========================================")
                Log.e("ChatRepository", "ОШИБКА ПАРСИНГА JSON")
                Log.e("ChatRepository", "========================================")
                Log.e("ChatRepository", "Причина: ${e.message}")
                Log.e("ChatRepository", "Полученный JSON:")
                Log.e("ChatRepository", content)
                Log.e("ChatRepository", "========================================")
                ConsultantResponse(message = content, options = null)
            }

            val chatMessage = ChatMessage(
                text = consultantResponse.message,
                isUser = false,
                rawJson = content,
                options = consultantResponse.options,
                isParsingError = isParsingError
            )

            Result.success(chatMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}