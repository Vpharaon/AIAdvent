package com.farionik.aiadvent.domain.model

import com.farionik.aiadvent.data.dto.ConsultantOption

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val rawJson: String? = null,
    val options: List<ConsultantOption>? = null,  // Структурированные варианты ответа
    val isParsingError: Boolean = false  // Флаг ошибки парсинга JSON
)