package com.farionik.aiadvent.domain.usecase

import com.farionik.aiadvent.domain.repository.ChatRepository

class SendMessageUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(message: String, apiKey: String): Result<String> {
        return repository.sendMessage(message, apiKey)
    }
}