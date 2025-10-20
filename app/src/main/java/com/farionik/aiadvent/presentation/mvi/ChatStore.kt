package com.farionik.aiadvent.presentation.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.farionik.aiadvent.domain.model.ChatMessage

interface ChatStore : Store<ChatStore.Intent, ChatStore.State, ChatStore.Label> {

    sealed interface Intent {
        data class SendMessage(val message: String) : Intent
        data class UpdateInputText(val text: String) : Intent
        data class UpdateTemperature(val temperature: Float) : Intent
    }

    data class State(
        val inputText: String = "",
        val messages: List<ChatMessage> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val temperature: Float = 0.0f
    )

    sealed interface Label {
        data class Error(val message: String) : Label
    }
}