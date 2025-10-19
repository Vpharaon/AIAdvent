package com.farionik.aiadvent.presentation.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.farionik.aiadvent.BuildConfig
import com.farionik.aiadvent.data.network.ApiClient
import com.farionik.aiadvent.data.repository.ChatRepositoryImpl
import com.farionik.aiadvent.domain.model.ChatMessage
import com.farionik.aiadvent.domain.usecase.SendMessageUseCase
import com.farionik.aiadvent.presentation.mvi.ChatStore
import com.farionik.aiadvent.presentation.mvi.ChatStoreFactory
import com.farionik.aiadvent.presentation.theme.AIAdventTheme

class MainActivity : ComponentActivity() {
    private val apiClient = ApiClient()
    private val repository = ChatRepositoryImpl(apiClient)
    private val sendMessageUseCase = SendMessageUseCase(repository)
    private val storeFactory: StoreFactory = DefaultStoreFactory()
    private val store: ChatStore = ChatStoreFactory(
        storeFactory,
        sendMessageUseCase,
        BuildConfig.API_KEY
    ).create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIAdventTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        store = store,
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            store.dispose()
            apiClient.close()
        }
    }
}

@Composable
fun Greeting(store: ChatStore, name: String, modifier: Modifier = Modifier) {
    val state by store.states.collectAsState(initial = ChatStore.State())

    ChatScreen(
        state = state,
        onIntent = store::accept,
        modifier = modifier
    )
}

@Composable
fun ChatScreen(
    state: ChatStore.State,
    onIntent: (ChatStore.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Автоскролл к последнему сообщению
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Список сообщений с приятным фоном
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5)) // Нежный серо-бежевый фон для чата
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages) { message ->
                    ChatMessageItem(
                        message = message,
                        onOptionSelected = { selectedOption ->
                            // Отправляем выбранный вариант как сообщение пользователя
                            onIntent(ChatStore.Intent.SendMessage(selectedOption))
                        },
                        onRetry = {
                            // Повторяем последний запрос
                            onIntent(ChatStore.Intent.RetryLastMessage)
                        }
                    )
                }

                // Индикатор загрузки
                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Кнопка "Начать сначала" - показываем только после того, как показаны модели телефонов
        // Минимум 15 сообщений: приветствие + 7 вопросов AI + 6 ответов пользователя + модели
        // (приветствие содержит первый вопрос о бюджете)
        if (state.messages.size >= 15 && !state.isLoading) {
            Surface(
                shadowElevation = 4.dp,
                color = Color(0xFFF0F4F8)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { onIntent(ChatStore.Intent.RestartConversation) },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Начать сначала",
                            tint = Color(0xFF7FA1C3),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Начать выбор сначала",
                            color = Color(0xFF5A7BA6)
                        )
                    }
                }
            }
        }

        // Поле ввода и кнопка отправки с отдельным фоном
        Surface(
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = { onIntent(ChatStore.Intent.UpdateInputText(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Введите сообщение...") },
                    enabled = !state.isLoading,
                    maxLines = 3
                )

                IconButton(
                    onClick = { onIntent(ChatStore.Intent.SendMessage(state.inputText)) },
                    enabled = state.inputText.isNotBlank() && !state.isLoading
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить"
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onOptionSelected: (String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = if (message.options != null) 350.dp else 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    Color(0xFFB8D4E8) // Пастельный голубой для пользователя
                } else {
                    Color(0xFFFFFFFF) // Белый фон для AI
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (message.isUser) {
                            Color(0xFF2C3E50) // Темно-серый текст на пастельном фоне
                        } else {
                            Color(0xFF2C3E50) // Темно-серый текст для читаемости
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Кнопка refresh для сообщений с ошибкой парсинга
                    if (!message.isUser && message.isParsingError) {
                        IconButton(onClick = onRetry) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Повторить запрос",
                                tint = Color(0xFF7FA1C3) // Пастельный синий
                            )
                        }
                    }
                }


                // Отображение вариантов ответа
                if (!message.isUser && message.options != null) {
                    message.options.forEach { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF0F4F8) // Нежный серо-голубой
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            onClick = { onOptionSelected(option.title) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF5A7BA6), // Мягкий синий
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                if (option.pros.isNotEmpty()) {
                                    Text(
                                        text = "✅ Плюсы:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF6AAF73), // Пастельный зеленый
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    option.pros.forEach { pro ->
                                        Text(
                                            text = "• $pro",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF5A6C7D), // Мягкий серо-синий
                                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                        )
                                    }
                                }

                                if (option.cons.isNotEmpty()) {
                                    Text(
                                        text = "❌ Минусы:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFE07A7A), // Пастельный красный
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    option.cons.forEach { con ->
                                        Text(
                                            text = "• $con",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF5A6C7D), // Мягкий серо-синий
                                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val storeFactory: StoreFactory = DefaultStoreFactory()
    val apiClient = ApiClient()
    val repository = ChatRepositoryImpl(apiClient)
    val sendMessageUseCase = SendMessageUseCase(repository)
    val store = ChatStoreFactory(storeFactory, sendMessageUseCase, "").create()

    AIAdventTheme {
        Greeting(store, "Android")
    }
}