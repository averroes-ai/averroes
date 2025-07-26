package com.rizilab.fiqhadvisor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rizilab.fiqhadvisor.ui.viewmodel.FiqhAIViewModel
import com.rizilab.fiqhadvisor.ui.viewmodel.AnalysisState
import com.rizilab.fiqhadvisor.ui.components.AnalysisResultCard

@Composable
fun HomeScreen(viewModel: FiqhAIViewModel, onNavigateToAnalysis: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val analysisState by viewModel.analysisState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Text(
            text = "Fiqh Advisor",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Islamic Cryptocurrency Analysis",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Enter token symbol") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.analyzeToken(searchQuery)
                        }
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Access Tokens
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("SOL", "BTC", "ETH", "USDC", "RAY")) { token ->
                FilterChip(
                    onClick = { 
                        searchQuery = token
                        viewModel.analyzeToken(token)
                    },
                    label = { Text(token) },
                    selected = false
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Analysis Result
        when (val state = analysisState) {
            is AnalysisState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AnalysisState.Success -> {
                AnalysisResultCard(
                    analysis = state.result,
                    onViewDetails = { onNavigateToAnalysis(state.result.queryId) }
                )
            }
            is AnalysisState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Enter a token symbol to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}