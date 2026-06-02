package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    // Trades operations
    @Query("SELECT * FROM trades ORDER BY timestamp DESC")
    fun getAllTradesFlow(): Flow<List<Trade>>

    @Query("SELECT * FROM trades ORDER BY timestamp DESC")
    suspend fun getAllTrades(): List<Trade>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: Trade)

    @Delete
    suspend fun deleteTrade(trade: Trade)

    @Query("DELETE FROM trades")
    suspend fun deleteAllTrades()

    // Users operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserFlow(userId: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): User?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getAnyUser(): User?

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    // Portfolio accounts queries
    @Query("SELECT * FROM portfolio_accounts ORDER BY id ASC")
    fun getAllPortfoliosFlow(): Flow<List<PortfolioAccount>>

    @Query("SELECT * FROM portfolio_accounts ORDER BY id ASC")
    suspend fun getAllPortfolios(): List<PortfolioAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolio(portfolio: PortfolioAccount): Long

    @Update
    suspend fun updatePortfolio(portfolio: PortfolioAccount)

    @Delete
    suspend fun deletePortfolio(portfolio: PortfolioAccount)

    @Query("DELETE FROM portfolio_accounts")
    suspend fun deleteAllPortfolios()

    @Query("SELECT * FROM trades WHERE portfolioId = :portfolioId ORDER BY timestamp DESC")
    fun getTradesForPortfolioFlow(portfolioId: Int): Flow<List<Trade>>

    @Query("SELECT * FROM trades WHERE portfolioId = :portfolioId ORDER BY timestamp DESC")
    suspend fun getTradesForPortfolio(portfolioId: Int): List<Trade>

    // Mistakes operations
    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC")
    fun getAllMistakesFlow(): Flow<List<Mistake>>

    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC")
    suspend fun getAllMistakes(): List<Mistake>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: Mistake): Long

    @Delete
    suspend fun deleteMistake(mistake: Mistake)

    @Update
    suspend fun updateMistake(mistake: Mistake)

    @Query("DELETE FROM mistakes")
    suspend fun deleteAllMistakes()
}
