package com.rizilab.averroes.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.rizilab.averroes.R
import androidx.lifecycle.viewmodel.compose.viewModel

// Mobile Wallet Adapter import (re-enabled)
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.rizilab.averroes.data.wallet.NetworkConfig
import com.rizilab.averroes.data.wallet.WalletPersistence
import com.rizilab.averroes.presentation.wallet.SharedWalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onSkipAuth: () -> Unit,
    activityResultSender: ActivityResultSender? = null,
    walletPersistence: WalletPersistence? = null,
    sharedWalletViewModel: SharedWalletViewModel? = null,
    viewModel: AuthViewModel = viewModel { AuthViewModel(activityResultSender, walletPersistence) }
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showSeedVaultDialog by remember { mutableStateOf(false) }

    // Collect effects
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToMain -> onAuthSuccess()
                is AuthEffect.ShowWalletSelector -> {
                    // TODO: Show wallet selector when Mobile Wallet Adapter is integrated
                }
                is AuthEffect.OpenPhantomPlayStore -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=app.phantom"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to web browser if Play Store not available
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=app.phantom"))
                        context.startActivity(intent)
                    }
                }
                is AuthEffect.ShowError -> {
                    snackbarMessage = effect.message
                    showSnackbar = true
                }
                is AuthEffect.ShowSuccess -> {
                    snackbarMessage = effect.message
                    showSnackbar = true
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo - Clean transparent PNG with card background
            Card(
                modifier = Modifier.size(140.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_averroes),
                        contentDescription = "Averroes Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Text
            Text(
                text = "Welcome to Averroes",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Islamic Finance AI Assistant",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features List
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
                        text = "Features:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FeatureItem("🤖", "AI-powered Islamic finance guidance")
                    FeatureItem("💰", "Halal cryptocurrency analysis")
                    FeatureItem("👛", "Solana wallet integration")
                    FeatureItem("🔒", "Secure seed vault")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Connect Wallet Button
            Button(
                onClick = { showSeedVaultDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isConnecting && !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting...")
                } else {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connect Solana Wallet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue as Guest Button
            OutlinedButton(
                onClick = { viewModel.handleIntent(AuthIntent.ContinueAsGuest) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isConnecting && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading...")
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue as Guest",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Error Message
            AnimatedVisibility(visible = state.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextButton(
                            onClick = { viewModel.handleIntent(AuthIntent.RetryConnection) }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Text(
                text = "By connecting, you agree to our Terms of Service\nand acknowledge our Privacy Policy",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }

    // Show seed vault dialog when connecting
    if (showSeedVaultDialog) {
        SeedVaultDialog(
            onDismiss = { showSeedVaultDialog = false },
            onConfirm = { seedPhrase ->
                showSeedVaultDialog = false
                // Connect wallet with seed vault integration (devnet hardcoded)
                viewModel.handleIntent(AuthIntent.ConnectWallet(seedPhrase, "devnet"))
            }
        )
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeedVaultDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit // seedPhrase only, network is hardcoded to devnet
) {
    var selectedOption by remember { mutableStateOf("connect") } // "connect", "import", "generate"
    var seedPhrase by remember { mutableStateOf("") }
    var showSeedInput by remember { mutableStateOf(false) }
    // Network is hardcoded to devnet for debugging

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = "Security",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secure Wallet Connection")
            }
        },
        text = {
            Column {
                Text(
                    text = "Choose how you want to add a wallet to your secure vault:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connect existing wallet option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "connect" }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "connect",
                        onClick = { selectedOption = "connect" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Connect External Wallet",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Connect your Phantom or other Solana wallet via Mobile Wallet Adapter",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Import seed phrase option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedOption = "import"
                            showSeedInput = true
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "import",
                        onClick = {
                            selectedOption = "import"
                            showSeedInput = true
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Import Seed Phrase",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Import an existing 12/24 word seed phrase",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Generate new wallet option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "generate" }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "generate",
                        onClick = { selectedOption = "generate" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Generate New Wallet",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Create a new wallet with secure seed phrase",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Seed phrase input field (shown when import is selected)
                if (showSeedInput && selectedOption == "import") {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = seedPhrase,
                        onValueChange = { seedPhrase = it },
                        label = { Text("Seed Phrase") },
                        placeholder = { Text("Enter your 12 or 24 word seed phrase...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 4
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Network info (hardcoded to devnet for debugging)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = "Network",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Network: Devnet (Debug Mode)",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Hardcoded for debugging to avoid connection issues",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Security notice
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Security",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your seed phrase is encrypted and stored securely on your device only.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedOption) {
                        "connect" -> onConfirm(null)
                        "import" -> onConfirm(seedPhrase.takeIf { it.isNotBlank() })
                        "generate" -> onConfirm("GENERATE_NEW")
                    }
                },
                enabled = when (selectedOption) {
                    "import" -> seedPhrase.isNotBlank()
                    else -> true
                }
            ) {
                Text(
                    when (selectedOption) {
                        "connect" -> "Connect"
                        "import" -> "Import"
                        "generate" -> "Generate"
                        else -> "Continue"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
