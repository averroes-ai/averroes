package com.rizilab.fiqhadvisor.core

import android.util.Log
import com.rizilab.fiqhadvisor.fiqhcore.*

/** Manager class for FiqhAI system integration Handles initialization and core AI operations */
class FiqhAIManager {
    private val TAG = "FiqhAIManager"
    private var aiSystem: FiqhAiSystem? = null

    init {
        Log.d(TAG, "🔧 FiqhAIManager initializing...")
    }

    /** Initialize the AI system (synchronous, then upgrade asynchronously) */
    suspend fun initialize(): Boolean {
        Log.d(TAG, "🚀 Creating FiqhAI system (sync constructor)...")

        return try {
            // Direct UniFFI call (no reflection needed!)
            aiSystem = FiqhAiSystem.newFiqhAi()

            Log.d(TAG, "✅ FiqhAI system created with Mock agent!")
            Log.d(TAG, "🔄 Upgrading to Groq agent...")

            // Direct async method call (this is suspend!)
            aiSystem?.initializeGroqAgent()

            Log.d(TAG, "✅ Successfully upgraded to Groq agent!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize AI system: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /** Analyze a cryptocurrency token */
    suspend fun analyzeToken(token: String): String {
        Log.d(TAG, "🔍 Analyzing token: $token")

        return try {
            if (aiSystem == null) {
                Log.e(TAG, "❌ AI system not initialized!")
                return "Error: AI system not initialized"
            }

            // Direct UniFFI method call - returns QueryResponse
            val result: QueryResponse = aiSystem!!.analyzeToken(token)

            Log.d(TAG, "✅ Analysis completed for $token")
            Log.d(TAG, "📊 Confidence: ${(result.confidence * 100).toInt()}%")

            result.response // Return the actual response string
        } catch (e: Exception) {
            Log.e(TAG, "❌ Token analysis failed: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }

    /** Process a general query */
    suspend fun query(question: String): String {
        Log.d(TAG, "🤔 Processing query: ${question.take(50)}")

        return try {
            if (aiSystem == null) {
                Log.e(TAG, "❌ AI system not initialized!")
                return "Error: AI system not initialized"
            }

            // Direct UniFFI method call - returns QueryResponse
            val result: QueryResponse = aiSystem!!.query(question)

            Log.d(TAG, "✅ Query completed")
            Log.d(TAG, "📊 Confidence: ${(result.confidence * 100).toInt()}%")

            result.response // Return the actual response string
        } catch (e: Exception) {
            Log.e(TAG, "❌ Query failed: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }

    /** Get current agent info */
    fun getAgentInfo(): String {
        Log.d(TAG, "📊 Getting agent info...")

        return try {
            if (aiSystem == null) {
                Log.d(TAG, "❓ AI system not initialized")
                return "Not initialized"
            }

            // Direct UniFFI method call
            val result = aiSystem!!.getAgentInfo()

            Log.d(TAG, "📊 Agent info: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get agent info: ${e.javaClass.simpleName}: ${e.message}")
            "Error: ${e.message}"
        }
    }

    /** Check if using real AI */
    fun isUsingRealAI(): Boolean {
        Log.d(TAG, "🤖 Checking real AI status...")

        return try {
            if (aiSystem == null) {
                Log.d(TAG, "❓ AI system not initialized")
                return false
            }

            // Direct UniFFI method call
            val result = aiSystem!!.isUsingRealAi()

            Log.d(TAG, "🤖 Using real AI: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check real AI status: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }
}
