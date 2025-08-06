package com.rizilab.averroes.presentation.chat

import androidx.lifecycle.viewModelScope
import com.rizilab.averroes.presentation.base.MviViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// UniFFI imports for fiqh_core integration (re-enabled)
import com.rizilab.averroes.core.AverroesManager

/**
 * Chat Message
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

/**
 * Chat State
 */
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val isInitialized: Boolean = false
)

/**
 * Chat Intents
 */
sealed class ChatIntent {
    object InitializeFiqhCore : ChatIntent()
    data class UpdateInput(val text: String) : ChatIntent()
    data class SendMessage(val message: String = "") : ChatIntent()
    data class AnalyzeToken(val userInput: String) : ChatIntent() // Now accepts any text input
    object ClearChat : ChatIntent()
    object ClearError : ChatIntent()
}

/**
 * Chat Effects
 */
sealed class ChatEffect {
    object ScrollToBottom : ChatEffect()
    data class ShowError(val message: String) : ChatEffect()
    data class ShowSuccess(val message: String) : ChatEffect()
}

/**
 * Chat ViewModel using MVI pattern
 * Real fiqh_core integration ready for WSL
 */
class ChatViewModel : MviViewModel<ChatState, ChatIntent, ChatEffect>(ChatState()) {
    // Real AI system integration using fiqh_core
    private var fiqhAiManager: AverroesManager? = null

    init {
        handleIntent(ChatIntent.InitializeFiqhCore)
    }

