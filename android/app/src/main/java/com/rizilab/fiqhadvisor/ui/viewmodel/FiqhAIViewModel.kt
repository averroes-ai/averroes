package com.rizilab.fiqhadvisor.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizilab.fiqhadvisor.AISystemDiagnostics
import com.rizilab.fiqhadvisor.core.FiqhAIManager
import com.rizilab.fiqhadvisor.core.FiqhAiConfig
import com.rizilab.fiqhadvisor.core.QueryResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FiqhAIViewModel
@Inject
constructor(
        private val fiqhManager: FiqhAIManager,
        @ApplicationContext private val context: Context
) : ViewModel() {

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private var isInitialized = false
    private var initializationFailed = false
    private var chatSessionStarted = false

    private suspend fun ensureInitialized(): Boolean {
        if (initializationFailed) {
            return false
        }

        if (!isInitialized) {
            try {
                Log.d("FiqhAIViewModel", "Attempting to initialize AI system...")

                // Check if manager is already ready
                if (fiqhManager.isReady()) {
                    isInitialized = true
                    return true
                }

                val config =
                        FiqhAiConfig(
                                openaiApiKey = "",
                                groqApiKey =
                                        "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",
                                grokApiKey =
                                        "xai-udKDRvvyrDuAYyrfye8thCLUgiKCNveOyYQrHjHnHCJ5pWUWH9TtfEQ73OI7Poh5b0UJZJvAPhYCKFEE",
                                modelName = "llama3-8b-8192",
                                qdrantUrl = "", // Disable vector DB for mobile to avoid crashes
                                databasePath =
                                        context.getDir("fiqh_data", Context.MODE_PRIVATE)
                                                .absolutePath, // Proper Android path
                                solanaRpcUrl = "https://api.mainnet-beta.solana.com",
                                enableSolana = true,
                                preferredModel = "groq"
                        )

                Log.d("FiqhAIViewModel", "Initializing FiqhAIManager with config...")
                Log.d("FiqhAIViewModel", "Config database path: ${config.databasePath}")

                fiqhManager.initialize(context, config)

                // Verify initialization worked
                if (fiqhManager.isReady()) {
                    isInitialized = true
                    Log.i("FiqhAIViewModel", "✅ AI system initialized successfully")
                    return true
                } else {
                    Log.e(
                            "FiqhAIViewModel",
                            "❌ AI system initialization failed - manager not ready"
                    )
                    initializationFailed = true
                    return false
                }
            } catch (e: Exception) {
                Log.e("FiqhAIViewModel", "❌ Failed to initialize AI system: ${e.message}", e)
                initializationFailed = true
                return false
            }
        }
        return true
    }

    private fun provideFallbackResponse(query: String): String {
        return when {
            query.contains("bitcoin", ignoreCase = true) -> {
                """
                **Bitcoin Analysis (Fallback Mode)**
                
                🔴 **Ruling: Haram (Prohibited)**
                
                **Islamic Reasoning:**
                • **Excessive Gharar (Uncertainty)**: Bitcoin's extreme price volatility creates excessive uncertainty, which is prohibited in Islamic finance
                • **Speculation (Maysir)**: Often used for gambling-like speculation rather than legitimate trade
                • **No Intrinsic Value**: Lacks tangible backing or utility beyond speculation
                
                **Confidence: 70%**
                
                *Note: This is a simplified analysis. For detailed guidance, consult qualified Islamic scholars.*
                """.trimIndent()
            }
            query.contains("halal", ignoreCase = true) ||
                    query.contains("haram", ignoreCase = true) -> {
                """
                **Islamic Finance Analysis (Fallback Mode)**
                
                I can help analyze cryptocurrencies and financial instruments from an Islamic perspective. However, the AI system is currently unavailable.
                
                **General Islamic Finance Principles:**
                • Avoid Riba (Interest/Usury)
                • Avoid Gharar (Excessive Uncertainty)
                • Avoid Maysir (Gambling/Speculation)
                • Ensure Halal underlying assets
                
                Please try again later when the full AI system is available for detailed analysis.
                """.trimIndent()
            }
            else -> {
                """
                **FiqhAdvisor (Fallback Mode)**
                
                I'm your Islamic finance AI assistant, but I'm currently experiencing technical difficulties.
                
                I can help you with:
                • Cryptocurrency Islamic compliance analysis
                • Islamic finance principles
                • Halal/Haram determinations for financial instruments
                
                Please try again in a few moments, or ask about specific cryptocurrencies like "Is Bitcoin halal?"
                """.trimIndent()
            }
        }
    }

    fun analyzeToken(symbol: String) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading

            try {
                Log.d("FiqhAIViewModel", "🔍 Analyzing token: $symbol")

                if (ensureInitialized()) {
                    Log.d(
                            "FiqhAIViewModel",
                            "✅ System initialized, calling fiqhManager.analyzeToken()"
                    )
                    val result = fiqhManager.analyzeToken(symbol.uppercase())
                    result.fold(
                            onSuccess = { response ->
                                Log.i("FiqhAIViewModel", "✅ Token analysis successful for $symbol")
                                Log.d(
                                        "FiqhAIViewModel",
                                        "Response: ${response.response.take(100)}..."
                                )
                                _analysisState.value = AnalysisState.Success(response)
                            },
                            onFailure = { error ->
                                Log.e(
                                        "FiqhAIViewModel",
                                        "❌ Token analysis failed: ${error.message}"
                                )
                                Log.e(
                                        "FiqhAIViewModel",
                                        "Error type: ${error::class.java.simpleName}"
                                )
                                _analysisState.value =
                                        AnalysisState.Error(
                                                when {
                                                    error.message?.contains("network") == true ->
                                                            "Network error. Please check your connection."
                                                    error.message?.contains("not found") == true ->
                                                            "Token '$symbol' not found. Please check the symbol."
                                                    error.message?.contains("rate limit") == true ->
                                                            "Too many requests. Please wait a moment."
                                                    else -> "Analysis failed: ${error.message}"
                                                }
                                        )
                            }
                    )
                } else {
                    Log.w(
                            "FiqhAIViewModel",
                            "⚠️ System not initialized, providing fallback response"
                    )
                    val fallbackResponse = provideFallbackResponse("$symbol token analysis")
                    _analysisState.value = AnalysisState.FallbackResponse(fallbackResponse)
                }
            } catch (e: Exception) {
                Log.e("FiqhAIViewModel", "💥 Exception during token analysis", e)
                val fallbackResponse = provideFallbackResponse("$symbol token analysis")
                _analysisState.value = AnalysisState.FallbackResponse(fallbackResponse)
            }
        }
    }

    fun analyzeGeneralQuery(text: String) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading

            try {
                Log.d("FiqhAIViewModel", "🔍 Analyzing general query: $text")

                if (ensureInitialized()) {
                    Log.d(
                            "FiqhAIViewModel",
                            "✅ System initialized, calling fiqhManager.analyzeText()"
                    )
                    val result = fiqhManager.analyzeText(text)
                    result.fold(
                            onSuccess = { response ->
                                Log.i("FiqhAIViewModel", "✅ General query analysis successful")
                                Log.d(
                                        "FiqhAIViewModel",
                                        "Response: ${response.response.take(100)}..."
                                )
                                _analysisState.value = AnalysisState.Success(response)
                            },
                            onFailure = { error ->
                                Log.e(
                                        "FiqhAIViewModel",
                                        "❌ General query analysis failed: ${error.message}"
                                )
                                Log.e(
                                        "FiqhAIViewModel",
                                        "Error type: ${error::class.java.simpleName}"
                                )
                                val fallbackResponse = provideFallbackResponse(text)
                                _analysisState.value =
                                        AnalysisState.FallbackResponse(fallbackResponse)
                            }
                    )
                } else {
                    Log.w(
                            "FiqhAIViewModel",
                            "⚠️ System not initialized, providing fallback response"
                    )
                    val fallbackResponse = provideFallbackResponse(text)
                    _analysisState.value = AnalysisState.FallbackResponse(fallbackResponse)
                }
            } catch (e: Exception) {
                Log.e("FiqhAIViewModel", "💥 Exception during general query analysis", e)
                val fallbackResponse = provideFallbackResponse(text)
                _analysisState.value = AnalysisState.FallbackResponse(fallbackResponse)
            }
        }
    }

    /** Send a chat message using the proper chat session functionality */
    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading

            try {
                Log.d("FiqhAIViewModel", "🔍 Sending chat message: $message")

                if (ensureInitialized()) {
                    // Start chat session if not already started
                    if (!chatSessionStarted) {
                        val sessionId = fiqhManager.startChatSession("user123", "id")
                        chatSessionStarted = true
                        Log.d("FiqhAIViewModel", "✅ Started chat session: $sessionId")
                    }

                    Log.d(
                            "FiqhAIViewModel",
                            "✅ System initialized, sending chat message via FiqhAIManager"
                    )
                    val result = fiqhManager.sendChatMessage(message)
                    result.fold(
                            onSuccess = { response ->
                                Log.i("FiqhAIViewModel", "✅ Chat message successful")
                                Log.d(
                                        "FiqhAIViewModel",
                                        "Response: ${response.response.take(100)}..."
                                )
                                _analysisState.value = AnalysisState.Success(response)
                            },
                            onFailure = { error ->
                                Log.e("FiqhAIViewModel", "❌ Chat message failed: ${error.message}")
                                Log.e(
                                        "FiqhAIViewModel",
                                        "Error type: ${error::class.java.simpleName}"
                                )
                                val fallbackResponse = provideFallbackResponse(message)
                                _analysisState.value =
                                        AnalysisState.FallbackResponse(fallbackResponse)
                            }
                    )
                } else {
                    Log.w(
                            "FiqhAIViewModel",
                            "⚠️ System not initialized, providing fallback response"
                    )
                    val fallbackResponse = provideFallbackResponse(message)
                    _analysisState.value = AnalysisState.FallbackResponse(fallbackResponse)
                }
            } catch (e: Exception) {
                Log.e("FiqhAIViewModel", "💥 Exception during chat message", e)
                val fallbackResponse = provideFallbackResponse(message)
                _analysisState.value = AnalysisState.FallbackResponse(fallbackResponse)
            }
        }
    }

    /** Test AI response generation for debugging */
    fun testAIResponse(query: String) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading

            try {
                Log.d("FiqhAIViewModel", "Testing AI response for: $query")

                // Use diagnostic tool to test AI response
                val response = AISystemDiagnostics.testAIResponse(query)

                // Create a mock QueryResponse for testing
                val mockResponse =
                        QueryResponse(
                                queryId = java.util.UUID.randomUUID().toString(),
                                response = response,
                                confidence = 0.85,
                                sources = listOf("AI System Diagnostic"),
                                followUpQuestions =
                                        listOf(
                                                "Would you like more details about the AI system configuration?",
                                                "Should I run additional diagnostic tests?"
                                        ),
                                timestamp = System.currentTimeMillis().toULong(),
                                analysisId = java.util.UUID.randomUUID().toString()
                        )

                _analysisState.value = AnalysisState.Success(mockResponse)
            } catch (e: Exception) {
                Log.e("FiqhAIViewModel", "AI response test failed: ${e.message}", e)
                val fallbackResponse = provideFallbackResponse(query)
                _analysisState.value = AnalysisState.FallbackResponse(fallbackResponse)
            }
        }
    }
}

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Loading : AnalysisState()
    data class Success(val result: QueryResponse) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
    data class FallbackResponse(val message: String) : AnalysisState()
}
