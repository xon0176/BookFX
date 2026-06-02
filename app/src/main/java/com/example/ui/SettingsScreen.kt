package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.TradeViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(viewModel: TradeViewModel, modifier: Modifier = Modifier) {
    if (viewModel.isViewingSettingsProfile) {
        MyProfileScreen(viewModel = viewModel, modifier = modifier)
    } else {
        SettingsMainContent(
            viewModel = viewModel,
            onNavigateToProfile = { viewModel.isViewingSettingsProfile = true },
            modifier = modifier
        )
    }
}

@Composable
fun MyProfileScreen(viewModel: TradeViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val user = viewModel.currentUser
    val userEmail = user?.email ?: "ctkartik47@gmail.com"

    var profileName by remember { mutableStateOf(user?.name ?: "Kartik") }
    var profileCountry by remember { mutableStateOf(user?.country ?: "India") }
    var profileInstrument by remember { mutableStateOf(user?.primaryInstrument ?: "Equity") }

    var newPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var countryExpanded by remember { mutableStateOf(false) }
    var instrumentExpanded by remember { mutableStateOf(false) }

    val countriesList = listOf("India", "United States", "United Kingdom", "Germany", "Australia", "Canada", "Singapore", "Japan", "United Arab Emirates")
    val instrumentsList = listOf("Equity", "Commodities", "Forex", "Crypto", "Indices")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- EMAIL ---
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Email",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            OutlinedTextField(
                value = userEmail,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE2E8F0),
                    disabledContainerColor = if (isSystemInDarkMode) Color(0xFF1F2937) else Color(0xFFFFFFFF),
                    disabledTextColor = TextPrimary,
                    disabledLeadingIconColor = TextSecondary
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
            )
        }

        // --- PASSWORD ---
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Change Password (Leave blank to keep unchanged)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = { Text("Enter new password", color = TextSecondary, fontSize = 15.sp) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    val icon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Toggle password visibility",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E6FF2),
                    unfocusedBorderColor = if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE2E8F0),
                    focusedContainerColor = if (isSystemInDarkMode) Color(0xFF1F2937) else Color(0xFFFFFFFF),
                    unfocusedContainerColor = if (isSystemInDarkMode) Color(0xFF111827) else Color(0xFFFFFFFF),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLeadingIconColor = TextSecondary,
                    unfocusedLeadingIconColor = TextSecondary,
                    focusedTrailingIconColor = TextSecondary,
                    unfocusedTrailingIconColor = TextSecondary
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
            )
        }

        // --- FULL NAME ---
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Full Name",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                placeholder = { Text("E.g. Kartik C T", color = TextSecondary, fontSize = 15.sp) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E6FF2),
                    unfocusedBorderColor = if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE2E8F0),
                    focusedContainerColor = if (isSystemInDarkMode) Color(0xFF1F2937) else Color(0xFFFFFFFF),
                    unfocusedContainerColor = if (isSystemInDarkMode) Color(0xFF111827) else Color(0xFFFFFFFF),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLeadingIconColor = TextSecondary,
                    unfocusedLeadingIconColor = TextSecondary
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
            )
        }

        // --- COUNTRY ---
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Country",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = profileCountry,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE2E8F0),
                        disabledContainerColor = if (isSystemInDarkMode) Color(0xFF111827) else Color(0xFFFFFFFF),
                        disabledTextColor = TextPrimary,
                        disabledLeadingIconColor = TextSecondary,
                        disabledTrailingIconColor = TextSecondary
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { countryExpanded = true }
                )
                
                DropdownMenu(
                    expanded = countryExpanded,
                    onDismissRequest = { countryExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    countriesList.forEach { country ->
                        DropdownMenuItem(
                            text = { Text(country, fontSize = 15.sp) },
                            onClick = {
                                profileCountry = country
                                countryExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // --- TRADING INSTRUMENT ---
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Trading Instrument",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = profileInstrument,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE2E8F0),
                        disabledContainerColor = if (isSystemInDarkMode) Color(0xFF111827) else Color(0xFFFFFFFF),
                        disabledTextColor = TextPrimary,
                        disabledLeadingIconColor = TextSecondary,
                        disabledTrailingIconColor = TextSecondary
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { instrumentExpanded = true }
                )
                
                DropdownMenu(
                    expanded = instrumentExpanded,
                    onDismissRequest = { instrumentExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    instrumentsList.forEach { instr ->
                        DropdownMenuItem(
                            text = { Text(instr, fontSize = 15.sp) },
                            onClick = {
                                profileInstrument = instr
                                instrumentExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- SAVE CHANGES BUTTON ---
        Button(
            onClick = {
                val current = viewModel.currentUser
                if (current != null) {
                    val updatedPassword = if (newPassword.isNotBlank()) newPassword else current.passwordHash
                    val updated = current.copy(
                        name = profileName.trim(),
                        country = profileCountry.trim(),
                        primaryInstrument = profileInstrument.trim(),
                        passwordHash = updatedPassword
                    )
                    viewModel.handleUpdateUser(updated)
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6FF2))
        ) {
            Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun SettingsMainContent(
    viewModel: TradeViewModel,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = viewModel.currentUser
    val userEmail = user?.email ?: "ctkartik47@gmail.com"
    val userFirstLetter = userEmail.getOrNull(0)?.uppercaseChar()?.toString() ?: "K"

    var showAboutDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. ACCOUNT SECTION ---
        Text(
            text = "Account",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToProfile() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE9F0FE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userFirstLetter,
                        color = Color(0xFF2E6FF2),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userEmail,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Manage Profile & Security",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Manage Profile",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // --- 2. SESSION & SAFETY SECTION ---
        Text(
            text = "Session & Safety",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Sign Out row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.handleLogout() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFCE8E6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = Color(0xFFC5221F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Sign Out", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5221F))
                        Text(text = "Exit current device log session safely", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFEEEEEE)
                )

                // Clear All Data row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showConfirmDeleteDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFCE8E6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All Data",
                            tint = Color(0xFFC5221F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Clear All Data", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5221F))
                        Text(text = "Permanently remove all data", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // --- 3. SUPPORT SECTION ---
        Text(
            text = "Support & Actions",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Suggest Feature Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://t.me/xon0176")
                            )
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open Telegram: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFFDE7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Suggest Feature",
                            tint = Color(0xFFFBC02D),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Suggest Feature", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Recommend features directly to developers via Telegram", fontSize = 11.sp, color = TextSecondary)
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Suggest details",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFEEEEEE)
                )

                // About App row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFECEFF1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = Color(0xFF455A64),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "About App", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "App details & developer details", fontSize = 11.sp, color = TextSecondary)
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "About details",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Build version info centered
        Text(
            text = "Version 1.0.0",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        )
    }

    // Confirm Clear All Data dialog
    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Clear All Data?", fontWeight = FontWeight.Bold, color = Color(0xFFC5221F)) },
            text = {
                Text("This will permanently delete all portfolios, trades, and mistakes from this device. This action cannot be undone.", fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteDialog = false
                        viewModel.handleDeleteAccount()
                        Toast.makeText(context, "All data has been cleared successfully.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5221F))
                ) {
                    Text("Clear All Data", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // About App Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About BookFx", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("BookFx Journal is a professional local companion database designed to empower individual retail traders to track statistics in real-time, tag and remember mistakes, and visual metrics.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Built on SQLite Room Database with Material 3 guidelines.", fontSize = 12.sp, color = TextSecondary)
                    Text("Developed for high performance and strict zero-collection offline-first data safety.", fontSize = 12.sp, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6FF2))
                ) {
                    Text("Close")
                }
            }
        )
    }
}
