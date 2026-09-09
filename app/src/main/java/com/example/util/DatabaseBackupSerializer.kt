package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.example.data.entity.AccountEntity
import com.example.data.entity.StockHoldingEntity
import com.example.data.entity.TransactionEntity
import com.example.data.export.DatabaseBackupPayload
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility responsible for serializing Room Database entities into a structured JSON string
 * and handling manual backup storage operations on local Android storage.
 */
object DatabaseBackupSerializer {

    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val humanDateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss 'WIB'", Locale("id", "ID"))

    /**
     * Generates a standard default backup file name.
     */
    fun generateBackupFileName(timestampMillis: Long = System.currentTimeMillis()): String {
        val stamp = fileTimestampFormat.format(Date(timestampMillis))
        return "aynaas_finance_backup_$stamp.json"
    }

    /**
     * Serializes Room Database records (Accounts, Transactions, Stock Holdings) into a formatted JSON string.
     */
    fun serializeToJson(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        stockHoldings: List<StockHoldingEntity>,
        timestampMillis: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()

        // Metadata
        root.put("version", 1)
        root.put("app", "AYNAAS Finance")
        root.put("exportTimestampMillis", timestampMillis)
        root.put("exportDateFormatted", humanDateFormat.format(Date(timestampMillis)))

        // Summary Counts
        val summaryObj = JSONObject()
        summaryObj.put("totalAccounts", accounts.size)
        summaryObj.put("totalTransactions", transactions.size)
        summaryObj.put("totalStockHoldings", stockHoldings.size)
        root.put("summary", summaryObj)

        // 1. Accounts Array
        val accountsArray = JSONArray()
        for (acc in accounts) {
            val accObj = JSONObject()
            accObj.put("id", acc.id)
            accObj.put("name", acc.name)
            accObj.put("type", acc.type)
            accObj.put("balance", acc.balance)
            accObj.put("accountNumber", acc.accountNumber)
            accObj.put("colorHex", acc.colorHex)
            accountsArray.put(accObj)
        }
        root.put("accounts", accountsArray)

        // 2. Transactions Array
        val transactionsArray = JSONArray()
        for (txn in transactions) {
            val txnObj = JSONObject()
            txnObj.put("id", txn.id)
            txnObj.put("type", txn.type)
            txnObj.put("amount", txn.amount)
            txnObj.put("category", txn.category)
            txnObj.put("note", txn.note)
            txnObj.put("dateMillis", txn.dateMillis)
            if (txn.sourceAccountId != null) {
                txnObj.put("sourceAccountId", txn.sourceAccountId)
            } else {
                txnObj.put("sourceAccountId", JSONObject.NULL)
            }
            if (txn.destinationAccountId != null) {
                txnObj.put("destinationAccountId", txn.destinationAccountId)
            } else {
                txnObj.put("destinationAccountId", JSONObject.NULL)
            }
            transactionsArray.put(txnObj)
        }
        root.put("transactions", transactionsArray)

        // 3. Stock Holdings Array
        val stocksArray = JSONArray()
        for (stock in stockHoldings) {
            val stockObj = JSONObject()
            stockObj.put("id", stock.id)
            stockObj.put("ticker", stock.ticker)
            stockObj.put("companyName", stock.companyName)
            stockObj.put("lots", stock.lots)
            stockObj.put("avgPrice", stock.avgPrice)
            stockObj.put("currentPrice", stock.currentPrice)
            stockObj.put("lastUpdatedMillis", stock.lastUpdatedMillis)
            stocksArray.put(stockObj)
        }
        root.put("stockHoldings", stocksArray)

        // Format with 2 spaces indent for readability
        return root.toString(2)
    }

