package com.rizilab.fiqhadvisor

import android.util.Log
import com.rizilab.fiqhadvisor.core.FiqhAIManager
import com.rizilab.fiqhadvisor.core.FiqhAiConfig
import kotlinx.coroutines.runBlocking

/** Enhanced test class to debug the AI system integration */
object DebugIntegration {

    fun runFullDiagnostics(): Boolean {
        return try {
            Log.d("DebugIntegration", "🔍 Starting full diagnostics...")

            // Test 1: Configuration creation
            Log.d("DebugIntegration", "📋 Testing configuration creation...")
            val config =
                    FiqhAiConfig(
                            openaiApiKey = "",
                            groqApiKey = "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",
                            grokApiKey =
                                    "xai-udKDRvvyrDuAYyrfye8thCLUgiKCNveOyYQrHjHnHCJ5pWUWH9TtfEQ73OI7Poh5b0UJZJvAPhYCKFEE",
                            modelName = "llama3-8b-8192",
                            qdrantUrl = "http://localhost:6333",
                            databasePath = "",
                            solanaRpcUrl = "https://api.mainnet-beta.solana.com",
                            enableSolana = true,
                            preferredModel = "groq"
                    )
            Log.d("DebugIntegration", "✅ Configuration created")
            Log.d("DebugIntegration", "   - Groq API Key: ${config.groqApiKey.take(10)}...")
            Log.d("DebugIntegration", "   - Grok API Key: ${config.grokApiKey.take(10)}...")
            Log.d("DebugIntegration", "   - OpenAI API Key: ${config.openaiApiKey}")
            Log.d("DebugIntegration", "   - Preferred Model: ${config.preferredModel}")
            Log.d("DebugIntegration", "   - Solana RPC: ${config.solanaRpcUrl}")

            // Test 2: System initialization
            Log.d("DebugIntegration", "🚀 Testing system initialization...")
            runBlocking {
                try {
                    val manager = FiqhAIManager()
                    Log.d("DebugIntegration", "✅ FiqhAIManager created successfully")

                    // Test 3: System configuration check
                    Log.d("DebugIntegration", "⚙️ Checking manager...")
                    Log.d("DebugIntegration", "   - Manager ready: ${manager.isReady()}")

                    // Note: Cannot test full functionality without Android Context
                    Log.d("DebugIntegration", "ℹ️ Full initialization requires Android Context")

                    Log.d("DebugIntegration", "🎉 Basic diagnostics completed successfully")
                    true
                } catch (e: Exception) {
                    Log.e("DebugIntegration", "❌ System initialization failed: ${e.message}", e)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("DebugIntegration", "❌ Diagnostics failed: ${e.message}", e)
            false
        }
    }

    fun quickConfigTest(): Boolean {
        return try {
            Log.d("DebugIntegration", "🔧 Testing configuration only...")

            val config =
                    FiqhAiConfig(
                            openaiApiKey = "",
                            groqApiKey = "test-groq-key",
                            grokApiKey = "test-grok-key",
                            modelName = "llama3-8b-8192",
                            qdrantUrl = "http://localhost:6333",
                            databasePath = "",
                            solanaRpcUrl = "https://api.mainnet-beta.solana.com",
                            enableSolana = true,
                            preferredModel = "groq"
                    )

            Log.d("DebugIntegration", "✅ Configuration test completed")
            Log.d("DebugIntegration", "   - Config: ${config::class.java.simpleName}")
            true
        } catch (e: Exception) {
            Log.e("DebugIntegration", "❌ Configuration test failed: ${e.message}", e)
            false
        }
    }
}
