package com.example.data.export

import com.example.data.entity.AccountEntity
import com.example.data.entity.StockHoldingEntity
import com.example.data.entity.TransactionEntity

/**
 * Payload data class encapsulating the full state of the Room Database for manual export/backup.
 */
data class DatabaseBackupPayload(
    val version: Int = 1,
    val app: String = "AYNAAS Finance",
    val exportTimestampMillis: Long = System.currentTimeMillis(),
    val exportDateFormatted: String,
    val totalAccounts: Int,
    val totalTransactions: Int,
    val totalStockHoldings: Int,
    val accounts: List<AccountEntity>,
    val transactions: List<TransactionEntity>,
    val stockHoldings: List<StockHoldingEntity>
)

/**
 * Supported file export formats in AYNAAS Finance.
 */
enum class ExportFormat(val title: String, val extension: String) {
    JSON("Cadangan Database (JSON)", "json"),
    CSV("Riwayat Transaksi (CSV)", "csv")
}

/**
 * Represents the status/result of an export operation.
 */
sealed interface ExportStatus {
    data object Idle : ExportStatus
    data object InProgress : ExportStatus
    data class Success(
        val fileName: String,
        val filePath: String?,
        val jsonString: String,
        val sizeBytes: Long,
        val totalAccounts: Int,
        val totalTransactions: Int,
        val totalStockHoldings: Int,
        val message: String
    ) : ExportStatus
    data class Failure(val errorMessage: String) : ExportStatus
}
