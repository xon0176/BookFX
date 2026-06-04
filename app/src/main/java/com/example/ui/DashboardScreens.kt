package com.example.ui

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF as AndroidRectF
import android.graphics.Typeface as AndroidTypeface
import android.graphics.Path as AndroidPath
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Trade
import com.example.ui.theme.*
import com.example.viewmodel.TradeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManageScreen(viewModel: TradeViewModel) {
    val scrollState = rememberScrollState()
    val portfolios by viewModel.allPortfolios.collectAsState()
    val trades by viewModel.allTrades.collectAsState()
    val activePortfolio = viewModel.activePortfolio

    // Expand/Collapse create portfolio card state
    var showCreatePortfolioForm by remember { mutableStateOf(false) }
    var editingPortfolioId by remember { mutableStateOf<Int?>(null) }
    var portfolioToDelete by remember { mutableStateOf<com.example.data.PortfolioAccount?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (!showCreatePortfolioForm) {
            // 1. Portfolio Accounts Monitor Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.currentMainTab = "DASHBOARD" },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Portfolio Accounts Manager",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${portfolios.size} Portfolio Entities Configured",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                // Button to toggle create portfolio form
                Button(
                    onClick = {
                        editingPortfolioId = null
                        viewModel.portfolioNameInput = ""
                        viewModel.portfolioBrokerInput = ""
                        viewModel.portfolioStartingEquityInput = "10000.00"
                        viewModel.portfolioTypeInput = "Live Cash"
                        viewModel.portfolioCurrencyInput = "USD"
                        viewModel.portfolioDescInput = ""
                        showCreatePortfolioForm = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFEEF2FF),
                        contentColor = if (isSystemInDarkMode) Color(0xFF60A5FA) else Color(0xFF2E6FF2)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Toggle add account",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Account",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Render Portfolios List Card
            portfolios.forEach { portfolio ->
            val isActive = activePortfolio?.id == portfolio.id
            val pTrades = trades.filter { it.portfolioId == portfolio.id }
            val pProfit = pTrades.sumOf { it.profit }
            val pBroker = pTrades.sumOf { it.brokerage }
            val currentActiveEquity = portfolio.startingEquity + pProfit - pBroker
            val plDiff = currentActiveEquity - portfolio.startingEquity
            val plPercent = if (portfolio.startingEquity > 0) (plDiff / portfolio.startingEquity) * 100.0 else 0.0
            val isProfit = plDiff >= 0

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) (if (isSystemInDarkMode) Color(0xFF60A5FA) else Color(0xFF2E6FF2)) else (if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFF1F3F9))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.activePortfolio = portfolio }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top name & type badge row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = portfolio.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSystemInDarkMode) Color(0xFF047857).copy(alpha = 0.2f) else Color(0xFFE6F4EA))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = if (isSystemInDarkMode) Color(0xFF4ADE80) else Color(0xFF137333),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Broker: ${portfolio.broker}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Account Type Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (portfolio.type) {
                                        "Live Cash" -> if (isSystemInDarkMode) Color(0xFFB45309).copy(alpha = 0.2f) else Color(0xFFFFF7E6)
                                        "Prop Firm" -> if (isSystemInDarkMode) Color(0xFF4338CA).copy(alpha = 0.2f) else Color(0xFFEEF2FF)
                                        else -> if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFF3F4F6)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = portfolio.type.uppercase(),
                                color = when (portfolio.type) {
                                    "Live Cash" -> if (isSystemInDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706)
                                    "Prop Firm" -> if (isSystemInDarkMode) Color(0xFF818CF8) else Color(0xFF4F46E5)
                                    else -> TextSecondary
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (portfolio.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = portfolio.description,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats Dashboard Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSystemInDarkMode) Color(0xFF111827) else Color(0xFFF8FAFC))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Starting Capital", fontSize = 10.sp, color = TextSecondary)
                            Text("$${String.format("%.2f", portfolio.startingEquity)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column {
                            Text("Current Equity", fontSize = 10.sp, color = TextSecondary)
                            Text("$${String.format("%.2f", currentActiveEquity)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total P/L", fontSize = 10.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isProfit) "↑" else "↓",
                                    color = if (isProfit) GreenAccent else RedAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                                Text(
                                    text = "${if (isProfit) "+" else ""}${String.format("%.1f", plPercent)}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProfit) GreenAccent else RedAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${pTrades.size} journals logged",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Delete Button option for every account
                            TextButton(
                                onClick = { portfolioToDelete = portfolio },
                                colors = ButtonDefaults.textButtonColors(contentColor = RedAccent),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Account", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Edit Button option for every account
                            TextButton(
                                onClick = {
                                    editingPortfolioId = portfolio.id
                                    viewModel.portfolioNameInput = portfolio.name
                                    viewModel.portfolioBrokerInput = portfolio.broker
                                    viewModel.portfolioStartingEquityInput = portfolio.startingEquity.toString()
                                    viewModel.portfolioTypeInput = portfolio.type
                                    viewModel.portfolioCurrencyInput = portfolio.currency
                                    viewModel.portfolioDescInput = portfolio.description
                                    showCreatePortfolioForm = true
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = if (isSystemInDarkMode) Color(0xFF60A5FA) else Color(0xFF2E6FF2)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Account", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (!isActive) {
                                TextButton(
                                    onClick = { viewModel.activePortfolio = portfolio },
                                    colors = ButtonDefaults.textButtonColors(contentColor = if (isSystemInDarkMode) Color(0xFF60A5FA) else Color(0xFF2E6FF2)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Set Active", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // 2. Add / Edit Portfolio Card Form
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                showCreatePortfolioForm = false
                                editingPortfolioId = null
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editingPortfolioId != null) "Edit Portfolio Account" else "Add Custom Portfolio Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (editingPortfolioId != null) "Update your stats, balances, and configurations for this account." else "Separate stats, balances, and journals by configuring a new custom portfolio entity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Account Name
                    OutlinedTextField(
                        value = viewModel.portfolioNameInput,
                        onValueChange = { viewModel.portfolioNameInput = it },
                        label = { Text("Account / Portfolio Name") },
                        placeholder = { Text("e.g. My Swing Project, Prop $50k Challenge") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = Primary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Broker Name & Starting Balance Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.portfolioBrokerInput,
                            onValueChange = { viewModel.portfolioBrokerInput = it },
                            label = { Text("Broker Name") },
                            placeholder = { Text("e.g. Pepperstone, FTMO") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = Primary
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.portfolioStartingEquityInput,
                            onValueChange = { viewModel.portfolioStartingEquityInput = it },
                            label = { Text("Starting Balance") },
                            prefix = { Text("$", color = TextSecondary) },
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = Primary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Account Type Segment Toggle Selection Buttons
                    Text(
                        text = "Account Type Selection",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSystemInDarkMode) Color(0xFF111827) else Color(0xFFF1F3F9))
                            .border(1.dp, if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Live Cash", "Prop Firm", "Demo").forEach { type ->
                            val isSel = viewModel.portfolioTypeInput == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSel) (if (isSystemInDarkMode) Color(0xFF1F2937) else Color.White) else Color.Transparent)
                                    .clickable { viewModel.portfolioTypeInput = type }
                                    .border(
                                        width = if (isSel) 1.dp else 0.dp,
                                        color = if (isSel) (if (isSystemInDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)) else Color.Transparent,
                                        shape = RoundedCornerShape(9.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) (if (isSystemInDarkMode) Color(0xFF60A5FA) else Color(0xFF2E6FF2)) else TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description text box
                    OutlinedTextField(
                        value = viewModel.portfolioDescInput,
                        onValueChange = { viewModel.portfolioDescInput = it },
                        label = { Text("Account Description / Strategy Goals") },
                        placeholder = { Text("e.g. Swing breakout challenge targeting 10% risk rewards.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = Primary
                        ),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val currentEditId = editingPortfolioId
                            if (currentEditId != null) {
                                viewModel.handleUpdatePortfolio(currentEditId)
                            } else {
                                viewModel.handleAddPortfolio()
                            }
                            showCreatePortfolioForm = false
                            editingPortfolioId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (editingPortfolioId != null) Icons.Default.Save else Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (editingPortfolioId != null) "Save Account Changes" else "Create Account Entity",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 3. Log A Trade Transaction Form has been moved to a dialog opened from the Trading Journal calendar
    }

    portfolioToDelete?.let { portfolio ->
        AlertDialog(
            onDismissRequest = { portfolioToDelete = null },
            title = { Text("Delete Portfolio Account?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${portfolio.name}'? This action will remove the portfolio and all of its logged trading journals, and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleDeletePortfolio(portfolio)
                        portfolioToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("Delete Forever", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { portfolioToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SparklineChart(
    profits: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
    val fillGradient = Brush.verticalGradient(
        colors = listOf(
            lineColor.copy(alpha = 0.2f),
            lineColor.copy(alpha = 0.0f)
        )
    )

    Canvas(modifier = modifier) {
        if (profits.isEmpty()) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }

        var cumulative = 0.0
        val points = mutableListOf<Double>()
        points.add(0.0)
        for (p in profits) {
            cumulative += p
            points.add(cumulative)
        }

        val minPoint = points.minOrNull() ?: 0.0
        val maxPoint = points.maxOrNull() ?: 0.0
        val range = maxPoint - minPoint
        val safeRange = if (range == 0.0) 1.0 else range

        val path = Path()
        val fillPath = Path()
        val stepX = size.width / (points.size - 1)
        
        points.forEachIndexed { index, point ->
            val pct = (point - minPoint) / safeRange
            val x = index * stepX
            val y = size.height - (pct * size.height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        
        if (points.isNotEmpty()) {
            fillPath.lineTo((points.size - 1) * stepX, size.height)
            fillPath.close()
        }

        drawPath(
            path = fillPath,
            brush = fillGradient
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun JournalScreen(viewModel: TradeViewModel) {
    val trades by viewModel.allTrades.collectAsState()
    val mistakes by viewModel.allMistakes.collectAsState()
    val activePortfolio = viewModel.activePortfolio
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    val today = remember { Calendar.getInstance() }
    val weekdayAndDateStr = remember(selectedDate) {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(selectedDate.time)
    }
    var selectedTab by remember { mutableStateOf("Calendar") }
    var showLogTradeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.journalSelectedDateEpochMilli) {
        viewModel.journalSelectedDateEpochMilli?.let { epoch ->
            val cal = Calendar.getInstance().apply { timeInMillis = epoch }
            selectedDate = cal
            viewModel.journalSelectedDateEpochMilli = null
        }
    }

    LaunchedEffect(viewModel.showLogTradeDialogInJournal) {
        if (viewModel.showLogTradeDialogInJournal) {
            showLogTradeDialog = true
            viewModel.showLogTradeDialogInJournal = false
        }
    }
    
    val monthName = remember(selectedDate) {
        selectedDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())?.uppercase() ?: ""
    }
    
    val filteredTrades = remember(trades, selectedDate) {
        val targetYear = selectedDate.get(Calendar.YEAR)
        val targetMonth = selectedDate.get(Calendar.MONTH)
        val targetDay = selectedDate.get(Calendar.DAY_OF_MONTH)
        val cal = Calendar.getInstance()
        trades.filter {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) == targetYear &&
            cal.get(Calendar.MONTH) == targetMonth &&
            cal.get(Calendar.DAY_OF_MONTH) == targetDay
        }
    }

    val daysWithTrades = remember(trades, selectedDate) {
        val targetYear = selectedDate.get(Calendar.YEAR)
        val targetMonth = selectedDate.get(Calendar.MONTH)
        val cal = Calendar.getInstance()
        trades.filter {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) == targetYear &&
            cal.get(Calendar.MONTH) == targetMonth
        }.map {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.DAY_OF_MONTH)
        }.toSet()
    }

    val dailyPlMap = remember(trades, selectedDate) {
        val targetYear = selectedDate.get(Calendar.YEAR)
        val targetMonth = selectedDate.get(Calendar.MONTH)
        val cal = Calendar.getInstance()
        trades.filter {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) == targetYear &&
            cal.get(Calendar.MONTH) == targetMonth
        }.groupBy {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.DAY_OF_MONTH)
        }.mapValues { (_, dayTrades) ->
            dayTrades.sumOf { it.profit - it.brokerage }
        }
    }

    val dailyMistakesMap = remember(mistakes, selectedDate) {
        val targetYear = selectedDate.get(Calendar.YEAR)
        val targetMonth = selectedDate.get(Calendar.MONTH)
        val cal = Calendar.getInstance()
        mistakes.filter {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) == targetYear &&
            cal.get(Calendar.MONTH) == targetMonth
        }.groupBy {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.DAY_OF_MONTH)
        }
    }

    val maxAbsPl = remember(dailyPlMap) {
        val values = dailyPlMap.values.map { Math.abs(it) }
        if (values.isEmpty()) 1.0 else (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    }

    val baseCapitalByTab = remember(activePortfolio, viewModel.currentUser) {
        activePortfolio?.startingEquity ?: viewModel.currentUser?.totalEquity ?: 10000.0
    }

    val performance = remember(trades, selectedDate, selectedTab) {
        val cal = Calendar.getInstance()
        when (selectedTab) {
            "Calendar" -> {
                val targetYear = selectedDate.get(Calendar.YEAR)
                val targetMonth = selectedDate.get(Calendar.MONTH)
                trades.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == targetYear &&
                    cal.get(Calendar.MONTH) == targetMonth
                }.sumOf { it.profit }
            }
            "Monthly" -> {
                val targetYear = selectedDate.get(Calendar.YEAR)
                trades.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == targetYear
                }.sumOf { it.profit }
            }
            else -> { // "Yearly"
                trades.sumOf { it.profit }
            }
        }
    }

    val periodBrokerage = remember(trades, selectedDate, selectedTab) {
        val cal = Calendar.getInstance()
        when (selectedTab) {
            "Calendar" -> {
                val targetYear = selectedDate.get(Calendar.YEAR)
                val targetMonth = selectedDate.get(Calendar.MONTH)
                trades.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == targetYear &&
                    cal.get(Calendar.MONTH) == targetMonth
                }.sumOf { it.brokerage }
            }
            "Monthly" -> {
                val targetYear = selectedDate.get(Calendar.YEAR)
                trades.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == targetYear
                }.sumOf { it.brokerage }
            }
            else -> { // "Yearly"
                trades.sumOf { it.brokerage }
            }
        }
    }

    val periodProfits = remember(trades, selectedDate, selectedTab) {
        val cal = Calendar.getInstance()
        val filtered = when (selectedTab) {
            "Calendar" -> {
                val targetYear = selectedDate.get(Calendar.YEAR)
                val targetMonth = selectedDate.get(Calendar.MONTH)
                trades.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == targetYear &&
                    cal.get(Calendar.MONTH) == targetMonth
                }
            }
            "Monthly" -> {
                val targetYear = selectedDate.get(Calendar.YEAR)
                trades.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == targetYear
                }
            }
            else -> { // "Yearly"
                trades
            }
        }
        filtered.sortedBy { it.timestamp }.map { it.profit - it.brokerage }
    }

    // Dismiss dialog when trade is successfully added or updated
    LaunchedEffect(viewModel.manageMessage) {
        if (viewModel.manageMessage == "Trade added successfully!" || viewModel.manageMessage == "Trade updated successfully!") {
            showLogTradeDialog = false
            viewModel.manageMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground) // Premium Dynamic Theme Background
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.clickable { viewModel.currentMainTab = "DASHBOARD" }
            )
            Text("Trading Journal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(onClick = { viewModel.showCertificateDialog = true }) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = "Trophy", tint = Color(0xFFFBBF24))
            }
        }

        // Performance Card (Designed to match the screenshot precisely)
        val netPerformance = performance - periodBrokerage
        val isPositiveProfit = netPerformance >= 0
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPositiveProfit) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Section
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (selectedTab) {
                            "Calendar" -> "${monthName} PERFORMANCE"
                            "Monthly" -> "YEAR ${selectedDate.get(Calendar.YEAR)} PERFORMANCE"
                            else -> "ALL-TIME PERFORMANCE"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val profitValStr = String.format(Locale.US, "%.0f", if (netPerformance >= 0) netPerformance else -netPerformance)
                        Text(
                            text = (if (isPositiveProfit) "+$" else "$") + profitValStr,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPositiveProfit) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        
                        // ROI Badge
                        val periodRoi = if (baseCapitalByTab > 0) (netPerformance / baseCapitalByTab) * 100.0 else 0.0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPositiveProfit) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "~ " + (if (periodRoi >= 0) "+" else "") + String.format(Locale.US, "%.1f%%", periodRoi) + " ROI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositiveProfit) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                    
                    Text(
                        text = "Brokerage Paid: -$" + String.format(Locale.US, "%.2f", periodBrokerage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (periodBrokerage > 0) Color(0xFFEF4444) else TextSecondary
                    )
                }

                // Mini Sparkline Chart Section
                SparklineChart(
                    profits = periodProfits,
                    isPositive = isPositiveProfit,
                    modifier = Modifier
                        .weight(1f)
                        .height(75.dp)
                        .padding(start = 12.dp)
                )
            }
        }
        
        // Tabs Custom Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .padding(4.dp)
        ) {
            val tabs = listOf("Calendar", "Monthly", "Yearly")
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Primary else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        when (selectedTab) {
            "Calendar" -> {
                // Calendar Month/Year Selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Prev Month",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp).clickable { 
                            selectedDate = (selectedDate.clone() as Calendar).apply { 
                                add(Calendar.MONTH, -1) 
                                set(Calendar.DAY_OF_MONTH, 1)
                            } 
                        }
                    )
                    Text(
                        text = "${monthName.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.get(Calendar.YEAR)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp).clickable { 
                            selectedDate = (selectedDate.clone() as Calendar).apply { 
                                add(Calendar.MONTH, 1) 
                                set(Calendar.DAY_OF_MONTH, 1)
                            } 
                        }
                    )
                }

                // Interactive Calendar Day Grid
                val daysInMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfMonth = (selectedDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
                val dayOfWeekOfFirst = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
                val emptyCellsBefore = dayOfWeekOfFirst - 1
                val totalCells = emptyCellsBefore + daysInMonth
                val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

                // Week Day Headers Row
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    dayLabels.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                // Chunk Cells into Row Grids of 7 days
                val cellRange = 0 until totalCells
                val rows = cellRange.chunked(7)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rows.forEach { rowCells ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowCells.forEach { cellIndex ->
                                val dayNumber = cellIndex - emptyCellsBefore + 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayNumber in 1..daysInMonth) {
                                        val isSelected = selectedDate.get(Calendar.DAY_OF_MONTH) == dayNumber
                                        val hasTradesOnThisDay = daysWithTrades.contains(dayNumber)
                                        val isToday = today.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                                      today.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                                                      today.get(Calendar.DAY_OF_MONTH) == dayNumber

                                        val pnl = dailyPlMap[dayNumber]

                                        val cellShape = RoundedCornerShape(10.dp)
                                        val cellModifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clip(cellShape)

                                        // Calculate background color and text colors
                                        val (pnlBgColor, pnlTextColor, pnlSecondaryTextColor) = remember(pnl, isSelected, isToday, isSystemInDarkMode, maxAbsPl) {
                                            if (pnl != null) {
                                                if (pnl > 0.0) {
                                                    val ratio = pnl / maxAbsPl
                                                    if (isSystemInDarkMode) {
                                                        if (ratio <= 0.33) {
                                                            Triple(Color(0xFF0F3E2B), Color(0xFFA7F3D0), Color(0xFFD1FAE5))
                                                        } else if (ratio <= 0.66) {
                                                            Triple(Color(0xFF064E3B), Color(0xFFD1FAE5), Color(0xFFF1FDF7))
                                                        } else {
                                                            Triple(Color(0xFF047857), Color.White, Color.White.copy(alpha = 0.9f))
                                                        }
                                                    } else {
                                                        if (ratio <= 0.33) {
                                                            Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFF166534))
                                                        } else if (ratio <= 0.66) {
                                                            Triple(Color(0xFFA7F3D0), Color(0xFF065F46), Color(0xFF064E3B))
                                                        } else {
                                                            Triple(Color(0xFF34D399), Color(0xFF064E3B), Color.White)
                                                        }
                                                    }
                                                } else if (pnl < 0.0) {
                                                    val absPnl = Math.abs(pnl)
                                                    val ratio = absPnl / maxAbsPl
                                                    if (isSystemInDarkMode) {
                                                        if (ratio <= 0.33) {
                                                            Triple(Color(0xFF451C1E), Color(0xFFFCA5A5), Color(0xFFFECACA))
                                                        } else if (ratio <= 0.66) {
                                                            Triple(Color(0xFF7F1D1D), Color(0xFFFEE2E2), Color(0xFFFFF5F5))
                                                        } else {
                                                            Triple(Color(0xFF991B1B), Color.White, Color.White.copy(alpha = 0.9f))
                                                        }
                                                    } else {
                                                        if (ratio <= 0.33) {
                                                            Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Color(0xFF991B1B))
                                                        } else if (ratio <= 0.66) {
                                                            Triple(Color(0xFFFECACA), Color(0xFF991B1B), Color(0xFF7F1D1D))
                                                        } else {
                                                            Triple(Color(0xFFFCA5A5), Color(0xFF7F1D1D), Color.White)
                                                        }
                                                    }
                                                } else {
                                                    if (isSystemInDarkMode) {
                                                        Triple(Color(0xFF1E293B), Color(0xFF94A3B8), Color(0xFF64748B))
                                                    } else {
                                                        Triple(Color(0xFFE2E8F0), Color(0xFF475569), Color(0xFF64748B))
                                                    }
                                                }
                                            } else {
                                                if (isSelected) {
                                                    Triple(Primary, Color.White, Color.White.copy(alpha = 0.8f))
                                                } else if (isToday) {
                                                    Triple(Primary.copy(alpha = 0.1f), Primary, Primary.copy(alpha = 0.7f))
                                                } else {
                                                    Triple(Color.Transparent, if (isSystemInDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A), if (isSystemInDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                                                }
                                            }
                                        }

                                        val cellBorderModifier = if (isSelected && pnl != null) {
                                            Modifier.border(2.5.dp, Primary, cellShape)
                                        } else if (isToday && pnl == null) {
                                            Modifier.border(1.5.dp, Primary, cellShape)
                                        } else {
                                            Modifier
                                        }

                                        val formattedPnl = remember(pnl) {
                                            if (pnl == null) "" else {
                                                if (pnl == pnl.toInt().toDouble()) {
                                                    pnl.toInt().toString()
                                                } else {
                                                    val formatted1 = String.format(Locale.US, "%.1f", pnl)
                                                    if (formatted1.toDouble() == pnl) {
                                                        formatted1
                                                    } else {
                                                        String.format(Locale.US, "%.2f", pnl)
                                                    }
                                                }
                                            }
                                        }

                                        Box(
                                            modifier = cellModifier
                                                .background(pnlBgColor)
                                                .then(cellBorderModifier)
                                                .clickable {
                                                    selectedDate = (selectedDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNumber) }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    color = pnlTextColor,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Bold
                                                )
                                                if (pnl != null) {
                                                    Spacer(modifier = Modifier.height(1.dp))
                                                    Text(
                                                        text = formattedPnl,
                                                        color = pnlSecondaryTextColor,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowCells.size < 7) {
                                repeat(7 - rowCells.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Selected Day Row Header with beautiful layout
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Day",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = weekdayAndDateStr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                // Unified card holding Date Header, Add Action list/empty state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card Header Row: June 1, 2026 & Blue circular button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cardDateStr = remember(selectedDate) {
                                val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.US)
                                sdf.format(selectedDate.time)
                            }
                            Text(
                                text = cardDateStr,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Primary)
                                    .clickable {
                                        viewModel.clearTradeForm()
                                        showLogTradeDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Log Trade Entry",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Content inside the card
                        if (filteredTrades.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No trades logged for this day.",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                filteredTrades.forEach { trade ->
                                    TradeJournalCard(
                                        trade = trade,
                                        onEdit = {
                                            viewModel.startEditTrade(trade)
                                            showLogTradeDialog = true
                                        },
                                        onDelete = {
                                            viewModel.handleDeleteTrade(trade)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "Monthly" -> {
                // Year Selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Prev Year",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp).clickable { 
                            selectedDate = (selectedDate.clone() as Calendar).apply { 
                                add(Calendar.YEAR, -1) 
                                set(Calendar.DAY_OF_MONTH, 1)
                            } 
                        }
                    )
                    Text(
                        text = "Year ${selectedDate.get(Calendar.YEAR)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Year",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp).clickable { 
                            selectedDate = (selectedDate.clone() as Calendar).apply { 
                                add(Calendar.YEAR, 1) 
                                set(Calendar.DAY_OF_MONTH, 1)
                            } 
                        }
                    )
                }

                // 12 Months elegant Grid of Cells (3 columns) matching the screenshot
                val currentYear = selectedDate.get(Calendar.YEAR)
                val monthRows = (0..11).chunked(3)
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    monthRows.forEach { rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowMonths.forEach { mIdx ->
                                MonthGridCell(
                                    monthIdx = mIdx,
                                    currentYear = currentYear,
                                    trades = trades,
                                    baseCapital = baseCapitalByTab,
                                    onClick = {
                                        selectedDate = (selectedDate.clone() as Calendar).apply {
                                            set(Calendar.MONTH, mIdx)
                                            set(Calendar.DAY_OF_MONTH, 1)
                                        }
                                        selectedTab = "Calendar"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            "Yearly" -> {
                // Determine which years have trades or display a selection of standard years
                val presentCal = Calendar.getInstance()
                val targetYears = run {
                    val cal = Calendar.getInstance()
                    val yearsSet = trades.map {
                        cal.timeInMillis = it.timestamp
                        cal.get(Calendar.YEAR)
                    }.toSet().toMutableList()
                    val curYear = presentCal.get(Calendar.YEAR)
                    if (!yearsSet.contains(curYear)) {
                        yearsSet.add(curYear)
                    }
                    yearsSet.sortedDescending()
                }

                val yearRows = targetYears.chunked(3)
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    yearRows.forEach { rowYears ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowYears.forEach { yr ->
                                YearGridCell(
                                    year = yr,
                                    trades = trades,
                                    baseCapital = baseCapitalByTab,
                                    onClick = {
                                        selectedDate = (selectedDate.clone() as Calendar).apply {
                                            set(Calendar.YEAR, yr)
                                            set(Calendar.MONTH, 0)
                                            set(Calendar.DAY_OF_MONTH, 1)
                                        }
                                        selectedTab = "Monthly"
                                    }
                                )
                            }
                            // Fill remaining spaces in the row
                            if (rowYears.size < 3) {
                                repeat(3 - rowYears.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

            // Trading Certificate Popup Dialog Overlay
            if (viewModel.showCertificateDialog) {
                val activeCertificateTrades = remember(trades, selectedDate, selectedTab) {
                    val cal = Calendar.getInstance()
                    when (selectedTab) {
                        "Calendar" -> {
                            val targetYear = selectedDate.get(Calendar.YEAR)
                            val targetMonth = selectedDate.get(Calendar.MONTH)
                            trades.filter {
                                cal.timeInMillis = it.timestamp
                                cal.get(Calendar.YEAR) == targetYear &&
                                cal.get(Calendar.MONTH) == targetMonth
                            }
                        }
                        "Monthly" -> {
                            val targetYear = selectedDate.get(Calendar.YEAR)
                            trades.filter {
                                cal.timeInMillis = it.timestamp
                                cal.get(Calendar.YEAR) == targetYear
                            }
                        }
                        else -> { // "Yearly"
                            trades
                        }
                    }
                }
                
                val netPlVal = activeCertificateTrades.sumOf { it.profit } - activeCertificateTrades.sumOf { it.brokerage }
                val wins = activeCertificateTrades.filter { it.profit > 0 }.size
                val total = activeCertificateTrades.size
                val winRateVal = if (total > 0) (wins.toDouble() / total) * 100.0 else 0.0
                val roiVal = if (baseCapitalByTab > 0) (netPlVal / baseCapitalByTab) * 100.0 else 0.0
                val userName = viewModel.currentUser?.name ?: "Trader"
                
                val certPeriodTitle = when (selectedTab) {
                    "Calendar" -> "MONTH OF ${monthName} ${selectedDate.get(Calendar.YEAR)}"
                    "Monthly" -> "YEAR ${selectedDate.get(Calendar.YEAR)} PERFORMANCE"
                    else -> "ALL-TIME PORTFOLIO"
                }

                TradingCertificateDialog(
                    userName = userName,
                    periodTitle = certPeriodTitle,
                    netPl = netPlVal,
                    winRate = winRateVal,
                    roi = roiVal,
                    tradesCount = total,
                    onDismiss = { viewModel.showCertificateDialog = false }
                )
            }

            // Modal Log A Trade Form Dialog
            if (showLogTradeDialog) {
                Dialog(onDismissRequest = { showLogTradeDialog = false }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val isEditing = viewModel.editingTradeId != null
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                                            contentDescription = if (isEditing) "Edit Trade Icon" else "Add Trade Icon",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isEditing) "Edit Trade" else "Add Trade",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                IconButton(onClick = { showLogTradeDialog = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                                }
                            }



                            // BUY / SELL Segment Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F3F9))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (viewModel.tradeIsBuy) Color(0xFF137333) else Color.Transparent)
                                        .clickable { viewModel.tradeIsBuy = true }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("BUY", color = if (viewModel.tradeIsBuy) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!viewModel.tradeIsBuy) Color(0xFFC5221F) else Color.Transparent)
                                        .clickable { viewModel.tradeIsBuy = false }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("SELL", color = if (!viewModel.tradeIsBuy) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Fields
                            OutlinedTextField(
                                value = viewModel.tradeSymbol,
                                onValueChange = { viewModel.tradeSymbol = it.uppercase() },
                                label = { Text("Pair") },
                                placeholder = { Text("XAUUSD") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeEntryPrice,
                                onValueChange = { newValue ->
                                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                                    val firstDotIdx = filtered.indexOf('.')
                                    viewModel.tradeEntryPrice = if (firstDotIdx != -1) {
                                        val before = filtered.substring(0, firstDotIdx + 1)
                                        val after = filtered.substring(firstDotIdx + 1).filter { it.isDigit() }
                                        before + after
                                    } else {
                                        filtered
                                    }
                                },
                                label = { Text("Entry Price") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeExitPrice,
                                onValueChange = { newValue ->
                                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                                    val firstDotIdx = filtered.indexOf('.')
                                    viewModel.tradeExitPrice = if (firstDotIdx != -1) {
                                        val before = filtered.substring(0, firstDotIdx + 1)
                                        val after = filtered.substring(firstDotIdx + 1).filter { it.isDigit() }
                                        before + after
                                    } else {
                                        filtered
                                    }
                                },
                                label = { Text("Exit Price") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeSize,
                                onValueChange = { newValue ->
                                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                                    val firstDotIdx = filtered.indexOf('.')
                                    viewModel.tradeSize = if (firstDotIdx != -1) {
                                        val before = filtered.substring(0, firstDotIdx + 1)
                                        val after = filtered.substring(firstDotIdx + 1).filter { it.isDigit() }
                                        before + after
                                    } else {
                                        filtered
                                    }
                                },
                                label = { Text("Lots") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeBrokerage,
                                onValueChange = { newValue ->
                                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                                    val firstDotIdx = filtered.indexOf('.')
                                    viewModel.tradeBrokerage = if (firstDotIdx != -1) {
                                        val before = filtered.substring(0, firstDotIdx + 1)
                                        val after = filtered.substring(firstDotIdx + 1).filter { it.isDigit() }
                                        before + after
                                    } else {
                                        filtered
                                    }
                                },
                                label = { Text("Brokerage ($)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeProfit,
                                onValueChange = { newValue ->
                                    val isNeg = newValue.startsWith("-")
                                    val clean = if (isNeg) newValue.substring(1) else newValue
                                    val filtered = clean.filter { it.isDigit() || it == '.' }
                                    val firstDotIdx = filtered.indexOf('.')
                                    val finalVal = if (firstDotIdx != -1) {
                                        val before = filtered.substring(0, firstDotIdx + 1)
                                        val after = filtered.substring(firstDotIdx + 1).filter { it.isDigit() }
                                        before + after
                                    } else {
                                        filtered
                                    }
                                    viewModel.tradeProfit = if (isNeg) "-$finalVal" else finalVal
                                },
                                label = { Text("Gross P/L ($)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeNotes,
                                onValueChange = { viewModel.tradeNotes = it },
                                label = { Text("Comments") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                minLines = 2
                            )

                            viewModel.manageMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    color = if (msg.contains("successfully")) Color(0xFF137333) else Color(0xFFC5221F),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showLogTradeDialog = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        val epochMilli = selectedDate.timeInMillis
                                        viewModel.handleAddTrade(epochMilli)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Save Record", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun RowScope.MonthGridCell(
    monthIdx: Int,
    currentYear: Int,
    trades: List<Trade>,
    baseCapital: Double,
    onClick: () -> Unit
) {
    val monthsList = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val mName = monthsList[monthIdx]
    
    // Filter trades for this month
    val monthTrades = remember(trades, currentYear, monthIdx) {
        val cal = Calendar.getInstance()
        trades.filter {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) == currentYear &&
            cal.get(Calendar.MONTH) == monthIdx
        }
    }
    
    val hasTrades = monthTrades.isNotEmpty()
    val mProfit = monthTrades.sumOf { it.profit }
    val mBrokerage = monthTrades.sumOf { it.brokerage }
    val netProfit = mProfit - mBrokerage
    
    val isPositive = netProfit >= 0
    val borderStroke = if (hasTrades) {
         androidx.compose.foundation.BorderStroke(2.dp, if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444))
    } else {
         androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE2E8F0))
    }
    
    Card(
        modifier = Modifier
            .weight(1f)
            .height(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (hasTrades) DarkSurface else DarkBackground.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = mName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            if (hasTrades) {
                // Formatting matches screenshot (no negative sign for raw loss, but colored red)
                val profitText = if (mProfit >= 0) {
                    "+$" + String.format(Locale.US, "%.2f", mProfit)
                } else {
                    "$" + String.format(Locale.US, "%.2f", -mProfit)
                }
                
                Text(
                    text = profitText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (mProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val roi = if (baseCapital > 0) (mProfit / baseCapital) * 100.0 else 0.0
                val roiText = (if (roi >= 0) "+" else "") + String.format(Locale.US, "%.1f%%", roi)
                Text(
                    text = roiText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (mProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                
                if (mBrokerage > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-$" + String.format(Locale.US, "%.2f", mBrokerage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFEF4444)
                    )
                }
            } else {
                Text(
                    text = "-",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun RowScope.YearGridCell(
    year: Int,
    trades: List<Trade>,
    baseCapital: Double,
    onClick: () -> Unit
) {
    val yearTrades = remember(trades, year) {
        val cal = Calendar.getInstance()
        trades.filter {
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) == year
        }
    }
    
    val hasTrades = yearTrades.isNotEmpty()
    val yProfit = yearTrades.sumOf { it.profit }
    val yBrokerage = yearTrades.sumOf { it.brokerage }
    val netProfit = yProfit - yBrokerage
    
    val isPositive = netProfit >= 0
    val borderStroke = if (hasTrades) {
         androidx.compose.foundation.BorderStroke(2.dp, if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444))
    } else {
         androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE2E8F0))
    }
    
    Card(
        modifier = Modifier
            .weight(1f)
            .height(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (hasTrades) DarkSurface else DarkBackground.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = year.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            if (hasTrades) {
                val profitText = if (yProfit >= 0) {
                    "+$" + String.format(Locale.US, "%.2f", yProfit)
                } else {
                    "$" + String.format(Locale.US, "%.2f", -yProfit)
                }
                
                Text(
                    text = profitText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (yProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val roi = if (baseCapital > 0) (yProfit / baseCapital) * 100.0 else 0.0
                val roiText = (if (roi >= 0) "+" else "") + String.format(Locale.US, "%.1f%%", roi)
                Text(
                    text = roiText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (yProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                
                if (yBrokerage > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-$" + String.format(Locale.US, "%.2f", yBrokerage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFEF4444)
                    )
                }
            } else {
                Text(
                    text = "-",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }
    }
}




@Composable
fun TradeJournalCard(trade: Trade, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = remember(trade.timestamp) { sdf.format(Date(trade.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFF1F3F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = trade.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background((if (trade.isBuy) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (trade.isBuy) "BUY" else "SELL",
                                color = if (trade.isBuy) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    val formattedPr = if (trade.profit >= 0) "+$${String.format("%.2f", trade.profit)}" else "-$${String.format("%.2f", -trade.profit)}"
                    Text(
                        text = formattedPr,
                        color = if (trade.profit >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${trade.size} Lots",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(color = if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE2E8F0), modifier = Modifier.padding(bottom = 12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Entry Price", fontSize = 11.sp, color = TextSecondary)
                            Text("$${String.format("%.4f", trade.entryPrice)}", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text("Exit Price", fontSize = 11.sp, color = TextSecondary)
                            val exitText = if (trade.exitPrice == 0.0) "N/A" else "$${String.format("%.4f", trade.exitPrice)}"
                            Text(exitText, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text("Fees / Broker", fontSize = 11.sp, color = TextSecondary)
                            Text("$${String.format("%.2f", trade.brokerage)}", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }

                    if (trade.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Notes", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = trade.notes,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Log", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Log")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        TextButton(
                            onClick = onEdit,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Log", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Log")
                        }
                    }
                }
            }
        }
    }
}

data class DrawdownCalculation(
    val list: List<Double>,
    val maxDd: Double,
    val currentDd: Double,
    val daysUnder: Long
)

@Composable
fun PerformanceCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    iconText: String,
    badgeColor: Color,
    iconColor: Color
) {
    Card(
        modifier = modifier.height(115.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F3F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    color = iconColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AnalyticsScreen(viewModel: TradeViewModel) {
    val trades by viewModel.allTrades.collectAsState()
    val activePortfolio = viewModel.activePortfolio
    
    // Filter trades by active portfolio
    val filteredTrades = remember(trades, activePortfolio) {
        if (activePortfolio != null) {
            trades.filter { it.portfolioId == activePortfolio.id }
        } else {
            trades
        }
    }
    
    val baseEquity = activePortfolio?.startingEquity ?: 10000.0

    // States for visual interactivity filters
    var selectedYear by remember { mutableStateOf(2026) }
    var weekdayTab by remember { mutableStateOf("Profit") }
    var historyTab by remember { mutableStateOf("Daily P/L") }
    var equityPointLimit by remember { mutableStateOf(20) }
    var drawdownPointLimit by remember { mutableStateOf(20) }

    // Aggregate statistics
    val totalTrades = filteredTrades.size
    val winTrades = filteredTrades.filter { it.profit > 0 }
    val lossTrades = filteredTrades.filter { it.profit <= 0 }
    val winCount = winTrades.size
    val lossCount = lossTrades.size
    val winRate = if (totalTrades > 0) (winCount.toFloat() / totalTrades.toFloat()) * 100f else 0f
    
    val totalNetPl = filteredTrades.sumOf { it.profit - it.brokerage }
    val roi = if (baseEquity > 0.0) (totalNetPl / baseEquity) * 100.0 else 0.0
    
    val totalWinsVal = winTrades.sumOf { it.profit }
    val totalLossesVal = lossTrades.sumOf { -it.profit }
    val avgWin = if (winCount > 0) totalWinsVal / winCount else 0.0
    val avgLoss = if (lossCount > 0) totalLossesVal / lossCount else 0.0
    
    val expectancy = if (totalTrades > 0) {
        val winProb = winCount.toDouble() / totalTrades
        val lossProb = lossCount.toDouble() / totalTrades
        (winProb * avgWin) - (lossProb * avgLoss)
    } else {
        0.0
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Performance Stats Header Grid
        Text(
            text = "Performance Stats",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val totalPlText = if (totalNetPl == 0.0) "$0" else (if (totalNetPl > 0) "+$" else "-$") + String.format(Locale.US, "%.0f", Math.abs(totalNetPl))
            val roiText = (if (roi > 0) "+" else "") + String.format(Locale.US, "%.1f%%", roi)
            val expectancyText = (if (expectancy >= 0) "+$" else "-$") + String.format(Locale.US, "%.1f", Math.abs(expectancy))

            PerformanceCard(
                modifier = Modifier.weight(1f),
                title = "Total P/L",
                value = totalPlText,
                iconText = "$",
                badgeColor = Color(0xFFE6F4EA),
                iconColor = Color(0xFF137333)
            )
            PerformanceCard(
                modifier = Modifier.weight(1f),
                title = "ROI",
                value = roiText,
                iconText = "%",
                badgeColor = Color(0xFFE8F0FE),
                iconColor = Color(0xFF1A73E8)
            )
            PerformanceCard(
                modifier = Modifier.weight(1f),
                title = "Expectancy",
                value = expectancyText,
                iconText = "~",
                badgeColor = Color(0xFFF1F3F4),
                iconColor = Color(0xFF5F6368)
            )
        }

        // 2. Brokerage Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F3F9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFF7E6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Brokerage Breakdown",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Brokerage Breakdown",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val grossProfitVal = winTrades.sumOf { it.profit }
                    val feesVal = filteredTrades.sumOf { it.brokerage }
                    val netProfitVal = totalNetPl

                    val grossPercent = if (baseEquity > 0.0) (grossProfitVal / baseEquity) * 100.0 else 0.0
                    val feesPercent = if (grossProfitVal > 0.0) (feesVal / grossProfitVal) * 100.0 else if (baseEquity > 0) (feesVal / baseEquity) * 100.0 else 0.0
                    val netPercent = if (baseEquity > 0.0) (netProfitVal / baseEquity) * 100.0 else 0.0

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Gross Profit", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+$${String.format(Locale.US, "%.0f", grossProfitVal)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenAccent)
                        Text("(+${String.format(Locale.US, "%.1f", grossPercent)}%)", fontSize = 11.sp, color = GreenAccent)
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "to",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Fees", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("-$${String.format(Locale.US, "%.0f", feesVal)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RedAccent)
                        Text("(${String.format(Locale.US, "%.1f", feesPercent)}%)", fontSize = 11.sp, color = RedAccent)
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "to",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Net Profit", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val sign = if (netProfitVal >= 0) "+" else "-"
                        val color = if (netProfitVal >= 0) GreenAccent else RedAccent
                        Text("$sign$${String.format(Locale.US, "%.0f", Math.abs(netProfitVal))}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
                        val pctSign = if (netPercent >= 0) "+" else ""
                        Text("($pctSign${String.format(Locale.US, "%.1f", netPercent)}%)", fontSize = 11.sp, color = color)
                    }
                }
            }
        }

        // 3. Consistency Matrix Calendar Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F3F9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Consistency",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Daily trading activity",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    
                    var showYearDropdown by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F3F9).copy(alpha = 0.7f))
                                .clickable { showYearDropdown = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = selectedYear.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        
                        DropdownMenu(
                            expanded = showYearDropdown,
                            onDismissRequest = { showYearDropdown = false }
                        ) {
                            listOf(2024, 2025, 2026, 2027).forEach { yr ->
                                DropdownMenuItem(
                                    text = { Text(yr.toString()) },
                                    onClick = {
                                        selectedYear = yr
                                        showYearDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    for (m in 0 until 12) {
                        val monthName = months[m]
                        val daysInMonth = when (m) {
                            1 -> if (selectedYear % 4 == 0) 29 else 28
                            3, 5, 8, 10 -> 30
                            else -> 31
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = monthName, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val cols = 5
                            val rows = 6
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (r in 0 until rows) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (c in 0 until cols) {
                                            val day = r * cols + c + 1
                                            if (day <= daysInMonth) {
                                                val todayTrades = filteredTrades.filter {
                                                    val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                                                    cal.get(Calendar.YEAR) == selectedYear &&
                                                    cal.get(Calendar.MONTH) == m &&
                                                    cal.get(Calendar.DAY_OF_MONTH) == day
                                                }
                                                val dotColor = if (todayTrades.isNotEmpty()) {
                                                    val dailyNet = todayTrades.sumOf { it.profit - it.brokerage }
                                                    if (dailyNet > 0) GreenAccent else RedAccent
                                                } else {
                                                    if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(dotColor, CircleShape)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.size(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(RedAccent, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Loss", fontSize = 10.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.size(6.dp).background(GreenAccent, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Win", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }

        // 4. Equity Curve Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFF1F3F9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val orderedTrades = remember(filteredTrades) { filteredTrades.sortedBy { it.timestamp } }
                val graphLimit = equityPointLimit
                val displayTrades = remember(orderedTrades, graphLimit) {
                    if (orderedTrades.size > graphLimit) orderedTrades.takeLast(graphLimit) else orderedTrades
                }
                
                val equityNodes = remember(displayTrades, baseEquity) {
                    val list = mutableListOf<Double>()
                    var currentEq = baseEquity
                    list.add(currentEq)
                    for (t in displayTrades) {
                        currentEq += (t.profit - t.brokerage)
                        list.add(currentEq)
                    }
                    list
                }
                
                val minEq = equityNodes.minOrNull() ?: baseEquity
                val maxEq = equityNodes.maxOrNull() ?: baseEquity
                val range = if (maxEq - minEq > 0) maxEq - minEq else 100.0
                
                val displayStartEquity = equityNodes.firstOrNull() ?: baseEquity
                val currentEquity = equityNodes.lastOrNull() ?: baseEquity
                val changeValue = currentEquity - displayStartEquity
                val changePercent = if (displayStartEquity > 0.0) (changeValue / displayStartEquity) * 100.0 else 0.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Total Equity",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(end = 2.dp, bottom = 4.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%,.2f", currentEquity),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val changeColor = if (changeValue >= 0.0) Color(0xFF10B981) else Color(0xFFEF4444)
                        val changeBgColor = changeColor.copy(alpha = 0.1f)
                        val arrowSymbol = if (changeValue >= 0.0) "↑" else "↓"
                        val changeFormatted = if (changeValue >= 0.0) {
                            "+$${String.format(Locale.US, "%,.2f", changeValue)}"
                        } else {
                            "$${String.format(Locale.US, "%,.2f", changeValue)}"
                        }
                        
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(changeBgColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$arrowSymbol $changeFormatted (${String.format(Locale.US, "%.1f", changePercent)}%)",
                                color = changeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val isPositiveTrend = changeValue >= 0.0
                    val curveColor = if (isPositiveTrend) Color(0xFF10B981) else Color(0xFFEF4444)
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val pointsCount = equityNodes.size
                        
                        if (pointsCount > 1) {
                            val xStep = width / (pointsCount - 1)
                            val path = Path()
                            val fillPath = Path()
                            
                            val firstEq = equityNodes[0]
                            val firstNormalizedY = ((firstEq - minEq) / range)
                            val firstY = height - (firstNormalizedY.toFloat() * height)
                            path.moveTo(0f, firstY)
                            fillPath.moveTo(0f, height)
                            fillPath.lineTo(0f, firstY)
                            
                            var prevX = 0f
                            var prevY = firstY
                            
                            for (i in 1 until pointsCount) {
                                val eq = equityNodes[i]
                                val x = i * xStep
                                val normalizedY = ((eq - minEq) / range)
                                val y = height - (normalizedY.toFloat() * height)
                                
                                val controlX1 = prevX + (x - prevX) / 2f
                                val controlY1 = prevY
                                val controlX2 = prevX + (x - prevX) / 2f
                                val controlY2 = y
                                
                                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                
                                prevX = x
                                prevY = y
                            }
                            fillPath.lineTo(prevX, height)
                            fillPath.close()
                            
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        curveColor.copy(alpha = 0.15f),
                                        curveColor.copy(alpha = 0.0f)
                                    ),
                                    startY = 0f,
                                    endY = height
                                )
                            )
                            
                            drawPath(
                                path = path,
                                color = curveColor,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Baseline: $${String.format(Locale.US, "%,.1f", baseEquity)}", 
                        fontSize = 11.sp, 
                        color = TextSecondary
                    )
                    Text(
                        text = "Balance: $${String.format(Locale.US, "%,.2f", currentEquity)}", 
                        fontSize = 11.sp, 
                        color = if (changeValue >= 0.0) Color(0xFF10B981) else Color(0xFFEF4444), 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. Weekday Distribution Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F3F9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekday Distribution",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSystemInDarkMode) Color(0xFF111827) else Color(0xFFF1F3F9).copy(alpha = 0.8f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Profit", "Loss", "Combined").forEach { tab ->
                        val isSelected = weekdayTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) (if (isSystemInDarkMode) Color(0xFF1F2937) else Color.White) else Color.Transparent)
                                .clickable { weekdayTab = tab }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                val weekdaysAbbr = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                val weekdayValues = remember(filteredTrades, weekdayTab) {
                    val map = mutableMapOf<Int, Double>()
                    for (day in 2..6) {
                        map[day] = 0.0
                    }
                    
                    for (t in filteredTrades) {
                        val cal = Calendar.getInstance().apply { timeInMillis = t.timestamp }
                        val d = cal.get(Calendar.DAY_OF_WEEK)
                        if (d in 2..6) {
                            when (weekdayTab) {
                                "Profit" -> {
                                    if (t.profit > 0) {
                                        map[d] = (map[d] ?: 0.0) + t.profit
                                    }
                                }
                                "Loss" -> {
                                    if (t.profit <= 0) {
                                        map[d] = (map[d] ?: 0.0) + Math.abs(t.profit)
                                    }
                                }
                                "Combined" -> {
                                    map[d] = (map[d] ?: 0.0) + (t.profit - t.brokerage)
                                }
                            }
                        }
                    }
                    map
                }
                
                val maxVal = weekdayValues.values.maxOrNull()?.let { if (Math.abs(it) > 0.0) Math.abs(it) else 100.0 } ?: 100.0
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (dayNum in 2..6) {
                        val valDouble = weekdayValues[dayNum] ?: 0.0
                        val dayName = weekdaysAbbr[dayNum - 2]
                        val ratio = if (maxVal > 0.0) (Math.abs(valDouble) / maxVal).toFloat() else 0f
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (valDouble != 0.0) "$${String.format(Locale.US, "%.0f", valDouble)}" else "-",
                                fontSize = 10.sp,
                                color = if (valDouble >= 0) GreenAccent else RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(ratio.coerceIn(0.04f, 1f))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (valDouble >= 0) GreenAccent.copy(alpha = 0.8f) else RedAccent.copy(alpha = 0.8f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = dayName, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 7. Profit / Loss History Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F3F9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Profit / Loss History",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSystemInDarkMode) Color(0xFF111827) else Color(0xFFF1F3F9).copy(alpha = 0.8f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Daily P/L", "Monthly P/L").forEach { tab ->
                        val isSelected = historyTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) (if (isSystemInDarkMode) Color(0xFF1F2937) else Color.White) else Color.Transparent)
                                .clickable { historyTab = tab }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                val historyList = remember(filteredTrades, historyTab) {
                    val entries = mutableListOf<Pair<String, Double>>()
                    if (filteredTrades.isEmpty()) {
                        if (historyTab == "Daily P/L") {
                            entries.add(Pair("T-3", 0.0))
                            entries.add(Pair("T-2", 0.0))
                            entries.add(Pair("T-1", 0.0))
                            entries.add(Pair("Today", 0.0))
                        } else {
                            entries.add(Pair("Jan", 0.0))
                            entries.add(Pair("Feb", 0.0))
                            entries.add(Pair("Mar", 0.0))
                            entries.add(Pair("Apr", 0.0))
                        }
                    } else {
                        if (historyTab == "Daily P/L") {
                            val dateFormat = SimpleDateFormat("MMM d", Locale.US)
                            val grouped = filteredTrades.groupBy {
                                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                                dateFormat.format(cal.time)
                            }
                            val sortedDays = grouped.keys.toList().sortedByDescending { key ->
                                filteredTrades.first { dateFormat.format(Date(it.timestamp)) == key }.timestamp
                            }.take(5).reversed()
                            
                            sortedDays.forEach { dayName ->
                                val sum = grouped[dayName]?.sumOf { it.profit - it.brokerage } ?: 0.0
                                entries.add(Pair(dayName, sum))
                            }
                        } else {
                            val monthFormat = SimpleDateFormat("MMM", Locale.US)
                            val grouped = filteredTrades.groupBy {
                                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                                monthFormat.format(cal.time)
                            }
                            val monthsOrdered = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            monthsOrdered.forEach { m ->
                                if (grouped.containsKey(m)) {
                                    val sum = grouped[m]?.sumOf { it.profit - it.brokerage } ?: 0.0
                                    entries.add(Pair(m, sum))
                                }
                            }
                            if (entries.isEmpty()) {
                                entries.add(Pair("Jan", 0.0))
                                entries.add(Pair("Feb", 0.0))
                            }
                        }
                    }
                    entries
                }
                
                val maxHistoryVal = historyList.map { Math.abs(it.second) }.maxOrNull()?.let { if (it > 0.0) it else 100.0 } ?: 100.0
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    historyList.forEach { (label, value) ->
                        val ratio = (Math.abs(value) / maxHistoryVal).toFloat()
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (value != 0.0) "$${String.format(Locale.US, "%.0f", value)}" else "-",
                                fontSize = 10.sp,
                                color = if (value >= 0) GreenAccent else RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(ratio.coerceIn(0.04f, 1f))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (value >= 0) GreenAccent.copy(alpha = 0.8f) else RedAccent.copy(alpha = 0.8f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (viewModel.showCertificateDialog) {
        val userName = viewModel.currentUser?.name ?: "Trader"
        TradingCertificateDialog(
            userName = userName,
            periodTitle = "LIFETIME PERFORMANCE",
            netPl = totalNetPl,
            winRate = winRate.toDouble(),
            roi = roi,
            tradesCount = totalTrades,
            onDismiss = { viewModel.showCertificateDialog = false }
        )
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(text = value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
    Divider(color = TextSecondary.copy(alpha = 0.05f))
}

@Composable
fun CommunityScreen(viewModel: TradeViewModel) {
    val posts by viewModel.communityPosts
    var expandPoster by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Setup publisher
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Broadcasting Setup Area", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Share technical setups or trading analysis ideas.", fontSize = 11.sp, color = TextSecondary)
                        }
                        IconButton(onClick = { expandPoster = !expandPoster }) {
                            Icon(
                                imageVector = if (expandPoster) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandPoster) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = viewModel.newPostTitle,
                                onValueChange = { viewModel.newPostTitle = it },
                                label = { Text("Setup Title (e.g. BTC breakout)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewModel.newPostContent,
                                onValueChange = { viewModel.newPostContent = it },
                                label = { Text("Detailed Analysis & target/stop rationale") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                minLines = 3
                            )
                            Button(
                                onClick = {
                                    viewModel.handleCreatePost()
                                    expandPoster = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Post to Community Feed", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Live Feed items
        items(posts) { post ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Primary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.author.take(2).uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(post.author, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                            Text(post.timestamp, color = TextSecondary, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = post.title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = post.description,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = TextSecondary.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.handleToggleLike(post.id) }) {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (post.isLiked) RedAccent else TextSecondary
                            )
                        }
                        Text(
                            text = "${post.likes} Upvotes",
                            fontSize = 12.sp,
                            color = if (post.isLiked) RedAccent else TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Rank & tips based on interactive metrics/performance
fun getRankAndTip(tradesCount: Int, winRate: Double, netPl: Double, roi: Double): Pair<String, String> {
    return when {
        tradesCount <= 0 -> "NOVICE TRADER" to "Ready to log your first setup, keep learning!"
        tradesCount in 1..4 -> "GRINDING TRADER" to "Building foundations, sizing down, and logging charts."
        netPl > 0 && roi >= 20.0 && winRate >= 55.0 -> "APEX LEGEND" to "Highest level of systematic market mastery!"
        netPl > 0 && roi >= 10.0 -> "PROP ELITE" to "Professional metrics ready for institution backing."
        netPl > 0 && winRate >= 50.0 -> "CONSISTENT TRADER" to "Flawless execution, high emotional discipline established."
        netPl > 0 -> "SURVIVING TRADER" to "Maintaining safety edge and guarding your drawdown."
        else -> "GRINDING TRADER" to "Analyzing journal insights, refining edge, and refining sizes."
    }
}

@Composable
fun TradingCertificateDialog(
    userName: String,
    periodTitle: String,
    netPl: Double,
    winRate: Double,
    roi: Double,
    tradesCount: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val (rank, tip) = remember(tradesCount, winRate, netPl, roi) {
        getRankAndTip(tradesCount, winRate, netPl, roi)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                // Elegant double border inner container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .border(1.5.dp, Color(0xFFC5C2B7), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                        .border(0.8.dp, Color(0xFFE5E2D7), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    // Background shadow decorative element
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.02f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(160.dp),
                            tint = Color.Black
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Title: TRADING CERTIFICATE
                        Text(
                            text = "TRADING CERTIFICATE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF4A453A),
                            letterSpacing = 1.5.sp
                        )
                        
                        // Subtitle: MONTH OF MAY 2026
                        Text(
                            text = periodTitle.uppercase(Locale.US),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF7C7565),
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // This certifies that
                        Text(
                            text = "This certifies that",
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 14.sp,
                            color = Color(0xFF7C7565)
                        )

                        // Big Elegant Name
                        Text(
                            text = userName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = Color(0xFF1F2937)
                        )

                        // Subtle line under Name
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(1.dp)
                                .background(Color(0xFFE5E2D7))
                        )

                        // has achieved the rank of
                        Text(
                            text = "has achieved the rank of",
                            fontSize = 11.sp,
                            color = Color(0xFF7C7565)
                        )

                        // Rank badge shape
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFFBCAE99), RoundedCornerShape(50.dp))
                                .background(Color(0xFFFBF9F6), RoundedCornerShape(50.dp))
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                             contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rank,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF8C7B65),
                                letterSpacing = 1.2.sp
                            )
                        }

                        // Tip underneath rank
                        Text(
                            text = tip,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Wide Subtle Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFECEAE4))
                        )

                        // Stats row: 3 columns with vertical dividers
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // NET P/L
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                val prefix = if (netPl >= 0) "+$" else "-$"
                                val formattedVal = "$prefix${String.format(Locale.US, "%.0f", Math.abs(netPl))}"
                                Text(
                                    text = formattedVal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (netPl >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "NET P/L",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF978F80)
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(Color(0xFFE5E2D7))
                            )

                            // WIN RATE
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", winRate),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1F2937)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "WIN RATE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF978F80)
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(Color(0xFFE5E2D7))
                            )

                            // ROI
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                val sign = if (roi >= 0) "+" else ""
                                Text(
                                    text = "$sign${String.format(Locale.US, "%.1f%%", roi)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (roi >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "ROI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF978F80)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Verified bottom stamp bar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Verified by BookFx",
                                fontSize = 10.sp,
                                color = Color(0xFF9CA3AF),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )

                            // Check Circle seal
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFF3F1EC), CircleShape)
                                    .border(0.8.dp, Color(0xFFD4CFC5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Seal",
                                    tint = Color(0xFF8C7B65),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share Button
                Button(
                    onClick = {
                        shareCertificateAsImage(
                            context = context,
                            userName = userName,
                            periodTitle = periodTitle,
                            netPl = netPl,
                            winRate = winRate,
                            roi = roi,
                            rank = rank,
                            tip = tip
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share, 
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A5F6B)),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun shareCertificateAsImage(
    context: android.content.Context,
    userName: String,
    periodTitle: String,
    netPl: Double,
    winRate: Double,
    roi: Double,
    rank: String,
    tip: String
) {
    try {
        val bitmap = generateCertificateBitmap(userName, periodTitle, netPl, winRate, roi, rank, tip)
        val imagesFolder = File(context.cacheDir, "images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "bookfx_certificate.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "My BookFx Performance")
            putExtra(Intent.EXTRA_TEXT, "🎯 Proud of my trading performance certificate from BookFx!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Certificate Photo via"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun generateCertificateBitmap(
    userName: String,
    periodTitle: String,
    netPl: Double,
    winRate: Double,
    roi: Double,
    rank: String,
    tip: String
): Bitmap {
    val width = 1200
    val height = 1600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    // 1. Draw Cream/White Background
    val bgPaint = AndroidPaint().apply {
        color = 0xFFFFFFFF.toInt() // White background
        style = AndroidPaint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Fill slight warm cream background inside
    val innerBgPaint = AndroidPaint().apply {
        color = 0xFFFAFBF9.toInt() // #FAFBF9
        style = AndroidPaint.Style.FILL
    }
    canvas.drawRect(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat(), innerBgPaint)

    // 2. Outer Border (Double frame)
    // Primary outer border
    val border1Paint = AndroidPaint().apply {
        color = 0xFFC5C2B7.toInt() // C5C2B7
        style = AndroidPaint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    val rOuter = AndroidRectF(50f, 50f, (width - 50).toFloat(), (height - 50).toFloat())
    canvas.drawRoundRect(rOuter, 36f, 36f, border1Paint)

    // Inner finer border
    val border2Paint = AndroidPaint().apply {
        color = 0xFFE5E2D7.toInt() // E5E2D7
        style = AndroidPaint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    val rInner = AndroidRectF(66f, 66f, (width - 66).toFloat(), (height - 66).toFloat())
    canvas.drawRoundRect(rInner, 24f, 24f, border2Paint)

    // 3. Watermark Trophy in Background (Center of the certificate, drawn beautifully)
    val wmPaint = AndroidPaint().apply {
        color = 0x05000000 // Alpha ~0.02
        style = AndroidPaint.Style.FILL
        isAntiAlias = true
    }
    val wmStrokePaint = AndroidPaint().apply {
        color = 0x0A000000 // Alpha ~0.04
        style = AndroidPaint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    // Draw stylized trophy at the center (X=600, Y=800)
    val trophyPath = AndroidPath().apply {
        // Upper bowl
        moveTo(500f, 700f)
        lineTo(700f, 700f)
        cubicTo(700f, 850f, 500f, 850f, 500f, 700f)
        // Stand/stem
        moveTo(580f, 840f)
        lineTo(620f, 840f)
        lineTo(620f, 920f)
        lineTo(580f, 920f)
        close()
        // Base
        moveTo(520f, 920f)
        lineTo(680f, 920f)
        lineTo(660f, 960f)
        lineTo(540f, 960f)
        close()
        // Left Handle
        moveTo(500f, 730f)
        cubicTo(430f, 730f, 430f, 800f, 500f, 800f)
        // Right Handle
        moveTo(700f, 730f)
        cubicTo(770f, 730f, 770f, 800f, 700f, 800f)
    }
    canvas.drawPath(trophyPath, wmPaint)
    canvas.drawPath(trophyPath, wmStrokePaint)

    // 4. Texts
    val textPaint = AndroidPaint().apply {
        textAlign = AndroidPaint.Align.CENTER
        isAntiAlias = true
    }

    // Title: TRADING CERTIFICATE
    textPaint.apply {
        color = 0xFF4A453A.toInt()
        textSize = 42f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText("T R A D I N G   C E R T I F I C A T E", 600f, 220f, textPaint)

    // Subtitle: LIFETIME PERFORMANCE
    textPaint.apply {
        color = 0xFF7C7565.toInt()
        textSize = 28f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText(periodTitle.uppercase(Locale.US), 600f, 290f, textPaint)

    // "This certifies that"
    textPaint.apply {
        color = 0xFF7C7565.toInt()
        textSize = 36f
        typeface = AndroidTypeface.create(AndroidTypeface.SERIF, AndroidTypeface.ITALIC)
    }
    canvas.drawText("This certifies that", 600f, 385f, textPaint)

    // User's Name
    textPaint.apply {
        color = 0xFF1F2937.toInt()
        textSize = 64f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText(userName, 600f, 490f, textPaint)

    // Underline beneath Name
    val linePaint = AndroidPaint().apply {
        color = 0xFFE5E2D7.toInt()
        strokeWidth = 3f
        style = AndroidPaint.Style.STROKE
    }
    canvas.drawLine(440f, 530f, 760f, 530f, linePaint)

    // "has achieved the rank of"
    textPaint.apply {
        color = 0xFF7C7565.toInt()
        textSize = 28f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.NORMAL)
    }
    canvas.drawText("has achieved the rank of", 600f, 600f, textPaint)

    // Rank Badge
    val badgeRect = AndroidRectF(400f, 650f, 800f, 740f)
    val badgeBgPaint = AndroidPaint().apply {
        color = 0xFFFBF9F6.toInt()
        style = AndroidPaint.Style.FILL
        isAntiAlias = true
    }
    val badgeBorderPaint = AndroidPaint().apply {
        color = 0xFFBCAE99.toInt()
        style = AndroidPaint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    canvas.drawRoundRect(badgeRect, 45f, 45f, badgeBgPaint)
    canvas.drawRoundRect(badgeRect, 45f, 45f, badgeBorderPaint)

    // Rank Name in Badge
    textPaint.apply {
        color = 0xFF8C7B65.toInt()
        textSize = 34f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText(rank, 600f, 708f, textPaint)

    // Tip/Motto
    textPaint.apply {
        color = 0xFF9CA3AF.toInt()
        textSize = 26f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.ITALIC)
    }
    canvas.drawText(tip, 600f, 795f, textPaint)

    // Divider before Stats
    val dividerPaint = AndroidPaint().apply {
        color = 0xFFECEAE4.toInt()
        strokeWidth = 2.5f
        style = AndroidPaint.Style.STROKE
    }
    canvas.drawLine(150f, 870f, 1050f, 870f, dividerPaint)

    // Stats Section (Y baseline 960 to 1040)
    val sign = if (roi >= 0) "+" else ""
    val prefix = if (netPl >= 0) "+$" else "-$"
    val formattedNetPl = "$prefix${String.format(Locale.US, "%.0f", Math.abs(netPl))}"
    
    // Value Net PL
    textPaint.apply {
        color = if (netPl >= 0) 0xFF10B981.toInt() else 0xFFEF4444.toInt()
        textSize = 46f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText(formattedNetPl, 300f, 960f, textPaint)
    
    // Label Net PL
    textPaint.apply {
        color = 0xFF978F80.toInt()
        textSize = 24f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText("NET P/L", 300f, 1015f, textPaint)

    // Divider 1 (X = 450)
    val verticalDividerPaint = AndroidPaint().apply {
        color = 0xFFE5E2D7.toInt()
        strokeWidth = 3.5f
    }
    canvas.drawLine(450f, 930f, 450f, 1030f, verticalDividerPaint)

    // Win Rate (Col 2)
    textPaint.apply {
        color = 0xFF1F2937.toInt()
        textSize = 46f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText(String.format(Locale.US, "%.1f%%", winRate), 600f, 960f, textPaint)
    
    textPaint.apply {
        color = 0xFF978F80.toInt()
        textSize = 24f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText("WIN RATE", 600f, 1015f, textPaint)

    // Divider 2 (X = 750)
    canvas.drawLine(750f, 930f, 750f, 1030f, verticalDividerPaint)

    // ROI (Col 3)
    textPaint.apply {
        color = if (roi >= 0) 0xFF10B981.toInt() else 0xFFEF4444.toInt()
        textSize = 46f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText("$sign${String.format(Locale.US, "%.1f%%", roi)}", 900f, 960f, textPaint)
    
    textPaint.apply {
        color = 0xFF978F80.toInt()
        textSize = 24f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.BOLD)
    }
    canvas.drawText("ROI", 900f, 1015f, textPaint)

    // Divider after Stats
    canvas.drawLine(150f, 1100f, 1050f, 1100f, dividerPaint)

    // 5. Verification Bottom Stamp (Y = 1260)
    val leftTextPaint = AndroidPaint().apply {
        color = 0xFF9CA3AF.toInt()
        textSize = 28f
        typeface = AndroidTypeface.create(AndroidTypeface.SANS_SERIF, AndroidTypeface.ITALIC)
        textAlign = AndroidPaint.Align.LEFT
        isAntiAlias = true
    }
    canvas.drawText("Verified by BookFx", 180f, 1260f, leftTextPaint)

    // Right Verification Badge / Seal (Center at X = 1000, Y = 1250)
    val sealBgPaint = AndroidPaint().apply {
        color = 0xFF8C7B65.toInt()
        style = AndroidPaint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(1000f, 1250f, 40f, sealBgPaint)

    val checkPaint = AndroidPaint().apply {
        color = 0xFFFFFFFF.toInt()
        style = AndroidPaint.Style.STROKE
        strokeWidth = 6f
        strokeCap = AndroidPaint.Cap.ROUND
        isAntiAlias = true
    }
    val checkPath = AndroidPath().apply {
        moveTo(980f, 1250f)
        lineTo(995f, 1265f)
        lineTo(1020f, 1235f)
    }
    canvas.drawPath(checkPath, checkPaint)

    return bitmap
}
