package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.AccountEntity
import com.example.data.entity.RecurringTransactionEntity
import com.example.data.entity.SavingsGoalEntity
import com.example.data.entity.StockHoldingEntity
import com.example.data.export.DatabaseBackupPayload
import com.example.data.export.ExportStatus
import com.example.domain.model.CashflowSummary
import com.example.domain.model.NetWorthSummary
import com.example.domain.model.StockPosition
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.repository.AynaasRepository
import com.example.domain.repository.AverageDownCalculation
import com.example.domain.repository.DividendCalculation
import com.example.domain.repository.StockRefreshSummary
import com.example.util.CategoryDistributionItem
import com.example.util.DatabaseBackupSerializer
import com.example.util.FinancialChartAnalytics
import com.example.util.MonthlyTrendPoint
import com.example.util.TransactionCsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(
    private val repository: AynaasRepository
) : ViewModel() {

    // Sensor nominal global state
    private val _isBalanceVisible = MutableStateFlow(true)
    val isBalanceVisible: StateFlow<Boolean> = _isBalanceVisible.asStateFlow()

    fun toggleBalanceVisibility() {
        _isBalanceVisible.value = !_isBalanceVisible.value
    }

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<TransactionItem>> = repository.enrichedTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stockPositions: StateFlow<List<StockPosition>> = repository.stockPositions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val netWorthSummary: StateFlow<NetWorthSummary> = repository.netWorthSummary
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetWorthSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        )

    val cashflowSummary: StateFlow<CashflowSummary> = repository.cashflowSummary
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CashflowSummary(0.0, 0.0, 0.0, 0.0)
        )

    // Filter for Transactions screen
    private val _selectedFilter = MutableStateFlow("ALL") // ALL, INCOME, EXPENSE, TRANSFER
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
        allTransactions,
        _selectedFilter,
        _searchQuery
    ) { txns, filter, query ->
        txns.filter { txn ->
            val matchesFilter = when (filter) {
                "INCOME" -> txn.type == TransactionType.INCOME
                "EXPENSE" -> txn.type == TransactionType.EXPENSE
                "TRANSFER" -> txn.type == TransactionType.TRANSFER
                else -> true
            }
            val matchesQuery = if (query.isBlank()) true else {
                txn.category.contains(query, ignoreCase = true) ||
                txn.note.contains(query, ignoreCase = true) ||
                (txn.sourceAccountName?.contains(query, ignoreCase = true) ?: false) ||
                (txn.destinationAccountName?.contains(query, ignoreCase = true) ?: false)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        category: String,
        note: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.createTransaction(
                type = type,
                amount = amount,
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                category = category,
                note = note,
                dateMillis = dateMillis
            )
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    fun addAccount(
        name: String,
        type: String,
        balance: Double,
        accountNumber: String = "",
        colorHex: Long = 0xFF1E3A8A
    ) {
        viewModelScope.launch {
            repository.createAccount(name, type, balance, accountNumber, colorHex)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun addStockHolding(
        ticker: String,
        companyName: String,
        lots: Int,
        avgPrice: Double,
        currentPrice: Double
    ) {
        viewModelScope.launch {
            repository.addStockHolding(ticker, companyName, lots, avgPrice, currentPrice)
        }
    }

    fun updateStockHolding(holding: StockHoldingEntity) {
        viewModelScope.launch {
            repository.updateStockHolding(holding)
        }
    }

    fun updateStockPrice(id: Long, newPrice: Double) {
        viewModelScope.launch {
            repository.updateStockCurrentPrice(id, newPrice)
        }
    }

    fun deleteStockHolding(holding: StockHoldingEntity) {
        viewModelScope.launch {
            repository.deleteStockHolding(holding)
        }
    }

    // ------------------------------------------------------------------------
    // Stock Price Live Sync (Yahoo Finance API) & Calculators (V1.2)
    // ------------------------------------------------------------------------
    private val _isRefreshingStocks = MutableStateFlow(false)
    val isRefreshingStocks: StateFlow<Boolean> = _isRefreshingStocks.asStateFlow()

    private val _stockSyncStatusMessage = MutableStateFlow<String?>(null)
    val stockSyncStatusMessage: StateFlow<String?> = _stockSyncStatusMessage.asStateFlow()

    fun clearStockSyncMessage() {
        _stockSyncStatusMessage.value = null
    }

    fun refreshStockPrices(onComplete: ((StockRefreshSummary) -> Unit)? = null) {
        viewModelScope.launch {
            if (_isRefreshingStocks.value) return@launch
            _isRefreshingStocks.value = true
            _stockSyncStatusMessage.value = "Memperbarui harga saham BEI via Yahoo Finance..."
            try {
                val holdings = repository.allStockHoldings.stateIn(viewModelScope).value
                val summary = repository.refreshAllStockPrices(holdings)
                _stockSyncStatusMessage.value = summary.message
                onComplete?.invoke(summary)
            } catch (e: Exception) {
                _stockSyncStatusMessage.value = "Gagal memperbarui harga: ${e.localizedMessage}"
            } finally {
                _isRefreshingStocks.value = false
            }
        }
    }

    fun refreshSingleStockPrice(id: Long, onComplete: ((StockRefreshSummary) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val summary = repository.refreshSingleStockPrice(id)
                _stockSyncStatusMessage.value = summary.message
                onComplete?.invoke(summary)
            } catch (e: Exception) {
                _stockSyncStatusMessage.value = "Gagal memperbarui harga: ${e.localizedMessage}"
            }
        }
    }

    fun calculateAverageDown(
        currentLots: Int,
        currentAvg: Double,
        newLots: Int,
        newPrice: Double,
        brokerFeePercent: Double = 0.15
    ): AverageDownCalculation {
        return repository.stockRepository.calculateAverageDown(
            currentLots = currentLots,
            currentAvg = currentAvg,
            newLots = newLots,
            newPrice = newPrice,
            brokerFeePercent = brokerFeePercent
        )
    }

    fun calculateDividend(
        totalShares: Long,
        currentPrice: Double,
        dividendPerShare: Double
    ): DividendCalculation {
        return repository.stockRepository.calculateDividend(
            totalShares = totalShares,
            currentPrice = currentPrice,
            dividendPerShare = dividendPerShare
        )
    }

    // --- Data Export & Room Database Backup ---

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    private val _backupPayload = MutableStateFlow<DatabaseBackupPayload?>(null)
    val backupPayload: StateFlow<DatabaseBackupPayload?> = _backupPayload.asStateFlow()

    private val _serializedBackupJson = MutableStateFlow<String?>(null)
    val serializedBackupJson: StateFlow<String?> = _serializedBackupJson.asStateFlow()

    /**
     * Prepares and serializes the current Room Database snapshot to a JSON string.
     */
    fun loadBackupSnapshot(onReady: ((DatabaseBackupPayload, String) -> Unit)? = null) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.InProgress
            try {
                val payload = repository.getFullDatabaseBackup()
                val jsonString = DatabaseBackupSerializer.serializeToJson(
                    accounts = payload.accounts,
                    transactions = payload.transactions,
                    stockHoldings = payload.stockHoldings,
                    timestampMillis = payload.exportTimestampMillis
                )
                _backupPayload.value = payload
                _serializedBackupJson.value = jsonString
                _exportStatus.value = ExportStatus.Idle
                onReady?.invoke(payload, jsonString)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Failure("Gagal memuat database: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Writes the current backup JSON to a user-chosen SAF Document URI.
     */
    fun saveBackupToUri(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.InProgress
            try {
                val payload = _backupPayload.value ?: repository.getFullDatabaseBackup()
                val json = _serializedBackupJson.value ?: DatabaseBackupSerializer.serializeToJson(
                    accounts = payload.accounts,
                    transactions = payload.transactions,
                    stockHoldings = payload.stockHoldings,
                    timestampMillis = payload.exportTimestampMillis
                )
                val success = DatabaseBackupSerializer.writeJsonToUri(context, uri, json)
                if (success) {
                    val sizeBytes = json.toByteArray(Charsets.UTF_8).size.toLong()
                    _exportStatus.value = ExportStatus.Success(
                        fileName = fileName,
                        filePath = uri.lastPathSegment ?: fileName,
                        jsonString = json,
                        sizeBytes = sizeBytes,
                        totalAccounts = payload.totalAccounts,
                        totalTransactions = payload.totalTransactions,
                        totalStockHoldings = payload.totalStockHoldings,
                        message = "Cadangan berhasil disimpan ke berkas lokal!"
                    )
                } else {
                    _exportStatus.value = ExportStatus.Failure("Gagal menulis berkas ke lokasi yang dipilih.")
                }
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Failure("Terjadi kesalahan: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Saves the backup JSON to the app's local device storage directory (Documents/backups).
     */
    fun saveBackupToAppLocalStorage(context: Context) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.InProgress
            try {
                val payload = _backupPayload.value ?: repository.getFullDatabaseBackup()
                val json = _serializedBackupJson.value ?: DatabaseBackupSerializer.serializeToJson(
                    accounts = payload.accounts,
                    transactions = payload.transactions,
                    stockHoldings = payload.stockHoldings,
                    timestampMillis = payload.exportTimestampMillis
                )
                val fileName = DatabaseBackupSerializer.generateBackupFileName(payload.exportTimestampMillis)
                val file = DatabaseBackupSerializer.saveToAppLocalBackupDir(context, fileName, json)

                _exportStatus.value = ExportStatus.Success(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    jsonString = json,
                    sizeBytes = file.length(),
                    totalAccounts = payload.totalAccounts,
                    totalTransactions = payload.totalTransactions,
                    totalStockHoldings = payload.totalStockHoldings,
                    message = "Cadangan disimpan di: ${file.name}"
                )
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Failure("Gagal menyimpan ke penyimpanan lokal: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Shares the backup JSON text via Android's native share sheet.
     */
    fun shareBackup(context: Context) {
        val payload = _backupPayload.value
        val json = _serializedBackupJson.value
        if (json != null) {
            val fileName = DatabaseBackupSerializer.generateBackupFileName(payload?.exportTimestampMillis ?: System.currentTimeMillis())
            DatabaseBackupSerializer.shareBackupJson(context, json, fileName)
        }
    }

    /**
     * Copies the backup JSON string to the Android clipboard.
     */
    fun copyBackupToClipboard(context: Context) {
        val json = _serializedBackupJson.value
        if (json != null) {
            DatabaseBackupSerializer.copyToClipboard(context, json)
        }
    }

    fun resetExportStatus() {
        _exportStatus.value = ExportStatus.Idle
    }

    // --- CSV Transaction History Export ---

    private val _csvExportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val csvExportStatus: StateFlow<ExportStatus> = _csvExportStatus.asStateFlow()

    private val _serializedCsv = MutableStateFlow<String?>(null)
    val serializedCsv: StateFlow<String?> = _serializedCsv.asStateFlow()

    /**
     * Generates and buffers the CSV text for the specified transactions (or allTransactions by default).
     */
    fun prepareCsvSnapshot(customTransactions: List<TransactionItem>? = null, onReady: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _csvExportStatus.value = ExportStatus.InProgress
            try {
                val listToExport = customTransactions ?: allTransactions.value
                val csvString = TransactionCsvExporter.exportToCsv(listToExport)
                _serializedCsv.value = csvString
                _csvExportStatus.value = ExportStatus.Idle
                onReady?.invoke(csvString)
            } catch (e: Exception) {
                _csvExportStatus.value = ExportStatus.Failure("Gagal memformat CSV: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Saves CSV string to a user-chosen Document Uri via SAF.
     */
    fun saveCsvToUri(context: Context, uri: Uri, fileName: String, customTransactions: List<TransactionItem>? = null) {
        viewModelScope.launch {
            _csvExportStatus.value = ExportStatus.InProgress
            try {
                val listToExport = customTransactions ?: allTransactions.value
                val csvString = _serializedCsv.value ?: TransactionCsvExporter.exportToCsv(listToExport)
                val success = TransactionCsvExporter.writeCsvToUri(context, uri, csvString)
                if (success) {
                    val sizeBytes = csvString.toByteArray(Charsets.UTF_8).size.toLong()
                    _csvExportStatus.value = ExportStatus.Success(
                        fileName = fileName,
                        filePath = uri.lastPathSegment ?: fileName,
                        jsonString = csvString,
                        sizeBytes = sizeBytes,
                        totalAccounts = 0,
                        totalTransactions = listToExport.size,
                        totalStockHoldings = 0,
                        message = "Berkas CSV riwayat transaksi berhasil disimpan!"
                    )
                } else {
                    _csvExportStatus.value = ExportStatus.Failure("Gagal menulis berkas CSV ke lokasi yang dipilih.")
                }
            } catch (e: Exception) {
                _csvExportStatus.value = ExportStatus.Failure("Terjadi kesalahan: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Saves CSV string directly to app's local document storage (/exports).
     */
    fun saveCsvToAppLocalStorage(context: Context, customTransactions: List<TransactionItem>? = null) {
        viewModelScope.launch {
            _csvExportStatus.value = ExportStatus.InProgress
            try {
                val listToExport = customTransactions ?: allTransactions.value
                val csvString = _serializedCsv.value ?: TransactionCsvExporter.exportToCsv(listToExport)
                val fileName = TransactionCsvExporter.generateCsvFileName()
                val file = TransactionCsvExporter.saveToAppLocalCsvDir(context, fileName, csvString)

                _csvExportStatus.value = ExportStatus.Success(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    jsonString = csvString,
                    sizeBytes = file.length(),
                    totalAccounts = 0,
                    totalTransactions = listToExport.size,
                    totalStockHoldings = 0,
                    message = "CSV disimpan di: ${file.name}"
                )
            } catch (e: Exception) {
                _csvExportStatus.value = ExportStatus.Failure("Gagal menyimpan berkas CSV: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Shares the CSV file via system sharesheet.
     */
    fun shareCsv(context: Context, customTransactions: List<TransactionItem>? = null) {
        val listToExport = customTransactions ?: allTransactions.value
        val csvString = _serializedCsv.value ?: TransactionCsvExporter.exportToCsv(listToExport)
        val fileName = TransactionCsvExporter.generateCsvFileName()
        TransactionCsvExporter.shareCsv(context, csvString, fileName)
    }

    /**
     * Copies CSV text to clipboard.
     */
    fun copyCsvToClipboard(context: Context, customTransactions: List<TransactionItem>? = null) {
        val listToExport = customTransactions ?: allTransactions.value
        val csvString = _serializedCsv.value ?: TransactionCsvExporter.exportToCsv(listToExport)
        TransactionCsvExporter.copyToClipboard(context, csvString)
    }

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allSavingsGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlyTrends: StateFlow<List<MonthlyTrendPoint>> = repository.allTransactions
        .map { txns -> FinancialChartAnalytics.calculateMonthlyTrends(txns) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val incomeCategoryDistribution: StateFlow<List<CategoryDistributionItem>> = repository.allTransactions
        .map { txns -> FinancialChartAnalytics.calculateCategoryDistribution(txns, "INCOME") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenseCategoryDistribution: StateFlow<List<CategoryDistributionItem>> = repository.allTransactions
        .map { txns -> FinancialChartAnalytics.calculateCategoryDistribution(txns, "EXPENSE") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createSavingsGoal(
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        deadlineMillis: Long,
        categoryIcon: String,
        colorHex: Long,
        note: String
    ) {
        viewModelScope.launch {
            repository.createSavingsGoal(title, targetAmount, currentAmount, deadlineMillis, categoryIcon, colorHex, note)
        }
    }

    fun updateSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    fun addFundsToGoal(id: Long, amount: Double) {
        viewModelScope.launch {
            repository.addFundsToGoal(id, amount)
        }
    }

    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurringTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createRecurringTransaction(
        title: String,
        type: String,
        amount: Double,
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        category: String,
        frequency: String,
        nextExecutionMillis: Long,
        note: String
    ) {
        viewModelScope.launch {
            repository.createRecurringTransaction(
                title, type, amount, sourceAccountId, destinationAccountId, category, frequency, nextExecutionMillis, note
            )
        }
    }

    fun updateRecurringTransaction(entity: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(entity)
        }
    }

    fun deleteRecurringTransaction(entity: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(entity)
        }
    }

    fun toggleRecurringActive(entity: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(entity.copy(isActive = !entity.isActive))
        }
    }

    fun resetCsvExportStatus() {
        _csvExportStatus.value = ExportStatus.Idle
    }
}

class FinanceViewModelFactory(
    private val repository: AynaasRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
