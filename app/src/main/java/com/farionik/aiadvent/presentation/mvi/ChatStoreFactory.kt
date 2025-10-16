package com.farionik.aiadvent.presentation.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.farionik.aiadvent.domain.model.ChatMessage
import com.farionik.aiadvent.domain.usecase.SendMessageUseCase
import com.farionik.aiadvent.domain.usecase.GetCountryInfoUseCase
import com.farionik.aiadvent.domain.usecase.ValidateCountryNameUseCase
import kotlinx.coroutines.launch

class ChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getCountryInfoUseCase: GetCountryInfoUseCase,
    private val validateCountryNameUseCase: ValidateCountryNameUseCase,
    private val apiKey: String,
) {

    fun create(): ChatStore =
        object : ChatStore, Store<ChatStore.Intent, ChatStore.State, ChatStore.Label> by storeFactory.create(
            name = "ChatStore",
            initialState = ChatStore.State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed interface Message {
        data class InputTextChanged(val text: String) : Message
        data class UserMessageAdded(val message: String) : Message
        data object LoadingStarted : Message
        data class AiMessageAdded(val response: String, val rawJson: String? = null) : Message
        data class LoadingFailed(val error: String) : Message
    }

    private inner class ExecutorImpl : CoroutineExecutor<ChatStore.Intent, Nothing, ChatStore.State, Message, ChatStore.Label>() {
        override fun executeIntent(intent: ChatStore.Intent, getState: () -> ChatStore.State) {
            when (intent) {
                is ChatStore.Intent.UpdateInputText -> {
                    dispatch(Message.InputTextChanged(intent.text))
                }
                is ChatStore.Intent.SendMessage -> {
                    sendMessage(intent.message)
                }
                is ChatStore.Intent.GetCountryInfo -> {
                    getCountryInfo(intent.countryName)
                }
            }
        }

        private fun sendMessage(message: String) {
            dispatch(Message.UserMessageAdded(message))
            scope.launch {
                dispatch(Message.LoadingStarted)
                sendMessageUseCase(message, apiKey)
                    .onSuccess { content ->
                        dispatch(Message.AiMessageAdded(content))
                    }
                    .onFailure { error ->
                        val errorMessage = "Ошибка: ${error.message}"
                        dispatch(Message.LoadingFailed(errorMessage))
                        publish(ChatStore.Label.Error(errorMessage))
                    }
            }
        }

        private fun getCountryInfo(countryName: String) {
            dispatch(Message.UserMessageAdded("Информация о стране: $countryName"))
            scope.launch {
                dispatch(Message.LoadingStarted)

                // Сначала проверяем, является ли введенный текст страной
                validateCountryNameUseCase(countryName, apiKey)
                    .onSuccess { isCountry ->
                        if (isCountry) {
                            // Если это страна, получаем информацию
                            getCountryInfoUseCase(countryName, apiKey)
                                .onSuccess { countryInfo ->
                                    val response = buildString {
                                        appendLine("🌍 ${countryInfo.countryName}")
                                        appendLine("Столица: ${countryInfo.capital}")
                                        appendLine("Население: ${countryInfo.population}")
                                        appendLine("Площадь: ${countryInfo.area} км²")
                                        appendLine("Регион: ${countryInfo.region}")
                                        appendLine("Язык: ${countryInfo.officialLanguage}")
                                        appendLine("Валюта: ${countryInfo.currency}")
                                        appendLine("Код страны: ${countryInfo.callingCode}")
                                        appendLine("Часовой пояс: ${countryInfo.timeZone}")
                                        if (countryInfo.interestingFacts.isNotEmpty()) {
                                            appendLine("\nИнтересные факты:")
                                            countryInfo.interestingFacts.forEach { fact ->
                                                appendLine("• $fact")
                                            }
                                        }
                                    }
                                    dispatch(Message.AiMessageAdded(response, countryInfo.rawJson))
                                }
                                .onFailure { error ->
                                    val errorMessage = "Ошибка получения информации: ${error.message}"
                                    dispatch(Message.LoadingFailed(errorMessage))
                                    publish(ChatStore.Label.Error(errorMessage))
                                }
                        } else {
                            // Если это не страна, сообщаем пользователю
                            val errorMessage = "❌ \"$countryName\" не является названием страны.\n\nПожалуйста, введите правильное название страны (например: Россия, Беларусь, США и т.д.)"
                            dispatch(Message.LoadingFailed(errorMessage))
                        }
                    }
                    .onFailure { error ->
                        val errorMessage = "Ошибка проверки: ${error.message}"
                        dispatch(Message.LoadingFailed(errorMessage))
                        publish(ChatStore.Label.Error(errorMessage))
                    }
            }
        }
    }

    private object ReducerImpl : Reducer<ChatStore.State, Message> {
        override fun ChatStore.State.reduce(msg: Message): ChatStore.State =
            when (msg) {
                is Message.InputTextChanged -> copy(inputText = msg.text)
                is Message.UserMessageAdded -> copy(
                    messages = messages + ChatMessage(text = msg.message, isUser = true),
                    inputText = "" // Очищаем поле ввода
                )
                is Message.LoadingStarted -> copy(isLoading = true, error = null)
                is Message.AiMessageAdded -> copy(
                    isLoading = false,
                    messages = messages + ChatMessage(text = msg.response, isUser = false, rawJson = msg.rawJson),
                    error = null
                )
                is Message.LoadingFailed -> copy(
                    isLoading = false,
                    messages = messages + ChatMessage(text = msg.error, isUser = false),
                    error = msg.error
                )
            }
    }
}