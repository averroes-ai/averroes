package com.rizilab.fiqhadvisor.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizilab.fiqhadvisor.ui.components.FiqhButton
import com.rizilab.fiqhadvisor.ui.components.FiqhCard
import com.rizilab.fiqhadvisor.ui.theme.FiqhColors
import com.rizilab.fiqhadvisor.ui.theme.FiqhTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onNavigateToChat: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FiqhColors.Background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top spacing for status bar
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header
        Text(
            text = "🕌",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Text(
            text = if (isLoginMode) "Welcome Back" else "Join Fiqh Advisor",
            style = FiqhTypography.Heading1.copy(fontSize = 24.sp),
            color = FiqhColors.Primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = if (isLoginMode) 
                "Sign in to continue your Islamic investment journey" 
            else 
                "Create your account to start analyzing cryptocurrencies",
            style = FiqhTypography.Body1.copy(fontSize = 14.sp),
            color = FiqhColors.OnBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        
        // Auth Form
        FiqhCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name field (only for registration)
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FiqhColors.Primary,
                            focusedLabelColor = FiqhColors.Primary
                        ),
                        singleLine = true
                    )
                }
                
                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FiqhColors.Primary,
                        focusedLabelColor = FiqhColors.Primary
                    ),
                    singleLine = true
                )
                
                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { if (!isLoginMode) focusManager.moveFocus(FocusDirection.Down) },
                        onDone = { handleAuth(email, password, name, isLoginMode, onNavigateToChat, { isLoading = it }, { errorMessage = it }) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FiqhColors.Primary,
                        focusedLabelColor = FiqhColors.Primary
                    ),
                    singleLine = true
                )
                
                // Confirm Password field (only for registration)
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleAuth(email, password, name, isLoginMode, onNavigateToChat, { isLoading = it }, { errorMessage = it }) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FiqhColors.Primary,
                            focusedLabelColor = FiqhColors.Primary
                        ),
                        singleLine = true
                    )
                }
                
                // Error message
                if (errorMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = FiqhColors.Error.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = FiqhColors.Error,
                            style = FiqhTypography.Body2,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // Auth Button
                FiqhButton(
                    onClick = {
                        handleAuth(email, password, name, isLoginMode, onNavigateToChat, { isLoading = it }, { errorMessage = it })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && 
                            (isLoginMode || (name.isNotBlank() && confirmPassword == password))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isLoginMode) "Sign In" else "Create Account",
                        style = FiqhTypography.Body1.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Switch between login and register
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isLoginMode) "Don't have an account?" else "Already have an account?",
                style = FiqhTypography.Body2,
                color = FiqhColors.OnBackground.copy(alpha = 0.7f)
            )
            TextButton(
                onClick = { 
                    isLoginMode = !isLoginMode
                    errorMessage = ""
                }
            ) {
                Text(
                    text = if (isLoginMode) "Sign Up" else "Sign In",
                    style = FiqhTypography.Body2.copy(fontWeight = FontWeight.Medium),
                    color = FiqhColors.Primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Terms and Privacy
        Text(
            text = "By continuing, you agree to our Terms of Service and Privacy Policy",
            style = FiqhTypography.Caption,
            color = FiqhColors.OnBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Bottom spacing to ensure content is always visible
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun handleAuth(
    email: String,
    password: String,
    name: String,
    isLoginMode: Boolean,
    onNavigateToChat: () -> Unit,
    setLoading: (Boolean) -> Unit,
    setError: (String) -> Unit
) {
    setLoading(true)
    setError("")
    
    // Basic validation
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        setError("Please enter a valid email address")
        setLoading(false)
        return
    }
    
    if (password.length < 6) {
        setError("Password must be at least 6 characters")
        setLoading(false)
        return
    }
    
    if (!isLoginMode && name.isBlank()) {
        setError("Please enter your full name")
        setLoading(false)
        return
    }
    
    // Simulate API call
    // TODO: Implement actual authentication with your backend
    CoroutineScope(Dispatchers.Main).launch {
        delay(2000) // Simulate network call
        
        // For now, just navigate to chat (in real app, handle auth result)
        setLoading(false)
        onNavigateToChat()
    }
}