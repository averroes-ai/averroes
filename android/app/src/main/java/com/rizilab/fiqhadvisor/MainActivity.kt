package com.rizilab.fiqhadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FiqhAIApp()
            }
        }
    }
}

@Composable
fun FiqhAIApp() {
    var analysisResult by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FiqhAI Advisor",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "Islamic Finance Token Analysis",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Button(
            onClick = {
                // TODO: Call Rust core for token analysis
                analysisResult = "Analysis complete! Token appears to be Halal based on preliminary screening."
                showResult = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50) // Green color
            ),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Analyze Token",
                color = Color.White
            )
        }
        
        if (showResult) {
            Text(
                text = analysisResult,
                fontSize = 14.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
} 