    override fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.InitializeFiqhCore -> initializeFiqhCore()
            is ChatIntent.UpdateInput -> updateInput(intent.text)
            is ChatIntent.SendMessage -> sendMessage(intent.message)
            is ChatIntent.AnalyzeToken -> analyzeToken(intent.userInput)
            is ChatIntent.ClearChat -> clearChat()
            is ChatIntent.ClearError -> clearError()
        }
    }

    private fun initializeFiqhCore() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            try {
                // Initialize real fiqh_core AI system
                fiqhAiManager = AverroesManager()
                val initialized = fiqhAiManager?.initialize() ?: false

                if (!initialized) {
                    throw Exception("Failed to initialize fiqh_core AI system")
                }

                // Small delay for UI feedback
                delay(1000)

                updateState {
                    copy(
                        isLoading = false,
                        isInitialized = true,
                        messages = listOf(
                            ChatMessage(
                                id = generateMessageId(),
                                content = "Assalamu Alaikum! I'm your Islamic Finance AI assistant powered by fiqh_core. I can help you with:\n\n" +
                                        "• Islamic finance principles\n" +
                                        "• Halal investment guidance\n" +
                                        "• Cryptocurrency analysis\n" +
                                        "• Sharia compliance questions\n\n" +
                                        "How can I assist you today?",
                                isUser = false
                            )
                        )
                    )
                }

                sendEffect(ChatEffect.ScrollToBottom)

            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        error = "Failed to initialize AI: ${e.message}"
                    )
                }
                sendEffect(ChatEffect.ShowError("Failed to initialize AI system"))
            }
        }
    }

    private fun updateInput(text: String) {
        updateState { copy(currentInput = text) }
    }

    private fun sendMessage(message: String) {
        val messageText = message.ifEmpty { state.value.currentInput }
        if (messageText.isBlank()) return

        viewModelScope.launch {
            val userMessage = ChatMessage(
                id = generateMessageId(),
                content = messageText,
                isUser = true
            )

            // Add user message and clear input
            updateState { 
                copy(
                    messages = messages + userMessage,
                    currentInput = "",
                    isStreaming = true
                )
            }

            sendEffect(ChatEffect.ScrollToBottom)

            try {
                // Use real fiqh_core AI system for queries
                fiqhAiManager?.let { aiManager ->
                    val aiMessageId = generateMessageId()
                    aiManager.queryStream(
                        question = messageText,
                        onChunk = { accumulatedContent ->
                            val aiMessage = ChatMessage(
                                id = aiMessageId,
                                content = accumulatedContent,
                                isUser = false,
                                isStreaming = true
                            )
                            updateStreamingMessage(aiMessage)
                        },
                        onComplete = { finalResponse ->
                            val aiMessage = ChatMessage(
                                id = aiMessageId,
                                content = finalResponse,
                                isUser = false,
                                isStreaming = false
                            )
                            updateStreamingMessage(aiMessage)
                            updateState { copy(isStreaming = false) }
                        },
                        onError = { error ->
                            updateState {
                                copy(
                                    isStreaming = false,
                                    error = error
                                )
                            }
                            sendEffect(ChatEffect.ShowError("fiqh_core AI Error: $error"))
                        }
                    )
                } ?: run {
                    // Fallback to simulation if AI manager not available
                    simulateStreamingResponse(messageText)
                }
                
            } catch (e: Exception) {
                updateState { 
                    copy(
                        isStreaming = false,
                        error = e.message
                    )
                }
                sendEffect(ChatEffect.ShowError("Failed to get AI response"))
            }
        }
    }

    private fun analyzeToken(userInput: String) {
        viewModelScope.launch {
            val userMessage = ChatMessage(
                id = generateMessageId(),
                content = userInput,
                isUser = true
            )

            updateState {
                copy(
                    messages = messages + userMessage,
                    isStreaming = true
                )
            }

            sendEffect(ChatEffect.ScrollToBottom)

            try {
                // Use real fiqh_core AI system for any user input analysis
                fiqhAiManager?.let { aiManager ->
                    val aiMessageId = generateMessageId()
                    aiManager.analyzeTokenStream(
                        token = userInput, // Now accepts any text input
                        onChunk = { accumulatedContent ->
                            val aiMessage = ChatMessage(
                                id = aiMessageId,
                                content = accumulatedContent,
                                isUser = false,
                                isStreaming = true
                            )
                            updateStreamingMessage(aiMessage)
                        },
                        onComplete = { finalResponse ->
                            val aiMessage = ChatMessage(
                                id = aiMessageId,
                                content = finalResponse,
                                isUser = false,
                                isStreaming = false
                            )
                            updateStreamingMessage(aiMessage)
                            updateState { copy(isStreaming = false) }
                        },
                        onError = { error ->
                            updateState {
                                copy(
                                    isStreaming = false,
                                    error = error
                                )
                            }
                            sendEffect(ChatEffect.ShowError("AI Analysis Error: $error"))
                        }
                    )
                } ?: run {
                    // Fallback to simulation if AI manager not available
                    simulateTokenAnalysisResponse(userInput)
                }

            } catch (e: Exception) {
                updateState {
                    copy(
                        isStreaming = false,
                        error = e.message
                    )
                }
                sendEffect(ChatEffect.ShowError("Failed to analyze input"))
            }
        }
    }

    private suspend fun simulateStreamingResponse(question: String) {
        val aiMessageId = generateMessageId()

        // Simulate different responses based on question content
        val response = when {
            question.contains("halal", ignoreCase = true) ->
                "Based on Islamic finance principles, I can help you understand what makes an investment halal. The key criteria include:\n\n" +
                "1. No Riba (Interest): The investment should not involve interest-based transactions\n" +
                "2. No Gharar (Excessive uncertainty): Avoid highly speculative investments\n" +
                "3. No Haram activities: The business should not be involved in prohibited activities\n" +
                "4. Asset-backed: The investment should have real underlying value\n\n" +
                "Would you like me to analyze a specific investment for you?"

            question.contains("crypto", ignoreCase = true) || question.contains("bitcoin", ignoreCase = true) ->
                "Regarding cryptocurrencies from an Islamic perspective:\n\n" +
                "✅ Generally Halal cryptocurrencies:\n" +
                "• Bitcoin (BTC) - Decentralized, no interest mechanisms\n" +
                "• Ethereum (ETH) - Smart contract platform with utility\n" +
                "• Cardano (ADA) - Research-driven, sustainable\n\n" +
                "❌ Generally Haram cryptocurrencies:\n" +
                "• Interest-bearing tokens (DeFi lending)\n" +
                "• Gambling/casino tokens\n" +
                "• Highly speculative meme coins\n\n" +
                "Would you like me to analyze a specific cryptocurrency?"

            else ->
                "Thank you for your question about Islamic finance. I'm here to help you navigate financial decisions according to Islamic principles.\n\n" +
                "I can assist with:\n" +
                "• Investment analysis\n" +
                "• Cryptocurrency evaluation\n" +
                "• Islamic banking principles\n" +
                "• Sharia compliance questions\n\n" +
                "Please feel free to ask me anything specific!"
        }

        // Simulate streaming by adding chunks
        val chunks = response.chunked(20)
        var currentContent = ""

        for (chunk in chunks) {
            currentContent += chunk

            val aiMessage = ChatMessage(
                id = aiMessageId,
                content = currentContent,
                isUser = false,
                isStreaming = true
            )

            updateState {
                copy(
                    messages = messages.map { msg ->
                        if (msg.id == aiMessageId) aiMessage
                        else msg
                    }.let { updatedMessages ->
                        if (updatedMessages.none { it.id == aiMessageId }) {
                            updatedMessages + aiMessage
                        } else {
                            updatedMessages
                        }
                    }
                )
            }

            sendEffect(ChatEffect.ScrollToBottom)
            delay(100) // Simulate streaming delay
        }

        // Mark streaming as complete
        updateState {
            copy(
                messages = messages.map { msg ->
                    if (msg.id == aiMessageId) msg.copy(isStreaming = false)
                    else msg
                },
                isStreaming = false
            )
        }
    }

    private suspend fun simulateTokenAnalysisResponse(token: String) {
        val aiMessageId = generateMessageId()

        val response = "# Token Analysis: ${token.uppercase()}\n\n" +
                "## Islamic Compliance Assessment\n\n" +
                "**Status:** Under Review\n" +
                "**Confidence:** 75%\n\n" +
                "## Analysis Summary\n" +
                "Based on available information about $token, here's my preliminary assessment:\n\n" +
                "### Positive Factors:\n" +
                "• Appears to have real utility\n" +
                "• No obvious interest-based mechanisms\n" +
                "• Legitimate use case\n\n" +
                "### Concerns:\n" +
                "• Need more information about governance\n" +
                "• Volatility considerations\n\n" +
                "### Recommendation:\n" +
                "Further research recommended. Consider consulting with Islamic finance scholars for definitive ruling.\n\n" +
                "Would you like me to analyze any specific aspects of this token?"

        // Simulate streaming
        val chunks = response.chunked(30)
        var currentContent = ""

        for (chunk in chunks) {
            currentContent += chunk

            val aiMessage = ChatMessage(
                id = aiMessageId,
                content = currentContent,
                isUser = false,
                isStreaming = true
            )

            updateState {
                copy(
                    messages = messages.map { msg ->
                        if (msg.id == aiMessageId) aiMessage
                        else msg
                    }.let { updatedMessages ->
                        if (updatedMessages.none { it.id == aiMessageId }) {
                            updatedMessages + aiMessage
                        } else {
                            updatedMessages
                        }
                    }
                )
            }

            sendEffect(ChatEffect.ScrollToBottom)
            delay(80)
        }

        // Mark streaming as complete
        updateState {
            copy(
                messages = messages.map { msg ->
                    if (msg.id == aiMessageId) msg.copy(isStreaming = false)
                    else msg
                },
                isStreaming = false
            )
        }
    }

    /**
     * Stream AI response from fiqh_core (ready for WSL)
     */
    private suspend fun streamAiResponse(response: String, aiMessageId: String) {
        // Stream the response by adding chunks
        val chunks = response.chunked(25) // Smaller chunks for better streaming effect
        var currentContent = ""

        for (chunk in chunks) {
            currentContent += chunk

            val aiMessage = ChatMessage(
                id = aiMessageId,
                content = currentContent,
                isUser = false,
                isStreaming = true
            )

            updateState {
                copy(
                    messages = messages.map { msg ->
                        if (msg.id == aiMessageId) aiMessage
                        else msg
                    }.let { updatedMessages ->
                        if (updatedMessages.none { it.id == aiMessageId }) {
                            updatedMessages + aiMessage
                        } else {
                            updatedMessages
                        }
                    }
                )
            }

            sendEffect(ChatEffect.ScrollToBottom)
            delay(50) // Faster streaming for better UX
        }

        // Mark streaming as complete
        updateState {
            copy(
                messages = messages.map { msg ->
                    if (msg.id == aiMessageId) msg.copy(isStreaming = false)
                    else msg
                },
                isStreaming = false
            )
        }
    }

    private fun clearChat() {
        updateState { copy(messages = emptyList()) }
    }

    private fun clearError() {
        updateState { copy(error = null) }
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(0..999).random()}"
    }

    private fun updateStreamingMessage(aiMessage: ChatMessage) {
        updateState {
            copy(
                messages = messages.map { msg ->
                    if (msg.id == aiMessage.id) aiMessage
                    else msg
                }.let { updatedMessages ->
                    if (updatedMessages.none { it.id == aiMessage.id }) {
                        updatedMessages + aiMessage
                    } else {
                        updatedMessages
                    }
                }
            )
        }
        sendEffect(ChatEffect.ScrollToBottom)
    }
}
