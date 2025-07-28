package com.rizilab.fiqhadvisor.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rizilab.fiqhadvisor.core.FiqhAIManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Clean ViewModel for testing core Rust->UniFFI->Kotlin functionality Focus: Initialize AI system
 * and test basic operations
 */
class FiqhAIViewModel(application: Application) : AndroidViewModel(application) {

    private val aiManager = FiqhAIManager() // Remove getInstance() call

    // Simple UI states for testing
    private val _status = MutableStateFlow("🔧 Not initialized")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _lastResponse = MutableStateFlow<String?>(null)
    val lastResponse: StateFlow<String?> = _lastResponse.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _systemInfo = MutableStateFlow("System not checked")
    val systemInfo: StateFlow<String> = _systemInfo.asStateFlow()

    init {
        Log.d(TAG, "🚀 FiqhAIViewModel created")
        updateSystemInfo()
    }

    /** Initialize the AI system (sync constructor - ferrostar/sprucekit pattern) */
    fun initializeAI() {
        viewModelScope.launch {
            _status.value = "Creating AI system..."

            val success = aiManager.initialize()
            if (success) {
                _isInitialized.value = true
                _status.value = "✅ AI system ready"
                updateSystemInfo()
            } else {
                _isInitialized.value = false
                _status.value = "❌ Failed to create AI system"
            }
        }
    }

    /** Core test: Analyze a token using Rust AI via UniFFI */
    fun testTokenAnalysis(token: String = "BTC") {
        if (!_isInitialized.value) {
            _status.value = "❌ Not ready - initialize first!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _status.value = "🔍 Analyzing $token via Rust AI..."

            try {
                Log.d(TAG, "🔍 Testing token analysis for: $token")
                val result = aiManager.analyzeToken(token)

                _lastResponse.value = result
                _status.value = "✅ $token Analysis completed"
                Log.d(TAG, "✅ Token analysis successful!")
                Log.d(TAG, "Response preview: ${result.take(100)}...")
            } catch (e: Exception) {
                _status.value = "💥 Analysis Exception: ${e.message}"
                Log.e(TAG, "💥 Exception during token analysis", e)
            } finally {
                _isLoading.value = false
                updateSystemInfo()
            }
        }
    }

    /** Core test: Process general query using Rust AI via UniFFI */
    fun testQuery(query: String = "Is Bitcoin halal in Islam?") {
        if (!_isInitialized.value) {
            _status.value = "❌ Not ready - initialize first!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _status.value = "💬 Processing query via Rust AI..."

            try {
                Log.d(TAG, "💬 Testing query: $query")
                val result = aiManager.query(query)

                _lastResponse.value = result
                _status.value = "✅ Query completed"
                Log.d(TAG, "✅ Query processing successful!")
                Log.d(TAG, "Response preview: ${result.take(100)}...")
            } catch (e: Exception) {
                _status.value = "💥 Query Exception: ${e.message}"
                Log.e(TAG, "💥 Exception during query processing", e)
            } finally {
                _isLoading.value = false
                updateSystemInfo()
            }
        }
    }

    /** Update system information */
    fun updateSystemInfo() {
        _systemInfo.value = buildString {
            appendLine("🔧 FiqhAI System Status:")
            appendLine("• Ready: ${_isInitialized.value}")
            appendLine("• Agent: ${aiManager.getAgentInfo()}")
            appendLine("• Real AI: ${aiManager.isUsingRealAI()}")
        }
        Log.d(TAG, _systemInfo.value)
    }

    /** Clear the last response */
    fun clearResponse() {
        _lastResponse.value = null
        _status.value = "🔧 Response cleared"
    }

    /** Quick test sequence for demo */
    fun runFullTest() {
        viewModelScope.launch {
            Log.d(TAG, "🚀 Starting full test sequence...")

            // Test 1: Initialize
            initializeAI()

            // Wait a bit then test token analysis
            kotlinx.coroutines.delay(2000)
            if (_isInitialized.value) {
                testTokenAnalysis("BTC")
            }
        }
    }

    companion object {
        private const val TAG = "FiqhAIViewModel"
    }
}
