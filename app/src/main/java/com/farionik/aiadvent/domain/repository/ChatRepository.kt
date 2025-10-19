package com.farionik.aiadvent.domain.repository

import com.farionik.aiadvent.domain.model.ChatMessage

interface ChatRepository {
    suspend fun sendMessage(messageHistory: List<ChatMessage>, apiKey: String): Result<ChatMessage>
}