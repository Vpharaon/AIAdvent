package com.farionik.aiadvent.domain.usecase

import com.farionik.aiadvent.domain.model.ChatMessage
import com.farionik.aiadvent.domain.repository.ChatRepository

class SendMessageUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(messageHistory: List<ChatMessage>, apiKey: String): Result<ChatMessage> {
        return repository.sendMessage(messageHistory, apiKey)
    }
}