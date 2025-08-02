package com.rizilab.fiqhadvisor.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToAuth: () -> Unit = {}, onNavigateToHome: () -> Unit = {}) {
    var startAnimation by remember { mutableStateOf(false) }

    // Animation values
    val alphaAnimation by
            animateFloatAsState(
                    targetValue = if (startAnimation) 1f else 0f,
                    animationSpec = tween(1000),
                    label = "alpha"
            )

    val scaleAnimation by
            animateFloatAsState(
                    targetValue = if (startAnimation) 1f else 0.3f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                    label = "scale"
            )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // Show splash for 2.5 seconds
        onNavigateToAuth() // Navigate to authentication
    }

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    Color(0xFF1E3A8A), // Deep blue
                                                                    Color(
                                                                            0xFF3B82F6
                                                                    ), // Bright blue
                                                                    Color(0xFF60A5FA) // Light blue
                                                            )
                                            )
                            ),
            contentAlignment = Alignment.Center
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(alphaAnimation).scale(scaleAnimation)
        ) {
            // App Logo/Icon placeholder
            Card(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.2f)
                            ),
                    elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                            text = "🕌",
                            fontSize = 48.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Title
            Text(
                    text = "FiqhAdvisor",
                    fontSize = 32.sp,
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

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator
            CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
            )
        }

        // Footer
        Text(
                text = "Powered by AI & Islamic Scholarship",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
        )
    }
}
