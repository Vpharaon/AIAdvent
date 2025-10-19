package com.farionik.aiadvent.presentation.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.farionik.aiadvent.data.dto.ConsultantResponse
import com.farionik.aiadvent.domain.model.ChatMessage
import com.farionik.aiadvent.domain.model.SystemPrompts
import kotlinx.serialization.json.Json

interface ChatStore : Store<ChatStore.Intent, ChatStore.State, ChatStore.Label> {

    sealed interface Intent {
        data class SendMessage(val message: String) : Intent
        data class UpdateInputText(val text: String) : Intent
        data object RetryLastMessage : Intent
        data object RestartConversation : Intent
    }

    data class State(
        val inputText: String = "",
        val messages: List<ChatMessage> = createInitialMessages(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    sealed interface Label {
        data class Error(val message: String) : Label
    }

    companion object {
        private fun createInitialMessages(): List<ChatMessage> {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            return try {
                val consultantResponse = json.decodeFromString<ConsultantResponse>(SystemPrompts.INITIAL_GREETING)
                listOf(
                    ChatMessage(
                        text = consultantResponse.message,
                        isUser = false,
                        rawJson = SystemPrompts.INITIAL_GREETING,
                        options = consultantResponse.options
                    )
                )
            } catch (e: Exception) {
                listOf(
                    ChatMessage(
                        text = "Здравствуйте! Помогу вам найти идеальный смартфон.",
                        isUser = false
                    )
                )
            }
        }
    }
}