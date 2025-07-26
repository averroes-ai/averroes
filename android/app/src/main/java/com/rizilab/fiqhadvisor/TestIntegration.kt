package com.rizilab.fiqhadvisor

import android.util.Log
import com.rizilab.fiqhadvisor.core.FiqhAIManager
import com.rizilab.fiqhadvisor.core.FiqhAiConfig
import kotlinx.coroutines.runBlocking

/**
 * Test class to verify the Rust-Kotlin integration works
 */
object TestIntegration {
    
    fun testBasicIntegration(): Boolean {
        return try {
            Log.d("TestIntegration", "Starting basic integration test...")
            
            // Test configuration creation
            val config = FiqhAiConfig(
                openaiApiKey = "",
                groqApiKey = "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",
                grokApiKey = "xai-udKDRvvyrDuAYyrfye8thCLUgiKCNveOyYQrHjHnHCJ5pWUWH9TtfEQ73OI7Poh5b0UJZJvAPhYCKFEE",
                modelName = "llama3-8b-8192", 
                qdrantUrl = "http://localhost:6333",
                databasePath = "",
                solanaRpcUrl = "https://api.mainnet-beta.solana.com",
                enableSolana = true,
                preferredModel = "groq"
            )
            
            Log.d("TestIntegration", "Configuration created successfully")
            
            // Test manager creation
            runBlocking {
                val manager = FiqhAIManager()
                Log.d("TestIntegration", "FiqhAIManager created successfully")
                Log.d("TestIntegration", "Manager ready: ${manager.isReady()}")
                
                // Note: Full initialization would require Android Context
                Log.d("TestIntegration", "Basic integration test completed")
            }
            
            true
        } catch (e: Exception) {
            Log.e("TestIntegration", "Integration test failed: ${e.message}", e)
            false
        }
    }
    
    fun testConfigCreation(): Boolean {
        return try {
            Log.d("TestIntegration", "Testing config creation...")
            
            val config = FiqhAiConfig(
                openaiApiKey = "test-openai",
                groqApiKey = "test-groq", 
                grokApiKey = "test-grok",
                modelName = "llama3-8b-8192",
                qdrantUrl = "http://localhost:6333",
                databasePath = "",
                solanaRpcUrl = "https://api.mainnet-beta.solana.com",
                enableSolana = true,
                preferredModel = "groq"
            )
            
            Log.d("TestIntegration", "Config created: ${config.preferredModel}")
            true
        } catch (e: Exception) {
            Log.e("TestIntegration", "Config test failed: ${e.message}", e)
            false
        }
    }
}