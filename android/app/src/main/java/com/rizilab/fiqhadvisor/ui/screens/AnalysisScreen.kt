package com.rizilab.fiqhadvisor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizilab.fiqhadvisor.ui.viewmodel.AnalysisState
import com.rizilab.fiqhadvisor.ui.viewmodel.FiqhAIViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(viewModel: FiqhAIViewModel, modifier: Modifier = Modifier) {
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var tokenInput by remember { mutableStateOf("") }
    var textQuery by remember { mutableStateOf("") }
    var selectedInputType by remember { mutableIntStateOf(0) }
    val inputTypes = listOf("Token", "Text", "Audio", "Contract")

    Column(
            modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Input Type Selector
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            inputTypes.forEachIndexed { index, type ->
                SegmentedButton(
                        shape =
                                SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = inputTypes.size
                                ),
                        onClick = { selectedInputType = index },
                        selected = index == selectedInputType
                ) { Text(type) }
            }
        }

        // Input Section
        when (selectedInputType) {
            0 ->
                    TokenInputSection(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            onAnalyze = {
                                keyboardController?.hide()
                                viewModel.analyzeToken(tokenInput)
                            },
                            isLoading = analysisState is AnalysisState.Loading
                    )
            1 ->
                    TextInputSection(
                            value = textQuery,
                            onValueChange = { textQuery = it },
                            onAnalyze = {
                                keyboardController?.hide()
                                // Text analysis will be implemented when general AI text processing
                                // is available
                            },
                            isLoading = analysisState is AnalysisState.Loading
                    )
            2 ->
                    AudioInputSection(
                            onRecord = {
                                // Audio recording functionality - requires microphone permissions
                                // and recording implementation
                            },
                            onAnalyze = {
                                // Audio analysis functionality - would process recorded audio using
                                // FiqhAIManager.analyzeAudio()
                            },
                            isRecording = false,
                            isLoading = analysisState is AnalysisState.Loading
                    )
            3 ->
                    ContractInputSection(
                            onAnalyze = { address ->
                                // Contract analysis functionality - would use
                                // FiqhAIManager.analyzeContract() for smart contract evaluation
                            },
                            isLoading = analysisState is AnalysisState.Loading
                    )
        }

        // Analysis Results
        when (val currentState = analysisState) {
            is AnalysisState.Loading -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }
            is AnalysisState.Success -> {
                AnalysisResultCard(analysis = currentState.result)
            }
            is AnalysisState.Error -> {
                ErrorCard(
                        error = currentState.message,
                        onDismiss = {
                            // Error dismissal - could add clearError() method to ViewModel for
                            // better UX
                        }
                )
            }
            is AnalysisState.Idle -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = "Enter a token symbol to get started",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is AnalysisState.FallbackResponse -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                                "💡 AI Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )

                        Text(
                                text = currentState.message,
                                style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenInputSection(
        value: String,
        onValueChange: (String) -> Unit,
        onAnalyze: () -> Unit,
        isLoading: Boolean
) {
    Card {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                    "🪙 Token Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("Token Symbol (e.g., BTC, SOL, USDT)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAnalyze() }),
                    enabled = !isLoading
            )

            Button(
                    onClick = onAnalyze,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && value.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Analyze Token")
            }
        }
    }
}

@Composable
private fun TextInputSection(
        value: String,
        onValueChange: (String) -> Unit,
        onAnalyze: () -> Unit,
        isLoading: Boolean
) {
    Card {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                    "💭 Text Query Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("Ask about Islamic compliance...") },
                    placeholder = { Text("e.g., Apakah Bitcoin halal menurut syariat Islam?") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isLoading
            )

            Button(
                    onClick = onAnalyze,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && value.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Analyze Text")
            }
        }
    }
}

@Composable
private fun AudioInputSection(
        onRecord: () -> Unit,
        onAnalyze: () -> Unit,
        isRecording: Boolean,
        isLoading: Boolean
) {
    Card {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    "🎤 Audio Query Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )

            Text(
                    "Record your voice query in Indonesian",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                        onClick = onRecord,
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor =
                                                if (isRecording) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.primary
                                )
                ) {
                    Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }

                Button(onClick = onAnalyze, enabled = !isLoading && !isRecording) {
                    if (isLoading) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Analyze")
                }
            }

            if (isRecording) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recording...")
                }
            }
        }
    }
}

@Composable
private fun ContractInputSection(onAnalyze: (String) -> Unit, isLoading: Boolean) {
    var contractAddress by remember { mutableStateOf("") }

    Card {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                    "📄 Contract Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                    value = contractAddress,
                    onValueChange = { contractAddress = it },
                    label = { Text("Contract Address") },
                    placeholder = { Text("Paste Solana/Ethereum contract address...") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
            )

            Button(
                    onClick = { onAnalyze(contractAddress) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && contractAddress.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Analyze Contract")
            }
        }
    }
}

@Composable
private fun AnalysisResultCard(analysis: com.rizilab.fiqhadvisor.core.QueryResponse) {
    Card(
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                    "📊 Analysis Result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )

            Text(
                    text = "Response: ${analysis.response}",
                    style = MaterialTheme.typography.bodyMedium
            )

            Text(
                    text = "Sources: ${analysis.sources.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorCard(error: String, onDismiss: () -> Unit) {
    Card(
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                    )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
