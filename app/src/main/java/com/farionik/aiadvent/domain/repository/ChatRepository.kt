package com.farionik.aiadvent.domain.repository

interface ChatRepository {
    suspend fun sendMessage(message: String, apiKey: String): Result<String>
}