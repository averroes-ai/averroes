package com.rizilab.fiqhadvisor.core

// UniFFI imports - only used internally for conversion
import android.content.Context
import android.util.Log
import com.rizilab.fiqhadvisor.fiqhcore.FfiConverterTypeFiqhAIConfig
import com.rizilab.fiqhadvisor.fiqhcore.FfiConverterTypeFiqhAISystem
import com.rizilab.fiqhadvisor.fiqhcore.UniffiLib
import com.rizilab.fiqhadvisor.fiqhcore.uniffiRustCallAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ============================================================================
// WRAPPER TYPES - Clean Architecture Layer
// These types wrap the UniFFI-generated types and provide a clean API
// ============================================================================

/** Configuration for the FiqhAI system - Kotlin wrapper for UniFFI FiqhAiConfig */
data class FiqhAiConfig(
        val openaiApiKey: String,
        val groqApiKey: String,
        val grokApiKey: String,
        val modelName: String,
        val qdrantUrl: String,
        val databasePath: String,
        val solanaRpcUrl: String,
        val enableSolana: Boolean,
        val preferredModel: String
)

/** Response from AI query - Kotlin wrapper for UniFFI QueryResponse */
data class QueryResponse(
        val queryId: String,
        val response: String,
        val confidence: Double,
        val sources: List<String>,
        val followUpQuestions: List<String>,
        val timestamp: ULong,
        val analysisId: String? // Make nullable to match UniFFI
)

/** Chat message - Kotlin wrapper for UniFFI ChatMessage */
data class ChatMessage(
        val id: String,
        val content: String,
        val isUser: Boolean,
        val timestamp: ULong
)

/** User analysis statistics - Kotlin wrapper for UniFFI UserAnalysisStats */
data class UserAnalysisStats(
        val totalQueries: UInt,
        val halalCount: UInt,
        val haramCount: UInt,
        val averageConfidence: Double
)

/** Analysis history - Kotlin wrapper for UniFFI AnalysisHistory */
data class AnalysisHistory(val entries: List<QueryResponse>)

/** Solana token information - Kotlin wrapper for UniFFI SolanaTokenInfo */
data class SolanaTokenInfo(
        val symbol: String,
        val name: String,
        val mintAddress: String,
        val decimals: UInt,
        val supply: ULong
)

/** Backtest result - Kotlin wrapper for UniFFI BacktestResult */
data class BacktestResult(
        val analysisId: String,
        val accuracy: Double,
        val totalTests: UInt,
        val correctPredictions: UInt
)

/** Islamic principles enum - Kotlin wrapper for UniFFI IslamicPrinciple */
enum class IslamicPrinciple {
    HALAL,
    HARAM,
    MAKRUH,
    MUBAH,
    MUSTAHAB
}

// ============================================================================
// INTERNAL CONVERSION FUNCTIONS
// Convert between wrapper types and UniFFI types
// ============================================================================

/** Convert wrapper FiqhAiConfig to UniFFI FiqhAiConfig */
private fun FiqhAiConfig.toUniffi(): com.rizilab.fiqhadvisor.fiqhcore.FiqhAiConfig {
    return com.rizilab.fiqhadvisor.fiqhcore.FiqhAiConfig(
            openaiApiKey = this.openaiApiKey,
            groqApiKey = this.groqApiKey,
            grokApiKey = this.grokApiKey,
            modelName = this.modelName,
            qdrantUrl = this.qdrantUrl,
            databasePath = this.databasePath,
            solanaRpcUrl = this.solanaRpcUrl,
            enableSolana = this.enableSolana,
            preferredModel = this.preferredModel
    )
}

/** Convert UniFFI QueryResponse to wrapper QueryResponse */
private fun com.rizilab.fiqhadvisor.fiqhcore.QueryResponse.toWrapper(): QueryResponse {
    return QueryResponse(
            queryId = this.queryId,
            response = this.response,
            confidence = this.confidence,
            sources = this.sources ?: emptyList(),
            followUpQuestions = this.followUpQuestions ?: emptyList(),
            timestamp = this.timestamp,
            analysisId = this.analysisId
    )
}

