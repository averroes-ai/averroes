package com.rizilab.averroes

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class averroesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("averroesApp", "🚀 averroes Application Starting")
        
        // Initialize logging
        Log.d("averroesApp", "📱 Application initialized for core Rust→UniFFI→Kotlin testing")
    }
}