package com.rizilab.averroes.presentation.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Collect effects
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SplashEffect.NavigateToAuth -> onNavigateToAuth()
                is SplashEffect.NavigateToMain -> onNavigateToMain()
                is SplashEffect.ShowError -> {
                    // Handle error - could show a snackbar or dialog
                }
            }
        }
    }

    // Animated alpha for fade-in effect
    val alpha by animateFloatAsState(
        targetValue = if (state.isLoading) 1f else 0f,
        animationSpec = tween(1000),
        label = "splash_alpha"
    )

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = state.loadingProgress,
        animationSpec = tween(500),
        label = "progress_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Deep blue
                        Color(0xFF3B82F6), // Blue
                        Color(0xFF60A5FA)  // Light blue
                    )
                )
            )
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo/Icon
            Card(
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🕌",
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = "Averroes",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Islamic Finance AI Assistant",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Loading Progress
            if (state.isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .width(200.dp)
                            .height(4.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Loading Message
                    Text(
                        text = state.loadingMessage,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Percentage
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Powered by Islamic Finance AI",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Built with ❤️ for the Ummah",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}
