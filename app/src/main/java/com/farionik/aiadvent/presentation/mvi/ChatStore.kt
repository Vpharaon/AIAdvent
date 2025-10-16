package com.farionik.aiadvent.presentation.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.farionik.aiadvent.domain.model.ChatMessage

interface ChatStore : Store<ChatStore.Intent, ChatStore.State, ChatStore.Label> {

    sealed interface Intent {
        data class SendMessage(val message: String) : Intent
        data class UpdateInputText(val text: String) : Intent
        data class GetCountryInfo(val countryName: String) : Intent
    }

    data class State(
        val inputText: String = "",
        val messages: List<ChatMessage> = listOf(
            ChatMessage(
                text = "👋 Привет! Я помогу тебе узнать информацию о любой стране.\n\n" +
                        "Просто введи название страны (например: Россия, Франция, Казахстан) и получи:\n" +
                        "• Столицу\n" +
                        "• Население\n" +
                        "• Площадь\n" +
                        "• Язык и валюту\n" +
                        "• Интересные факты\n\n" +
                        "Также ты сможешь просмотреть сырой JSON ответ!",
                isUser = false
            )
        ),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    sealed interface Label {
        data class Error(val message: String) : Label
    }
}