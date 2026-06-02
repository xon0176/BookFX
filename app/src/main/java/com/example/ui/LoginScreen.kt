package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.TradeViewModel

@Composable
fun LoginScreen(viewModel: TradeViewModel, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }

    // Consistent color tokens matching the OnboardingScreen
    val lightBg = Color(0xFFFFFFFF)
    val lightFieldBg = Color(0xFFF1F3F9)
    val darkText = Color(0xFF111827) // Slate 900
    val secondaryText = Color(0xFF8F9BB3) // Light grey blue
    val primaryBlue = Color(0xFF2E6FF2) // Royal blue progress & buttons

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = lightBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Close Button Left
            IconButton(
                onClick = { viewModel.navigateToRegister() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = darkText
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Candlestick logo
            CandlestickLogo(modifier = Modifier.size(width = 24.dp, height = 32.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = darkText
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Log in to your BookFx account to resume tracking your performance.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = secondaryText
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Text Fields Form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Email Address field
                OutlinedTextField(
                    value = viewModel.loginEmailInput,
                    onValueChange = { viewModel.loginEmailInput = it },
                    placeholder = { Text("Email Address", color = secondaryText) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "Email Icon",
                            tint = secondaryText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = lightFieldBg,
                        unfocusedContainerColor = lightFieldBg,
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = darkText,
                        unfocusedTextColor = darkText,
                        cursorColor = primaryBlue
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                // Password field
                OutlinedTextField(
                    value = viewModel.loginPasswordInput,
                    onValueChange = { viewModel.loginPasswordInput = it },
                    placeholder = { Text("Password", color = secondaryText) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Password Icon",
                            tint = secondaryText
                        )
                    },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = "Toggle Visibility", tint = secondaryText)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = lightFieldBg,
                        unfocusedContainerColor = lightFieldBg,
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = darkText,
                        unfocusedTextColor = darkText,
                        cursorColor = primaryBlue
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            // Error Display Banner
            viewModel.authError?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = RedAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = RedAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
            Spacer(modifier = Modifier.weight(1f))

            // Login Button (big, blue, filled)
            Button(
                onClick = { viewModel.handleLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text(
                    text = "Log In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer link
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row {
                    Text(text = "Don't have an account? ", color = secondaryText, fontSize = 14.sp)
                    Text(
                        text = "Sign Up",
                        color = primaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { viewModel.navigateToRegister() }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
