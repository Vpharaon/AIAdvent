package com.farionik.aiadvent.data.repository

import com.farionik.aiadvent.data.network.ApiClient
import com.farionik.aiadvent.domain.model.ChatMessage
import com.farionik.aiadvent.domain.repository.ChatRepository

class ChatRepositoryImpl(
    private val apiClient: ApiClient,
) : ChatRepository {

    override suspend fun sendMessage(userMessage: String, apiKey: String, temperature: Float): Result<ChatMessage> {
        return try {
            val response = apiClient.sendMessage(userMessage, apiKey, temperature)
            val content = response.choices.firstOrNull()?.message?.content ?: "Нет ответа"

            val chatMessage = ChatMessage(
                text = content,
                isUser = false
            )

            Result.success(chatMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}