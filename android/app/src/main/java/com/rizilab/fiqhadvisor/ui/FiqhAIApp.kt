package com.rizilab.fiqhadvisor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizilab.fiqhadvisor.ui.screens.AnalysisScreen
import com.rizilab.fiqhadvisor.ui.screens.ChatScreen
import com.rizilab.fiqhadvisor.ui.screens.HistoryScreen
import com.rizilab.fiqhadvisor.ui.screens.SettingsScreen
import com.rizilab.fiqhadvisor.ui.viewmodel.FiqhAIViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiqhAIApp(
    modifier: Modifier = Modifier,
    viewModel: FiqhAIViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Analysis", "Chat", "History", "Settings")
    val icons = listOf(
        Icons.Default.Search,
        Icons.Default.Chat,
        Icons.Default.History,
        Icons.Default.Settings
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕌 FiqhAI",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Islamic Token Advisor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AnalysisScreen(viewModel = viewModel)
                1 -> ChatScreen(viewModel = viewModel)
                2 -> HistoryScreen(viewModel = viewModel)
                3 -> SettingsScreen(viewModel = viewModel)
            }
        }

        // Bottom Navigation
        NavigationBar {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    icon = { Icon(icons[index], contentDescription = tab) },
                    label = { Text(tab) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }
    }
} 