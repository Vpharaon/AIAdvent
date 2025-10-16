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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.states
import androidx.compose.runtime.collectAsState
import com.farionik.aiadvent.BuildConfig
import com.farionik.aiadvent.data.network.ApiClient
import com.farionik.aiadvent.data.repository.ChatRepositoryImpl
import com.farionik.aiadvent.domain.model.ChatMessage
import com.farionik.aiadvent.domain.usecase.SendMessageUseCase
import com.farionik.aiadvent.domain.usecase.GetCountryInfoUseCase
import com.farionik.aiadvent.domain.usecase.ValidateCountryNameUseCase
import com.farionik.aiadvent.presentation.mvi.ChatStore
import com.farionik.aiadvent.presentation.mvi.ChatStoreFactory
import com.farionik.aiadvent.presentation.theme.AIAdventTheme

class MainActivity : ComponentActivity() {
    private val apiClient = ApiClient()
    private val repository = ChatRepositoryImpl(apiClient)
    private val sendMessageUseCase = SendMessageUseCase(repository)
    private val getCountryInfoUseCase = GetCountryInfoUseCase(repository)
    private val validateCountryNameUseCase = ValidateCountryNameUseCase(repository)
    private val storeFactory: StoreFactory = DefaultStoreFactory()
    private val store: ChatStore = ChatStoreFactory(
        storeFactory,
        sendMessageUseCase,
        getCountryInfoUseCase,
        validateCountryNameUseCase,
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
                .background(Color(0xFFE3F2FD)) // Светло-голубой фон для чата
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages) { message ->
                    ChatMessageItem(message = message)
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
                    onClick = { onIntent(ChatStore.Intent.GetCountryInfo(state.inputText)) },
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
fun ChatMessageItem(message: ChatMessage) {
    var showJsonDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    Color(0xFF4A90E2) // Приятный синий для пользователя
                } else {
                    Color(0xFFF5F5F5) // Светло-серый фон для AI
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isUser) {
                        Color.White // Белый текст на синем фоне
                    } else {
                        Color(0xFF212121) // Почти черный текст для читаемости
                    }
                )

                // Кнопка просмотра JSON (только для сообщений с rawJson)
                if (!message.isUser && message.rawJson != null) {
                    TextButton(
                        onClick = { showJsonDialog = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("{ } Посмотреть JSON")
                    }
                }
            }
        }
    }

    // Диалог для отображения raw JSON
    if (showJsonDialog && message.rawJson != null) {
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("Raw JSON") },
            text = {
                Text(
                    text = message.rawJson,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showJsonDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val storeFactory: StoreFactory = DefaultStoreFactory()
    val apiClient = ApiClient()
    val repository = ChatRepositoryImpl(apiClient)
    val sendMessageUseCase = SendMessageUseCase(repository)
    val getCountryInfoUseCase = GetCountryInfoUseCase(repository)
    val validateCountryNameUseCase = ValidateCountryNameUseCase(repository)
    val store = ChatStoreFactory(storeFactory, sendMessageUseCase, getCountryInfoUseCase, validateCountryNameUseCase, "").create()

    AIAdventTheme {
        Greeting(store, "Android")
    }
}