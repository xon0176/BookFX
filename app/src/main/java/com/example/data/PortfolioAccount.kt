package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "portfolio_accounts")
data class PortfolioAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val broker: String = "Unknown Broker",
    val startingEquity: Double = 100.0,
    val type: String = "Live Cash", // "Live Cash", "Prop Firm", "Demo"
    val currency: String = "USD",
    val description: String = ""
) : Serializable
