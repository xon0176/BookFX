package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun CandlestickLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Candlestick (tall, body in middle)
        androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 8.dp, height = 32.dp)) {
            val wickWidth = 2.dp.toPx()
            val wickHeight = size.height
            val bodyWidth = size.width
            val bodyHeight = size.height * 0.6f
            
            // wick
            drawRect(
                color = Primary,
                topLeft = androidx.compose.ui.geometry.Offset((size.width - wickWidth)/2, 0f),
                size = androidx.compose.ui.geometry.Size(wickWidth, wickHeight)
            )
            // body
            drawRect(
                color = Primary,
                topLeft = androidx.compose.ui.geometry.Offset(0f, (size.height - bodyHeight)/2),
                size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight)
            )
        }
        // Right Candlestick (shorter, body offset upwards)
        androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 8.dp, height = 32.dp)) {
            val wickWidth = 2.dp.toPx()
            val wickHeight = size.height
            val bodyWidth = size.width
            val bodyHeight = size.height * 0.5f
            
            // wick
            drawRect(
                color = Primary,
                topLeft = androidx.compose.ui.geometry.Offset((size.width - wickWidth)/2, 0f),
                size = androidx.compose.ui.geometry.Size(wickWidth, wickHeight)
            )
            // body offset
            drawRect(
                color = Primary,
                topLeft = androidx.compose.ui.geometry.Offset(0f, (size.height - bodyHeight)/3),
                size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight)
            )
        }
    }
}

