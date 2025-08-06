package com.rizilab.averroes.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.rizilab.averroes.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.rizilab.averroes.presentation.navigation.MainTab
import com.rizilab.averroes.presentation.wallet.SharedWalletViewModel

/**
 * Splash screen
 */
@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onNavigateToMain() // Skip auth for now
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_averroes),
                contentDescription = "Averroes Logo",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Averroes",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Islamic Finance AI",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Auth screen placeholder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onSkipAuth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = "Auth",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to Averroes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your Islamic Finance AI Assistant",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAuthSuccess,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onSkipAuth) {
            Text("Skip for now")
        }
    }
}

/**
 * Main screen with bottom navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onBack: () -> Unit,
    activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null,
    sharedWalletViewModel: SharedWalletViewModel? = null
) {
    var selectedTab by remember { mutableStateOf(MainTab.AI_CHAT) }
    var selectedCryptoId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                getIconForTab(tab),
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            if (tab != MainTab.CRYPTO_LIST) {
                                selectedCryptoId = null
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                MainTab.AI_CHAT -> {
                    com.rizilab.averroes.presentation.chat.ChatScreen()
                }
                MainTab.CRYPTO_LIST -> {
                    com.rizilab.averroes.presentation.crypto.CryptoScreen()
                }
                MainTab.WALLET -> {
                    WalletScreen(
                        onBack = onBack,
                        activityResultSender = activityResultSender,
                        sharedWalletViewModel = sharedWalletViewModel
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(onBack = onBack)
                }
            }
        }
    }
}

/**
 * Get icon for navigation tab
 */
@Composable
private fun getIconForTab(tab: MainTab): ImageVector {
    return when (tab) {
        MainTab.AI_CHAT -> Icons.Default.Chat
        MainTab.CRYPTO_LIST -> Icons.Default.CurrencyBitcoin
        MainTab.WALLET -> Icons.Default.AccountBalanceWallet
        MainTab.SETTINGS -> Icons.Default.Settings
    }
}

/**
 * Placeholder screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Islamic Finance AI") })
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Chat",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AI Chat Interface",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Chat with Islamic Finance AI using UniFFI integration",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoListScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Halal Cryptocurrencies") })
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CurrencyBitcoin,
                    contentDescription = "Crypto",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Halal Crypto Analysis",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Browse halal cryptocurrencies with Islamic compliance analysis",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender? = null,
    sharedWalletViewModel: SharedWalletViewModel? = null
) {
    val context = LocalContext.current

    // Use shared wallet state from persistence
    val walletState by sharedWalletViewModel?.walletState?.collectAsState() ?: remember {
        mutableStateOf(com.rizilab.averroes.data.wallet.WalletConnectionState.NotConnected)
    }

    val isLoading by sharedWalletViewModel?.isLoading?.collectAsState() ?: remember {
        mutableStateOf(false)
    }

    // Refresh wallet state when screen is displayed
    LaunchedEffect(Unit) {
        sharedWalletViewModel?.refreshConnection()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Wallet") })

        if (isLoading) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading wallet information...")
                }
            }
        } else if (walletState is com.rizilab.averroes.data.wallet.WalletConnectionState.Connected) {
            val connectedWallet = walletState as com.rizilab.averroes.data.wallet.WalletConnectionState.Connected
            // Connected wallet view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Wallet Balance Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Balance",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "◎",
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "%.4f".format(connectedWallet.balance),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SOL",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "≈ $${(connectedWallet.balance * 23.45).let { "%.2f".format(it) }} USD",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Wallet Address Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Wallet Address",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${connectedWallet.publicKey.take(8)}...${connectedWallet.publicKey.takeLast(8)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = connectedWallet.accountLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            OutlinedButton(
                                onClick = { /* Copy address */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { /* View on explorer */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Explorer",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Explorer")
                            }
                        }
                    }
                }

                // Quick Actions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Quick Actions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { /* Send action */ }
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Send",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { /* Receive action */ }
                            ) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = "Receive",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Receive",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { /* Swap action */ }
                            ) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = "Swap",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Swap",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Disconnect Button
                Button(
                    onClick = {
                        if (activityResultSender != null) {
                            sharedWalletViewModel?.clearConnection()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = "Disconnect",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Wallet")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(8.dp))

                // Debug: Clear All Wallet Data Button
                OutlinedButton(
                    onClick = {
                        sharedWalletViewModel?.clearConnection()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Clear Data",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Wallet Data (Debug)")
                }
            }
        } else {
            // Not connected view
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connect Your Solana Wallet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect your Phantom wallet to view balance and manage your assets",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Please connect your wallet from the login screen",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Make sure you have Phantom wallet installed",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )

                    // Error handling removed - using simple connection state
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeedVaultDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?, String) -> Unit // Added network parameter
) {
    var selectedOption by remember { mutableStateOf("connect") } // "connect", "import", "generate"
    var seedPhrase by remember { mutableStateOf("") }
    var showSeedInput by remember { mutableStateOf(false) }
    var selectedNetwork by remember { mutableStateOf("testnet") } // "mainnet", "testnet", "devnet"

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

                // Network Selection
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = "Network",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Solana Network",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Testnet option
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedNetwork = "testnet" }
                            ) {
                                RadioButton(
                                    selected = selectedNetwork == "testnet",
                                    onClick = { selectedNetwork = "testnet" }
                                )
                                Text("Testnet", fontSize = 12.sp)
                            }

                            // Mainnet option
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedNetwork = "mainnet" }
                            ) {
                                RadioButton(
                                    selected = selectedNetwork == "mainnet",
                                    onClick = { selectedNetwork = "mainnet" }
                                )
                                Text("Mainnet", fontSize = 12.sp)
                            }

                            // Devnet option
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedNetwork = "devnet" }
                            ) {
                                RadioButton(
                                    selected = selectedNetwork == "devnet",
                                    onClick = { selectedNetwork = "devnet" }
                                )
                                Text("Devnet", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "⚠️ Make sure this matches your wallet's network setting",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
                        "connect" -> onConfirm(null, selectedNetwork)
                        "import" -> onConfirm(seedPhrase.takeIf { it.isNotBlank() }, selectedNetwork)
                        "generate" -> onConfirm("GENERATE_NEW", selectedNetwork)
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "App Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configure your app preferences and account settings",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}
