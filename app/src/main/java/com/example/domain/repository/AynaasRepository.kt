package com.example.domain.repository

import com.example.data.dao.AccountDao
import com.example.data.dao.RecurringTransactionDao
import com.example.data.dao.SavingsGoalDao
import com.example.data.dao.StockHoldingDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.AccountEntity
import com.example.data.entity.RecurringTransactionEntity
import com.example.data.entity.SavingsGoalEntity
import com.example.data.entity.StockHoldingEntity
import com.example.data.entity.TransactionEntity
import com.example.data.export.DatabaseBackupPayload
import com.example.domain.model.CashflowSummary
import com.example.domain.model.NetWorthSummary
import com.example.domain.model.StockPosition
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.util.DatabaseBackupSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AynaasRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    val stockHoldingDao: StockHoldingDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    val stockRepository: StockRepository = StockRepository(stockHoldingDao)
) {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkAndExecuteRecurringTransactions()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allStockHoldings: Flow<List<StockHoldingEntity>> = stockRepository.allStockHoldings
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()
    val allRecurringTransactions: Flow<List<RecurringTransactionEntity>> = recurringTransactionDao.getAllRecurringTransactions()

    val stockPositions: Flow<List<StockPosition>> = stockRepository.stockPositions

    val enrichedTransactions: Flow<List<TransactionItem>> = combine(
        allTransactions,
        allAccounts
    ) { transactions, accounts ->
        val accountMap = accounts.associateBy { it.id }
        transactions.map { txn ->
            TransactionItem(
                entity = txn,
                sourceAccountName = txn.sourceAccountId?.let { accountMap[it]?.name },
                destinationAccountName = txn.destinationAccountId?.let { accountMap[it]?.name }
            )
        }
    }

    val netWorthSummary: Flow<NetWorthSummary> = combine(
        allAccounts,
        stockPositions
    ) { accounts, stocks ->
        val totalCash = accounts.sumOf { it.balance }
        val totalStockMarket = stocks.sumOf { it.marketValue }
        val totalStockModal = stocks.sumOf { it.modal }
        val totalStockPL = totalStockMarket - totalStockModal
        val stockPLPercent = if (totalStockModal > 0.0) (totalStockPL / totalStockModal) * 100.0 else 0.0
        val netWorth = totalCash + totalStockMarket

        NetWorthSummary(
            totalCashAndBank = totalCash,
            totalStockMarketValue = totalStockMarket,
            totalStockModal = totalStockModal,
            totalStockPL = totalStockPL,
            totalStockPLPercentage = stockPLPercent,
            totalNetWorth = netWorth
        )
    }

    val cashflowSummary: Flow<CashflowSummary> = allTransactions.map { transactions ->
        var income = 0.0
        var expense = 0.0
        var transferVolume = 0.0

        for (txn in transactions) {
            when (txn.type.uppercase()) {
                "INCOME" -> income += txn.amount
                "EXPENSE" -> expense += txn.amount
                "TRANSFER" -> transferVolume += txn.amount
            }
        }

        CashflowSummary(
            totalIncome = income,
            totalExpense = expense,
            netCashflow = income - expense,
            totalTransferVolume = transferVolume
        )
    }

    suspend fun createTransaction(
        type: TransactionType,
        amount: Double,
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        category: String,
        note: String,
        dateMillis: Long = System.currentTimeMillis()
    ): Long {
        when (type) {
            TransactionType.INCOME -> {
                if (destinationAccountId != null) {
                    val dest = accountDao.getAccountById(destinationAccountId)
                    if (dest != null) {
                        accountDao.updateBalance(dest.id, dest.balance + amount)
                    }
                }
            }
            TransactionType.EXPENSE -> {
                if (sourceAccountId != null) {
                    val src = accountDao.getAccountById(sourceAccountId)
                    if (src != null) {
                        accountDao.updateBalance(src.id, src.balance - amount)
                    }
                }
            }
            TransactionType.TRANSFER -> {
                if (sourceAccountId != null && destinationAccountId != null) {
                    val src = accountDao.getAccountById(sourceAccountId)
                    val dest = accountDao.getAccountById(destinationAccountId)
                    if (src != null && dest != null) {
                        accountDao.updateBalance(src.id, src.balance - amount)
                        accountDao.updateBalance(dest.id, dest.balance + amount)
                    }
                }
            }
        }

        val entity = TransactionEntity(
            type = type.name,
            amount = amount,
            sourceAccountId = sourceAccountId,
            destinationAccountId = destinationAccountId,
            category = category,
            note = note,
            dateMillis = dateMillis
        )
        return transactionDao.insertTransaction(entity)
    }

    suspend fun deleteTransaction(transactionId: Long) {
        val txn = transactionDao.getTransactionById(transactionId) ?: return
        when (txn.type.uppercase()) {
            "INCOME" -> {
                txn.destinationAccountId?.let { destId ->
                    val dest = accountDao.getAccountById(destId)
                    if (dest != null) {
                        accountDao.updateBalance(dest.id, dest.balance - txn.amount)
                    }
                }
            }
            "EXPENSE" -> {
                txn.sourceAccountId?.let { srcId ->
                    val src = accountDao.getAccountById(srcId)
                    if (src != null) {
                        accountDao.updateBalance(src.id, src.balance + txn.amount)
                    }
                }
            }
            "TRANSFER" -> {
                if (txn.sourceAccountId != null && txn.destinationAccountId != null) {
                    val src = accountDao.getAccountById(txn.sourceAccountId)
                    val dest = accountDao.getAccountById(txn.destinationAccountId)
                    if (src != null && dest != null) {
                        accountDao.updateBalance(src.id, src.balance + txn.amount)
                        accountDao.updateBalance(dest.id, dest.balance - txn.amount)
                    }
                }
            }
        }
        transactionDao.deleteTransactionById(transactionId)
    }

    suspend fun createAccount(
        name: String,
        type: String,
        balance: Double,
        accountNumber: String = "",
        colorHex: Long = 0xFF1E3A8A
    ): Long {
        val account = AccountEntity(
            name = name,
            type = type,
            balance = balance,
            accountNumber = accountNumber,
            colorHex = colorHex
        )
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    suspend fun addStockHolding(
        ticker: String,
        companyName: String,
        lots: Int,
        avgPrice: Double,
        currentPrice: Double
    ): Long {
        val holding = StockHoldingEntity(
            ticker = ticker.uppercase().trim(),
            companyName = companyName.trim(),
            lots = lots,
            avgPrice = avgPrice,
            currentPrice = currentPrice,
            lastUpdatedMillis = System.currentTimeMillis()
        )
        return stockHoldingDao.insertStockHolding(holding)
    }

    suspend fun updateStockHolding(holding: StockHoldingEntity) {
        stockHoldingDao.updateStockHolding(holding.copy(lastUpdatedMillis = System.currentTimeMillis()))
    }

    suspend fun updateStockCurrentPrice(id: Long, newPrice: Double) {
        val holding = stockHoldingDao.getStockHoldingById(id) ?: return
        stockHoldingDao.updateStockHolding(
            holding.copy(
                currentPrice = newPrice,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteStockHolding(holding: StockHoldingEntity) {
        stockHoldingDao.deleteStockHolding(holding)
    }

    suspend fun refreshAllStockPrices(holdings: List<StockHoldingEntity>): StockRefreshSummary {
        return stockRepository.refreshAllStockPrices(holdings)
    }

    suspend fun refreshSingleStockPrice(id: Long): StockRefreshSummary {
        return stockRepository.refreshStockPrice(id)
    }

    /**
     * Captures a snapshot of all Room Database tables and returns a structured payload.
     */
    suspend fun getFullDatabaseBackup(): DatabaseBackupPayload {
        val accounts = allAccounts.first()
        val transactions = allTransactions.first()
        val stockHoldings = allStockHoldings.first()
        val timestamp = System.currentTimeMillis()
        val json = DatabaseBackupSerializer.serializeToJson(accounts, transactions, stockHoldings, timestamp)
        return DatabaseBackupSerializer.deserializeFromJson(json)
    }

    /**
     * Serializes the entire Room Database into a formatted JSON string for manual backup.
     */
    suspend fun exportDatabaseToJsonString(): String {
        val accounts = allAccounts.first()
        val transactions = allTransactions.first()
        val stockHoldings = allStockHoldings.first()
        return DatabaseBackupSerializer.serializeToJson(accounts, transactions, stockHoldings)
    }

    suspend fun createSavingsGoal(
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        deadlineMillis: Long,
        categoryIcon: String,
        colorHex: Long,
        note: String
    ): Long {
        val goal = SavingsGoalEntity(
            title = title.trim(),
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            deadlineMillis = deadlineMillis,
            categoryIcon = categoryIcon,
            colorHex = colorHex,
            note = note.trim()
        )
        return savingsGoalDao.insertGoal(goal)
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        savingsGoalDao.updateGoal(goal)
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        savingsGoalDao.deleteGoal(goal)
    }

    suspend fun addFundsToGoal(id: Long, amount: Double) {
        savingsGoalDao.addFundsToGoal(id, amount)
    }

    suspend fun createRecurringTransaction(
        title: String,
        type: String,
        amount: Double,
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        category: String,
        frequency: String,
        nextExecutionMillis: Long,
        note: String
    ): Long {
        val entity = RecurringTransactionEntity(
            title = title.trim(),
            type = type.uppercase(),
            amount = amount,
            sourceAccountId = sourceAccountId,
            destinationAccountId = destinationAccountId,
            category = category.trim(),
            frequency = frequency.uppercase(),
            nextExecutionMillis = nextExecutionMillis,
            isActive = true,
            note = note.trim()
        )
        return recurringTransactionDao.insertRecurringTransaction(entity)
    }

    suspend fun updateRecurringTransaction(entity: RecurringTransactionEntity) {
        recurringTransactionDao.updateRecurringTransaction(entity)
    }

    suspend fun deleteRecurringTransaction(entity: RecurringTransactionEntity) {
        recurringTransactionDao.deleteRecurringTransaction(entity)
    }

    suspend fun checkAndExecuteRecurringTransactions() {
        val now = System.currentTimeMillis()
        val activeList = recurringTransactionDao.getActiveRecurringTransactions()
        for (item in activeList) {
            if (item.nextExecutionMillis <= now) {
                val txnType = try {
                    TransactionType.valueOf(item.type)
                } catch (e: Exception) {
                    TransactionType.EXPENSE
                }
                createTransaction(
                    type = txnType,
                    amount = item.amount,
                    sourceAccountId = item.sourceAccountId,
                    destinationAccountId = item.destinationAccountId,
                    category = item.category,
                    note = "${item.title} (Rutin: ${item.frequency})",
                    dateMillis = now
                )

                val dayMillis = 86400000L
                val nextMillis = when (item.frequency) {
                    "DAILY" -> item.nextExecutionMillis + dayMillis
                    "WEEKLY" -> item.nextExecutionMillis + (dayMillis * 7)
                    else -> item.nextExecutionMillis + (dayMillis * 30)
                }

                val updated = item.copy(nextExecutionMillis = nextMillis)
                recurringTransactionDao.updateRecurringTransaction(updated)
            }
        }
    }
}
