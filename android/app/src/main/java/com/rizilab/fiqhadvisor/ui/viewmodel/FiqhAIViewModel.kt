package com.rizilab.fiqhadvisor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizilab.fiqhadvisor.core.FiqhAIManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FiqhAIUiState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isRecording: Boolean = false,
    val currentAnalysis: Any? = null, // Would be QueryResponse
    val chatHistory: List<Any> = emptyList(), // Would be List<ChatMessage>
    val analysisHistory: List<Any> = emptyList(), // Would be List<HistoryEntry>
    val error: String? = null
)

class FiqhAIViewModel : ViewModel() {
    private val fiqhAIManager = FiqhAIManager.instance
    
    private val _isAnalyzing = MutableStateFlow(false)
    private val _isRecording = MutableStateFlow(false)
    private val _currentAnalysis = MutableStateFlow<Any?>(null)
    private val _chatHistory = MutableStateFlow<List<Any>>(emptyList())
    private val _analysisHistory = MutableStateFlow<List<Any>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FiqhAIUiState> = combine(
        fiqhAIManager.isInitialized,
        fiqhAIManager.isLoading,
        _isAnalyzing,
        _isRecording,
        _currentAnalysis,
        _chatHistory,
        _analysisHistory,
        _error
    ) { isInitialized, isLoading, isAnalyzing, isRecording, analysis, chatHistory, analysisHistory, error ->
        FiqhAIUiState(
            isInitialized = isInitialized,
            isLoading = isLoading,
            isAnalyzing = isAnalyzing,
            isRecording = isRecording,
            currentAnalysis = analysis,
            chatHistory = chatHistory,
            analysisHistory = analysisHistory,
            error = error
        )
    }.asStateFlow()

    // Initialization
    fun initialize(openAiApiKey: String? = null) {
        viewModelScope.launch {
            fiqhAIManager.initialize(openAiApiKey)
                .onFailure { exception ->
                    _error.value = "Initialization failed: ${exception.message}"
                }
        }
    }

    // Token Analysis
    fun analyzeToken(tokenTicker: String, userId: String? = "android_user") {
        if (tokenTicker.isBlank()) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _error.value = null
            
            fiqhAIManager.analyzeToken(tokenTicker, userId)
                .onSuccess { response ->
                    _currentAnalysis.value = response
                }
                .onFailure { exception ->
                    _error.value = "Token analysis failed: ${exception.message}"
                }
            
            _isAnalyzing.value = false
        }
    }

    // Text Analysis
    fun analyzeText(text: String, userId: String? = "android_user") {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _error.value = null
            
            fiqhAIManager.analyzeText(text, userId)
                .onSuccess { response ->
                    _currentAnalysis.value = response
                }
                .onFailure { exception ->
                    _error.value = "Text analysis failed: ${exception.message}"
                }
            
            _isAnalyzing.value = false
        }
    }

    // Contract Analysis
    fun analyzeContract(contractAddress: String, userId: String? = "android_user") {
        if (contractAddress.isBlank()) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _error.value = null
            
            fiqhAIManager.analyzeContract(contractAddress, userId)
                .onSuccess { response ->
                    _currentAnalysis.value = response
                }
                .onFailure { exception ->
                    _error.value = "Contract analysis failed: ${exception.message}"
                }
            
            _isAnalyzing.value = false
        }
    }

    // Audio Recording (Mock implementation)
    private var mockAudioData: ByteArray? = null
    
    fun startAudioRecording() {
        _isRecording.value = !_isRecording.value
        
        if (_isRecording.value) {
            // Mock recording - in real implementation, would use MediaRecorder
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000) // Simulate 3 second recording
                mockAudioData = "mock_audio_data".toByteArray()
                _isRecording.value = false
            }
        }
    }

    fun analyzeAudioRecording(userId: String? = "android_user") {
        val audioData = mockAudioData ?: run {
            _error.value = "No audio data available. Please record audio first."
            return
        }
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _error.value = null
            
            fiqhAIManager.analyzeAudio(audioData, userId)
                .onSuccess { response ->
                    _currentAnalysis.value = response
                    mockAudioData = null // Clear after use
                }
                .onFailure { exception ->
                    _error.value = "Audio analysis failed: ${exception.message}"
                }
            
            _isAnalyzing.value = false
        }
    }

    // Chat Operations
    fun startChatSession(userId: String = "android_user"): String {
        return fiqhAIManager.startChatSession(userId)
    }

    fun sendChatMessage(message: String, context: String? = null) {
        if (message.isBlank()) return
        
        viewModelScope.launch {
            fiqhAIManager.sendChatMessage(message, context)
                .onSuccess { response ->
                    // Update chat history
                    val currentHistory = fiqhAIManager.getChatHistory()
                    _chatHistory.value = currentHistory
                }
                .onFailure { exception ->
                    _error.value = "Chat message failed: ${exception.message}"
                }
        }
    }

    fun clearChatSession() {
        fiqhAIManager.clearChatSession()
        _chatHistory.value = emptyList()
    }

    // History Operations
    fun loadUserHistory(userId: String = "android_user", limit: Int = 20) {
        viewModelScope.launch {
            fiqhAIManager.getUserHistory(userId, limit)
                .onSuccess { history ->
                    _analysisHistory.value = history.entries
                }
                .onFailure { exception ->
                    _error.value = "Failed to load history: ${exception.message}"
                }
        }
    }

    // Error Handling
    fun clearError() {
        _error.value = null
    }

    fun clearCurrentAnalysis() {
        _currentAnalysis.value = null
    }
} 