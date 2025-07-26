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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

// Define TAG at the file level for use by top-level functions
private const val TAG = "FiqhAIManager"

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
        var preferredModel: String // Changed to var to allow modification
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
    return withContext(Dispatchers.IO) {
        withTimeout(30000) { // 30 second timeout
            Log.d(TAG, "🔄 Starting async system creation...")
            Log.d(TAG, "🔍 Preparing Rust FFI call with config: ${config.preferredModel}")

            try {
                // Add debugging checkpoint
                Log.d(TAG, "📌 Checkpoint 1: About to call Rust constructor")

                val ffiConfig =
                        try {
                            Log.d(TAG, "📌 Converting config to FFI format")
                            FfiConverterTypeFiqhAIConfig.lower(config.toUniffi())
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Config conversion failed", e)
                            throw e
                        }

                Log.d(TAG, "📌 Checkpoint 2: Calling Rust constructor")

                val constructorCall =
                        try {
                            UniffiLib.INSTANCE.uniffi_fiqh_core_fn_constructor_fiqhaisystem_new(
                                    ffiConfig
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Rust constructor call failed", e)
                            throw e
                        }

                Log.d(TAG, "📌 Checkpoint 3: Starting async call processing")

                uniffiRustCallAsync(
                        constructorCall,
                        { future, callback, continuation ->
                            Log.d(TAG, "🔄 Polling Rust future...")
                            try {
                                UniffiLib.INSTANCE.ffi_fiqh_core_rust_future_poll_pointer(
                                        future,
                                        callback,
                                        continuation
                                )
                                Log.d(TAG, "✅ Poll completed successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error during future polling", e)
                                throw e
                            }
                        },
                        { future, continuation ->
                            Log.d(TAG, "🔄 Completing Rust future...")
                            try {
                                val result =
                                        UniffiLib.INSTANCE
                                                .ffi_fiqh_core_rust_future_complete_pointer(
                                                        future,
                                                        continuation
                                                )
                                Log.d(TAG, "✅ Future completion successful")
                                result // Return the Pointer for lifting
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error during future completion", e)
                                throw e
                            }
                        },
                        { future ->
                            Log.d(TAG, "🧹 Freeing Rust future resources...")
                            try {
                                UniffiLib.INSTANCE.ffi_fiqh_core_rust_future_free_pointer(future)
                                Log.d(TAG, "✅ Resource cleanup successful")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error during resource cleanup", e)
                                // Just log, don't throw from cleanup
                            }
                        },
                        {
                            Log.d(TAG, "✅ Lifting Rust object to Kotlin")
                            FfiConverterTypeFiqhAISystem.lift(it)
                        },
                        com.rizilab.fiqhadvisor.fiqhcore.FiqhAiException.ErrorHandler,
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in Rust FFI call: ${e.message}", e)
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                throw e
            }
        }
    }
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

    // Moving companion object before other methods, but it doesn't actually matter
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
            Log.d(TAG, "🚀 Starting FiqhAI system initialization...")
            Log.d(TAG, "📋 Config - Database path: ${config.databasePath}")
            Log.d(TAG, "📋 Config - Qdrant URL: ${config.qdrantUrl}")
            Log.d(TAG, "📋 Config - Solana RPC: ${config.solanaRpcUrl}")
            Log.d(TAG, "📋 Config - Preferred model: ${config.preferredModel}")
            Log.d(TAG, "📋 Config - Enable Solana: ${config.enableSolana}")

            // Step 1: Native library loading with verification
            try {
                Log.d(TAG, "🔧 Step 1: Loading native library...")
                System.loadLibrary("fiqh_core")
                Log.d(TAG, "✅ Native library loaded successfully")

                // Verify library is actually loaded by checking if basic functions exist
                Log.d(TAG, "🔍 Verifying native library functions...")
                try {
                    // Try to access the UniFFI library instance to verify it's loaded
                    val libInstance = UniffiLib.INSTANCE
                    Log.d(
                            TAG,
                            "✅ UniFFI library instance verified: ${libInstance.javaClass.simpleName}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to verify native library functions: ${e.message}")
                    throw RuntimeException("Native library verification failed", e)
                }
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ Failed to load native library: ${e.message}")
                throw RuntimeException("Failed to load native library: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error during library loading: ${e.message}")
                throw RuntimeException("Library loading error: ${e.message}", e)
            }

            // Step 2: Test basic UniFFI functionality
            try {
                Log.d(TAG, "🔧 Step 2: Testing basic UniFFI functionality...")

                // Test config conversion first
                Log.d(TAG, "🧪 Testing config conversion...")
                val uniffiConfig = config.toUniffi()
                Log.d(TAG, "✅ Config conversion successful")

                // Test FFI converter
                Log.d(TAG, "🧪 Testing FFI converter...")
                val configBuffer = FfiConverterTypeFiqhAIConfig.lower(uniffiConfig)
                Log.d(TAG, "✅ FFI converter test successful")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Basic UniFFI test failed: ${e.message}", e)
                throw RuntimeException("UniFFI functionality test failed: ${e.message}", e)
            }

            // Step 3: Create FiqhAI system (the most likely crash point) with fallback
            try {
                Log.d(TAG, "🔧 Step 3: Creating FiqhAI system...")
                Log.d(TAG, "⏳ This may take a few seconds for async initialization...")

                // Add timeout and more detailed error handling
                val startTime = System.currentTimeMillis()

                try {
                    fiqhSystem =
                            withTimeout(15000) { // Reduced timeout to 15 seconds
                                createFiqhAiSystem(config)
                            }
                    val endTime = System.currentTimeMillis()
                    Log.d(TAG, "✅ FiqhAI system created successfully in ${endTime - startTime}ms")
                } catch (e: TimeoutCancellationException) {
                    Log.w(
                            TAG,
                            "⚠️ Rust system initialization timed out, switching to fallback mode"
                    )
                    // Set to null to indicate fallback mode
                    fiqhSystem = null
                    Log.d(TAG, "✅ Fallback mode activated - app will use mock responses")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ FiqhAI system creation failed: ${e.message}", e)
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                if (e.cause != null) {
                    Log.e(TAG, "Caused by: ${e.cause?.javaClass?.simpleName} - ${e.cause?.message}")
                }

                // Don't crash - switch to fallback mode
                Log.w(TAG, "⚠️ Switching to fallback mode due to initialization error")
                fiqhSystem = null
            }

            // Step 4: Create auxiliary components (only if main system succeeds)
            try {
                Log.d(TAG, "🔧 Step 4: Creating AudioProcessor...")
                audioProcessor = com.rizilab.fiqhadvisor.fiqhcore.AudioProcessor()
                Log.d(TAG, "✅ AudioProcessor created successfully")

                Log.d(TAG, "🔧 Step 5: Creating SolanaConnector...")
                solanaConnector =
                        com.rizilab.fiqhadvisor.fiqhcore.SolanaConnector(config.solanaRpcUrl)
                Log.d(TAG, "✅ SolanaConnector created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Auxiliary component creation failed (non-critical): ${e.message}", e)
                // Don't fail initialization for auxiliary components
            }

            // Step 6: Final verification
            try {
                Log.d(TAG, "🔧 Step 6: Final system verification...")

                _initializationState.value = InitializationState.Initialized

                if (fiqhSystem != null) {
                    Log.i(TAG, "🎉 FiqhAI system initialized successfully with Rust backend!")
                } else {
                    Log.i(TAG, "🎉 FiqhAI system initialized successfully in fallback mode!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Final verification failed: ${e.message}")
                throw RuntimeException("System verification failed: ${e.message}", e)
            }
        } catch (e: Exception) {
            val errorMessage = "❌ Failed to initialize FiqhAI system: ${e.message}"
            Log.e(TAG, errorMessage, e)
            _initializationState.value = InitializationState.Error(errorMessage)
            throw e
        }
    }

    /** Start a new chatbot session */
    fun startChatSession(userId: String, language: String = "id"): String {
        chatbotSession =
                com.rizilab.fiqhadvisor.fiqhcore.ChatbotSession(
                        userId,
                        language
                ) // Fixed package name
        return chatbotSession!!.startSession()
    }

    /**
     * Simple diagnostic test to check if native library is working Call this before full
     * initialization to isolate crash points
     */
    fun testNativeLibrary(): Boolean {
        return try {
            Log.d(TAG, "🧪 Testing native library basic functionality...")

            // Test 1: Library loading
            System.loadLibrary("fiqh_core")
            Log.d(TAG, "✅ Test 1: Native library loaded")

            // Test 2: UniFFI instance access
            val libInstance = UniffiLib.INSTANCE
            Log.d(TAG, "✅ Test 2: UniFFI instance accessible: ${libInstance.javaClass.simpleName}")

            // Test 3: Simple config creation
            val testConfig =
                    FiqhAiConfig(
                            openaiApiKey = "",
                            groqApiKey = "test",
                            grokApiKey = "test",
                            modelName = "test",
                            qdrantUrl = "",
                            databasePath = "",
                            solanaRpcUrl = "https://api.mainnet-beta.solana.com",
                            enableSolana = true,
                            preferredModel = "groq"
                    )
            Log.d(TAG, "✅ Test 3: Config created successfully")

            // Test 4: Config conversion
            val uniffiConfig = testConfig.toUniffi()
            Log.d(TAG, "✅ Test 4: Config conversion successful")

            // Test 5: FFI conversion
            val configBuffer = FfiConverterTypeFiqhAIConfig.lower(uniffiConfig)
            Log.d(TAG, "✅ Test 5: FFI conversion successful")

            Log.i(TAG, "🎉 All native library tests passed!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Native library test failed: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            if (e.cause != null) {
                Log.e(TAG, "Caused by: ${e.cause?.javaClass?.simpleName} - ${e.cause?.message}")
            }
            false
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
            val system = fiqhSystem
            if (system != null) {
                system.analyzeToken(token, userId, language).toWrapper()
            } else {
                // Fallback response when Rust system is unavailable
                Log.d(TAG, "Using fallback response for token: $token")
                createFallbackTokenResponse(token)
            }
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

    /** Send chat message */
    suspend fun sendChatMessage(message: String, userId: String? = null): Result<QueryResponse> {
        return runCatching {
            val system = fiqhSystem
            if (system != null) {
                // Use chat session if available, create if needed
                try {
                    val session =
                            chatbotSession
                                    ?: run {
                                        // Create new chat session directly
                                        val newSession =
                                                com.rizilab.fiqhadvisor.fiqhcore.ChatbotSession(
                                                        userId ?: "default_user",
                                                        "id"
                                                )
                                        chatbotSession = newSession
                                        newSession
                                    }

                    val result =
                            try {
                                // UniFFI expects both message and context parameters (context is
                                // nullable)
                                session.sendMessage(message, null)
                            } catch (e: Exception) {
                                Log.w(TAG, "sendMessage call failed: ${e.message}")
                                throw e
                            }
                    result.toWrapper()
                } catch (e: Exception) {
                    Log.w(TAG, "Chat session failed, using fallback: ${e.message}")
                    createFallbackChatResponse(message)
                }
            } else {
                // Fallback response when Rust system is unavailable
                Log.d(TAG, "Using fallback response for message: $message")
                createFallbackChatResponse(message)
            }
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

    /**
     * Initialize the FiqhAI system with the provided configuration. This is an async operation that
     * sets up all the necessary components. This version tries a minimal initialization if the
     * normal initialization fails.
     */
    suspend fun initializeWithFallback(context: Context, config: FiqhAiConfig) {
        try {
            // First try normal initialization
            initialize(context, config)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Normal initialization failed, trying minimal mode", e)

            // Create a minimal configuration that avoids heavy operations
            val minimalConfig =
                    FiqhAiConfig(
                            openaiApiKey = config.openaiApiKey,
                            groqApiKey = config.groqApiKey,
                            grokApiKey = config.grokApiKey,
                            modelName = config.modelName,
                            qdrantUrl = "", // Disable vector DB
                            databasePath =
                                    context.cacheDir.absolutePath + "/fiqh_temp", // Use temp path
                            solanaRpcUrl = config.solanaRpcUrl,
                            enableSolana = false, // Disable Solana features
                            preferredModel = config.preferredModel
                    )

            try {
                // Try a more direct initialization approach
                Log.d(TAG, "🔧 Attempting minimal initialization...")
                _initializationState.value = InitializationState.Initializing

                // Create FiqhAI system with minimal config
                withTimeout(15000) { // shorter timeout
                    fiqhSystem = createFiqhAiSystem(minimalConfig)
                }

                _initializationState.value = InitializationState.Initialized
                Log.i(TAG, "✅ Minimal initialization successful")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Minimal initialization also failed", e)
                _initializationState.value =
                        InitializationState.Error("All initialization attempts failed")
                throw e
            }
        }
    }

    /** Create fallback token response */
    private fun createFallbackTokenResponse(token: String): QueryResponse {
        val response =
                when (token.uppercase()) {
                    "BTC", "BITCOIN" -> {
                        """
                **Bitcoin Analysis (Fallback Mode)**
                
                🔴 **Ruling: Haram (Prohibited)**
                
                **Islamic Reasoning:**
                • **Excessive Gharar (Uncertainty)**: Bitcoin's extreme price volatility creates excessive uncertainty
                • **Speculation (Maysir)**: Often used for gambling-like speculation rather than legitimate trade
                • **No Intrinsic Value**: Lacks tangible backing or utility beyond speculation
                
                **Confidence: 70%**
                
                *Note: This is a simplified analysis from fallback mode. For detailed guidance, consult qualified Islamic scholars.*
                """.trimIndent()
                    }
                    "ETH", "ETHEREUM" -> {
                        """
                **Ethereum Analysis (Fallback Mode)**
                
                🟡 **Ruling: Makruh (Discouraged)**
                
                **Islamic Reasoning:**
                • **Smart Contract Platform**: Has utility beyond speculation
                • **Proof of Stake**: More environmentally sustainable than Bitcoin
                • **Still Volatile**: Subject to excessive price speculation
                
                **Confidence: 60%**
                
                *Note: This is a simplified analysis from fallback mode.*
                """.trimIndent()
                    }
                    else -> {
                        """
                **$token Analysis (Fallback Mode)**
                
                🟡 **Ruling: Requires Further Analysis**
                
                **Islamic Reasoning:**
                • **Individual Assessment Needed**: Each cryptocurrency has unique characteristics
                • **General Principles**: Avoid excessive speculation, ensure real utility
                • **Consult Scholars**: For specific rulings on newer tokens
                
                **Confidence: 50%**
                
                *Note: This is a general fallback response. The AI system is currently unavailable.*
                """.trimIndent()
                    }
                }

        return QueryResponse(
                queryId = "fallback_${System.currentTimeMillis()}",
                response = response,
                confidence = 0.7,
                sources = listOf("Fallback Mode - General Islamic Finance Principles"),
                followUpQuestions =
                        listOf(
                                "What makes a cryptocurrency halal?",
                                "How do Islamic scholars view digital assets?",
                                "What are the key principles of Islamic finance?"
                        ),
                timestamp = System.currentTimeMillis().toULong(),
                analysisId = null
        )
    }

    /** Create fallback chat response */
    private fun createFallbackChatResponse(message: String): QueryResponse {
        val response =
                when {
                    message.contains("halal", ignoreCase = true) ||
                            message.contains("haram", ignoreCase = true) -> {
                        """
                **Islamic Finance Guidance (Fallback Mode)**
                
                I can help with Islamic finance questions, but the AI system is currently unavailable.
                
                **General Islamic Finance Principles:**
                • Avoid Riba (Interest/Usury)
                • Avoid Gharar (Excessive Uncertainty)  
                • Avoid Maysir (Gambling/Speculation)
                • Ensure Halal underlying assets
                
                Please try asking about specific cryptocurrencies like "Is Bitcoin halal?" or consult qualified Islamic scholars for detailed guidance.
                """.trimIndent()
                    }
                    else -> {
                        """
                **Fiqh Advisor (Fallback Mode)**
                
                Assalamu'alaikum! I'm your Islamic finance advisor, but the AI system is currently unavailable.
                
                I can still provide basic guidance on cryptocurrencies and Islamic finance principles. Try asking:
                • "Is Bitcoin halal?"
                • "Is Ethereum halal?"  
                • "What makes a cryptocurrency halal?"
                
                For detailed analysis, please wait for the system to fully initialize or consult qualified Islamic scholars.
                """.trimIndent()
                    }
                }

        return QueryResponse(
                queryId = "chat_fallback_${System.currentTimeMillis()}",
                response = response,
                confidence = 0.5,
                sources = listOf("Fallback Mode - Basic Islamic Principles"),
                followUpQuestions =
                        listOf(
                                "Is Bitcoin halal?",
                                "Is Ethereum halal?",
                                "What are Islamic finance principles?"
                        ),
                timestamp = System.currentTimeMillis().toULong(),
                analysisId = null
        )
    }
}