/** Convert UniFFI ChatMessage to wrapper ChatMessage */
private fun com.rizilab.fiqhadvisor.fiqhcore.ChatMessage.toWrapper(): ChatMessage {
    return ChatMessage(
            id = this.id,
            content = this.content,
            isUser = this.isUserMessage,
            timestamp = this.timestamp
    )
}

/** Convert UniFFI HistoryEntry to wrapper QueryResponse */
private fun com.rizilab.fiqhadvisor.fiqhcore.HistoryEntry.toQueryResponse(): QueryResponse {
    return QueryResponse(
            queryId = this.analysisId,
            response = this.summary,
            confidence = this.confidence,
            sources = emptyList(), // HistoryEntry doesn't have sources
            followUpQuestions = emptyList(), // HistoryEntry doesn't have follow-up questions
            timestamp = this.analyzedAt,
            analysisId = this.analysisId
    )
}

/** Convert UniFFI AnalysisHistory to wrapper AnalysisHistory */
private fun com.rizilab.fiqhadvisor.fiqhcore.AnalysisHistory.toWrapper(): AnalysisHistory {
    return AnalysisHistory(entries = this.entries.map { it.toQueryResponse() })
}

/** Convert UniFFI UserAnalysisStats to wrapper UserAnalysisStats */
private fun com.rizilab.fiqhadvisor.fiqhcore.UserAnalysisStats.toWrapper(): UserAnalysisStats {
    return UserAnalysisStats(
            totalQueries = this.totalAnalyses,
            halalCount = this.halalCount,
            haramCount = this.haramCount,
            averageConfidence = this.averageConfidence
    )
}

/** Convert UniFFI SolanaTokenInfo to wrapper SolanaTokenInfo */
private fun com.rizilab.fiqhadvisor.fiqhcore.SolanaTokenInfo.toWrapper(): SolanaTokenInfo {
    return SolanaTokenInfo(
            symbol = this.metadata.symbol,
            name = this.metadata.name,
            mintAddress = this.metadata.contractAddress,
            decimals = this.metadata.decimals,
            supply = this.holders ?: 0UL
    )
}

/** Convert UniFFI BacktestResult to wrapper BacktestResult */
private fun com.rizilab.fiqhadvisor.fiqhcore.BacktestResult.toWrapper(): BacktestResult {
    return BacktestResult(
            analysisId = this.analysisId,
            accuracy = if (this.rulingChanged) 0.5 else 1.0, // Mock accuracy based on ruling change
            totalTests = 1U, // Mock values since actual structure is different
            correctPredictions = if (this.rulingChanged) 0U else 1U
    )
}

/** Create FiqhAiSystem using the proper async constructor pattern */
private suspend fun createFiqhAiSystem(
        config: FiqhAiConfig
): com.rizilab.fiqhadvisor.fiqhcore.FiqhAiSystem {
    return uniffiRustCallAsync(
            UniffiLib.INSTANCE.uniffi_fiqh_core_fn_constructor_fiqhaisystem_new(
                    FfiConverterTypeFiqhAIConfig.lower(config.toUniffi())
            ),
            { future, callback, continuation ->
                UniffiLib.INSTANCE.ffi_fiqh_core_rust_future_poll_pointer(
                        future,
                        callback,
                        continuation
                )
            },
            { future, continuation ->
                UniffiLib.INSTANCE.ffi_fiqh_core_rust_future_complete_pointer(future, continuation)
            },
            { future -> UniffiLib.INSTANCE.ffi_fiqh_core_rust_future_free_pointer(future) },
            { FfiConverterTypeFiqhAISystem.lift(it) },
            com.rizilab.fiqhadvisor.fiqhcore.FiqhAiException.ErrorHandler,
    )
}

