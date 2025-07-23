package com.rizilab.fiqhadvisor.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.rizilab.fiqhadvisor.fiqhcore.FiqhAiConfig
import com.rizilab.fiqhadvisor.fiqhcore.FiqhAiSystem
import com.rizilab.fiqhadvisor.fiqhcore.AudioProcessor
import com.rizilab.fiqhadvisor.fiqhcore.SolanaConnector
import com.rizilab.fiqhadvisor.fiqhcore.ChatbotSession
import com.rizilab.fiqhadvisor.fiqhcore.QueryResponse
import com.rizilab.fiqhadvisor.fiqhcore.AnalysisHistory
import com.rizilab.fiqhadvisor.fiqhcore.UserAnalysisStats
import com.rizilab.fiqhadvisor.fiqhcore.ChatMessage
import com.rizilab.fiqhadvisor.fiqhcore.SolanaTokenInfo
import com.rizilab.fiqhadvisor.fiqhcore.BacktestResult
import com.rizilab.fiqhadvisor.fiqhcore.IslamicPrinciple
import com.rizilab.fiqhadvisor.fiqhcore.createMobileConfig

/**
 * Android manager for FiqhAI system that wraps the Rust `UniFFI` interface. Provides a
 * Kotlin-friendly API for the Android application.
 */
class FiqhAIManager
private constructor(private val context: Context, private val config: FiqhAiConfig) {
    companion object {
        private const val TAG = "FiqhAIManager"
        private const val LIBRARY_NAME = "fiqh_core"

        @Volatile private var INSTANCE: FiqhAIManager? = null

        /** Get singleton instance of FiqhAIManager */
        fun getInstance(context: Context, config: FiqhAiConfig? = null): FiqhAIManager {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                FiqhAIManager(
                                        context.applicationContext,
                                        config ?: createDefaultConfig()
                                )
                        INSTANCE = instance
                        instance
                    }
        }

        /** Create default configuration for mobile use */
        fun createDefaultConfig(): FiqhAiConfig {
            return createMobileConfig(openaiApiKey = null, enableVectorSearch = true)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Core system components
    private var fiqhSystem: FiqhAiSystem? = null
    private var audioProcessor: AudioProcessor? = null
    private var solanaConnector: SolanaConnector? = null
    private var activeChatSession: ChatbotSession? = null

    // State management
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        initializeSystem()
    }

    /** Initialize the native library and Rust system */
    private fun initializeSystem() {
        scope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Load native library
                System.loadLibrary(LIBRARY_NAME)
                Log.d(TAG, "Native library loaded successfully")

                // Initialize core components
                fiqhSystem = FiqhAiSystem(config) // Regular constructor (now synchronous)
                audioProcessor = AudioProcessor() // Regular constructor
                solanaConnector = SolanaConnector(config.solanaRpcUrl) // Regular constructor

                _isInitialized.value = true
                Log.d(TAG, "FiqhAI system initialized successfully")
            } catch (e: Exception) {
                val errorMsg = "Failed to initialize FiqhAI system: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _error.value = errorMsg
            } finally {
                _isLoading.value = false
            }
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
        activeChatSession = ChatbotSession(userId, language) // Regular constructor
        return activeChatSession!!.startSession()
    }

    /** Send message to chatbot */
    suspend fun sendChatMessage(message: String, context: String? = null): Result<QueryResponse> {
        return runCatching {
            val session = activeChatSession ?: throw IllegalStateException("No active chat session")
            session.sendMessage(message, context)
        }
                .onFailure { error -> Log.e(TAG, "Chat message failed", error) }
    }

    /** Get chat conversation history */
    suspend fun getChatHistory(): Result<List<ChatMessage>> {
        return runCatching {
            val session = activeChatSession ?: throw IllegalStateException("No active chat session")
            session.getConversationHistory()
        }
                .onFailure { error -> Log.e(TAG, "Failed to get chat history", error) }
    }

    /** Clear current chat session */
    suspend fun clearChatSession() {
        activeChatSession?.clearSession()
        activeChatSession = null
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
        return _isInitialized.value && fiqhSystem != null
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
        scope.cancel()
        activeChatSession = null
        fiqhSystem = null
        audioProcessor = null
        solanaConnector = null
        _isInitialized.value = false
        INSTANCE = null
        Log.d(TAG, "FiqhAIManager cleaned up")
    }

    /** Restart the system (useful for configuration changes) */
    fun restart() {
        cleanup()
        initializeSystem()
    }
}
