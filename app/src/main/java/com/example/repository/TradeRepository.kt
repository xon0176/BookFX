package com.example.repository

import com.example.data.Trade
import com.example.data.TradeDao
import com.example.data.User
import com.example.data.Mistake
import kotlinx.coroutines.flow.Flow

class TradeRepository(private val tradeDao: TradeDao) {

    // Trades
    fun getAllTradesFlow(): Flow<List<Trade>> = tradeDao.getAllTradesFlow()
    
    suspend fun getAllTrades(): List<Trade> = tradeDao.getAllTrades()

    suspend fun insertTrade(trade: Trade) {
        tradeDao.insertTrade(trade)
    }

    suspend fun deleteTrade(trade: Trade) {
        tradeDao.deleteTrade(trade)
    }

    suspend fun clearTrades() {
        tradeDao.deleteAllTrades()
    }

    // Users & Session
    suspend fun registerUser(
        email: String,
        passwordHash: String,
        name: String,
        startingEquity: Double,
        traderAlias: String = "",
        country: String = "",
        primaryInstrument: String = "",
        currency: String = "USD"
    ): User {
        val existing = tradeDao.getUserByEmail(email)
        if (existing != null) {
            return existing
        }
        val user = User(
            email = email,
            passwordHash = passwordHash,
            name = name,
            totalEquity = startingEquity,
            traderAlias = traderAlias,
            country = country,
            primaryInstrument = primaryInstrument,
            currency = currency
        )
        val id = tradeDao.insertUser(user)
        return user.copy(id = id.toInt())
    }

    suspend fun validateLogin(email: String, passwordHash: String): User? {
        val user = tradeDao.getUserByEmail(email)
        if (user != null && user.passwordHash == passwordHash) {
            return user
        }
        return null
    }

    fun getUserFlow(userId: Int): Flow<User?> = tradeDao.getUserFlow(userId)

    suspend fun getAnyUser(): User? = tradeDao.getAnyUser()

    suspend fun updateUser(user: User) {
        tradeDao.updateUser(user)
    }

    // Portfolio Accounts
    fun getAllPortfoliosFlow(): Flow<List<com.example.data.PortfolioAccount>> = tradeDao.getAllPortfoliosFlow()

    suspend fun getAllPortfolios(): List<com.example.data.PortfolioAccount> = tradeDao.getAllPortfolios()

    suspend fun insertPortfolio(portfolio: com.example.data.PortfolioAccount): Long = tradeDao.insertPortfolio(portfolio)

    suspend fun updatePortfolio(portfolio: com.example.data.PortfolioAccount) = tradeDao.updatePortfolio(portfolio)

    suspend fun deletePortfolio(portfolio: com.example.data.PortfolioAccount) = tradeDao.deletePortfolio(portfolio)

    suspend fun clearPortfolios() = tradeDao.deleteAllPortfolios()

    suspend fun clearMistakes() = tradeDao.deleteAllMistakes()

    suspend fun clearUsers() = tradeDao.deleteAllUsers()

    fun getTradesForPortfolioFlow(portfolioId: Int): Flow<List<Trade>> = tradeDao.getTradesForPortfolioFlow(portfolioId)

    suspend fun getTradesForPortfolio(portfolioId: Int): List<Trade> = tradeDao.getTradesForPortfolio(portfolioId)

    // Mistakes
    fun getAllMistakesFlow(): Flow<List<Mistake>> = tradeDao.getAllMistakesFlow()

    suspend fun getAllMistakes(): List<Mistake> = tradeDao.getAllMistakes()

    suspend fun insertMistake(mistake: Mistake): Long = tradeDao.insertMistake(mistake)

    suspend fun deleteMistake(mistake: Mistake) = tradeDao.deleteMistake(mistake)

    suspend fun updateMistake(mistake: Mistake) = tradeDao.updateMistake(mistake)
}
