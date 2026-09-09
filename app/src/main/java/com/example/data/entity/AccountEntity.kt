package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // BANK, E_WALLET, CASH, INVESTMENT_RDN
    val balance: Double,
    val accountNumber: String = "",
    val colorHex: Long = 0xFF1E3A8A
)
