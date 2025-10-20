package com.farionik.aiadvent.presentation.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.farionik.aiadvent.domain.model.ChatMessage
import com.farionik.aiadvent.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.launch

class ChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val sendMessageUseCase: SendMessageUseCase,
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
        data class TemperatureChanged(val temperature: Float) : Message
        data class UserMessageAdded(val message: String) : Message
        data object LoadingStarted : Message
        data class AiMessageAdded(val chatMessage: ChatMessage) : Message
        data class LoadingFailed(val error: String) : Message
    }

    private inner class ExecutorImpl : CoroutineExecutor<ChatStore.Intent, Nothing, ChatStore.State, Message, ChatStore.Label>() {
        override fun executeIntent(intent: ChatStore.Intent, getState: () -> ChatStore.State) {
            when (intent) {
                is ChatStore.Intent.UpdateInputText -> {
                    dispatch(Message.InputTextChanged(intent.text))
                }
                is ChatStore.Intent.UpdateTemperature -> {
                    dispatch(Message.TemperatureChanged(intent.temperature))
                }
                is ChatStore.Intent.SendMessage -> {
                    sendMessage(intent.message, getState)
                }
            }
        }

        private fun sendMessage(message: String, getState: () -> ChatStore.State) {
            dispatch(Message.UserMessageAdded(message))
            scope.launch {
                dispatch(Message.LoadingStarted)

                val currentState = getState()
                val temperature = currentState.temperature

                sendMessageUseCase(message, apiKey, temperature)
                    .onSuccess { chatMessage ->
                        dispatch(Message.AiMessageAdded(chatMessage))
                    }
                    .onFailure { error ->
                        val errorMessage = "Ошибка: ${error.message}"
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
                is Message.TemperatureChanged -> copy(temperature = msg.temperature)
                is Message.UserMessageAdded -> copy(
                    messages = messages + ChatMessage(text = msg.message, isUser = true),
                    inputText = ""
                )
                is Message.LoadingStarted -> copy(isLoading = true, error = null)
                is Message.AiMessageAdded -> copy(
                    isLoading = false,
                    messages = messages + msg.chatMessage,
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