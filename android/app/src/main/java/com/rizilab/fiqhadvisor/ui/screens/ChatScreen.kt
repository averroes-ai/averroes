package com.rizilab.fiqhadvisor.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

// Message types
sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long

    data class User(override val id: String, override val timestamp: Long, val content: String) :
            ChatMessage()

    data class Assistant(
            override val id: String,
            override val timestamp: Long,
            val content: String,
            val isStreaming: Boolean = false,
            val confidence: Double? = null,
            val sources: List<String> = emptyList()
    ) : ChatMessage()

    data class System(
            override val id: String,
            override val timestamp: Long,
            val content: String,
            val type: SystemMessageType
    ) : ChatMessage()
}

enum class SystemMessageType {
    WELCOME,
    ANALYZING,
    ERROR
}

// Streaming callback interface
data class StreamingCallbacks(
        val onChunk: (String) -> Unit,
        val onComplete: () -> Unit,
        val onError: (String) -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
        onBack: () -> Unit = {},
        onAnalyzeToken: (String, StreamingCallbacks) -> Unit = { _, _ -> },
        onQueryStream: (String, StreamingCallbacks) -> Unit = { _, _ -> }
) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentInput by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Initialize with welcome message
    LaunchedEffect(Unit) {
        messages =
                listOf(
                        ChatMessage.System(
                                id = "welcome",
                                timestamp = System.currentTimeMillis(),
                                content =
                                        "Welcome to FiqhAdvisor! I'm here to help you with Islamic finance questions. You can ask me about cryptocurrency analysis, investment principles, or any Shariah-related financial matters.",
                                type = SystemMessageType.WELCOME
                        )
                )
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        // Header
        TopAppBar(
                title = {
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                                text = "FiqhAdvisor Chat",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Status indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                    modifier =
                                            Modifier.size(8.dp)
                                                    .background(
                                                            if (isStreaming) Color(0xFFEAB308)
                                                            else Color(0xFF10B981),
                                                            CircleShape
                                                    )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                    text = if (isStreaming) "Thinking..." else "Ready",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF6B7280)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // Messages List
        LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageItem(message = message, modifier = Modifier.fillMaxWidth())
            }

            // Typing indicator
            if (isStreaming) {
                item { TypingIndicator() }
            }
        }

        // Input Section
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Column {
                // Quick Actions
                LazyRow(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickActions =
                            listOf(
                                    "Is Bitcoin halal?",
                                    "Analyze Ethereum",
                                    "Islamic DeFi",
                                    "Staking rewards"
                            )

                    items(quickActions.size) { index ->
                        val action = quickActions[index]
                        AssistChip(
                                onClick = {
                                    if (!isStreaming) {
                                        isStreaming = true

                                        // Add user message
                                        val userMessage =
                                                ChatMessage.User(
                                                        id = java.util.UUID.randomUUID().toString(),
                                                        timestamp = System.currentTimeMillis(),
                                                        content = action
                                                )
                                        messages = messages + userMessage

                                        // Create assistant message with empty content for streaming
                                        val assistantId = java.util.UUID.randomUUID().toString()
                                        val assistantMessage =
                                                ChatMessage.Assistant(
                                                        id = assistantId,
                                                        timestamp = System.currentTimeMillis(),
                                                        content = "",
                                                        isStreaming = true,
                                                        confidence = 0.9
                                                )
                                        messages = messages + assistantMessage

                                        // Call streaming query with proper callbacks
                                        onQueryStream(
                                                action,
                                                StreamingCallbacks(
                                                        onChunk = { currentContent ->
                                                            // Update the existing assistant message
                                                            // currentContent is already accumulated
                                                            // from FiqhAIManager
                                                            messages =
                                                                    messages.map { msg ->
                                                                        if (msg is
                                                                                        ChatMessage.Assistant &&
                                                                                        msg.id ==
                                                                                                assistantId
                                                                        ) {
                                                                            msg.copy(
                                                                                    content =
                                                                                            currentContent,
                                                                                    isStreaming =
                                                                                            true
                                                                            )
                                                                        } else {
                                                                            msg
                                                                        }
                                                                    }
                                                        },
                                                        onComplete = {
                                                            // Properly handle completion
                                                            isStreaming = false
                                                            messages =
                                                                    messages.map { msg ->
                                                                        if (msg is
                                                                                        ChatMessage.Assistant &&
                                                                                        msg.id ==
                                                                                                assistantId
                                                                        ) {
                                                                            msg.copy(
                                                                                    isStreaming =
                                                                                            false
                                                                            )
                                                                        } else {
                                                                            msg
                                                                        }
                                                                    }
                                                        },
                                                        onError = { error ->
                                                            isStreaming = false
                                                            // Could add error message to chat
                                                        }
                                                )
                                        )
                                    }
                                },
                                label = { Text(action, fontSize = 12.sp) },
                                colors =
                                        AssistChipDefaults.assistChipColors(
                                                containerColor = Color(0xFFF1F5F9)
                                        )
                        )
                    }
                }

                Divider(color = Color(0xFFE2E8F0))

                // Input Field
                Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                            value = currentInput,
                            onValueChange = { currentInput = it },
                            placeholder = {
                                Text("Ask about Islamic finance...", color = Color(0xFF9CA3AF))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {}),
                            colors =
                                    OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFFE2E8F0)
                                    )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    FloatingActionButton(
                            onClick = {
                                if (currentInput.isNotBlank() && !isStreaming) {
                                    // Add user message
                                    val userMessage =
                                            ChatMessage.User(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    timestamp = System.currentTimeMillis(),
                                                    content = currentInput
                                            )

                                    messages = messages + userMessage
                                    isStreaming = true
                                    val inputToSend = currentInput
                                    currentInput = ""
                                    keyboardController?.hide()

                                    // Create assistant message with empty content for streaming
                                    val assistantId = java.util.UUID.randomUUID().toString()
                                    val assistantMessage =
                                            ChatMessage.Assistant(
                                                    id = assistantId,
                                                    timestamp = System.currentTimeMillis(),
                                                    content = "",
                                                    isStreaming = true,
                                                    confidence = 0.9
                                            )
                                    messages = messages + assistantMessage

                                    // Call streaming query with proper callbacks
                                    onQueryStream(
                                            inputToSend,
                                            StreamingCallbacks(
                                                    onChunk = { currentContent ->
                                                        // Update the existing assistant message
                                                        // currentContent is already accumulated
                                                        // from FiqhAIManager
                                                        messages =
                                                                messages.map { msg ->
                                                                    if (msg is
                                                                                    ChatMessage.Assistant &&
                                                                                    msg.id ==
                                                                                            assistantId
                                                                    ) {
                                                                        msg.copy(
                                                                                content =
                                                                                        currentContent,
                                                                                isStreaming = true
                                                                        )
                                                                    } else {
                                                                        msg
                                                                    }
                                                                }
                                                    },
                                                    onComplete = {
                                                        // Properly handle completion
                                                        isStreaming = false
                                                        messages =
                                                                messages.map { msg ->
                                                                    if (msg is
                                                                                    ChatMessage.Assistant &&
                                                                                    msg.id ==
                                                                                            assistantId
                                                                    ) {
                                                                        msg.copy(
                                                                                isStreaming = false
                                                                        )
                                                                    } else {
                                                                        msg
                                                                    }
                                                                }
                                                    },
                                                    onError = { error ->
                                                        isStreaming = false
                                                        // Could add error message to chat
                                                    }
                                            )
                                    )
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor =
                                    if (currentInput.isNotBlank() && !isStreaming) Color(0xFF3B82F6)
                                    else Color(0xFFE2E8F0),
                            contentColor =
                                    if (currentInput.isNotBlank() && !isStreaming) Color.White
                                    else Color(0xFF9CA3AF)
                    ) {
                        Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage, modifier: Modifier = Modifier) {
    when (message) {
        is ChatMessage.User -> UserMessageItem(message, modifier)
        is ChatMessage.Assistant -> AssistantMessageItem(message, modifier)
        is ChatMessage.System -> SystemMessageItem(message, modifier)
    }
}

@Composable
private fun UserMessageItem(message: ChatMessage.User, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Card(
                modifier = Modifier.widthIn(max = 280.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
        ) {
            Text(
                    text = message.content,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun AssistantMessageItem(message: ChatMessage.Assistant, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        // Avatar
        Box(
                modifier = Modifier.size(32.dp).background(Color(0xFF10B981), CircleShape),
                contentAlignment = Alignment.Center
        ) { Text(text = "🤖", fontSize = 16.sp) }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.widthIn(max = 280.dp)) {
            Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = message.content, fontSize = 16.sp, color = Color(0xFF1E293B))

                    // Streaming indicator
                    if (message.isStreaming) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(3) { index ->
                                val animatedAlpha by
                                        rememberInfiniteTransition(label = "typing")
                                                .animateFloat(
                                                        initialValue = 0.3f,
                                                        targetValue = 1f,
                                                        animationSpec =
                                                                infiniteRepeatable(
                                                                        animation = tween(600),
                                                                        repeatMode =
                                                                                RepeatMode.Reverse,
                                                                        initialStartOffset =
                                                                                StartOffset(
                                                                                        index * 200
                                                                                )
                                                                ),
                                                        label = "alpha"
                                                )

                                Box(
                                        modifier =
                                                Modifier.size(6.dp)
                                                        .background(
                                                                Color(0xFF3B82F6)
                                                                        .copy(
                                                                                alpha =
                                                                                        animatedAlpha
                                                                        ),
                                                                CircleShape
                                                        )
                                )

                                if (index < 2) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Metadata
            if (!message.isStreaming) {
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    message.confidence?.let { confidence ->
                        Text(
                                text = "${(confidence * 100).toInt()}% confident",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(text = "•", fontSize = 11.sp, color = Color(0xFF6B7280))

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                            text =
                                    SimpleDateFormat("HH:mm", Locale.getDefault())
                                            .format(Date(message.timestamp)),
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemMessageItem(message: ChatMessage.System, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Card(
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        when (message.type) {
                                            SystemMessageType.WELCOME -> Color(0xFFF0F9FF)
                                            SystemMessageType.ANALYZING -> Color(0xFFFFFBEB)
                                            SystemMessageType.ERROR -> Color(0xFFFEF2F2)
                                        }
                        ),
                shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                    text = message.content,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color =
                            when (message.type) {
                                SystemMessageType.WELCOME -> Color(0xFF1E40AF)
                                SystemMessageType.ANALYZING -> Color(0xFFA16207)
                                SystemMessageType.ERROR -> Color(0xFFDC2626)
                            },
                    textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
                modifier = Modifier.size(32.dp).background(Color(0xFF10B981), CircleShape),
                contentAlignment = Alignment.Center
        ) { Text(text = "🤖", fontSize = 16.sp) }

        Spacer(modifier = Modifier.width(12.dp))

        Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
                elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val animatedAlpha by
                            rememberInfiniteTransition(label = "typing")
                                    .animateFloat(
                                            initialValue = 0.3f,
                                            targetValue = 1f,
                                            animationSpec =
                                                    infiniteRepeatable(
                                                            animation = tween(600),
                                                            repeatMode = RepeatMode.Reverse,
                                                            initialStartOffset =
                                                                    StartOffset(index * 200)
                                                    ),
                                            label = "alpha"
                                    )

                    Box(
                            modifier =
                                    Modifier.size(8.dp)
                                            .background(
                                                    Color(0xFF3B82F6).copy(alpha = animatedAlpha),
                                                    CircleShape
                                            )
                    )

                    if (index < 2) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}
