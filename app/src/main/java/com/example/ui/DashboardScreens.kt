package com.example.ui

import androidx.compose.animation.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
                        containerColor = Color(0xFFEEF2FF),
                        contentColor = Color(0xFF2E6FF2)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) Color(0xFF2E6FF2) else Color(0xFFF1F3F9)
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
                                            .background(Color(0xFFE6F4EA))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = Color(0xFF137333),
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
                                        "Live Cash" -> Color(0xFFFFF7E6)
                                        "Prop Firm" -> Color(0xFFEEF2FF)
                                        else -> Color(0xFFF3F4F6)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = portfolio.type.uppercase(),
                                color = when (portfolio.type) {
                                    "Live Cash" -> Color(0xFFD97706)
                                    "Prop Firm" -> Color(0xFF4F46E5)
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
                            .background(Color(0xFFF8FAFC))
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
                                    color = if (isProfit) Color(0xFF137333) else Color(0xFFC5221F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                                Text(
                                    text = "${if (isProfit) "+" else ""}${String.format("%.1f", plPercent)}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProfit) Color(0xFF137333) else Color(0xFFC5221F)
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
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2E6FF2)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Account", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (!isActive) {
                                TextButton(
                                    onClick = { viewModel.activePortfolio = portfolio },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2E6FF2)),
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                            .background(Color(0xFFF1F3F9))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Live Cash", "Prop Firm", "Demo").forEach { type ->
                            val isSel = viewModel.portfolioTypeInput == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSel) Color.White else Color.Transparent)
                                    .clickable { viewModel.portfolioTypeInput = type }
                                    .border(
                                        width = if (isSel) 1.dp else 0.dp,
                                        color = if (isSel) Color(0xFFE2E8F0) else Color.Transparent,
                                        shape = RoundedCornerShape(9.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color(0xFF2E6FF2) else TextSecondary,
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
    val activePortfolio = viewModel.activePortfolio
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var selectedTab by remember { mutableStateOf("Calendar") }
    var showLogTradeDialog by remember { mutableStateOf(false) }
    
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
                val targetDay = selectedDate.get(Calendar.DAY_OF_MONTH)
                trades.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == targetYear &&
                    cal.get(Calendar.MONTH) == targetMonth &&
                    cal.get(Calendar.DAY_OF_MONTH) == targetDay
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
                val targetDay = selectedDate.get(Calendar.DAY_OF_MONTH)
                trades.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == targetYear &&
                    cal.get(Calendar.MONTH) == targetMonth &&
                    cal.get(Calendar.DAY_OF_MONTH) == targetDay
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

    // Dismiss dialog when trade is successfully added
    LaunchedEffect(viewModel.manageMessage) {
        if (viewModel.manageMessage == "Trade added successfully!") {
            showLogTradeDialog = false
            viewModel.manageMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground) // Premium Dynamic Theme Background
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
            Icon(Icons.Filled.EmojiEvents, contentDescription = "Trophy", tint = Color(0xFFFBBF24))
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

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Primary else Color.Transparent)
                                                .clickable {
                                                    selectedDate = (selectedDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNumber) }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    color = if (isSelected) Color.White else TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                                if (hasTradesOnThisDay) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else Primary)
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

                // Selected Day Date Card & Add FAB/Plus Button
                Text("Selected Day", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF374151) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Primary)
                            Text(
                                text = "${monthName.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.get(Calendar.DAY_OF_MONTH)}, ${selectedDate.get(Calendar.YEAR)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Primary)
                                .clickable {
                                    viewModel.manageMessage = null
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
                }

                // Trades List
                if (filteredTrades.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No trades logged for this day.", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredTrades) { trade ->
                            TradeJournalCard(trade = trade, onDelete = { viewModel.handleDeleteTrade(trade) })
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
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(monthRows.size) { rowIndex ->
                        val rowMonths = monthRows[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowMonths.forEach { mIdx ->
                                this@Row.MonthGridCell(
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
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(yearRows.size) { rowIndex ->
                        val rowYears = yearRows[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowYears.forEach { yr ->
                                this@Row.YearGridCell(
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
                                Text(
                                    text = "Log A Trade Transaction",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                IconButton(onClick = { showLogTradeDialog = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                                }
                            }

                            Text(
                                text = "Saving trade to active portfolio: '${activePortfolio?.name ?: "Default"}' for " +
                                       "${monthName.lowercase().replaceFirstChar { it.uppercase() }} " +
                                       "${selectedDate.get(Calendar.DAY_OF_MONTH)}, ${selectedDate.get(Calendar.YEAR)}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

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
                                onValueChange = { viewModel.tradeSymbol = it },
                                label = { Text("Trading Pair Symbol (e.g. EURUSD)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeEntryPrice,
                                onValueChange = { viewModel.tradeEntryPrice = it },
                                label = { Text("Entry Execution Price") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeExitPrice,
                                onValueChange = { viewModel.tradeExitPrice = it },
                                label = { Text("Exit Execution Price") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeSize,
                                onValueChange = { viewModel.tradeSize = it },
                                label = { Text("Volume (Lots / Units)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeBrokerage,
                                onValueChange = { viewModel.tradeBrokerage = it },
                                label = { Text("Brokerage / Fees paid ($)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeProfit,
                                onValueChange = { viewModel.tradeProfit = it },
                                label = { Text("Gross Profit / Loss ($ value)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = viewModel.tradeNotes,
                                onValueChange = { viewModel.tradeNotes = it },
                                label = { Text("Optional Comments / Notes") },
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
fun TradeJournalCard(trade: Trade, onDelete: () -> Unit) {
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
                            Text("$${String.format("%.4f", trade.exitPrice)}", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
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
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Log")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsScreen(viewModel: TradeViewModel) {
    val trades by viewModel.allTrades.collectAsState()
    val baseEquity = viewModel.currentUser?.totalEquity ?: 100.0

    if (trades.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PieChart,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Performance Metrics Standby",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Once you log full completed trades, we compile win rates, equity curves & metrics dynamically here.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    // Process statistics
    val totalTrades = trades.size
    val winTrades = trades.filter { it.profit > 0 }
    val lossTrades = trades.filter { it.profit <= 0 }
    val winCount = winTrades.size
    val lossCount = lossTrades.size
    val winRate = (winCount.toFloat() / totalTrades.toFloat()) * 100f
    
    val totalNetPl = trades.sumOf { it.profit - it.brokerage }
    
    val totalWinsVal = winTrades.sumOf { it.profit }
    val totalLossesVal = lossTrades.sumOf { -it.profit }
    val profitFactor = if (totalLossesVal > 0) totalWinsVal / totalLossesVal else totalWinsVal
    
    val avgWin = if (winCount > 0) totalWinsVal / winCount else 0.0
    val avgLoss = if (lossCount > 0) totalLossesVal / lossCount else 0.0

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-level summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Win Rate", fontSize = 11.sp, color = TextSecondary)
                    Text("${String.format("%.1f", winRate)}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (winRate >= 50f) GreenAccent else RedAccent)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Profit Factor", fontSize = 11.sp, color = TextSecondary)
                    Text(String.format("%.2f", profitFactor), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (profitFactor >= 1.0) Primary else RedAccent)
                }
            }
        }

        // Metrics Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Performance Breakdown", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                MetricItem("Total Trades", totalTrades.toString())
                MetricItem("Winning Trades", "$winCount  (${String.format("%.1f", (winCount.toFloat()/totalTrades)*100)}%)", GreenAccent)
                MetricItem("Losing Trades", "$lossCount  (${String.format("%.1f", (lossCount.toFloat()/totalTrades)*100)}%)", RedAccent)
                MetricItem("Average Win Size", "$${String.format("%.2f", avgWin)}", GreenAccent)
                MetricItem("Average Loss Size", "$${String.format("%.2f", avgLoss)}", RedAccent)
                MetricItem("Total Net Return", (if (totalNetPl>=0) "+" else "") + "$${String.format("%.2f", totalNetPl)}", if (totalNetPl >= 0) GreenAccent else RedAccent)
            }
        }

        // Canvas Donut Win/Loss Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Win / Loss Proportions",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val angleWin = (winCount.toFloat() / totalTrades.toFloat()) * 360f
                        val angleLoss = 360f - angleWin
                        
                        // Win chunk green
                        drawArc(
                            color = GreenAccent,
                            startAngle = -90f,
                            sweepAngle = angleWin,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx())
                        )
                        // Loss chunk red
                        drawArc(
                            color = RedAccent,
                            startAngle = -90f + angleWin,
                            sweepAngle = angleLoss,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx())
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${String.format("%.0f", winRate)}%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Win Rate", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(12.dp).background(GreenAccent, CircleShape))
                        Text("Wins ($winCount)", color = TextPrimary, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(12.dp).background(RedAccent, CircleShape))
                        Text("Losses ($lossCount)", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }

        // Beautiful Dynamic Canvas-compiled Equity Line Curve Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Performance Equity Curve",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Illustrates compound capital movement across chronologic trade list logs.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Build equity coordinates starting from initial equity
                // Chronological flow requires reversing standard order as newer is first indexed in DB, let's reverse items!
                val orderedTrades = trades.reversed()
                val equityNodes = remember(orderedTrades, baseEquity) {
                    val list = mutableListOf<Double>()
                    var currentEq = baseEquity
                    list.add(currentEq)
                    for (t in orderedTrades) {
                        currentEq += (t.profit - t.brokerage)
                        list.add(currentEq)
                    }
                    list
                }

                val minEq = equityNodes.minOrNull() ?: baseEquity
                val maxEq = equityNodes.maxOrNull() ?: baseEquity
                val range = if (maxEq - minEq > 0) maxEq - minEq else 100.0

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(DarkBackground, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val pointsCount = equityNodes.size
                    
                    if (pointsCount > 1) {
                        val xStep = width / (pointsCount - 1)
                        val path = Path()
                        val fillPath = Path()

                        for (i in 0 until pointsCount) {
                            val eq = equityNodes[i]
                            val x = i * xStep
                            // Draw y offset from bottom: lower equity maps to high Y coordinate
                            val normalizedY = ((eq - minEq) / range)
                            val y = height - (normalizedY.toFloat() * height)
                            
                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (i == pointsCount - 1) {
                                fillPath.lineTo(x, height)
                                fillPath.close()
                            }
                        }

                        // Bottom fading overlay
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Primary.copy(alpha = 0.3f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Outline line graph
                        drawPath(
                            path = path,
                            color = Primary,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Points accent draw
                        for (i in 0 until pointsCount) {
                            val eq = equityNodes[i]
                            val x = i * xStep
                            val normalizedY = ((eq - minEq) / range)
                            val y = height - (normalizedY.toFloat() * height)
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                            drawCircle(color = Primary, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Baseline: $${String.format("%.1f", baseEquity)}", fontSize = 11.sp, color = TextSecondary)
                    Text("Current Balance: $${String.format("%.2f", equityNodes.lastOrNull() ?: baseEquity)}", fontSize = 11.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
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
