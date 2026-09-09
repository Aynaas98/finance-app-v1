package com.example.domain.model

import com.example.data.entity.AccountEntity
import com.example.data.entity.StockHoldingEntity
import com.example.data.entity.TransactionEntity

enum class TransactionType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    TRANSFER("Transfer")
}

data class TransactionItem(
    val entity: TransactionEntity,
    val sourceAccountName: String?,
    val destinationAccountName: String?
) {
    val id: Long get() = entity.id
    val type: TransactionType get() = when (entity.type.uppercase()) {
        "INCOME" -> TransactionType.INCOME
        "EXPENSE" -> TransactionType.EXPENSE
        else -> TransactionType.TRANSFER
    }
    val amount: Double get() = entity.amount
    val category: String get() = entity.category
    val note: String get() = entity.note
    val dateMillis: Long get() = entity.dateMillis
}

data class StockPosition(
    val entity: StockHoldingEntity
) {
    val id: Long get() = entity.id
    val ticker: String get() = entity.ticker
    val companyName: String get() = entity.companyName
    val lots: Int get() = entity.lots
    val avgPrice: Double get() = entity.avgPrice
    val currentPrice: Double get() = entity.currentPrice

    // Formula Saham
    val totalShares: Long get() = lots * 100L
    val modal: Double get() = totalShares * avgPrice
    val marketValue: Double get() = totalShares * currentPrice
    val unrealizedPL: Double get() = marketValue - modal
    val unrealizedPLPercentage: Double get() = if (modal > 0.0) (unrealizedPL / modal) * 100.0 else 0.0
    val isProfitable: Boolean get() = unrealizedPL >= 0.0
}

data class NetWorthSummary(
    val totalCashAndBank: Double,
    val totalStockMarketValue: Double,
    val totalStockModal: Double,
    val totalStockPL: Double,
    val totalStockPLPercentage: Double,
    val totalNetWorth: Double
)

data class CashflowSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val netCashflow: Double,
    val totalTransferVolume: Double
)
