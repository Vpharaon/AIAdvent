package com.farionik.aiadvent.data.repository

import com.farionik.aiadvent.data.network.ApiClient
import com.farionik.aiadvent.domain.repository.ChatRepository

class ChatRepositoryImpl(
    private val apiClient: ApiClient,
) : ChatRepository {
    override suspend fun sendMessage(message: String, apiKey: String): Result<String> {
        return try {
            val response = apiClient.sendMessage(message, apiKey)
            val content = response.choices.firstOrNull()?.message?.content ?: "Нет ответа"
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}