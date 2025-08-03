package com.rizilab.averroes.core

import android.util.Log
import com.rizilab.averroes.fiqhcore.*

/** Kotlin implementation of the StreamCallback interface for handling streaming AI responses */
class StreamingCallback(
        private val onChunk: (StreamChunk) -> Unit = {},
        private val onError: (String) -> Unit = {},
        private val onComplete: (QueryResponse) -> Unit = {}
) : StreamCallback {

    private val TAG = "StreamingCallback"

    override fun onChunk(chunk: StreamChunk) {
        Log.d(TAG, "📝 Received chunk ${chunk.chunkIndex}: '${chunk.content.take(50)}...'")
        onChunk.invoke(chunk)
    }

    override fun onError(error: String) {
        Log.e(TAG, "❌ Streaming error: $error")
        onError.invoke(error)
    }

    override fun onComplete(finalResponse: QueryResponse) {
        Log.d(TAG, "✅ Streaming complete. Total response length: ${finalResponse.response.length}")
        Log.d(TAG, "📊 Confidence: ${(finalResponse.confidence * 100).toInt()}%")
        onComplete.invoke(finalResponse)
    }
}
