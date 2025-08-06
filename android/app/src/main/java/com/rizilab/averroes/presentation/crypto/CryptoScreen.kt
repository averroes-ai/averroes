package com.rizilab.averroes.presentation.crypto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizilab.averroes.data.model.HalalCrypto
import com.rizilab.averroes.data.model.HalalStatus
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoScreen(
    viewModel: CryptoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val filteredCryptos = remember(state) { viewModel.getFilteredCryptos() }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Halal Crypto",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredCryptos.size} cryptocurrencies",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Search and Filters
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.handleIntent(CryptoIntent.SearchCryptos(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search cryptocurrencies...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Filters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { viewModel.handleIntent(CryptoIntent.FilterByStatus(null)) },
                            label = { Text("All") },
                            selected = state.selectedFilter == null
                        )
                    }
                    items(HalalStatus.values()) { status ->
                        FilterChip(
                            onClick = { viewModel.handleIntent(CryptoIntent.FilterByStatus(status)) },
                            label = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            selected = state.selectedFilter == status,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (status) {
                                    HalalStatus.HALAL -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    HalalStatus.HARAM -> Color(0xFFF44336).copy(alpha = 0.2f)
                                    HalalStatus.QUESTIONABLE -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    HalalStatus.UNDER_REVIEW -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                                }
                            )
                        )
                    }
                }
            }
        }

        // Content
        Box(
            modifier = Modifier.weight(1f)
        ) {
            when {
                state.cryptos.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading halal cryptocurrencies...",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                state.cryptos.hasError -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.cryptos.error ?: "Unknown error",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.handleIntent(CryptoIntent.LoadCryptos) }
                        ) {
                            Text("Retry")
                        }
                    }
                }
                
                filteredCryptos.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No cryptocurrencies found",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        if (state.searchQuery.isNotBlank() || state.selectedFilter != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try adjusting your search or filters",
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCryptos) { crypto ->
                            CryptoCard(
                                crypto = crypto,
                                onClick = { viewModel.handleIntent(CryptoIntent.SelectCrypto(crypto)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CryptoCard(
    crypto: HalalCrypto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = crypto.rank.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Crypto Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = crypto.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = crypto.symbol,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Halal Status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (crypto.halalStatus) {
                                    HalalStatus.HALAL -> Color(0xFF4CAF50)
                                    HalalStatus.HARAM -> Color(0xFFF44336)
                                    HalalStatus.QUESTIONABLE -> Color(0xFFFF9800)
                                    HalalStatus.UNDER_REVIEW -> Color(0xFF9C27B0)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (crypto.halalStatus) {
                            HalalStatus.HALAL -> "Halal"
                            HalalStatus.HARAM -> "Haram"
                            HalalStatus.QUESTIONABLE -> "Questionable"
                            HalalStatus.UNDER_REVIEW -> "Under Review"
                        },
                        fontSize = 12.sp,
                        color = when (crypto.halalStatus) {
                            HalalStatus.HALAL -> Color(0xFF4CAF50)
                            HalalStatus.HARAM -> Color(0xFFF44336)
                            HalalStatus.QUESTIONABLE -> Color(0xFFFF9800)
                            HalalStatus.UNDER_REVIEW -> Color(0xFF9C27B0)
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Price Info
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatPrice(crypto.currentPrice),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (crypto.priceChange24h >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (crypto.priceChange24h >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (crypto.priceChange24h >= 0) "+" else ""}${String.format("%.2f", crypto.priceChange24h)}%",
                        fontSize = 12.sp,
                        color = if (crypto.priceChange24h >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    return when {
        price >= 1000 -> formatter.format(price)
        price >= 1 -> String.format("$%.2f", price)
        else -> String.format("$%.4f", price)
    }
}
