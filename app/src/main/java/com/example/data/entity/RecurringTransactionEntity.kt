package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val sourceAccountId: Long?,
    val destinationAccountId: Long?,
    val category: String,
    val frequency: String, // "MONTHLY", "WEEKLY", "DAILY"
    val nextExecutionMillis: Long,
    val isActive: Boolean = true,
    val note: String = ""
)