/**
 * Android manager for FiqhAI system that wraps the Rust `UniFFI` interface. Provides a
 * Kotlin-friendly API for the Android application.
 */
class FiqhAIManager {
    // Internal UniFFI types - not exposed to public API
    private var fiqhSystem: com.rizilab.fiqhadvisor.fiqhcore.FiqhAiSystem? = null
    private var audioProcessor: com.rizilab.fiqhadvisor.fiqhcore.AudioProcessor? = null
    private var solanaConnector: com.rizilab.fiqhadvisor.fiqhcore.SolanaConnector? = null
    private var chatbotSession: com.rizilab.fiqhadvisor.fiqhcore.ChatbotSession? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _initializationState =
            MutableStateFlow<InitializationState>(InitializationState.NotInitialized)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()

    sealed class InitializationState {
        object NotInitialized : InitializationState()
        object Initializing : InitializationState()
        object Initialized : InitializationState()
        data class Error(val message: String) : InitializationState()
    }

    companion object {
        private const val TAG = "FiqhAIManager"
    }

    /**
     * Initialize the FiqhAI system with the provided configuration. This is an async operation that
     * sets up all the necessary components.
     */
    suspend fun initialize(context: Context, config: FiqhAiConfig) {
        if (_initializationState.value is InitializationState.Initialized) {
            Log.w(TAG, "System already initialized")
            return
        }

        try {
            _initializationState.value = InitializationState.Initializing
            Log.d(TAG, "Starting FiqhAI system initialization...")
            Log.d(TAG, "Config - Database path: ${config.databasePath}")
            Log.d(TAG, "Config - Qdrant URL: ${config.qdrantUrl}")
            Log.d(TAG, "Config - Solana RPC: ${config.solanaRpcUrl}")
            Log.d(TAG, "Config - Preferred model: ${config.preferredModel}")

            try {
                Log.d(TAG, "Loading native library...")
                System.loadLibrary("fiqh_core")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                throw RuntimeException("Failed to load native library: ${e.message}", e)
            }

            try {
                Log.d(TAG, "Creating FiqhAI system with configuration...")
                // Initialize core components using the async factory function
                fiqhSystem = createFiqhAiSystem(config)
                Log.d(TAG, "FiqhAI system created successfully")

                Log.d(TAG, "Creating AudioProcessor...")
                audioProcessor = com.rizilab.fiqhadvisor.fiqhcore.AudioProcessor()
                Log.d(TAG, "AudioProcessor created successfully")

                Log.d(TAG, "Creating SolanaConnector...")
                solanaConnector =
                        com.rizilab.fiqhadvisor.fiqhcore.SolanaConnector(config.solanaRpcUrl)
                Log.d(TAG, "SolanaConnector created successfully")

                // ChatbotSession will be created when user starts a chat session

                _initializationState.value = InitializationState.Initialized
                Log.i(TAG, "✅ FiqhAI system initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during native system initialization: ${e.message}", e)
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                if (e.cause != null) {
                    Log.e(TAG, "Caused by: ${e.cause?.message}")
                }
                throw RuntimeException("Native system initialization failed: ${e.message}", e)
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to initialize FiqhAI system: ${e.message}"
            Log.e(TAG, errorMessage, e)
            _initializationState.value = InitializationState.Error(errorMessage)
            throw e
        }
    }

    // ============================================================================
    // ANALYSIS METHODS
    // ============================================================================

    /** Analyze text input */
    suspend fun analyzeText(
            text: String,
            userId: String? = null,
            language: String = "id"
    ): Result<QueryResponse> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.analyzeText(text, userId, language).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Text analysis failed", error) }
    }

