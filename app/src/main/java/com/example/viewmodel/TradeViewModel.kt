package com.example.viewmodel

import android.app.Application
import android.util.Log
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
    // "SPLASH", "ONBOARDING", "LOGIN", "MAIN"
    var currentScreen by mutableStateOf("SPLASH")
    var isCheckingEmailOnboarding by mutableStateOf(false)

    // Sub-modules on the MAIN screen
    // "DASHBOARD", "MANAGE", "JOURNAL", "ANALYTICS", "CALC", "COMMUNITY"
    var currentMainTab by mutableStateOf("DASHBOARD")
    var isViewingSettingsProfile by mutableStateOf(false)
    var showCertificateDialog by mutableStateOf(false)

    // Settings States
    private val appSettingsPrefs = application.getSharedPreferences("bookfx_prefs", android.content.Context.MODE_PRIVATE)

    private val _isDarkMode = mutableStateOf(appSettingsPrefs.getBoolean("is_dark_mode", false))
    var isDarkMode: Boolean
        get() = _isDarkMode.value
        set(value) {
            _isDarkMode.value = value
            appSettingsPrefs.edit().putBoolean("is_dark_mode", value).apply()
        }

    private val _enableWeekendTrading = mutableStateOf(appSettingsPrefs.getBoolean("enable_weekend_trading", true))
    var enableWeekendTrading: Boolean
        get() = _enableWeekendTrading.value
        set(value) {
            _enableWeekendTrading.value = value
            appSettingsPrefs.edit().putBoolean("enable_weekend_trading", value).apply()
        }

    fun handleUpdateCurrency(newCurrency: String) {
        val user = currentUser ?: return
        viewModelScope.launch {
            val updated = user.copy(currency = newCurrency)
            repository.updateUser(updated)
            currentUser = updated
            setUpdatedUserTime(user.email, System.currentTimeMillis())
            syncDataToCloud()
        }
    }

    fun handleUpdateUser(updatedUser: User) {
        viewModelScope.launch {
            repository.updateUser(updatedUser)
            currentUser = updatedUser
            setUpdatedUserTime(updatedUser.email, System.currentTimeMillis())
            syncDataToCloud()
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

    fun checkEmailAndProceedOnboarding(onSuccess: () -> Unit) {
        if (!validateStep1()) return
        
        viewModelScope.launch {
            isCheckingEmailOnboarding = true
            authError = null
            try {
                val email = emailInput.lowercase().trim()
                val hasCloud = com.example.data.CloudSyncManager.hasCloudAccount(getApplication(), email)
                if (hasCloud) {
                    authError = "This email is already registered. Please log in to restore your performance tracker."
                    loginEmailInput = email
                } else {
                    onSuccess()
                }
            } catch (e: Exception) {
                // Network check fallback
                onSuccess()
            } finally {
                isCheckingEmailOnboarding = false
            }
        }
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

    var editingTradeId by mutableStateOf<Int?>(null)
    var showLogTradeDialogInJournal by mutableStateOf(false)
    var journalSelectedDateEpochMilli by mutableStateOf<Long?>(null)

    fun startEditTrade(trade: Trade) {
        editingTradeId = trade.id
        tradeSymbol = trade.symbol
        tradeIsBuy = trade.isBuy
        tradeEntryPrice = if (trade.entryPrice == 0.0) "" else trade.entryPrice.toString()
        tradeExitPrice = if (trade.exitPrice == 0.0) "" else trade.exitPrice.toString()
        tradeSize = if (trade.size == 0.0) "" else trade.size.toString()
        tradeProfit = trade.profit.toString()
        tradeBrokerage = if (trade.brokerage == 0.0) "" else trade.brokerage.toString()
        tradeNotes = trade.notes
        manageMessage = null
    }

    fun clearTradeForm() {
        editingTradeId = null
        tradeSymbol = ""
        tradeIsBuy = true
        tradeEntryPrice = ""
        tradeExitPrice = ""
        tradeSize = ""
        tradeProfit = ""
        tradeBrokerage = ""
        tradeNotes = ""
        manageMessage = null
    }

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
            
            // Introduce a short, satisfying delay to render the brand's vector identity before landing
            kotlinx.coroutines.delay(1800)
            
            val user = repository.getAnyUser()
            if (user != null) {
                currentUser = user
                val portfolios = repository.getAllPortfolios()
                if (portfolios.isNotEmpty()) {
                    activePortfolio = portfolios.first()
                } else {
                    checkAndCreateDefaultPortfolio()
                }
                currentScreen = "MAIN"
                currentMainTab = "DASHBOARD"
                // On app startup, let's do a bidirectional merge to get the latest cloud changes!
                syncDataToCloud(pullAndMerge = true)
            } else {
                currentScreen = "ONBOARDING"
            }
        }
    }

    private fun getDeletedPortfolioKeys(email: String): Set<String> {
        val deletePrefs = getApplication<Application>().getSharedPreferences("bookfx_deletions", android.content.Context.MODE_PRIVATE)
        return deletePrefs.getStringSet("portfolios_${email.lowercase().trim()}", emptySet()) ?: emptySet()
    }

    private fun addDeletedPortfolioKey(email: String, key: String) {
        val deletePrefs = getApplication<Application>().getSharedPreferences("bookfx_deletions", android.content.Context.MODE_PRIVATE)
        val current = deletePrefs.getStringSet("portfolios_${email.lowercase().trim()}", emptySet()) ?: emptySet()
        val updated = current.toMutableSet().apply { add(key) }
        deletePrefs.edit().putStringSet("portfolios_${email.lowercase().trim()}", updated).apply()
    }

    private fun saveDeletedPortfolioKeys(email: String, keys: Set<String>) {
        val deletePrefs = getApplication<Application>().getSharedPreferences("bookfx_deletions", android.content.Context.MODE_PRIVATE)
        deletePrefs.edit().putStringSet("portfolios_${email.lowercase().trim()}", keys).apply()
    }

    private fun getDeletedTradeKeys(email: String): Set<String> {
        val deletePrefs = getApplication<Application>().getSharedPreferences("bookfx_deletions", android.content.Context.MODE_PRIVATE)
        return deletePrefs.getStringSet("trades_${email.lowercase().trim()}", emptySet()) ?: emptySet()
    }

    private fun addDeletedTradeKey(email: String, key: String) {
        val deletePrefs = getApplication<Application>().getSharedPreferences("bookfx_deletions", android.content.Context.MODE_PRIVATE)
        val current = deletePrefs.getStringSet("trades_${email.lowercase().trim()}", emptySet()) ?: emptySet()
        val updated = current.toMutableSet().apply { add(key) }
        deletePrefs.edit().putStringSet("trades_${email.lowercase().trim()}", updated).apply()
    }

    private fun saveDeletedTradeKeys(email: String, keys: Set<String>) {
        val deletePrefs = getApplication<Application>().getSharedPreferences("bookfx_deletions", android.content.Context.MODE_PRIVATE)
        deletePrefs.edit().putStringSet("trades_${email.lowercase().trim()}", keys).apply()
    }

    private fun getDeletedMistakeKeys(email: String): Set<String> {
        val deletePrefs = getApplication<Application>().getSharedPreferences("bookfx_deletions", android.content.Context.MODE_PRIVATE)
        return deletePrefs.getStringSet("mistakes_${email.lowercase().trim()}", emptySet()) ?: emptySet()
    }

    private fun addDeletedMistakeKey(email: String, key: String) {
        val deletePrefs = getApplication<Application>().getSharedPreferences("bookfx_deletions", android.content.Context.MODE_PRIVATE)
        val current = deletePrefs.getStringSet("mistakes_${email.lowercase().trim()}", emptySet()) ?: emptySet()
        val updated = current.toMutableSet().apply { add(key) }
        deletePrefs.edit().putStringSet("mistakes_${email.lowercase().trim()}", updated).apply()
    }

    private fun saveDeletedMistakeKeys(email: String, keys: Set<String>) {
        val deletePrefs = getApplication<Application>().getSharedPreferences("bookfx_deletions", android.content.Context.MODE_PRIVATE)
        deletePrefs.edit().putStringSet("mistakes_${email.lowercase().trim()}", keys).apply()
    }

    private fun getUpdatedPortfolioTime(email: String, key: String): Long {
        val prefs = getApplication<Application>().getSharedPreferences("bookfx_last_updated", android.content.Context.MODE_PRIVATE)
        return prefs.getLong("portfolio_${email.lowercase().trim()}_$key", 0L)
    }

    private fun setUpdatedPortfolioTime(email: String, key: String, timestamp: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("bookfx_last_updated", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("portfolio_${email.lowercase().trim()}_$key", timestamp).apply()
    }

    private fun getUpdatedTradeTime(email: String, key: String): Long {
        val prefs = getApplication<Application>().getSharedPreferences("bookfx_last_updated", android.content.Context.MODE_PRIVATE)
        return prefs.getLong("trade_${email.lowercase().trim()}_$key", 0L)
    }

    private fun setUpdatedTradeTime(email: String, key: String, timestamp: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("bookfx_last_updated", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("trade_${email.lowercase().trim()}_$key", timestamp).apply()
    }

    private fun getUpdatedMistakeTime(email: String, key: String): Long {
        val prefs = getApplication<Application>().getSharedPreferences("bookfx_last_updated", android.content.Context.MODE_PRIVATE)
        return prefs.getLong("mistake_${email.lowercase().trim()}_$key", 0L)
    }

    private fun setUpdatedMistakeTime(email: String, key: String, timestamp: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("bookfx_last_updated", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("mistake_${email.lowercase().trim()}_$key", timestamp).apply()
    }

    private fun getUpdatedUserTime(email: String): Long {
        val prefs = getApplication<Application>().getSharedPreferences("bookfx_last_updated", android.content.Context.MODE_PRIVATE)
        return prefs.getLong("user_${email.lowercase().trim()}", 0L)
    }

    private fun setUpdatedUserTime(email: String, timestamp: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("bookfx_last_updated", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("user_${email.lowercase().trim()}", timestamp).apply()
    }

    // Cloud Database Synchronization Engine
    fun syncDataToCloud(pullAndMerge: Boolean = true, onResult: ((Boolean) -> Unit)? = null) {
        val user = currentUser ?: return
        viewModelScope.launch {
            try {
                val localDeletedTradeKeys = getDeletedTradeKeys(user.email).toMutableSet()
                val localDeletedMistakeKeys = getDeletedMistakeKeys(user.email).toMutableSet()
                val localDeletedPortfolioKeys = getDeletedPortfolioKeys(user.email).toMutableSet()

                if (pullAndMerge) {
                    // 1. Pull the latest backups from the cloud
                    val cloudData = com.example.data.CloudSyncManager.findInCloud(getApplication(), user.email)
                    
                    if (cloudData != null) {
                        // Merge deletion sets from cloud
                        val cloudDeletedTradeKeys = cloudData.deletedTradeKeys
                        val cloudDeletedMistakeKeys = cloudData.deletedMistakeKeys
                        val cloudDeletedPortfolioKeys = cloudData.deletedPortfolioKeys
                        
                        localDeletedTradeKeys.addAll(cloudDeletedTradeKeys)
                        localDeletedMistakeKeys.addAll(cloudDeletedMistakeKeys)
                        localDeletedPortfolioKeys.addAll(cloudDeletedPortfolioKeys)
                        
                        // Save the combined deletion lists locally
                        saveDeletedTradeKeys(user.email, localDeletedTradeKeys)
                        saveDeletedMistakeKeys(user.email, localDeletedMistakeKeys)
                        saveDeletedPortfolioKeys(user.email, localDeletedPortfolioKeys)

                        // A. Merge Portfolios
                        val localPortfolios = repository.getAllPortfolios()
                        for (localP in localPortfolios) {
                            val localKey = localP.name.lowercase().trim()
                            if (localDeletedPortfolioKeys.contains(localKey)) {
                                repository.deletePortfolio(localP)
                            }
                        }
                        
                        val remainingLocalPortfolios = repository.getAllPortfolios()
                        for (cloudPort in cloudData.portfolios) {
                            val cloudKey = cloudPort.name.lowercase().trim()
                            if (localDeletedPortfolioKeys.contains(cloudKey)) {
                                continue
                            }
                            
                            val matchingLocal = remainingLocalPortfolios.find { it.name.lowercase().trim() == cloudKey }
                            if (matchingLocal == null) {
                                // Portfolio is in cloud but not local, insert it locally
                                repository.insertPortfolio(cloudPort.copy(id = 0))
                                val cloudTime = cloudData.lastUpdatedPortfolioKeys[cloudKey] ?: 0L
                                setUpdatedPortfolioTime(user.email, cloudKey, cloudTime)
                            } else {
                                // Same portfolio exists.
                                val cloudTime = cloudData.lastUpdatedPortfolioKeys[cloudKey] ?: 0L
                                val localTime = getUpdatedPortfolioTime(user.email, cloudKey)
                                
                                if (cloudTime > localTime) {
                                    // Cloud version is newer! Overwrite local with cloud details
                                    val mergedPort = matchingLocal.copy(
                                        startingEquity = cloudPort.startingEquity,
                                        broker = cloudPort.broker,
                                        type = cloudPort.type,
                                        description = cloudPort.description,
                                        currency = cloudPort.currency
                                    )
                                    repository.updatePortfolio(mergedPort)
                                    setUpdatedPortfolioTime(user.email, cloudKey, cloudTime)
                                } else if (localTime > cloudTime) {
                                    // Local version is newer, do nothing
                                } else {
                                    if (matchingLocal.startingEquity != cloudPort.startingEquity) {
                                        // Keep the non-100 or non-default values, or let the cloud override local default
                                        if (matchingLocal.startingEquity == 100.0 && cloudPort.startingEquity != 100.0) {
                                            repository.updatePortfolio(matchingLocal.copy(startingEquity = cloudPort.startingEquity, broker = cloudPort.broker, type = cloudPort.type, description = cloudPort.description))
                                        } else if (cloudPort.startingEquity != 100.0 && cloudPort.startingEquity > matchingLocal.startingEquity) {
                                            repository.updatePortfolio(matchingLocal.copy(startingEquity = cloudPort.startingEquity))
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Fetch updated local portfolios to get valid local IDs
                        val updatedLocalPortfolios = repository.getAllPortfolios()
                        val defaultPortId = updatedLocalPortfolios.firstOrNull()?.id ?: 1
                        
                        // B. Merge Trades
                        val localTrades = repository.getAllTrades()
                        for (localT in localTrades) {
                            val localKey = "${localT.symbol}_${localT.timestamp}"
                            if (localDeletedTradeKeys.contains(localKey)) {
                                repository.deleteTrade(localT)
                            }
                        }
                        
                        val remainingLocalTrades = repository.getAllTrades()
                        for (cloudTrade in cloudData.trades) {
                            val cloudKey = "${cloudTrade.symbol}_${cloudTrade.timestamp}"
                            if (localDeletedTradeKeys.contains(cloudKey)) {
                                continue
                            }
                            
                            val matchingLocalTrade = remainingLocalTrades.find { 
                                it.timestamp == cloudTrade.timestamp && 
                                it.symbol.lowercase().trim() == cloudTrade.symbol.lowercase().trim()
                            }
                            if (matchingLocalTrade == null) {
                                val originalPortName = cloudData.portfolios.find { p -> p.id == cloudTrade.portfolioId }?.name
                                val resolvedPortId = if (originalPortName != null) {
                                    updatedLocalPortfolios.find { it.name.lowercase().trim() == originalPortName.lowercase().trim() }?.id ?: defaultPortId
                                } else {
                                    defaultPortId
                                }
                                repository.insertTrade(cloudTrade.copy(id = 0, portfolioId = resolvedPortId))
                                val cloudTime = cloudData.lastUpdatedTradeKeys[cloudKey] ?: 0L
                                setUpdatedTradeTime(user.email, cloudKey, cloudTime)
                            } else {
                                // Match exists! Check which is newer
                                val cloudTime = cloudData.lastUpdatedTradeKeys[cloudKey] ?: 0L
                                val localTime = getUpdatedTradeTime(user.email, cloudKey)
                                
                                if (cloudTime > localTime) {
                                    // Cloud is newer! Update local entry details
                                    val cloudPortName = cloudData.portfolios.find { p -> p.id == cloudTrade.portfolioId }?.name
                                    val resolvedPortId = if (cloudPortName != null) {
                                        updatedLocalPortfolios.find { it.name.lowercase().trim() == cloudPortName.lowercase().trim() }?.id ?: matchingLocalTrade.portfolioId
                                    } else {
                                        matchingLocalTrade.portfolioId
                                    }
                                    
                                    val updatedTrade = matchingLocalTrade.copy(
                                        entryPrice = cloudTrade.entryPrice,
                                        exitPrice = cloudTrade.exitPrice,
                                        size = cloudTrade.size,
                                        profit = cloudTrade.profit,
                                        brokerage = cloudTrade.brokerage,
                                        notes = cloudTrade.notes,
                                        isBuy = cloudTrade.isBuy,
                                        portfolioId = resolvedPortId
                                    )
                                    repository.insertTrade(updatedTrade)
                                    setUpdatedTradeTime(user.email, cloudKey, cloudTime)
                                }
                            }
                        }
                        
                        // C. Merge Mistakes
                        val localMistakes = repository.getAllMistakes()
                        for (localM in localMistakes) {
                            val localKey = "${localM.timestamp}"
                            if (localDeletedMistakeKeys.contains(localKey)) {
                                repository.deleteMistake(localM)
                            }
                        }
                        
                        val remainingLocalMistakes = repository.getAllMistakes()
                        for (cloudMistake in cloudData.mistakes) {
                            val cloudKey = "${cloudMistake.timestamp}"
                            if (localDeletedMistakeKeys.contains(cloudKey)) {
                                continue
                            }
                            
                            val matchingLocalMistake = remainingLocalMistakes.find { 
                                it.timestamp == cloudMistake.timestamp
                            }
                            if (matchingLocalMistake == null) {
                                repository.insertMistake(cloudMistake.copy(id = 0))
                                val cloudTime = cloudData.lastUpdatedMistakeKeys[cloudKey] ?: 0L
                                setUpdatedMistakeTime(user.email, cloudKey, cloudTime)
                            } else {
                                // Match exists! Check which is newer
                                val cloudTime = cloudData.lastUpdatedMistakeKeys[cloudKey] ?: 0L
                                val localTime = getUpdatedMistakeTime(user.email, cloudKey)
                                
                                if (cloudTime > localTime) {
                                    // Cloud is newer! Update local mistake
                                    val updatedMistake = matchingLocalMistake.copy(
                                        content = cloudMistake.content
                                    )
                                    repository.updateMistake(updatedMistake)
                                    setUpdatedMistakeTime(user.email, cloudKey, cloudTime)
                                }
                            }
                        }
                        
                        // D. Merge User profile
                        val localUser = repository.getAnyUser()
                        if (localUser != null) {
                            val cloudUserTime = cloudData.lastUpdatedUser
                            val localUserTime = getUpdatedUserTime(user.email)
                            
                            if (cloudUserTime > localUserTime) {
                                val updatedUser = localUser.copy(
                                    name = cloudData.user.name,
                                    traderAlias = cloudData.user.traderAlias,
                                    country = cloudData.user.country,
                                    primaryInstrument = cloudData.user.primaryInstrument,
                                    currency = cloudData.user.currency,
                                    totalEquity = cloudData.user.totalEquity
                                )
                                repository.updateUser(updatedUser)
                                currentUser = updatedUser
                                setUpdatedUserTime(user.email, cloudUserTime)
                            } else if (localUserTime > cloudUserTime) {
                                // Local is newer, do nothing
                            } else {
                                val differentEquity = localUser.totalEquity != cloudData.user.totalEquity
                                if (differentEquity) {
                                    val cloudEquity = cloudData.user.totalEquity
                                    val updatedUser = localUser.copy(totalEquity = cloudEquity)
                                    repository.updateUser(updatedUser)
                                    currentUser = updatedUser
                                }
                            }
                        }
                    }
                }
                
                // 2. Fetch the latest fully merged datasets from the local database
                val mergedUser = repository.getAnyUser() ?: user
                val mergedPortfolios = repository.getAllPortfolios()
                val mergedTrades = repository.getAllTrades()
                val mergedMistakes = repository.getAllMistakes()
                
                // Keep the active portfolio pointer clean and matching local
                if (activePortfolio != null) {
                    val matchingActive = mergedPortfolios.find { it.name.lowercase().trim() == activePortfolio?.name?.lowercase()?.trim() }
                    if (matchingActive != null) {
                        activePortfolio = matchingActive
                    }
                } else if (mergedPortfolios.isNotEmpty()) {
                    activePortfolio = mergedPortfolios.first()
                }

                // Build modern maps of our local update times to synchronize upstream
                val userKey = user.email.lowercase().trim()
                val localLastUpdatedPortfolios = mutableMapOf<String, Long>()
                mergedPortfolios.forEach { p ->
                    val k = p.name.lowercase().trim()
                    localLastUpdatedPortfolios[k] = getUpdatedPortfolioTime(userKey, k)
                }
                val localLastUpdatedTrades = mutableMapOf<String, Long>()
                mergedTrades.forEach { t ->
                    val k = "${t.symbol}_${t.timestamp}"
                    localLastUpdatedTrades[k] = getUpdatedTradeTime(userKey, k)
                }
                val localLastUpdatedMistakes = mutableMapOf<String, Long>()
                mergedMistakes.forEach { m ->
                    val k = "${m.timestamp}"
                    localLastUpdatedMistakes[k] = getUpdatedMistakeTime(userKey, k)
                }
                val localLastUpdatedUser = getUpdatedUserTime(userKey)
                
                // 3. Save the complete merged state back to the cloud
                val success = com.example.data.CloudSyncManager.saveToCloud(
                    context = getApplication(),
                    user = mergedUser,
                    portfolios = mergedPortfolios,
                    trades = mergedTrades,
                    mistakes = mergedMistakes,
                    deletedTradeKeys = localDeletedTradeKeys.toList(),
                    deletedMistakeKeys = localDeletedMistakeKeys.toList(),
                    deletedPortfolioKeys = localDeletedPortfolioKeys.toList(),
                    lastUpdatedPortfolioKeys = localLastUpdatedPortfolios,
                    lastUpdatedTradeKeys = localLastUpdatedTrades,
                    lastUpdatedMistakeKeys = localLastUpdatedMistakes,
                    lastUpdatedUser = localLastUpdatedUser
                )
                if (success) {
                    Log.d("TradeViewModel", "Cloud Sync Completed successfully (pullAndMerge=$pullAndMerge).")
                } else {
                    Log.e("TradeViewModel", "Cloud Sync failed to upload merged registry state.")
                }
                onResult?.invoke(success)
            } catch (e: Exception) {
                Log.e("TradeViewModel", "Failed to sync with cloud: ${e.message}", e)
                onResult?.invoke(false)
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
                    email = emailInput.lowercase().trim(),
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
                
                // Instantly sync to simulated cloud server on sign up
                syncDataToCloud()
            } catch (e: Exception) {
                authError = "Failed to register: ${e.localizedMessage}"
            }
        }
    }

    fun handleLogin() {
        authError = null
        val email = loginEmailInput.lowercase().trim()
        val password = loginPasswordInput
        
        if (email.isBlank()) {
            authError = "Email Address cannot be empty."
            return
        }
        if (password.isBlank()) {
            authError = "Password cannot be empty."
            return
        }
        
        viewModelScope.launch {
            // Find remote account in cloud first to see if there is an authoritative backup
            val cloudData = com.example.data.CloudSyncManager.findInCloud(getApplication(), email)
            var user: User? = null
            
            if (cloudData != null) {
                if (cloudData.user.passwordHash == password) {
                    try {
                        // Clear any existing local tables before restoring cloud backup
                        repository.clearUsers()
                        repository.clearPortfolios()
                        repository.clearTrades()
                        repository.clearMistakes()
                        
                        // Restore User profile
                        val restoredUser = repository.registerUser(
                            email = cloudData.user.email,
                            passwordHash = cloudData.user.passwordHash,
                            name = cloudData.user.name,
                            startingEquity = cloudData.user.totalEquity,
                            traderAlias = cloudData.user.traderAlias,
                            country = cloudData.user.country,
                            primaryInstrument = cloudData.user.primaryInstrument,
                            currency = cloudData.user.currency
                        )
                        
                        // Restore Portfolios
                        for (port in cloudData.portfolios) {
                            repository.insertPortfolio(port.copy(id = 0))
                        }
                        
                        val restoredPortfolios = repository.getAllPortfolios()
                        val defaultPortId = restoredPortfolios.firstOrNull()?.id ?: 1
                        
                        // Restore Trades mapped with correct active portfolios
                        for (trade in cloudData.trades) {
                            val originalPortName = cloudData.portfolios.find { p -> p.id == trade.portfolioId }?.name
                            val resolvedPortId = restoredPortfolios.find { it.name == originalPortName }?.id ?: defaultPortId
                            repository.insertTrade(trade.copy(id = 0, portfolioId = resolvedPortId))
                        }
                        
                        // Restore Mistakes
                        for (mistake in cloudData.mistakes) {
                            repository.insertMistake(mistake.copy(id = 0))
                        }
                        
                        user = restoredUser
                    } catch (e: Exception) {
                        Log.e("TradeViewModel", "Error restoring from cloud backup registry", e)
                        authError = "Error synchronizing restored data: ${e.localizedMessage}"
                        return@launch
                    }
                } else {
                    authError = "Invalid email or password."
                    return@launch
                }
            } else {
                // If cloud data is not found (offline or never synced), validate locally
                user = repository.validateLogin(email, password)
            }
            
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
                
                // Synchronize session back to cloud to establish local/remote alignment
                syncDataToCloud()
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
        val exitVal = tradeExitPrice.toDoubleOrNull()
        if (tradeExitPrice.isNotBlank() && (exitVal == null || exitVal < 0.0)) {
            manageMessage = "Please enter a valid exit price or leave it blank."
            return
        }
        val exit = exitVal ?: 0.0
        val size = tradeSize.toDoubleOrNull() ?: 1.0
        val profitValue = tradeProfit.toDoubleOrNull() ?: 0.0
        val broker = tradeBrokerage.toDoubleOrNull() ?: 0.0
        
        if (entry <= 0.0) {
            manageMessage = "Please enter a valid entry price."
            return
        }

        viewModelScope.launch {
            val activeId = activePortfolio?.id ?: 0
            val oldTrade = allTrades.value.find { it.id == editingTradeId }
            val oldProfit = oldTrade?.profit ?: 0.0
            val oldBrokerage = oldTrade?.brokerage ?: 0.0
            val originalTimestamp = oldTrade?.timestamp ?: (customTimestamp ?: System.currentTimeMillis())

            val trade = Trade(
                id = editingTradeId ?: 0,
                symbol = symbol,
                isBuy = tradeIsBuy,
                entryPrice = entry,
                exitPrice = exit,
                size = size,
                profit = profitValue,
                brokerage = broker,
                notes = tradeNotes.trim(),
                portfolioId = activeId,
                timestamp = originalTimestamp
            )
            repository.insertTrade(trade)
            
            // Adjust current user's equity according to profit & brokerage deduction
            currentUser?.let { user ->
                val key = "${trade.symbol}_${trade.timestamp}"
                setUpdatedTradeTime(user.email, key, System.currentTimeMillis())
                
                val updatedEquity = user.totalEquity - oldProfit + oldBrokerage + profitValue - broker
                val updatedUser = user.copy(totalEquity = updatedEquity)
                repository.updateUser(updatedUser)
                currentUser = updatedUser
                setUpdatedUserTime(user.email, System.currentTimeMillis())
            }

            val isEditMode = editingTradeId != null
            clearTradeForm()
            
            manageMessage = if (isEditMode) "Trade updated successfully!" else "Trade added successfully!"
            syncDataToCloud()
        }
    }

    fun handleDeleteTrade(trade: Trade) {
        viewModelScope.launch {
            repository.deleteTrade(trade)
            currentUser?.let { user ->
                val key = "${trade.symbol}_${trade.timestamp}"
                addDeletedTradeKey(user.email, key)
                
                // Undo user equity adjustment upon trade removal
                val updatedEquity = user.totalEquity - trade.profit + trade.brokerage
                val updatedUser = user.copy(totalEquity = updatedEquity)
                repository.updateUser(updatedUser)
                currentUser = updatedUser
                setUpdatedUserTime(user.email, System.currentTimeMillis())
            }
            syncDataToCloud()
        }
    }

    // Mistakes Notepad actions
    fun handleAddMistake() {
        val content = mistakeInput.trim()
        if (content.isBlank()) return
        viewModelScope.launch {
            val m = Mistake(content = content)
            repository.insertMistake(m)
            currentUser?.let { user ->
                setUpdatedMistakeTime(user.email, "${m.timestamp}", System.currentTimeMillis())
            }
            mistakeInput = ""
            syncDataToCloud()
        }
    }

    fun handleUpdateMistake(mistake: Mistake) {
        viewModelScope.launch {
            repository.updateMistake(mistake)
            currentUser?.let { user ->
                setUpdatedMistakeTime(user.email, "${mistake.timestamp}", System.currentTimeMillis())
            }
            syncDataToCloud()
        }
    }

    fun handleDeleteMistake(mistake: Mistake) {
        viewModelScope.launch {
            repository.deleteMistake(mistake)
            currentUser?.let { user ->
                val key = "${mistake.timestamp}"
                addDeletedMistakeKey(user.email, key)
            }
            syncDataToCloud()
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
                currentUser?.let { user ->
                    setUpdatedPortfolioTime(user.email, portfolio.name.lowercase().trim(), System.currentTimeMillis())
                }
                manageMessage = "Starting capital updated to $${String.format("%.2f", newBalance)}"
                syncDataToCloud()
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
            currentUser?.let { user ->
                setUpdatedPortfolioTime(user.email, name.lowercase().trim(), System.currentTimeMillis())
            }
            
            // Reset input fields
            portfolioNameInput = ""
            portfolioBrokerInput = ""
            portfolioStartingEquityInput = "10000.00"
            portfolioTypeInput = "Live Cash"
            portfolioCurrencyInput = "USD"
            portfolioDescInput = ""
            
            manageMessage = "Portfolio Account '$name' created successfully!"
            syncDataToCloud()
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
            currentUser?.let { user ->
                setUpdatedPortfolioTime(user.email, name.lowercase().trim(), System.currentTimeMillis())
            }
            
            // Reset fields
            portfolioNameInput = ""
            portfolioBrokerInput = ""
            portfolioStartingEquityInput = "10000.00"
            portfolioTypeInput = "Live Cash"
            portfolioCurrencyInput = "USD"
            portfolioDescInput = ""
            
            manageMessage = "Portfolio Account '$name' updated successfully!"
            syncDataToCloud()
        }
    }

    fun handleDeletePortfolio(portfolio: com.example.data.PortfolioAccount) {
        viewModelScope.launch {
            repository.deletePortfolio(portfolio)
            currentUser?.let { user ->
                addDeletedPortfolioKey(user.email, portfolio.name.lowercase().trim())
            }
            val remaining = repository.getAllPortfolios()
            if (activePortfolio?.id == portfolio.id) {
                activePortfolio = remaining.firstOrNull()
            }
            manageMessage = "Portfolio Account '${portfolio.name}' deleted."
            syncDataToCloud()
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
            author = currentUser?.name ?: "Trader",
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
        viewModelScope.launch {
            try {
                repository.clearTrades()
                repository.clearPortfolios()
                repository.clearMistakes()
                repository.clearUsers()
                activePortfolio = null
                Log.d("TradeViewModel", "Successfully cleared local SQLite database tables on logout")
            } catch (e: Exception) {
                Log.e("TradeViewModel", "Error clearing local database on logout", e)
            }
        }
        
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

    fun refreshAllData() {
        viewModelScope.launch {
            try {
                val user = repository.getAnyUser()
                if (user != null) {
                    currentUser = user
                }
                val portfolios = repository.getAllPortfolios()
                if (portfolios.isNotEmpty()) {
                    val curId = activePortfolio?.id
                    if (curId != null) {
                        activePortfolio = portfolios.find { it.id == curId } ?: portfolios.first()
                    } else {
                        activePortfolio = portfolios.first()
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
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
