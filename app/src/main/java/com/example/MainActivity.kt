package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import com.example.viewmodel.TradeViewModel
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HighlightItem(
    val key: String,
    val title: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconBgColor: Color,
    val iconTintColor: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TradeViewModel = viewModel()
            MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = viewModel.currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400, easing = EaseInOutCubic)) + 
                            slideInVertically(animationSpec = tween(400, easing = EaseInOutCubic), initialOffsetY = { it / 8 }) togetherWith
                            fadeOut(animationSpec = tween(200, easing = EaseInOutCubic))
                        },
                        label = "ScreenSwitch"
                    ) { targetScreen ->
                        when (targetScreen) {
                            "ONBOARDING" -> OnboardingScreen(viewModel = viewModel)
                            "LOGIN" -> LoginScreen(viewModel = viewModel)
                            "MAIN" -> MainPortal(viewModel = viewModel)
                            else -> OnboardingScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainPortal(viewModel: TradeViewModel) {
    val trades by viewModel.allTrades.collectAsState()
    val mistakes by viewModel.allMistakes.collectAsState()
    val user = viewModel.currentUser
    val activePortfolio = viewModel.activePortfolio
    
    val userName = user?.name ?: "Kartik"
    val baseEquity = activePortfolio?.startingEquity ?: user?.totalEquity ?: 100.0
    
    // Perform live statistics computations, filtering by active portfolio
    val filteredTrades = if (activePortfolio != null) {
        trades.filter { it.portfolioId == activePortfolio.id }
    } else {
        trades
    }
    
    val totalProfitPrice = filteredTrades.sumOf { it.profit }
    val totalBrokerageFee = filteredTrades.sumOf { it.brokerage }
    val activeEquityVal = baseEquity + totalProfitPrice - totalBrokerageFee
    
    val plDifference = activeEquityVal - baseEquity
    val plPercent = if (baseEquity > 0) (plDifference / baseEquity) * 100.0 else 0.0

    BackHandler(enabled = viewModel.currentMainTab != "DASHBOARD") {
        if (viewModel.currentMainTab == "SETTINGS" && viewModel.isViewingSettingsProfile) {
            viewModel.isViewingSettingsProfile = false
        } else {
            viewModel.currentMainTab = "DASHBOARD"
        }
    }

    val isJournalOrNotesTab = viewModel.currentMainTab == "JOURNAL" || viewModel.currentMainTab == "NOTES"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        val isFullBleedTab = viewModel.currentMainTab == "JOURNAL" || viewModel.currentMainTab == "NOTES"
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(
                    if (isFullBleedTab) Modifier else Modifier.padding(horizontal = 16.dp)
                )
        ) {
            if (viewModel.currentMainTab != "MANAGE" && viewModel.currentMainTab != "JOURNAL") {
                Spacer(modifier = Modifier.height(16.dp))
                
                val elementColor = TextPrimary
                
                // Header: Dynamic layout matching the mockup precisely with correct contrast colors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFullBleedTab) Modifier.padding(horizontal = 16.dp) else Modifier
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.currentMainTab == "DASHBOARD") {
                        val isGrinding = filteredTrades.size >= 2
                        val stageLabel = if (isGrinding) "Grinding Trader" else "Novice Trader"
                        val avatarColor = if (isGrinding) Color(0xFFFFF7E6) else Color(0xFFE9ECEF)
                        val iconColor = if (isGrinding) Color(0xFFD97706) else TextSecondary
                        val avatarIcon = if (isGrinding) Icons.Filled.Construction else Icons.Outlined.School

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(avatarColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = avatarIcon,
                                    contentDescription = "Avatar",
                                    tint = iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(text = stageLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                                Text(text = userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { viewModel.currentMainTab = "SETTINGS" }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (viewModel.currentMainTab == "SETTINGS" && viewModel.isViewingSettingsProfile) {
                                    viewModel.isViewingSettingsProfile = false
                                } else {
                                    viewModel.currentMainTab = "DASHBOARD"
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = elementColor)
                            }
                            Text(
                                text = when (viewModel.currentMainTab) {
                                    "MANAGE" -> "Manage Trades"
                                    "JOURNAL" -> "Journal Records"
                                    "ANALYTICS" -> "Analytics"
                                    "SETTINGS" -> if (viewModel.isViewingSettingsProfile) "My Profile" else "Settings"
                                    "NOTES" -> "Notes & Lessons"
                                    else -> "BookFx"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = elementColor
                            )
                        }
                        
                        if (viewModel.currentMainTab == "ANALYTICS") {
                            IconButton(onClick = { viewModel.showCertificateDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Achievements",
                                    tint = Color(0xFFFFC107)
                                )
                            }
                        } else if (viewModel.currentMainTab != "SETTINGS" && viewModel.currentMainTab != "NOTES") {
                            IconButton(onClick = { viewModel.handleLogout() }) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = elementColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Tab-based navigation contents with smooth transitions
            AnimatedContent(
                targetState = viewModel.currentMainTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280, easing = EaseInOutCubic)) + 
                    slideInVertically(animationSpec = tween(280, easing = EaseInOutCubic), initialOffsetY = { it / 14 }) togetherWith
                    fadeOut(animationSpec = tween(140, easing = EaseInOutCubic))
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "MainTabSwitch"
            ) { targetTab ->
                when (targetTab) {
                    "DASHBOARD" -> {
                        DashboardWidgets(
                            activeEquity = activeEquityVal,
                            plDifference = plDifference,
                            plPercent = plPercent,
                            trades = filteredTrades,
                            totalProfit = totalProfitPrice,
                            totalBrokerage = totalBrokerageFee,
                            activePortfolio = viewModel.activePortfolio,
                            mistakes = mistakes,
                            mistakeInput = viewModel.mistakeInput,
                            onMistakeInputChange = { viewModel.mistakeInput = it },
                            onAddMistake = { viewModel.handleAddMistake() },
                            onUpdateMistake = { viewModel.handleUpdateMistake(it) },
                            onDeleteMistake = { viewModel.handleDeleteMistake(it) },
                            onNavigate = { viewModel.currentMainTab = it },
                            onDeleteTrade = { viewModel.handleDeleteTrade(it) },
                            viewModel = viewModel
                        )
                    }
                    "MANAGE" -> ManageScreen(viewModel = viewModel)
                    "JOURNAL" -> JournalScreen(viewModel = viewModel)
                    "ANALYTICS" -> AnalyticsScreen(viewModel = viewModel)
                    "SETTINGS" -> SettingsScreen(viewModel = viewModel)
                    "NOTES" -> NotesScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DashboardWidgets(
    activeEquity: Double,
    plDifference: Double,
    plPercent: Double,
    trades: List<Trade>,
    totalProfit: Double,
    totalBrokerage: Double,
    activePortfolio: com.example.data.PortfolioAccount?,
    mistakes: List<com.example.data.Mistake>,
    mistakeInput: String,
    onMistakeInputChange: (String) -> Unit,
    onAddMistake: () -> Unit,
    onUpdateMistake: (com.example.data.Mistake) -> Unit,
    onDeleteMistake: (com.example.data.Mistake) -> Unit,
    onNavigate: (String) -> Unit,
    onDeleteTrade: (Trade) -> Unit,
    viewModel: com.example.viewmodel.TradeViewModel
) {
    val scrollState = rememberScrollState()
    var showReorderSheet by remember { mutableStateOf(false) }

    // Math metrics
    val netPl = totalProfit - totalBrokerage
    
    val calendarNow = java.util.Calendar.getInstance()
    val monthTrades = trades.filter {
        val tradeCal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
        tradeCal.get(java.util.Calendar.YEAR) == calendarNow.get(java.util.Calendar.YEAR) &&
        tradeCal.get(java.util.Calendar.MONTH) == calendarNow.get(java.util.Calendar.MONTH)
    }
    val monthNetPl = monthTrades.sumOf { it.profit - it.brokerage }
    
    val totalCount = trades.size
    val winTrades = trades.filter { it.profit > 0 }
    val lossTrades = trades.filter { it.profit < 0 }
    val wins = winTrades.size
    val losses = lossTrades.size
    val winRate = if (totalCount > 0) (wins.toDouble() / totalCount) * 100.0 else 0.0
    
    val winSum = winTrades.sumOf { it.profit }
    val lossSum = lossTrades.sumOf { -it.profit }
    val pf = if (lossSum > 0) winSum / lossSum else winSum
    
    val avgWin = if (wins > 0) winSum / wins else 0.0
    val avgLoss = if (losses > 0) lossSum / losses else 0.0
    val avgRR = if (avgLoss > 0) avgWin / avgLoss else 0.0
    
    // Max DD & Current DD logic
    var peak = activePortfolio?.startingEquity ?: 100.0
    var currentEquity = peak
    var maxDdVal = 0.0
    val sortedTrades = trades.sortedBy { it.timestamp }
    for (t in sortedTrades) {
         currentEquity += (t.profit - t.brokerage)
         if (currentEquity > peak) {
             peak = currentEquity
         }
         val dd = peak - currentEquity
         if (dd > maxDdVal) {
             maxDdVal = dd
         }
    }
    
    var peakVal = activePortfolio?.startingEquity ?: 100.0
    var currEquity = peakVal
    for (t in sortedTrades) {
        currEquity += (t.profit - t.brokerage)
        if (currEquity > peakVal) {
            peakVal = currEquity
        }
    }
    val currentDdVal = Math.max(0.0, peakVal - currEquity)
    
    // Win/Loss Days
    val sdfDate = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
    val tradesByDay = trades.groupBy { sdfDate.format(java.util.Date(it.timestamp)) }
    val winDaysCount = tradesByDay.values.count { dayTrades ->
        dayTrades.sumOf { it.profit - it.brokerage } > 0
    }
    val lossDaysCount = tradesByDay.values.count { dayTrades ->
        dayTrades.sumOf { it.profit - it.brokerage } < 0
    }
    
    // Win Streak
    var maxWinStreak = 0
    var currentStreak = 0
    for (t in sortedTrades) {
        if (t.profit > 0) {
            currentStreak++
            if (currentStreak > maxWinStreak) {
                maxWinStreak = currentStreak
            }
        } else if (t.profit < 0) {
            currentStreak = 0
        }
    }
    
    val baseCapitalValue = activePortfolio?.startingEquity ?: 100.0
    val roiPercentage = if (baseCapitalValue > 0) (netPl / baseCapitalValue) * 100.0 else 0.0
    val roiString = (if (roiPercentage >= 0) "+" else "") + String.format(java.util.Locale.US, "%.1f%%", roiPercentage)
    val avgRRStr = if (avgRR > 0) "1:${String.format(java.util.Locale.US, "%.1f", avgRR)}" else "0.0"

    val allOptions = listOf(
        HighlightItem(
            key = "total_pl",
            title = "Total P/L",
            value = (if (netPl >= 0) "+$" else "-$") + String.format(java.util.Locale.US, "%.2f", if (netPl >= 0) netPl else -netPl),
            icon = Icons.Default.AttachMoney,
            iconBgColor = Color(0xFFFCE8E6),
            iconTintColor = Color(0xFFEA4335)
        ),
        HighlightItem(
            key = "brokerage",
            title = "Brokerage",
            value = "$${String.format(java.util.Locale.US, "%.2f", totalBrokerage)}",
            icon = Icons.Default.Receipt,
            iconBgColor = Color(0xFFF3EAE3),
            iconTintColor = Color(0xFF8B5A2B)
        ),
        HighlightItem(
            key = "month_pl",
            title = "Month P/L",
            value = (if (monthNetPl >= 0) "+$" else "-$") + String.format(java.util.Locale.US, "%.2f", if (monthNetPl >= 0) monthNetPl else -monthNetPl),
            icon = Icons.Default.CalendarToday,
            iconBgColor = Color(0xFFE8F5E9),
            iconTintColor = Color(0xFF34A853)
        ),
        HighlightItem(
            key = "roi",
            title = "ROI",
            value = roiString,
            icon = Icons.Default.Percent,
            iconBgColor = Color(0xFFEBF7EE),
            iconTintColor = Color(0xFF137333)
        ),
        HighlightItem(
            key = "win_rate",
            title = "Win Rate",
            value = String.format(java.util.Locale.US, "%.1f%%", winRate),
            icon = Icons.Default.PieChart,
            iconBgColor = Color(0xFFF3E8FF),
            iconTintColor = Color(0xFF8B5CF6)
        ),
        HighlightItem(
            key = "profit_factor",
            title = "Profit Factor",
            value = String.format(java.util.Locale.US, "%.2f", pf),
            icon = Icons.Default.Balance,
            iconBgColor = Color(0xFFE3F2FD),
            iconTintColor = Color(0xFF1A73E8)
        ),
        HighlightItem(
            key = "avg_rr",
            title = "Avg R:R",
            value = avgRRStr,
            icon = Icons.Default.CompareArrows,
            iconBgColor = Color(0xFFE0F7FA),
            iconTintColor = Color(0xFF00ACC1)
        ),
        HighlightItem(
            key = "trades",
            title = "Trades",
            value = "$totalCount",
            icon = Icons.Default.ShowChart,
            iconBgColor = Color(0xFFFFF3E0),
            iconTintColor = Color(0xFFFB8C00)
        ),
        HighlightItem(
            key = "max_dd",
            title = "Max DD",
            value = "-$${String.format(java.util.Locale.US, "%.2f", maxDdVal)}",
            icon = Icons.Default.TrendingDown,
            iconBgColor = Color(0xFFFCE8E6),
            iconTintColor = Color(0xFFEA4335)
        ),
        HighlightItem(
            key = "current_dd",
            title = "Current DD",
            value = "-$${String.format(java.util.Locale.US, "%.2f", currentDdVal)}",
            icon = Icons.Default.Warning,
            iconBgColor = Color(0xFFFFF3E0),
            iconTintColor = Color(0xFFFB8C00)
        ),
        HighlightItem(
            key = "win_days",
            title = "Win Days",
            value = "$winDaysCount days",
            icon = Icons.Default.CheckCircle,
            iconBgColor = Color(0xFFE8F5E9),
            iconTintColor = Color(0xFF34A853)
        ),
        HighlightItem(
            key = "loss_days",
            title = "Loss Days",
            value = "$lossDaysCount days",
            icon = Icons.Default.Cancel,
            iconBgColor = Color(0xFFFCE8E6),
            iconTintColor = Color(0xFFEA4335)
        ),
        HighlightItem(
            key = "win_streak",
            title = "Win Streak",
            value = "$maxWinStreak wins",
            icon = Icons.Default.TrendingUp,
            iconBgColor = Color(0xFFE0F2F1),
            iconTintColor = Color(0xFF00897B)
        )
    )

    // Resolve highlighted top 3 options based on highlightsOrderText
    val currentOrderKeys = viewModel.highlightsOrderText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val orderedHighlights = currentOrderKeys.mapNotNull { key ->
        allOptions.find { it.key == key }
    }
    val topHighlightsToShow = if (orderedHighlights.isNotEmpty()) orderedHighlights.take(3) else allOptions.take(3)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Total Equity widget matching mockup proportions
            Column {
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Total Equity", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "$",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%,.2f", activeEquity), 
                        fontSize = 40.sp, 
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                // Profit / Loss change chip
                val isProfit = plDifference >= 0
                val chipBg = if (isProfit) Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
                val chipColor = if (isProfit) Color(0xFF137333) else Color(0xFFC5221F)
                val prefix = if (isProfit) "+" else ""
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${if (isProfit) "↑" else "↓"} $prefix$${String.format(java.util.Locale.US, "%,.2f", plDifference)} (${String.format(java.util.Locale.US, "%.1f", plPercent)}%)",
                        color = chipColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful curved inline line graph directly below the chip
                val baseCapital = activePortfolio?.startingEquity ?: 10000.0
                val orderedTrades = remember(trades) { trades.sortedBy { it.timestamp } }
                val equityNodes = remember(orderedTrades, baseCapital) {
                    val list = mutableListOf<Double>()
                    var currentEq = baseCapital
                    list.add(currentEq)
                    for (t in orderedTrades) {
                        currentEq += (t.profit - t.brokerage)
                        list.add(currentEq)
                    }
                    list
                }

                val minEq = equityNodes.minOrNull() ?: baseCapital
                val maxEq = equityNodes.maxOrNull() ?: baseCapital
                val range = if (maxEq - minEq > 0) maxEq - minEq else 100.0
                val isPositiveTrend = plDifference >= 0.0
                val curveColor = if (isPositiveTrend) Color(0xFF10B981) else Color(0xFFEF4444)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val pointsCount = equityNodes.size

                        if (pointsCount > 1) {
                            val xStep = width / (pointsCount - 1)
                            val path = androidx.compose.ui.graphics.Path()
                            val fillPath = androidx.compose.ui.graphics.Path()

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
                                        curveColor.copy(alpha = 0.00f)
                                    ),
                                    startY = 0f,
                                    endY = height
                                )
                            )

                            drawPath(
                                path = path,
                                color = curveColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 3.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        } else {
                            // If no trades (just 1 baseline point), draw a clean middle horizontal line
                            drawLine(
                                color = TextSecondary.copy(alpha = 0.3f),
                                start = Offset(0f, height / 2f),
                                end = Offset(width, height / 2f),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                    }
                }
            }

            // Circular Navigation Buttons Section matching screenshots exactly!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickNavItem(
                    label = "Manage",
                    bgColor = Color(0xFFF1F3F9),
                    tintColor = Color(0xFF111827),
                    icon = Icons.Default.Edit,
                    onClick = { onNavigate("MANAGE") }
                )
                QuickNavItem(
                    label = "Journal",
                    bgColor = Color(0xFFDBEAFE),
                    tintColor = Color(0xFF2E6FF2),
                    icon = Icons.Default.Book,
                    onClick = { onNavigate("JOURNAL") }
                )
                QuickNavItem(
                    label = "Analytics",
                    bgColor = Color(0xFFF3E8FF),
                    tintColor = Color(0xFF8B5CF6),
                    icon = Icons.Default.BarChart,
                    onClick = { onNavigate("ANALYTICS") }
                )
                QuickNavItem(
                    label = "Notes",
                    bgColor = Color(0xFFE0F2F1),
                    tintColor = Color(0xFF00796B),
                    icon = Icons.Default.Notes,
                    onClick = { onNavigate("NOTES") }
                )
            }

            // Highlights Section with Custom Settings-Tuning Options reordering!
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Highlights", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F3F9), CircleShape)
                            .clickable { showReorderSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Reorder Highlights",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    topHighlightsToShow.forEach { highlight ->
                        HighlightStatCard(
                            title = highlight.title,
                            value = highlight.value,
                            icon = highlight.icon,
                            iconBgColor = highlight.iconBgColor,
                            iconTintColor = highlight.iconTintColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (topHighlightsToShow.size < 3) {
                        for (i in 0 until (3 - topHighlightsToShow.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

        // Recent Activity Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Recent Activity", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            
            if (trades.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFF1F3F9))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No recent trades registered.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Take the 5 most recent trades
                val recentFive = trades.sortedByDescending { it.timestamp }.take(5)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentFive.forEach { trade ->
                        val isProfit = trade.profit >= 0
                        val formatPl = (if (isProfit) "+$" else "-$") + String.format("%.2f", if (isProfit) trade.profit else -trade.profit)
                        val badgeBg = if (trade.isBuy) Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
                        val badgeColor = if (trade.isBuy) Color(0xFF137333) else Color(0xFFC5221F)
                        val plColor = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444)
                        val formattedDate = remember(trade.timestamp) {
                            try {
                                java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).format(java.util.Date(trade.timestamp))
                            } catch (e: Exception) {
                                ""
                            }
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.journalSelectedDateEpochMilli = trade.timestamp
                                    viewModel.startEditTrade(trade)
                                    viewModel.showLogTradeDialogInJournal = true
                                    viewModel.currentMainTab = "JOURNAL"
                                },
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFF1F3F9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Trade icon
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (isProfit) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = if (isProfit) Color(0xFF137333) else Color(0xFFC5221F),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    // Symbol & trade details
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = trade.symbol,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(badgeBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (trade.isBuy) "BUY" else "SELL",
                                                    color = badgeColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Date: $formattedDate",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                
                                // Profit, Brokerage & actions
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatPl,
                                            color = plColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (trade.brokerage > 0) {
                                            Text(
                                                text = "Fee: $${String.format("%.2f", trade.brokerage)}",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Drop in the bottom sheet overlay here
        if (showReorderSheet) {
            var localKeys by remember(showReorderSheet) {
                mutableStateOf(currentOrderKeys)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showReorderSheet = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .clickable(enabled = false) {},
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reorder Highlights",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "Done",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E6FF2),
                                modifier = Modifier
                                    .clickable {
                                        viewModel.updateHighlightsOrder(localKeys.joinToString(","))
                                        showReorderSheet = false
                                    }
                                    .padding(vertical = 4.dp, horizontal = 12.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF1F3F9))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.6f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            localKeys.forEachIndexed { index, key ->
                                val item = allOptions.find { it.key == key }
                                if (item != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(item.iconBgColor, RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = null,
                                                    tint = item.iconTintColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color(0xFF111827)
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (index > 0) {
                                                        val ml = localKeys.toMutableList()
                                                        val temp = ml[index]
                                                        ml[index] = ml[index - 1]
                                                        ml[index - 1] = temp
                                                        localKeys = ml
                                                    }
                                                },
                                                enabled = index > 0,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Move Up",
                                                    tint = if (index > 0) Color(0xFF2E6FF2) else Color.LightGray.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (index < localKeys.size - 1) {
                                                        val ml = localKeys.toMutableList()
                                                        val temp = ml[index]
                                                        ml[index] = ml[index + 1]
                                                        ml[index + 1] = temp
                                                        localKeys = ml
                                                    }
                                                },
                                                enabled = index < localKeys.size - 1,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Move Down",
                                                    tint = if (index < localKeys.size - 1) Color(0xFF2E6FF2) else Color.LightGray.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Drag Grabber",
                                                tint = Color(0xFF9CA3AF),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


    }
}

@Composable
fun QuickNavItem(
    label: String,
    bgColor: Color,
    tintColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(bgColor, CircleShape)
                .border(1.dp, Color(0xFFF1F3F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
fun HighlightStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFF1F3F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTintColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LightTradeJournalCard(trade: Trade, onDelete: () -> Unit) {
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background((if (trade.isBuy) GreenAccent else RedAccent).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (trade.isBuy) "BUY" else "SELL",
                                color = if (trade.isBuy) GreenAccent else RedAccent,
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
                        color = if (trade.profit >= 0) GreenAccent else RedAccent,
                        fontSize = 16.sp,
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
                    HorizontalDivider(color = if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFF1F3F9), modifier = Modifier.padding(bottom = 12.dp))
                    
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
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = RedAccent)
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
fun BottomNavigationBar(activeTab: String, onTabSelect: (String) -> Unit) {
    val isDarkBg = isSystemInDarkMode
    val containerColor = DarkSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = containerColor,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                label = "Dashboard",
                icon = Icons.Default.Dashboard,
                isActive = activeTab == "DASHBOARD",
                isDarkBg = isDarkBg,
                onClick = { onTabSelect("DASHBOARD") }
            )
            NavBarItem(
                label = "Accounts",
                icon = Icons.Default.AccountBalanceWallet,
                isActive = activeTab == "MANAGE",
                isDarkBg = isDarkBg,
                onClick = { onTabSelect("MANAGE") }
            )
            NavBarItem(
                label = "Ledger",
                icon = Icons.Default.Book,
                isActive = activeTab == "JOURNAL",
                isDarkBg = isDarkBg,
                onClick = { onTabSelect("JOURNAL") }
            )
            NavBarItem(
                label = "Stats",
                icon = Icons.Default.TrendingUp,
                isActive = activeTab == "ANALYTICS",
                isDarkBg = isDarkBg,
                onClick = { onTabSelect("ANALYTICS") }
            )
        }
    }
}

@Composable
fun NavBarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    isDarkBg: Boolean,
    onClick: () -> Unit
) {
    val activeColor = if (isDarkBg) Color(0xFF60A5FA) else Color(0xFF2E6FF2)
    val inactiveColor = if (isDarkBg) Color(0xFF9CA3AF) else Color(0xFF8F9BB3)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) activeColor else inactiveColor
        )
    }
}

@Composable
fun NotesScreen(viewModel: TradeViewModel) {
    val focusRequester = remember { FocusRequester() }
    
    // Track TextFieldValue locally to keep cursor selection stable and accurate.
    var notesValue by remember {
        val rawText = viewModel.simpleNotesText
        val initialText = if (rawText.isEmpty()) "• " else if (!rawText.startsWith("• ")) "• $rawText" else rawText
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }

    var showConfirmClear by remember { mutableStateOf(false) }

    // Sync with viewModel when empty or missing prefix to ensure valid start
    LaunchedEffect(Unit) {
        val rawText = viewModel.simpleNotesText
        if (rawText.isEmpty() || !rawText.startsWith("• ")) {
            val correctStart = if (rawText.isEmpty()) "• " else "• " + rawText
            viewModel.updateSimpleNotes(correctStart)
            notesValue = TextFieldValue(correctStart, TextRange(correctStart.length))
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Clear Notes?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This will delete all content from your Notes. This action cannot be undone.", fontSize = 14.sp, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClear = false
                        notesValue = TextFieldValue("• ", TextRange(2))
                        viewModel.updateSimpleNotes("• ")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .padding(bottom = 24.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSystemInDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { focusRequester.requestFocus() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Single Click Editable multi-line area matching requirements perfectly
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    val textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 26.sp
                    )
                    
                    BasicTextField(
                        value = notesValue,
                        onValueChange = { newVal ->
                            val finalVal = adjustNotesValue(notesValue, newVal)
                            notesValue = finalVal
                            viewModel.updateSimpleNotes(finalVal.text)
                        },
                        textStyle = textStyle,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Primary),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            if (notesValue.text.isEmpty() || notesValue.text == "• ") {
                                Text(
                                    text = "• Type your trading notes here...",
                                    style = textStyle.copy(color = TextSecondary.copy(alpha = 0.6f))
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showConfirmClear = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444).copy(alpha = 0.1f),
                contentColor = Color(0xFFEF4444)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear Notes",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Clear Notes", fontWeight = FontWeight.Bold)
        }
    }
}

fun keepSelectionOutOfPrefixes(text: String, selection: TextRange): TextRange {
    try {
        var start = selection.start
        var end = selection.end
        
        // Only protect the "• " prefix on the absolute first line (0..1 indices)
        if (text.startsWith("• ")) {
            if (start in 0..1) {
                start = 2
            }
            if (end in 0..1) {
                end = 2
            }
        }
        return TextRange(start.coerceIn(0, text.length), end.coerceIn(0, text.length))
    } catch (e: Exception) {
        return selection
    }
}

fun adjustNotesValue(oldVal: TextFieldValue, newVal: TextFieldValue): TextFieldValue {
    try {
        val oldText = oldVal.text
        var newText = newVal.text

        // Ensure the entire text is not completely empty, and always begins with "• "
        if (newText.isEmpty()) {
            newText = "• "
        } else if (!newText.startsWith("• ")) {
            if (newText.startsWith("•")) {
                newText = "• " + newText.removePrefix("•")
            } else {
                newText = "• " + newText
            }
        }

        // If selection changed but text is the same, adjust selection out of first line bullet
        if (oldText == newText) {
            val adjustedSelection = keepSelectionOutOfPrefixes(newText, newVal.selection)
            return newVal.copy(selection = adjustedSelection)
        }

        // Auto-bullets helper: If user presses enter and inserts a newline, 
        // we can add a helper bullet point "• " on the new line, which can be freely deleted.
        if (newText.length > oldText.length) {
            val addedCount = newText.length - oldText.length
            val selectionStart = newVal.selection.start.coerceIn(0, newText.length)
            val addedStart = (selectionStart - addedCount).coerceIn(0, newText.length)
            if (addedStart <= selectionStart) {
                val addedText = newText.substring(addedStart, selectionStart)
                if (addedText == "\n") {
                    val beforeInserted = newText.substring(0, selectionStart)
                    val afterInserted = newText.substring(selectionStart)
                    val updatedText = beforeInserted + "• " + afterInserted
                    val newSelection = TextRange((selectionStart + 2).coerceIn(0, updatedText.length))
                    val finalSel = keepSelectionOutOfPrefixes(updatedText, newSelection)
                    return newVal.copy(text = updatedText, selection = finalSel)
                }
            }
        }

        // Keep selection in range
        var newSelection = newVal.selection
        if (newText != newVal.text) {
            val diff = newText.length - newVal.text.length
            val originalStart = newVal.selection.start
            if (originalStart >= newVal.text.length) {
                newSelection = TextRange(newText.length)
            } else {
                newSelection = TextRange((originalStart + diff).coerceIn(0, newText.length))
            }
        }

        val finalSelection = keepSelectionOutOfPrefixes(newText, newSelection)
        return newVal.copy(text = newText, selection = finalSelection)
    } catch (e: Exception) {
        android.util.Log.e("NotesScreen", "Error during adjustNotesValue", e)
        return newVal
    }
}

