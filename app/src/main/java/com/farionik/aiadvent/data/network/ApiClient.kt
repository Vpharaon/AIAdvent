package com.farionik.aiadvent.data.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.farionik.aiadvent.data.dto.ApiRequest
import com.farionik.aiadvent.data.dto.ApiResponse
import com.farionik.aiadvent.data.dto.Message
import com.farionik.aiadvent.data.dto.ResponseFormat
import com.farionik.aiadvent.domain.model.SystemPrompts
import com.farionik.aiadvent.domain.model.ChatMessage

class ApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorClient", message)
                }
            }
            level = LogLevel.ALL
        }
    }

    companion object {
        const val API_URL = "https://api.z.ai/api/paas/v4/chat/completions"
    }

    suspend fun sendMessage(messageHistory: List<ChatMessage>, apiKey: String): ApiResponse = withContext(Dispatchers.IO) {
        // Формируем список сообщений для API
        val messages = mutableListOf<Message>()

        // 1. Добавляем системный промпт
        messages.add(
            Message(
                role = "system",
                content = SystemPrompts.SALES_CONSULTANT
            )
        )

        // 2. Добавляем всю историю сообщений (исключая начальное приветствие)
        messageHistory.forEachIndexed { index, chatMessage ->
            // Пропускаем первое сообщение (начальное приветствие)
            if (index == 0) return@forEachIndexed

            messages.add(
                Message(
                    role = if (chatMessage.isUser) "user" else "assistant",
                    content = if (chatMessage.isUser) {
                        chatMessage.text
                    } else {
                        // Для ассистента используем rawJson если есть, иначе text
                        chatMessage.rawJson ?: chatMessage.text
                    }
                )
            )
        }

        val request = ApiRequest(
            messages = messages,
            responseFormat = ResponseFormat(type = "json_object")
        )

        try {
            val response = client.post(API_URL) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }

            // Логируем статус ответа
            Log.d("KtorClient", "Response status: ${response.status}")

            // Получаем сырой текст ответа для отладки
            val rawResponse = response.body<String>()
            Log.d("KtorClient", "Raw JSON response: $rawResponse")

            // Пытаемся распарсить в ApiResponse
            val apiResponse = json.decodeFromString<ApiResponse>(rawResponse)
            Log.d("KtorClient", "Parsed successfully: choices size = ${apiResponse.choices.size}")

            // Проверяем на ошибки
            if (apiResponse.error != null) {
                throw Exception("API Error: ${apiResponse.error.message}")
            }

            apiResponse
        } catch (e: Exception) {
            Log.e("KtorClient", "Error in sendMessage: ${e.message}", e)
            throw e
        }
    }

    fun close() {
        client.close()
    }
}