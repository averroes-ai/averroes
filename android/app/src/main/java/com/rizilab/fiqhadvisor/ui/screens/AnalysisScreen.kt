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
import com.rizilab.fiqhadvisor.core.FiqhAIManager
import com.rizilab.fiqhadvisor.ui.viewmodel.FiqhAIViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: FiqhAIViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    var tokenInput by remember { mutableStateOf("") }
    var textQuery by remember { mutableStateOf("") }
    var selectedInputType by remember { mutableIntStateOf(0) }
    val inputTypes = listOf("Token", "Text", "Audio", "Contract")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Initialization Status
        if (!uiState.isInitialized) {
            InitializationCard(
                isLoading = uiState.isLoading,
                onInitialize = { viewModel.initialize() }
            )
        } else {
            // Input Type Selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                inputTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = inputTypes.size),
                        onClick = { selectedInputType = index },
                        selected = index == selectedInputType
                    ) {
                        Text(type)
                    }
                }
            }

            // Input Section
            when (selectedInputType) {
                0 -> TokenInputSection(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    onAnalyze = {
                        keyboardController?.hide()
                        viewModel.analyzeToken(tokenInput)
                    },
                    isLoading = uiState.isAnalyzing
                )
                1 -> TextInputSection(
                    value = textQuery,
                    onValueChange = { textQuery = it },
                    onAnalyze = {
                        keyboardController?.hide()
                        viewModel.analyzeText(textQuery)
                    },
                    isLoading = uiState.isAnalyzing
                )
                2 -> AudioInputSection(
                    onRecord = { viewModel.startAudioRecording() },
                    onAnalyze = { viewModel.analyzeAudioRecording() },
                    isRecording = uiState.isRecording,
                    isLoading = uiState.isAnalyzing
                )
                3 -> ContractInputSection(
                    onAnalyze = { address -> viewModel.analyzeContract(address) },
                    isLoading = uiState.isAnalyzing
                )
            }

            // Analysis Results
            uiState.currentAnalysis?.let { analysis ->
                AnalysisResultCard(analysis = analysis)
            }

            // Error Display
            uiState.error?.let { error ->
                ErrorCard(
                    error = error,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
}

@Composable
private fun InitializationCard(
    isLoading: Boolean,
    onInitialize: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Welcome to FiqhAI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Initialize the system to start analyzing tokens",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(onClick = onInitialize) {
                    Text("Initialize FiqhAI")
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
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
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRecord,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
                
                Button(
                    onClick = onAnalyze,
                    enabled = !isLoading && !isRecording
                ) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recording...")
                }
            }
        }
    }
}

@Composable
private fun ContractInputSection(
    onAnalyze: (String) -> Unit,
    isLoading: Boolean
) {
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Analyze Contract")
            }
        }
    }
}

@Composable
private fun AnalysisResultCard(analysis: Any) {
    // This would be implemented based on your QueryResponse model
    Card(
        colors = CardDefaults.cardColors(
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
            
            // This would display the actual analysis results
            Text("Analysis result will be displayed here based on QueryResponse")
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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