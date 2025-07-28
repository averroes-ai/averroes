package com.rizilab.fiqhadvisor

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizilab.fiqhadvisor.ui.viewmodel.FiqhAIViewModel

/**
 * Main Activity for testing core Rust->UniFFI->Kotlin functionality
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "🚀 Starting FiqhAI Core Test")
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FiqhAITestScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiqhAITestScreen() {
    val viewModel: FiqhAIViewModel = viewModel()
    val status by viewModel.status.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val systemInfo by viewModel.systemInfo.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "🧪 FiqhAI Core Test",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Testing: Rust AI → UniFFI → Kotlin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Status:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        
        // Test Controls
        Text(
            text = "🎛️ Test Controls:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.initializeAI() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("1. Initialize")
            }
            
            Button(
                onClick = { viewModel.testTokenAnalysis("BTC") },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("2. Test BTC")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.testQuery("Is Bitcoin halal in Islamic finance?") },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("3. Test Query")
            }
            
            Button(
                onClick = { viewModel.runFullTest() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("4. Full Test")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.updateSystemInfo() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Refresh Info")
            }
            
            Button(
                onClick = { viewModel.clearResponse() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear Response")
            }
        }
        
        // System Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System Information:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = systemInfo,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        
        // Response Display
        lastResponse?.let { response ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🤖 AI Response:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = response,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📋 Test Instructions:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = """
                        1. Initialize: Sets up Rust AI system via UniFFI
                        2. Test BTC: Analyzes Bitcoin using AI (Groq or Mock)
                        3. Test Query: Processes Islamic finance question
                        4. Full Test: Runs complete initialization + analysis
                        
                        ⚠️ Check Android Studio Logcat for detailed logs!
                        📱 Mock responses work without internet
                        🤖 Real Groq AI needs network connection
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}