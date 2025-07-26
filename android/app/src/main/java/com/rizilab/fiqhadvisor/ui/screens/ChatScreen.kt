package com.rizilab.fiqhadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizilab.fiqhadvisor.ui.components.FiqhCard
import com.rizilab.fiqhadvisor.ui.theme.FiqhColors
import com.rizilab.fiqhadvisor.ui.theme.FiqhTypography
import com.rizilab.fiqhadvisor.ui.viewmodel.AnalysisState
import com.rizilab.fiqhadvisor.ui.viewmodel.FiqhAIViewModel
import kotlinx.coroutines.launch

data class ChatMessage(
        val id: String,
        val content: String,
        val isUser: Boolean,
        val timestamp: Long,
        val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: FiqhAIViewModel) {
    var messages by remember {
        mutableStateOf(
                listOf(
                        ChatMessage(
                                id = "welcome",
                                content =
                                        "Assalamu'alaikum! I'm your Fiqh Advisor AI. I can help you analyze cryptocurrencies and tokens from an Islamic perspective. You can ask me about specific tokens (like 'Is Bitcoin halal?') or general questions about Islamic finance.\n\nWhat would you like to know?",
                                isUser = false,
                                timestamp = System.currentTimeMillis()
                        )
                )
        )
    }

    var inputText by remember { mutableStateOf("") }
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Handle analysis state changes
    LaunchedEffect(analysisState) {
        when (val currentState = analysisState) {
            is AnalysisState.Loading -> {
                // Add loading message if not already present
                if (messages.none { it.isLoading }) {
                    messages =
                            messages +
                                    ChatMessage(
                                            id = "loading_${System.currentTimeMillis()}",
                                            content = "Analyzing...",
                                            isUser = false,
                                            timestamp = System.currentTimeMillis(),
                                            isLoading = true
                                    )
                    // Scroll to bottom
                    scope.launch { listState.animateScrollToItem(messages.size - 1) }
                }
            }
            is AnalysisState.Success -> {
                // Remove loading message and add response
                messages =
                        messages.filter { !it.isLoading } +
                                ChatMessage(
                                        id = "response_${System.currentTimeMillis()}",
                                        content = formatResponse(currentState.result),
                                        isUser = false,
                                        timestamp = System.currentTimeMillis()
                                )
                // Scroll to bottom
                scope.launch { listState.animateScrollToItem(messages.size - 1) }
            }
            is AnalysisState.FallbackResponse -> {
                // Remove loading message and add fallback response
                messages =
                        messages.filter { !it.isLoading } +
                                ChatMessage(
                                        id = "fallback_${System.currentTimeMillis()}",
                                        content = currentState.message,
                                        isUser = false,
                                        timestamp = System.currentTimeMillis()
                                )
                // Scroll to bottom
                scope.launch { listState.animateScrollToItem(messages.size - 1) }
            }
            is AnalysisState.Error -> {
                // Remove loading message and add error
                messages =
                        messages.filter { !it.isLoading } +
                                ChatMessage(
                                        id = "error_${System.currentTimeMillis()}",
                                        content =
                                                "I apologize, but I encountered an error: ${currentState.message}\n\nPlease try again or rephrase your question.",
                                        isUser = false,
                                        timestamp = System.currentTimeMillis()
                                )
                // Scroll to bottom
                scope.launch { listState.animateScrollToItem(messages.size - 1) }
            }
            else -> {
                /* Idle state */
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(FiqhColors.Background)) {
        // Header
        TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🕌", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text(
                                    text = "Fiqh Advisor",
                                    style = FiqhTypography.Heading2.copy(fontSize = 18.sp),
                                    color = FiqhColors.Primary
                            )
                            Text(
                                    text = "Islamic AI Assistant",
                                    style = FiqhTypography.Caption,
                                    color = FiqhColors.OnBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                            onClick = {
                                // Settings menu - could implement user preferences, language
                                // selection, etc.
                            }
                    ) {
                        Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Settings",
                                tint = FiqhColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FiqhColors.Surface)
        )

        // Messages List
        LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
        ) { items(messages) { message -> ChatMessageItem(message = message) } }

        // Input Section
        Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FiqhColors.Surface,
                shadowElevation = 8.dp
        ) {
            Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                    "Ask about crypto from Islamic perspective...",
                                    style = FiqhTypography.Body2,
                                    color = FiqhColors.OnSurface.copy(alpha = 0.6f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions =
                                KeyboardActions(
                                        onSend = {
                                            if (inputText.isNotBlank()) {
                                                sendMessage(inputText, viewModel) { newMessage ->
                                                    messages = messages + newMessage
                                                    inputText = ""
                                                    keyboardController?.hide()
                                                    scope.launch {
                                                        listState.animateScrollToItem(
                                                                messages.size - 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                ),
                        shape = RoundedCornerShape(24.dp),
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FiqhColors.Primary,
                                        unfocusedBorderColor =
                                                FiqhColors.OnSurface.copy(alpha = 0.3f)
                                ),
                        maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                sendMessage(inputText, viewModel) { newMessage ->
                                    messages = messages + newMessage
                                    inputText = ""
                                    keyboardController?.hide()
                                    scope.launch {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = FiqhColors.Primary,
                        contentColor = Color.White
                ) {
                    Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick suggestions (show when no messages except welcome)
        if (messages.size == 1) {
            LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickSuggestions) { suggestion ->
                    SuggestionChip(
                            suggestion = suggestion,
                            onClick = {
                                sendMessage(suggestion, viewModel) { newMessage ->
                                    messages = messages + newMessage
                                    scope.launch {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            // AI Avatar
            Box(
                    modifier =
                            Modifier.size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(FiqhColors.Primary),
                    contentAlignment = Alignment.Center
            ) { Text(text = "🤖", fontSize = 16.sp) }
            Spacer(modifier = Modifier.width(8.dp))
        }

        FiqhCard(
                modifier = Modifier.widthIn(max = 280.dp),
                backgroundColor = if (message.isUser) FiqhColors.Primary else FiqhColors.Surface
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = FiqhColors.Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = message.content,
                                style = FiqhTypography.Body2,
                                color = FiqhColors.OnSurface
                        )
                    }
                } else {
                    Text(
                            text = message.content,
                            style = FiqhTypography.Body1,
                            color = if (message.isUser) Color.White else FiqhColors.OnSurface
                    )
                }
            }
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Avatar
            Box(
                    modifier =
                            Modifier.size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(FiqhColors.Secondary),
                    contentAlignment = Alignment.Center
            ) { Text(text = "👤", fontSize = 16.sp) }
        }
    }
}

@Composable
private fun SuggestionChip(suggestion: String, onClick: () -> Unit) {
    Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = FiqhColors.Primary.copy(alpha = 0.1f),
            border =
                    androidx.compose.foundation.BorderStroke(
                            1.dp,
                            FiqhColors.Primary.copy(alpha = 0.3f)
                    )
    ) {
        Text(
                text = suggestion,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = FiqhTypography.Body2,
                color = FiqhColors.Primary
        )
    }
}

private fun sendMessage(
        text: String,
        viewModel: FiqhAIViewModel,
        onMessageAdded: (ChatMessage) -> Unit
) {
    // Add user message
    val userMessage =
            ChatMessage(
                    id = "user_${System.currentTimeMillis()}",
                    content = text,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
            )
    onMessageAdded(userMessage)

    // Check if it's a token analysis request
    val tokenPattern = Regex("\\b[A-Z]{2,10}\\b") // Simple token pattern
    val foundTokens = tokenPattern.findAll(text.uppercase()).map { it.value }.toList()

    if (foundTokens.isNotEmpty() &&
                    (text.contains("analyze", ignoreCase = true) ||
                            text.contains("halal", ignoreCase = true) ||
                            text.contains("haram", ignoreCase = true))
    ) {
        // Analyze the first found token
        viewModel.analyzeToken(foundTokens.first())
    } else {
        // For general chat messages, use the chat session functionality
        viewModel.sendChatMessage(text)
    }
}

private fun formatResponse(queryResponse: com.rizilab.fiqhadvisor.core.QueryResponse): String {
    val confidence = (queryResponse.confidence * 100).toInt()
    val sources =
            if (queryResponse.sources.isNotEmpty()) {
                "\n\n📚 Islamic References:\n${queryResponse.sources.joinToString("\n") { "• $it" }}"
            } else ""

    return "${queryResponse.response}\n\n🎯 Confidence: ${confidence}%$sources"
}

private val quickSuggestions =
        listOf(
                "Is Bitcoin halal?",
                "Analyze SOL token",
                "What makes crypto haram?",
                "Analyze ETH",
                "Islamic view on DeFi",
                "Is staking halal?"
        )
