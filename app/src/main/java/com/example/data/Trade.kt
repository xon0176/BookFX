package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "trades")
data class Trade(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val isBuy: Boolean, // true for Buy, false for Sell
    val entryPrice: Double,
    val exitPrice: Double,
    val size: Double, // Lot size or contract unit
    val profit: Double, // Profit (positive) or Loss (negative)
    val brokerage: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val portfolioId: Int = 0 // Linked portfolio ID
) : Serializable
