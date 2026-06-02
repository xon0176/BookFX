package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Trade
import com.example.data.User
import com.example.data.Mistake
import com.example.repository.TradeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CommunityPost(
    val id: Int,
    val author: String,
    val title: String,
    val description: String,
    val timestamp: String,
    var likes: Int,
    var isLiked: Boolean = false
)

class TradeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TradeRepository
    
    // DB flows
    val allTrades: StateFlow<List<Trade>>
    val allPortfolios: StateFlow<List<com.example.data.PortfolioAccount>>
    val allMistakes: StateFlow<List<Mistake>>
    var activePortfolio by mutableStateOf<com.example.data.PortfolioAccount?>(null)
    
    // Active User State
    var currentUser by mutableStateOf<User?>(null)
        private set

    // Simple view-based navigation state
    // "ONBOARDING", "LOGIN", "MAIN"
    var currentScreen by mutableStateOf("ONBOARDING")

    // Sub-modules on the MAIN screen
    // "DASHBOARD", "MANAGE", "JOURNAL", "ANALYTICS", "CALC", "COMMUNITY"
    var currentMainTab by mutableStateOf("DASHBOARD")
    var isViewingSettingsProfile by mutableStateOf(false)

    // Settings States
    var isDarkMode by mutableStateOf(false)
    var enableWeekendTrading by mutableStateOf(true)

    fun handleUpdateCurrency(newCurrency: String) {
        val user = currentUser ?: return
        viewModelScope.launch {
            val updated = user.copy(currency = newCurrency)
            repository.updateUser(updated)
            currentUser = updated
        }
    }

    fun handleUpdateUser(updatedUser: User) {
        viewModelScope.launch {
            repository.updateUser(updatedUser)
            currentUser = updatedUser
        }
    }

    fun exportDataToJson(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val rootJson = org.json.JSONObject()
                
                // Trades Setup
                val tradesArray = org.json.JSONArray()
                allTrades.value.forEach { t ->
                    val tJson = org.json.JSONObject().apply {
                        put("id", t.id)
                        put("symbol", t.symbol)
                        put("isBuy", t.isBuy)
                        put("entryPrice", t.entryPrice)
                        put("exitPrice", t.exitPrice)
                        put("size", t.size)
                        put("profit", t.profit)
                        put("brokerage", t.brokerage)
                        put("timestamp", t.timestamp)
                        put("notes", t.notes)
                        put("portfolioId", t.portfolioId)
                    }
                    tradesArray.put(tJson)
                }
                rootJson.put("trades", tradesArray)

                // Portfolios Setup
                val portfoliosArray = org.json.JSONArray()
                allPortfolios.value.forEach { p ->
                    val pJson = org.json.JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("broker", p.broker)
                        put("startingEquity", p.startingEquity)
                        put("type", p.type)
                        put("currency", p.currency)
                        put("description", p.description)
                    }
                    portfoliosArray.put(pJson)
                }
                rootJson.put("portfolios", portfoliosArray)

                // Mistakes Setup
                val mistakesArray = org.json.JSONArray()
                allMistakes.value.forEach { m ->
                    val mJson = org.json.JSONObject().apply {
                        put("id", m.id)
                        put("content", m.content)
                        put("timestamp", m.timestamp)
                    }
                    mistakesArray.put(mJson)
                }
                rootJson.put("mistakes", mistakesArray)

                onComplete(rootJson.toString(2))
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete("")
            }
        }
    }

    fun importDataFromJson(jsonStr: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val rootJson = org.json.JSONObject(jsonStr)
                
                // Clear existing tables
                repository.clearTrades()
                repository.clearPortfolios()
                repository.clearMistakes()

                // Parse Portfolio accounts
                if (rootJson.has("portfolios")) {
                    val pArr = rootJson.getJSONArray("portfolios")
                    for (i in 0 until pArr.length()) {
                        val pObj = pArr.getJSONObject(i)
                        val id = pObj.optInt("id", 0)
                        val name = pObj.optString("name", "Imported Account")
                        val broker = pObj.optString("broker", "Unknown")
                        val startingEquity = pObj.optDouble("startingEquity", 100.0)
                        val type = pObj.optString("type", "Live Cash")
                        val currency = pObj.optString("currency", "USD")
                        val description = pObj.optString("description", "")
                        
                        val item = com.example.data.PortfolioAccount(
                            id = id,
                            name = name,
                            broker = broker,
                            startingEquity = startingEquity,
                            type = type,
                            currency = currency,
                            description = description
                        )
                        repository.insertPortfolio(item)
                    }
                }

                // Parse Trades
                if (rootJson.has("trades")) {
                    val tArr = rootJson.getJSONArray("trades")
                    for (i in 0 until tArr.length()) {
                        val tObj = tArr.getJSONObject(i)
                        val id = tObj.optInt("id", 0)
                        val symbol = tObj.optString("symbol", "N/A")
                        val isBuy = tObj.optBoolean("isBuy", true)
                        val entryPrice = tObj.optDouble("entryPrice", 0.0)
                        val exitPrice = tObj.optDouble("exitPrice", 0.0)
                        val size = tObj.optDouble("size", 1.0)
                        val profit = tObj.optDouble("profit", 0.0)
                        val brokerage = tObj.optDouble("brokerage", 0.0)
                        val timestamp = tObj.optLong("timestamp", System.currentTimeMillis())
                        val notes = tObj.optString("notes", "")
                        val portfolioId = tObj.optInt("portfolioId", 0)

                        val item = Trade(
                            id = id,
                            symbol = symbol,
                            isBuy = isBuy,
                            entryPrice = entryPrice,
                            exitPrice = exitPrice,
                            size = size,
                            profit = profit,
                            brokerage = brokerage,
                            timestamp = timestamp,
                            notes = notes,
                            portfolioId = portfolioId
                        )
                        repository.insertTrade(item)
                    }
                }

                // Parse Mistakes
                if (rootJson.has("mistakes")) {
                    val mArr = rootJson.getJSONArray("mistakes")
                    for (i in 0 until mArr.length()) {
                        val mObj = mArr.getJSONObject(i)
                        val id = mObj.optInt("id", 0)
                        val content = mObj.optString("content", "")
                        val timestamp = mObj.optLong("timestamp", System.currentTimeMillis())

                        val item = Mistake(
                            id = id,
                            content = content,
                            timestamp = timestamp
                        )
                        repository.insertMistake(item)
                    }
                }

                // Make sure we select the first active portfolio if needed
                val ports = repository.getAllPortfolios()
                if (ports.isNotEmpty()) {
                    activePortfolio = ports.first()
                } else {
                    activePortfolio = null
                }

                onResult(true, "Data imported successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Import failed: ${e.localizedMessage ?: "Invalid JSON format"}")
            }
        }
    }

    // Form states represent exact user inputs as requested
    var emailInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")
    var confirmPasswordInput by mutableStateOf("")
    var nameInput by mutableStateOf("")
    var authError by mutableStateOf<String?>(null)

    // Dedicated onboarding steps & state fields to support multiple slides
    var onboardingStep by mutableStateOf(1)
    var traderAliasInput by mutableStateOf("")
    var countryInput by mutableStateOf("India")
    var primaryInstrumentInput by mutableStateOf("Equity")
    var baseCapitalInput by mutableStateOf("100.00")
    var currencyInput by mutableStateOf("USD")

    fun validateStep1(): Boolean {
        authError = null
        if (emailInput.isBlank()) {
            authError = "Email Address cannot be empty."
            return false
        }
        if (!emailInput.contains("@") || !emailInput.contains(".")) {
            authError = "Please enter a valid email address."
            return false
        }
        if (passwordInput.isBlank()) {
            authError = "Password cannot be empty."
            return false
        }
        if (passwordInput.length < 6) {
            authError = "Password must be at least 6 characters."
            return false
        }
        if (passwordInput != confirmPasswordInput) {
            authError = "Passwords do not match."
            return false
        }
        return true
    }

    fun validateStep2(): Boolean {
        authError = null
        if (nameInput.isBlank()) {
            authError = "Full Name cannot be empty."
            return false
        }
        val cleanAlias = nameInput.trim().lowercase().replace("\\s+".toRegex(), "_")
        traderAliasInput = if (cleanAlias.isNotEmpty()) cleanAlias else "trader"
        return true
    }

    fun validateStep3(): Boolean {
        authError = null
        if (countryInput.isBlank()) {
            authError = "Please choose a Country."
            return false
        }
        if (primaryInstrumentInput.isBlank()) {
            authError = "Please choose a Primary Instrument."
            return false
        }
        val capital = baseCapitalInput.toDoubleOrNull()
        if (capital == null || capital <= 0.0) {
            authError = "Please enter a valid positive Base Capital."
            return false
        }
        return true
    }

    // Login Form State
    var loginEmailInput by mutableStateOf("")
    var loginPasswordInput by mutableStateOf("")

    // Manage/Add Trade Screen Fields
    var tradeSymbol by mutableStateOf("")
    var tradeIsBuy by mutableStateOf(true) // true = BUY, false = SELL
    var tradeEntryPrice by mutableStateOf("")
    var tradeExitPrice by mutableStateOf("")
    var tradeSize by mutableStateOf("")
    var tradeProfit by mutableStateOf("")
    var tradeBrokerage by mutableStateOf("")
    var tradeNotes by mutableStateOf("")
    var manageMessage by mutableStateOf<String?>(null)

    // Interactive Custom Portfolio fields
    var portfolioNameInput by mutableStateOf("")
    var portfolioBrokerInput by mutableStateOf("")
    var portfolioStartingEquityInput by mutableStateOf("10000.00")
    var portfolioTypeInput by mutableStateOf("Live Cash") // "Live Cash", "Prop Firm", "Demo"
    var portfolioCurrencyInput by mutableStateOf("USD")
    var portfolioDescInput by mutableStateOf("")

    // Mistakes Notepad Input
    var mistakeInput by mutableStateOf("")

    private val prefs = application.getSharedPreferences("bookfx_notes", android.content.Context.MODE_PRIVATE)

    var simpleNotesText by mutableStateOf(
        prefs.getString(
            "simple_notes",
            ""
        ) ?: ""
    )

    fun updateSimpleNotes(newText: String) {
        simpleNotesText = newText
        prefs.edit().putString("simple_notes", newText).apply()
    }

    var highlightsOrderText by mutableStateOf(
        prefs.getString(
            "highlights_order",
            "total_pl,brokerage,month_pl,roi,win_rate,profit_factor,avg_rr,trades,max_dd,current_dd,win_days,loss_days,win_streak"
        ) ?: "total_pl,brokerage,month_pl,roi,win_rate,profit_factor,avg_rr,trades,max_dd,current_dd,win_days,loss_days,win_streak"
    )

    fun updateHighlightsOrder(newText: String) {
        highlightsOrderText = newText
        prefs.edit().putString("highlights_order", newText).apply()
    }

    // Interactive Community Feed
    var communityPosts = mutableStateOf<List<CommunityPost>>(
        listOf(
            CommunityPost(
                id = 1,
                author = "EliteScalper99",
                title = "EURUSD Breakout Trade Idea",
                description = "EURUSD is breaking out of a clean daily ascending triangle. Watching for a retest of 1.0850 before taking a long position. Target is 1.0920, SL at 1.0820.",
                timestamp = "10 mins ago",
                likes = 14
            ),
            CommunityPost(
                id = 2,
                author = "CryptoMax",
                title = "BTCUSD Bullish Pennant",
                description = "Bitcoin forms a clear consolidation pennant on the 4H chart. A candle close above $68,500 should trigger a fast target rally to $71k. Highly bullish pattern.",
                timestamp = "1 hour ago",
                likes = 28
            ),
            CommunityPost(
                id = 3,
                author = "ForexQueen",
                title = "Gold (XAUUSD) Triple Top?",
                description = "Gold is approaching key psychological resistance at $2,430. Watching for bearish divergence on the RSI to scale into a swing short position.",
                timestamp = "3 hours ago",
                likes = 8
            )
        )
    )

    var newPostTitle by mutableStateOf("")
    var newPostContent by mutableStateOf("")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TradeRepository(database.tradeDao())
        
        allTrades = repository.getAllTradesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
            
        allPortfolios = repository.getAllPortfoliosFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allMistakes = repository.getAllMistakesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
            
        // Sequential, safe database initializations to prevent SQLite write collisions or locked exceptions
        viewModelScope.launch {
            checkAndPreloadDefaultMistakes()
            
            val user = repository.getAnyUser()
            if (user != null) {
                currentUser = user
                currentScreen = "MAIN"
                currentMainTab = "DASHBOARD"
                val portfolios = repository.getAllPortfolios()
                if (portfolios.isNotEmpty()) {
                    activePortfolio = portfolios.first()
                } else {
                    checkAndCreateDefaultPortfolio()
                }
            }
        }
    }

    // Actions
    fun handleRegister() {
        authError = null
        val startingEquity = baseCapitalInput.toDoubleOrNull() ?: 100.00
        
        viewModelScope.launch {
            try {
                val finalName = if (nameInput.isBlank()) {
                    emailInput.substringBefore("@")
                } else {
                    nameInput
                }
                
                val user = repository.registerUser(
                    email = emailInput.trim(),
                    passwordHash = passwordInput, // Plain for local mock DB validation
                    name = finalName.trim(),
                    startingEquity = startingEquity,
                    traderAlias = traderAliasInput.trim(),
                    country = countryInput.trim(),
                    primaryInstrument = primaryInstrumentInput.trim(),
                    currency = currencyInput.trim()
                )
                
                currentUser = user
                currentScreen = "MAIN"
                currentMainTab = "DASHBOARD"
                checkAndCreateDefaultPortfolio()
            } catch (e: Exception) {
                authError = "Failed to register: ${e.localizedMessage}"
            }
        }
    }

    fun handleLogin() {
        authError = null
        if (loginEmailInput.isBlank()) {
            authError = "Email Address cannot be empty."
            return
        }
        if (loginPasswordInput.isBlank()) {
            authError = "Password cannot be empty."
            return
        }
        
        viewModelScope.launch {
            val user = repository.validateLogin(loginEmailInput.trim(), loginPasswordInput)
            if (user != null) {
                currentUser = user
                currentScreen = "MAIN"
                currentMainTab = "DASHBOARD"
                checkAndCreateDefaultPortfolio()
            } else {
                authError = "Invalid email or password."
            }
        }
    }

    fun navigateToLogin() {
        authError = null
        currentScreen = "LOGIN"
    }

    fun navigateToRegister() {
        authError = null
        currentScreen = "ONBOARDING"
    }

    fun handleAddTrade(customTimestamp: Long? = null) {
        manageMessage = null
        val symbol = tradeSymbol.trim().uppercase()
        if (symbol.isBlank()) {
            manageMessage = "Please enter a trading symbol (e.g. EURUSD)"
            return
        }
        val entry = tradeEntryPrice.toDoubleOrNull() ?: 0.0
        val exit = tradeExitPrice.toDoubleOrNull() ?: 0.0
        val size = tradeSize.toDoubleOrNull() ?: 1.0
        val profitValue = tradeProfit.toDoubleOrNull() ?: 0.0
        val broker = tradeBrokerage.toDoubleOrNull() ?: 0.0
        
        if (entry <= 0.0 || exit <= 0.0) {
            manageMessage = "Please enter valid entry and exit prices."
            return
        }

        viewModelScope.launch {
            val activeId = activePortfolio?.id ?: 0
            val trade = Trade(
                symbol = symbol,
                isBuy = tradeIsBuy,
                entryPrice = entry,
                exitPrice = exit,
                size = size,
                profit = profitValue,
                brokerage = broker,
                notes = tradeNotes.trim(),
                portfolioId = activeId,
                timestamp = customTimestamp ?: System.currentTimeMillis()
            )
            repository.insertTrade(trade)
            
            // Adjust current user's equity according to profit & brokerage deduction
            currentUser?.let { user ->
                val updatedEquity = user.totalEquity + profitValue - broker
                val updatedUser = user.copy(totalEquity = updatedEquity)
                repository.updateUser(updatedUser)
                currentUser = updatedUser
            }

            // Clear manage fields
            tradeSymbol = ""
            tradeEntryPrice = ""
            tradeExitPrice = ""
            tradeSize = ""
            tradeProfit = ""
            tradeBrokerage = ""
            tradeNotes = ""
            
            manageMessage = "Trade added successfully!"
        }
    }

    fun handleDeleteTrade(trade: Trade) {
        viewModelScope.launch {
            repository.deleteTrade(trade)
            // Undo user equity adjustment upon trade removal
            currentUser?.let { user ->
                val updatedEquity = user.totalEquity - trade.profit + trade.brokerage
                val updatedUser = user.copy(totalEquity = updatedEquity)
                repository.updateUser(updatedUser)
                currentUser = updatedUser
            }
        }
    }

    // Mistakes Notepad actions
    fun handleAddMistake() {
        val content = mistakeInput.trim()
        if (content.isBlank()) return
        viewModelScope.launch {
            val m = Mistake(content = content)
            repository.insertMistake(m)
            mistakeInput = ""
        }
    }

    fun handleUpdateMistake(mistake: Mistake) {
        viewModelScope.launch {
            repository.updateMistake(mistake)
        }
    }

    fun handleDeleteMistake(mistake: Mistake) {
        viewModelScope.launch {
            repository.deleteMistake(mistake)
        }
    }

    suspend fun checkAndPreloadDefaultMistakes() {
        // Keep empty notepad/mistakes by default as requested
    }

    fun updateStartingBalance(newBalance: Double) {
        viewModelScope.launch {
            activePortfolio?.let { portfolio ->
                val updated = portfolio.copy(startingEquity = newBalance)
                repository.updatePortfolio(updated)
                activePortfolio = updated
                manageMessage = "Starting capital updated to $${String.format("%.2f", newBalance)}"
            }
        }
    }

    suspend fun checkAndCreateDefaultPortfolio() {
        val list = repository.getAllPortfolios()
        if (list.isEmpty()) {
            val defaultAcc = com.example.data.PortfolioAccount(
                name = "Personal Core Portfolio",
                broker = "Pepperstone Live",
                startingEquity = currentUser?.totalEquity ?: 10000.0,
                type = "Live Cash",
                currency = currentUser?.currency ?: "USD",
                description = "Core trading account for forex and equity indices."
            )
            val idLong = repository.insertPortfolio(defaultAcc)
            val insertedAcc = defaultAcc.copy(id = idLong.toInt())
            activePortfolio = insertedAcc
        } else {
            if (activePortfolio == null) {
                activePortfolio = list.first()
            }
        }
    }

    fun handleAddPortfolio() {
        val name = portfolioNameInput.trim()
        if (name.isBlank()) {
            manageMessage = "Portfolio Name cannot be empty."
            return
        }
        val starting = portfolioStartingEquityInput.toDoubleOrNull() ?: 10000.0
        val broker = if (portfolioBrokerInput.isBlank()) "Standard Broker" else portfolioBrokerInput.trim()
        val type = portfolioTypeInput
        val currency = portfolioCurrencyInput
        val desc = portfolioDescInput.trim()

        viewModelScope.launch {
            val p = com.example.data.PortfolioAccount(
                name = name,
                broker = broker,
                startingEquity = starting,
                type = type,
                currency = currency,
                description = desc
            )
            val idLong = repository.insertPortfolio(p)
            val insertedAcc = p.copy(id = idLong.toInt())
            if (activePortfolio == null) {
                activePortfolio = insertedAcc
            }
            
            // Reset input fields
            portfolioNameInput = ""
            portfolioBrokerInput = ""
            portfolioStartingEquityInput = "10000.00"
            portfolioTypeInput = "Live Cash"
            portfolioCurrencyInput = "USD"
            portfolioDescInput = ""
            
            manageMessage = "Portfolio Account '$name' created successfully!"
        }
    }

    fun handleUpdatePortfolio(id: Int) {
        val name = portfolioNameInput.trim()
        if (name.isBlank()) {
            manageMessage = "Portfolio Name cannot be empty."
            return
        }
        val starting = portfolioStartingEquityInput.toDoubleOrNull() ?: 10000.0
        val broker = if (portfolioBrokerInput.isBlank()) "Standard Broker" else portfolioBrokerInput.trim()
        val type = portfolioTypeInput
        val currency = portfolioCurrencyInput
        val desc = portfolioDescInput.trim()

        viewModelScope.launch {
            val updated = com.example.data.PortfolioAccount(
                id = id,
                name = name,
                broker = broker,
                startingEquity = starting,
                type = type,
                currency = currency,
                description = desc
            )
            repository.updatePortfolio(updated)
            if (activePortfolio?.id == id) {
                activePortfolio = updated
            }
            
            // Reset fields
            portfolioNameInput = ""
            portfolioBrokerInput = ""
            portfolioStartingEquityInput = "10000.00"
            portfolioTypeInput = "Live Cash"
            portfolioCurrencyInput = "USD"
            portfolioDescInput = ""
            
            manageMessage = "Portfolio Account '$name' updated successfully!"
        }
    }

    fun handleDeletePortfolio(portfolio: com.example.data.PortfolioAccount) {
        viewModelScope.launch {
            repository.deletePortfolio(portfolio)
            val remaining = repository.getAllPortfolios()
            if (activePortfolio?.id == portfolio.id) {
                activePortfolio = remaining.firstOrNull()
            }
            manageMessage = "Portfolio Account '${portfolio.name}' deleted."
        }
    }

    fun handleToggleLike(postId: Int) {
        val list = communityPosts.value.map { post ->
            if (post.id == postId) {
                if (post.isLiked) {
                    post.copy(likes = post.likes - 1, isLiked = false)
                } else {
                    post.copy(likes = post.likes + 1, isLiked = true)
                }
            } else {
                post
            }
        }
        communityPosts.value = list
    }

    fun handleCreatePost() {
        if (newPostTitle.isBlank() || newPostContent.isBlank()) return
        val newId = (communityPosts.value.maxOfOrNull { it.id } ?: 0) + 1
        val post = CommunityPost(
            id = newId,
            author = currentUser?.name ?: "Kartik",
            title = newPostTitle.trim(),
            description = newPostContent.trim(),
            timestamp = "Just now",
            likes = 0
        )
        communityPosts.value = listOf(post) + communityPosts.value
        newPostTitle = ""
        newPostContent = ""
    }

    fun handleLogout() {
        currentUser = null
        currentScreen = "ONBOARDING"
        onboardingStep = 1
        isViewingSettingsProfile = false
        
        emailInput = ""
        passwordInput = ""
        confirmPasswordInput = ""
        nameInput = ""
        traderAliasInput = ""
        countryInput = "India"
        primaryInstrumentInput = "Equity"
        baseCapitalInput = "100.00"
        currencyInput = "USD"
        authError = null
    }

    fun handleDeleteAccount() {
        viewModelScope.launch {
            try {
                repository.clearTrades()
                repository.clearPortfolios()
                repository.clearMistakes()
                repository.clearUsers()
                currentUser = null
                activePortfolio = null
                currentScreen = "ONBOARDING"
                onboardingStep = 1
                isViewingSettingsProfile = false

                emailInput = ""
                passwordInput = ""
                confirmPasswordInput = ""
                nameInput = ""
                traderAliasInput = ""
                countryInput = "India"
                primaryInstrumentInput = "Equity"
                baseCapitalInput = "100.00"
                currencyInput = "USD"
                authError = null
            } catch (e: Exception) {
                // handle error if needed
            }
        }
    }
}