    /**
     * Parses the JSON backup string back into a DatabaseBackupPayload for verification or future restore.
     */
    fun deserializeFromJson(jsonString: String): DatabaseBackupPayload {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val app = root.optString("app", "AYNAAS Finance")
        val timestamp = root.optLong("exportTimestampMillis", System.currentTimeMillis())
        val formattedDate = root.optString("exportDateFormatted", "")

        val summaryObj = root.optJSONObject("summary")
        val totalAccounts = summaryObj?.optInt("totalAccounts", 0) ?: 0
        val totalTransactions = summaryObj?.optInt("totalTransactions", 0) ?: 0
        val totalStockHoldings = summaryObj?.optInt("totalStockHoldings", 0) ?: 0

        // Parse Accounts
        val accountsList = mutableListOf<AccountEntity>()
        val accountsArray = root.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accountsArray.length()) {
            val obj = accountsArray.getJSONObject(i)
            accountsList.add(
                AccountEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    type = obj.getString("type"),
                    balance = obj.getDouble("balance"),
                    accountNumber = obj.optString("accountNumber", ""),
                    colorHex = obj.optLong("colorHex", 0xFF1E3A8AL)
                )
            )
        }

        // Parse Transactions
        val transactionsList = mutableListOf<TransactionEntity>()
        val transactionsArray = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until transactionsArray.length()) {
            val obj = transactionsArray.getJSONObject(i)
            val srcId = if (obj.isNull("sourceAccountId")) null else obj.optLong("sourceAccountId")
            val destId = if (obj.isNull("destinationAccountId")) null else obj.optLong("destinationAccountId")

            transactionsList.add(
                TransactionEntity(
                    id = obj.getLong("id"),
                    type = obj.getString("type"),
                    amount = obj.getDouble("amount"),
                    category = obj.getString("category"),
                    note = obj.optString("note", ""),
                    dateMillis = obj.getLong("dateMillis"),
                    sourceAccountId = srcId,
                    destinationAccountId = destId
                )
            )
        }

        // Parse Stock Holdings
        val stockList = mutableListOf<StockHoldingEntity>()
        val stocksArray = root.optJSONArray("stockHoldings") ?: JSONArray()
        for (i in 0 until stocksArray.length()) {
            val obj = stocksArray.getJSONObject(i)
            stockList.add(
                StockHoldingEntity(
                    id = obj.getLong("id"),
                    ticker = obj.getString("ticker"),
                    companyName = obj.getString("companyName"),
                    lots = obj.getInt("lots"),
                    avgPrice = obj.getDouble("avgPrice"),
                    currentPrice = obj.getDouble("currentPrice"),
                    lastUpdatedMillis = obj.optLong("lastUpdatedMillis", 0L)
                )
            )
        }

        return DatabaseBackupPayload(
            version = version,
            app = app,
            exportTimestampMillis = timestamp,
            exportDateFormatted = formattedDate,
            totalAccounts = if (totalAccounts > 0) totalAccounts else accountsList.size,
            totalTransactions = if (totalTransactions > 0) totalTransactions else transactionsList.size,
            totalStockHoldings = if (totalStockHoldings > 0) totalStockHoldings else stockList.size,
            accounts = accountsList,
            transactions = transactionsList,
            stockHoldings = stockList
        )
    }

    /**
     * Writes the serialized JSON backup string to a user-selected Storage Access Framework URI.
     */
    fun writeJsonToUri(context: Context, uri: Uri, jsonString: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves a copy of the backup JSON file to local app storage directory (Documents or filesDir/backups)
     * so that manual backups are permanently retained on the device.
     */
    fun saveToAppLocalBackupDir(context: Context, fileName: String, jsonString: String): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val backupDir = File(baseDir, "backups").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(backupDir, fileName)
        FileOutputStream(targetFile).use { output ->
            output.write(jsonString.toByteArray(Charsets.UTF_8))
            output.flush()
        }
        return targetFile
    }

    /**
     * Copies the JSON backup text directly to the system clipboard.
     */
    fun copyToClipboard(context: Context, jsonString: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AYNAAS Finance Backup JSON", jsonString)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Shares the JSON backup string through Android's system share sheet (e.g. to Google Drive, WhatsApp, Email, etc.).
     */
    fun shareBackupJson(context: Context, jsonString: String, fileName: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "AYNAAS Finance Backup - $fileName")
            putExtra(Intent.EXTRA_TEXT, jsonString)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Cadangan Database")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
