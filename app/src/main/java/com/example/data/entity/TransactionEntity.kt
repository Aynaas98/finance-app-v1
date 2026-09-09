package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // INCOME, EXPENSE, TRANSFER
    val amount: Double,
    val sourceAccountId: Long? = null,
    val destinationAccountId: Long? = null,
    val category: String,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis()
)
