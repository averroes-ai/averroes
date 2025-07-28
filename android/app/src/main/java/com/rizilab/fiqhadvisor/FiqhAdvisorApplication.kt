package com.rizilab.fiqhadvisor

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FiqhAdvisorApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("FiqhAdvisorApp", "🚀 FiqhAdvisor Application Starting")
        
        // Initialize logging
        Log.d("FiqhAdvisorApp", "📱 Application initialized for core Rust→UniFFI→Kotlin testing")
    }
}