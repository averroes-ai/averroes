package com.rizilab.fiqhadvisor

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FiqhAdvisorApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d("FiqhAdvisor", "Application started successfully")
        
        // Run comprehensive diagnostics
        try {
            Log.d("FiqhAdvisor", "🔍 Running comprehensive diagnostics...")
            AISystemDiagnostics.runComprehensiveDiagnostics()
            Log.d("FiqhAdvisor", "📊 Diagnostics completed successfully")
            
            Log.d("FiqhAdvisor", "✅ All systems operational")
        } catch (e: Exception) {
            Log.e("FiqhAdvisor", "❌ Diagnostics exception: ${e.message}", e)
        }
        
        // AI initialization will be done lazily when first needed
        // This prevents startup crashes due to native library loading issues
    }
}