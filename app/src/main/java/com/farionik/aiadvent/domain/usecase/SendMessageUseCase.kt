package com.farionik.aiadvent.domain.usecase

import com.farionik.aiadvent.domain.model.ChatMessage
import com.farionik.aiadvent.domain.repository.ChatRepository

class SendMessageUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(userMessage: String, apiKey: String, temperature: Float): Result<ChatMessage> {
        return repository.sendMessage(userMessage, apiKey, temperature)
    }
}