package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val name: String,
    val totalEquity: Double = 100.0,
    val traderAlias: String = "",
    val country: String = "",
    val primaryInstrument: String = "",
    val currency: String = "USD"
)
