package com.rizilab.fiqhadvisor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rizilab.fiqhadvisor.ui.viewmodel.FiqhAIViewModel
import com.rizilab.fiqhadvisor.ui.viewmodel.AnalysisState

@Composable
fun AnalysisDetailScreen(
    analysisId: String,
    viewModel: FiqhAIViewModel,
    onNavigateBack: () -> Unit
) {
    val analysisState by viewModel.analysisState.collectAsState()

    when (val state = analysisState) {
        is AnalysisState.Success -> {
            val result = state.result
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with back button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "Analysis Details",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }

                // Analysis Response
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Analysis Result",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.response,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Confidence Score
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Confidence Score",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${(result.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (result.confidence > 0.7) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }

                // Sources
                if (result.sources.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Sources",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                result.sources.forEach { source ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text("📚 ", style = MaterialTheme.typography.bodyMedium)
                                        Text(source, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Follow-up Questions
                if (result.followUpQuestions.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Follow-up Questions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                result.followUpQuestions.forEach { question ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text("❓ ", style = MaterialTheme.typography.bodyMedium)
                                        Text(question, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        is AnalysisState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is AnalysisState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red
                )
            }
        }
        is AnalysisState.Idle -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No analysis data available",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        is AnalysisState.FallbackResponse -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

