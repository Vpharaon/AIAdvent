package com.farionik.aiadvent.domain.repository

import com.farionik.aiadvent.domain.model.ChatMessage

interface ChatRepository {
    suspend fun sendMessage(userMessage: String, apiKey: String, temperature: Float): Result<ChatMessage>
}