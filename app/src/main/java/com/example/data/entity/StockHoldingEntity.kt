package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_holdings")
data class StockHoldingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ticker: String,
    val companyName: String,
    val lots: Int,
    val avgPrice: Double,
    val currentPrice: Double,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