    /** Analyze token by ticker symbol */
    suspend fun analyzeToken(
            token: String,
            userId: String? = null,
            language: String = "id"
    ): Result<QueryResponse> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.analyzeToken(token, userId, language).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Token analysis failed", error) }
    }

    /** Analyze contract address */
    suspend fun analyzeContract(
            contractAddress: String,
            userId: String? = null,
            language: String = "id"
    ): Result<QueryResponse> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.analyzeContract(contractAddress, userId, language).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Contract analysis failed", error) }
    }

    /** Analyze audio input with speech-to-text */
    suspend fun analyzeAudio(
            audioData: ByteArray,
            userId: String? = null,
            language: String = "id"
    ): Result<QueryResponse> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.analyzeAudio(audioData, userId, language).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Audio analysis failed", error) }
    }

    // ============================================================================
    // AUDIO PROCESSING
    // ============================================================================

    /** Transcribe audio to text */
    suspend fun transcribeAudio(audioData: ByteArray, language: String = "id"): Result<String> {
        return runCatching {
            val processor =
                    audioProcessor ?: throw IllegalStateException("Audio processor not initialized")
            processor.transcribeAudio(audioData, language)
        }
                .onFailure { error -> Log.e(TAG, "Audio transcription failed", error) }
    }

    /** Check if audio format is supported */
    fun isAudioFormatSupported(audioData: ByteArray): Boolean {
        return audioProcessor?.isSupportedFormat(audioData) ?: false
    }

    /** Get supported audio formats */
    fun getSupportedAudioFormats(): List<String> {
        return audioProcessor?.getSupportedFormats() ?: emptyList()
    }

    // ============================================================================
    // HISTORY AND STATISTICS
    // ============================================================================

    /** Get user analysis history */
    suspend fun getUserHistory(userId: String, limit: Int = 20): Result<AnalysisHistory> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.getUserHistory(userId, limit.toUInt()).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Failed to get user history", error) }
    }

    /** Get token analysis history */
    suspend fun getTokenHistory(token: String, limit: Int = 10): Result<AnalysisHistory> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.getTokenHistory(token, limit.toUInt()).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Failed to get token history", error) }
    }

    /** Get user statistics */
    suspend fun getUserStats(userId: String): Result<UserAnalysisStats> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.getUserStats(userId).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Failed to get user stats", error) }
    }

    // ============================================================================
    // CHATBOT FUNCTIONALITY
    // ============================================================================

    /** Start a new chatbot session */
    fun startChatSession(userId: String, language: String = "id"): String {
        chatbotSession =
                com.rizilab.fiqhadvisor.fiqhcore.ChatbotSession(
                        userId,
                        language
                ) // Regular constructor
        return chatbotSession!!.startSession()
    }

    /** Send message to chatbot */
    suspend fun sendChatMessage(message: String, context: String? = null): Result<QueryResponse> {
        return runCatching {
            val session = chatbotSession ?: throw IllegalStateException("No active chat session")
            session.sendMessage(message, context).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Chat message failed", error) }
    }

    /** Get chat conversation history */
    suspend fun getChatHistory(): Result<List<ChatMessage>> {
        return runCatching {
            val session = chatbotSession ?: throw IllegalStateException("No active chat session")
            session.getConversationHistory().map { it.toWrapper() }
        }
                .onFailure { error -> Log.e(TAG, "Failed to get chat history", error) }
    }

    /** Clear current chat session */
    suspend fun clearChatSession() {
        chatbotSession?.clearSession()
        chatbotSession = null
    }

    // ============================================================================
    // SOLANA INTEGRATION
    // ============================================================================

    /** Get Solana token information */
    suspend fun getTokenInfo(mintAddress: String): Result<SolanaTokenInfo> {
        return runCatching {
            val connector =
                    solanaConnector
                            ?: throw IllegalStateException("Solana connector not initialized")
            connector.getTokenInfo(mintAddress).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Failed to get token info", error) }
    }

    /** Get wallet tokens */
    suspend fun getWalletTokens(walletAddress: String): Result<List<String>> {
        return runCatching {
            val connector =
                    solanaConnector
                            ?: throw IllegalStateException("Solana connector not initialized")
            connector.getWalletTokens(walletAddress)
        }
                .onFailure { error -> Log.e(TAG, "Failed to get wallet tokens", error) }
    }

    /** Check Solana connection status */
    suspend fun isSolanaConnected(): Result<Boolean> {
        return runCatching {
            val connector =
                    solanaConnector
                            ?: throw IllegalStateException("Solana connector not initialized")
            connector.isConnected()
        }
                .onFailure { error -> Log.e(TAG, "Failed to check Solana connection", error) }
    }

    /** Get network name */
    fun getNetworkName(): String {
        return solanaConnector?.getNetworkName() ?: "Unknown"
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /** Run backtest for analysis */
    suspend fun runBacktest(analysisId: String): Result<BacktestResult> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.runBacktest(analysisId).toWrapper()
        }
                .onFailure { error -> Log.e(TAG, "Backtest failed", error) }
    }

    /** Get current system configuration */
    fun getConfig(): FiqhAiConfig? {
        val uniffiConfig = fiqhSystem?.getConfig() ?: return null
        return FiqhAiConfig(
                openaiApiKey = uniffiConfig.openaiApiKey,
                groqApiKey = uniffiConfig.groqApiKey,
                grokApiKey = uniffiConfig.grokApiKey,
                modelName = uniffiConfig.modelName,
                qdrantUrl = uniffiConfig.qdrantUrl,
                databasePath = uniffiConfig.databasePath,
                solanaRpcUrl = uniffiConfig.solanaRpcUrl,
                enableSolana = uniffiConfig.enableSolana,
                preferredModel = uniffiConfig.preferredModel
        )
    }

    /** Check if system is ready for operations */
    fun isReady(): Boolean {
        return _initializationState.value is InitializationState.Initialized && fiqhSystem != null
    }

    /** Get ruling display text */
    fun getRulingDisplayText(ruling: IslamicPrinciple): String {
        return when (ruling) {
            IslamicPrinciple.HALAL -> "Halal (Permitted)"
            IslamicPrinciple.HARAM -> "Haram (Prohibited)"
            IslamicPrinciple.MAKRUH -> "Makruh (Discouraged)"
            IslamicPrinciple.MUBAH -> "Mubah (Neutral)"
            IslamicPrinciple.MUSTAHAB -> "Mustahab (Recommended)"
        }
    }

    /** Get ruling emoji */
    fun getRulingEmoji(ruling: IslamicPrinciple): String {
        return when (ruling) {
            IslamicPrinciple.HALAL -> "✅"
            IslamicPrinciple.HARAM -> "❌"
            IslamicPrinciple.MAKRUH -> "⚠️"
            IslamicPrinciple.MUBAH -> "⚪"
            IslamicPrinciple.MUSTAHAB -> "⭐"
        }
    }

    /** Check if ruling is permissible */
    fun isRulingPermissible(ruling: IslamicPrinciple): Boolean {
        return ruling == IslamicPrinciple.HALAL ||
                ruling == IslamicPrinciple.MUSTAHAB ||
                ruling == IslamicPrinciple.MUBAH
    }

    // ============================================================================
    // LIFECYCLE MANAGEMENT
    // ============================================================================

    /** Clean up resources */
    fun cleanup() {
        coroutineScope.cancel()
        chatbotSession = null
        fiqhSystem = null
        audioProcessor = null
        solanaConnector = null
        _initializationState.value = InitializationState.NotInitialized
        Log.d(TAG, "FiqhAIManager cleaned up")
    }

    /** Restart the system (useful for configuration changes) */
    fun restart() {
        cleanup()
        // The initialize function is now async, so we need to call it from the UI thread
        // or handle the async nature appropriately. For now, we'll just call it directly.
        // In a real app, you'd call initialize(context, getConfig() ?: createDefaultConfig())
        // from a UI thread.
        // For this example, we'll just log that it's not directly possible here
        // without a context or a way to pass the config.
        Log.w(
                TAG,
                "Restarting FiqhAIManager requires calling initialize(context, config) from a UI thread."
        )
    }
}