@Composable
fun OnboardingScreen(viewModel: TradeViewModel, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Dropdown expanded states
    var countryExpanded by remember { mutableStateOf(false) }
    var instrumentExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    val countryList = listOf("India", "United States", "United Kingdom", "Singapore", "Australia", "Canada", "Germany", "UAE")
    val instrumentList = listOf("Equity", "Forex", "Futures & Options", "Crypto", "Commodities", "Indices", "Currencies")
    val currencyList = listOf("USD", "INR", "EUR", "GBP", "AUD")

    // Force light-theme colors specifically for onboarding slides as seen in screenshots
    val lightBg = Color(0xFFFFFFFF)
    val lightFieldBg = Color(0xFFF1F3F9)
    val darkText = Color(0xFF111827) // Slate 900
    val secondaryText = Color(0xFF8F9BB3) // Light grey blue
    val primaryBlue = Color(0xFF2E6FF2) // Royal blue progress & buttons
    val lightGrayTrack = Color(0xFFE9ECEF) // Progress bar track

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
            
            // Top Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (viewModel.onboardingStep > 1) {
                            viewModel.onboardingStep--
                            viewModel.authError = null
                        } else {
                            viewModel.navigateToLogin()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = darkText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step Indicator Bar
            val progressVal = when (viewModel.onboardingStep) {
                1 -> 0.33f
                2 -> 0.66f
                else -> 1.0f
            }
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = primaryBlue,
                trackColor = lightGrayTrack
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Display proper Icon based on Onboarding step to match specs exactly
            when (viewModel.onboardingStep) {
                1 -> {
                    // Logo representation
                    CandlestickLogo(modifier = Modifier.size(width = 24.dp, height = 32.dp))
                }
                2 -> {
                    // Blue Profile Outline Icon
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "About You",
                        tint = primaryBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
                3 -> {
                    // Sliders/Tuning Icon
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Preferences",
                        tint = primaryBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Titles & Subtitles matching image mockups
            val stepTitle = when (viewModel.onboardingStep) {
                1 -> "Welcome to BookFx"
                2 -> "Tell us about yourself"
                else -> "Trading Preferences"
            }

            val stepSubtitle = when (viewModel.onboardingStep) {
                1 -> "Create an account to backup your trades and sync across devices."
                2 -> "Your name will be used to personalize your trading account profile."
                else -> "Set up your default analytics. You can change these later in settings."
            }

            Text(
                text = stepTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = darkText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stepSubtitle,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = secondaryText
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Conditional Inputs representation based on the active step
            when (viewModel.onboardingStep) {
                1 -> {
                    // STEP 1 CONTENT: Email, Password, Confirm Password
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.emailInput,
                            onValueChange = { viewModel.emailInput = it },
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

                        OutlinedTextField(
                            value = viewModel.passwordInput,
                            onValueChange = { viewModel.passwordInput = it },
                            placeholder = { Text("Password", color = secondaryText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Password Icon",
                                    tint = secondaryText
                                )
                            },
                            trailingIcon = {
                                val visIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = visIcon, contentDescription = "Toggle Visibility", tint = secondaryText)
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

                        OutlinedTextField(
                            value = viewModel.confirmPasswordInput,
                            onValueChange = { viewModel.confirmPasswordInput = it },
                            placeholder = { Text("Confirm Password", color = secondaryText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Confirm Icon",
                                    tint = secondaryText
                                )
                            },
                            trailingIcon = {
                                val visIcon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(imageVector = visIcon, contentDescription = "Toggle Visibility", tint = secondaryText)
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                }
                2 -> {
                    // STEP 2 CONTENT (Image 1): Tell us about yourself
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.nameInput,
                            onValueChange = { viewModel.nameInput = it },
                            placeholder = { Text("Full Name", color = secondaryText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AssignmentInd,
                                    contentDescription = "Full Name Icon",
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
                            singleLine = true
                        )
                    }
                }
                3 -> {
                    // STEP 3 CONTENT (Image 2 & 3): Trading Preferences
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Country Dropdown Box
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = viewModel.countryInput,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Country", color = secondaryText) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = "Country Icon",
                                        tint = secondaryText
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown Arrow",
                                        tint = secondaryText,
                                        modifier = Modifier.clickable { countryExpanded = true }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { countryExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = lightFieldBg,
                                    unfocusedContainerColor = lightFieldBg,
                                    focusedBorderColor = primaryBlue,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = darkText,
                                    unfocusedTextColor = darkText
                                )
                            )

                            DropdownMenu(
                                expanded = countryExpanded,
                                onDismissRequest = { countryExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(lightBg)
                            ) {
                                countryList.forEach { countryName ->
                                    DropdownMenuItem(
                                        text = { Text(countryName, color = darkText) },
                                        onClick = {
                                            viewModel.countryInput = countryName
                                            countryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Primary Instrument Dropdown Box
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = viewModel.primaryInstrumentInput,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Primary Instrument", color = secondaryText) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = "Instrument Icon",
                                        tint = secondaryText
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown Arrow",
                                        tint = secondaryText,
                                        modifier = Modifier.clickable { instrumentExpanded = true }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { instrumentExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = lightFieldBg,
                                    unfocusedContainerColor = lightFieldBg,
                                    focusedBorderColor = primaryBlue,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = darkText,
                                    unfocusedTextColor = darkText
                                )
                            )

                            DropdownMenu(
                                expanded = instrumentExpanded,
                                onDismissRequest = { instrumentExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(lightBg)
                            ) {
                                instrumentList.forEach { instrumentName ->
                                    DropdownMenuItem(
                                        text = { Text(instrumentName, color = darkText) },
                                        onClick = {
                                            viewModel.primaryInstrumentInput = instrumentName
                                            instrumentExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Dual Row: Base Capital and Currency Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Base Capital field
                            OutlinedTextField(
                                value = viewModel.baseCapitalInput,
                                onValueChange = { viewModel.baseCapitalInput = it },
                                placeholder = { Text("Base Capital", color = secondaryText) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Capital Icon",
                                        tint = secondaryText
                                    )
                                },
                                modifier = Modifier.weight(1.5f),
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
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            // Currency dropdown field
                            Box(modifier = Modifier.weight(1.0f)) {
                                OutlinedTextField(
                                    value = viewModel.currencyInput,
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("Currency", color = secondaryText) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.AttachMoney,
                                            contentDescription = "Currency Icon",
                                            tint = secondaryText
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown Arrow",
                                            tint = secondaryText,
                                            modifier = Modifier.clickable { currencyExpanded = true }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currencyExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = lightFieldBg,
                                        unfocusedContainerColor = lightFieldBg,
                                        focusedBorderColor = primaryBlue,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = darkText,
                                        unfocusedTextColor = darkText
                                    )
                                )

                                DropdownMenu(
                                    expanded = currencyExpanded,
                                    onDismissRequest = { currencyExpanded = false },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(lightBg)
                                ) {
                                    currencyList.forEach { currencySym ->
                                        DropdownMenuItem(
                                            text = { Text(currencySym, color = darkText) },
                                            onClick = {
                                                viewModel.currencyInput = currencySym
                                                currencyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error Display Banner
            viewModel.authError?.let { errorName ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = RedAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorName,
                        color = RedAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
            Spacer(modifier = Modifier.weight(1f))

            // Blue Button at footer
            val buttonLabel = if (viewModel.onboardingStep < 3) "Continue" else "Complete Setup"
            Button(
                onClick = {
                    when (viewModel.onboardingStep) {
                        1 -> {
                            if (viewModel.validateStep1()) {
                                viewModel.onboardingStep = 2
                            }
                        }
                        2 -> {
                            if (viewModel.validateStep2()) {
                                viewModel.onboardingStep = 3
                            }
                        }
                        3 -> {
                            if (viewModel.validateStep3()) {
                                viewModel.handleRegister()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text(
                    text = buttonLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Slide 1 bottom login navigation helper link
            if (viewModel.onboardingStep == 1) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row {
                        Text(text = "Already have an account? ", color = secondaryText, fontSize = 14.sp)
                        Text(
                            text = "Log In",
                            color = primaryBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { viewModel.navigateToLogin() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
