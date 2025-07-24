package com.rizilab.fiqhadvisor.core

import android.content.Context
import android.util.Log
import com.rizilab.fiqhadvisor.fiqhcore.AnalysisHistory
import com.rizilab.fiqhadvisor.fiqhcore.AudioProcessor
import com.rizilab.fiqhadvisor.fiqhcore.BacktestResult
import com.rizilab.fiqhadvisor.fiqhcore.ChatMessage
import com.rizilab.fiqhadvisor.fiqhcore.ChatbotSession
import com.rizilab.fiqhadvisor.fiqhcore.FfiConverterTypeFiqhAIConfig
import com.rizilab.fiqhadvisor.fiqhcore.FfiConverterTypeFiqhAISystem
import com.rizilab.fiqhadvisor.fiqhcore.FiqhAiConfig
import com.rizilab.fiqhadvisor.fiqhcore.FiqhAiSystem
import com.rizilab.fiqhadvisor.fiqhcore.IslamicPrinciple
import com.rizilab.fiqhadvisor.fiqhcore.QueryResponse
import com.rizilab.fiqhadvisor.fiqhcore.SolanaConnector
import com.rizilab.fiqhadvisor.fiqhcore.SolanaTokenInfo
import com.rizilab.fiqhadvisor.fiqhcore.UniffiLib
import com.rizilab.fiqhadvisor.fiqhcore.UserAnalysisStats
import com.rizilab.fiqhadvisor.fiqhcore.uniffiRustCallAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Create FiqhAiSystem using the proper async constructor pattern
suspend fun createFiqhAiSystem(config: FiqhAiConfig): FiqhAiSystem {
    return uniffiRustCallAsync(
            UniffiLib.INSTANCE.uniffi_fiqh_core_fn_constructor_fiqhaisystem_new(
                    FfiConverterTypeFiqhAIConfig.lower(config)
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
    private var fiqhSystem: FiqhAiSystem? = null
    private var audioProcessor: AudioProcessor? = null
    private var solanaConnector: SolanaConnector? = null
    private var chatbotSession: ChatbotSession? = null

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

            try {
                System.loadLibrary("fiqh_core")
                Log.d(TAG, "Native library loaded successfully")

                // Initialize core components using the async factory function
                fiqhSystem = createFiqhAiSystem(config)
                audioProcessor = AudioProcessor() // Regular constructor
                solanaConnector = SolanaConnector(config.solanaRpcUrl) // Regular constructor
                // ChatbotSession will be created when user starts a chat session

                _initializationState.value = InitializationState.Initialized
                Log.i(TAG, "FiqhAI system initialized successfully")
            } catch (e: UnsatisfiedLinkError) {
                throw RuntimeException("Failed to load native library: ${e.message}", e)
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
            system.analyzeText(text, userId, language)
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
            system.analyzeToken(token, userId, language)
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
            system.analyzeContract(contractAddress, userId, language)
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
            system.analyzeAudio(audioData, userId, language)
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
            system.getUserHistory(userId, limit.toUInt())
        }
                .onFailure { error -> Log.e(TAG, "Failed to get user history", error) }
    }

    /** Get token analysis history */
    suspend fun getTokenHistory(token: String, limit: Int = 10): Result<AnalysisHistory> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.getTokenHistory(token, limit.toUInt())
        }
                .onFailure { error -> Log.e(TAG, "Failed to get token history", error) }
    }

    /** Get user statistics */
    suspend fun getUserStats(userId: String): Result<UserAnalysisStats> {
        return runCatching {
            val system = fiqhSystem ?: throw IllegalStateException("System not initialized")
            system.getUserStats(userId)
        }
                .onFailure { error -> Log.e(TAG, "Failed to get user stats", error) }
    }

    // ============================================================================
    // CHATBOT FUNCTIONALITY
    // ============================================================================

    /** Start a new chatbot session */
    fun startChatSession(userId: String, language: String = "id"): String {
        chatbotSession = ChatbotSession(userId, language) // Regular constructor
        return chatbotSession!!.startSession()
    }

    /** Send message to chatbot */
    suspend fun sendChatMessage(message: String, context: String? = null): Result<QueryResponse> {
        return runCatching {
            val session = chatbotSession ?: throw IllegalStateException("No active chat session")
            session.sendMessage(message, context)
        }
                .onFailure { error -> Log.e(TAG, "Chat message failed", error) }
    }

    /** Get chat conversation history */
    suspend fun getChatHistory(): Result<List<ChatMessage>> {
        return runCatching {
            val session = chatbotSession ?: throw IllegalStateException("No active chat session")
            session.getConversationHistory()
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
            connector.getTokenInfo(mintAddress)
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
            system.runBacktest(analysisId)
        }
                .onFailure { error -> Log.e(TAG, "Backtest failed", error) }
    }

    /** Get current system configuration */
    fun getConfig(): FiqhAiConfig? {
        return fiqhSystem?.getConfig()
    }

    /** Check if system is ready for operations */
    fun isReady(): Boolean {
        return _initializationState.value is InitializationState.Initialized && fiqhSystem != null
    }

    /** Get ruling display text */
    fun getRulingDisplayText(ruling: IslamicPrinciple): String {
        return getRulingDisplayText(ruling)
    }

    /** Get ruling emoji */
    fun getRulingEmoji(ruling: IslamicPrinciple): String {
        return getRulingEmoji(ruling)
    }

    /** Check if ruling is permissible */
    fun isRulingPermissible(ruling: IslamicPrinciple): Boolean {
        return isRulingPermissible(ruling)
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
