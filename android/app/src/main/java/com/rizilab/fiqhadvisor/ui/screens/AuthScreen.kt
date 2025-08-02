package com.rizilab.fiqhadvisor.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AuthMode {
    LOGIN,
    SIGNUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit = {}, onSkipAuth: () -> Unit = {}) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    Color(0xFF1E3A8A),
                                                                    Color(0xFF3B82F6),
                                                                    Color(0xFF60A5FA)
                                                            )
                                            )
                            )
                            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Card(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.2f)
                            ),
                    elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "🕌", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = "FiqhAdvisor",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
            )

            Text(
                    text =
                            if (authMode == AuthMode.LOGIN) "Welcome back"
                            else "Create your account",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Auth Form
        Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Toggle
                Row(
                        modifier =
                                Modifier.background(
                                                Color.Gray.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                        )
                                        .padding(4.dp)
                ) {
                    listOf(AuthMode.LOGIN to "Login", AuthMode.SIGNUP to "Sign Up").forEach {
                            (mode, text) ->
                        Box(
                                modifier =
                                        Modifier.weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                        if (authMode == mode) Color(0xFF3B82F6)
                                                        else Color.Transparent
                                                )
                                                .clickable { authMode = mode }
                                                .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    text = text,
                                    color = if (authMode == mode) Color.White else Color.Gray,
                                    fontWeight =
                                            if (authMode == mode) FontWeight.Medium
                                            else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Form Fields
                AnimatedVisibility(
                        visible = authMode == AuthMode.SIGNUP,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                ) {
                    Column {
                        OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF3B82F6)
                                        )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3B82F6)
                                )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                        if (isPasswordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription =
                                                if (isPasswordVisible) "Hide password"
                                                else "Show password"
                                )
                            }
                        },
                        visualTransformation =
                                if (isPasswordVisible) VisualTransformation.None
                                else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3B82F6)
                                )
                )

                AnimatedVisibility(
                        visible = authMode == AuthMode.SIGNUP,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF3B82F6)
                                        )
                        )
                    }
                }

                // Error Message
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Auth Button
                Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null

                            // Simple validation
                            when {
                                email.isBlank() -> errorMessage = "Email is required"
                                password.isBlank() -> errorMessage = "Password is required"
                                authMode == AuthMode.SIGNUP && fullName.isBlank() ->
                                        errorMessage = "Full name is required"
                                authMode == AuthMode.SIGNUP && password != confirmPassword ->
                                        errorMessage = "Passwords don't match"
                                else -> {
                                    // Simulate auth success
                                    onAuthSuccess()
                                }
                            }
                            isLoading = false
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                                text =
                                        if (authMode == AuthMode.LOGIN) "Login"
                                        else "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skip Auth Button
                TextButton(onClick = onSkipAuth, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Continue as Guest", color = Color(0xFF6B7280), fontSize = 14.sp)
                }

                if (authMode == AuthMode.LOGIN) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { /* Handle forgot password */}) {
                        Text(text = "Forgot Password?", color = Color(0xFF3B82F6), fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Terms and Privacy
        Text(
                text = "By continuing, you agree to our Terms of Service and Privacy Policy",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
        )
    }
}
