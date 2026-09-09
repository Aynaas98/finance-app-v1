package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.AccountEntity
import com.example.data.entity.StockHoldingEntity
import com.example.data.entity.TransactionEntity
import com.example.domain.model.TransactionItem
import com.example.util.DatabaseBackupSerializer
import com.example.util.TransactionCsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AYNAAS Finance", appName)
  }

  @Test
  fun testDatabaseBackupSerializationAndDeserialization() {
    val accounts = listOf(
      AccountEntity(
        id = 1,
        name = "BCA Tabungan",
        type = "BANK",
        balance = 12500000.0,
        accountNumber = "1234567890",
        colorHex = 0xFF38BDF8L
      ),
      AccountEntity(
        id = 2,
        name = "GoPay",
        type = "E_WALLET",
        balance = 350000.0,
        accountNumber = "08123456789",
        colorHex = 0xFF34D399L
      )
    )

    val transactions = listOf(
      TransactionEntity(
        id = 101,
        type = "INCOME",
        amount = 7500000.0,
        category = "Gaji",
        note = "Gaji Bulanan",
        dateMillis = 1750000000000L,
        sourceAccountId = null,
        destinationAccountId = 1
      ),
      TransactionEntity(
        id = 102,
        type = "EXPENSE",
        amount = 45000.0,
        category = "Makanan & Minuman",
        note = "Makan siang",
        dateMillis = 1750001000000L,
        sourceAccountId = 2,
        destinationAccountId = null
      )
    )

    val stockHoldings = listOf(
      StockHoldingEntity(
        id = 201,
        ticker = "BBCA",
        companyName = "Bank Central Asia",
        lots = 10,
        avgPrice = 9500.0,
        currentPrice = 10200.0,
        lastUpdatedMillis = 1750000000000L
      )
    )

    // 1. Serialize to JSON string
    val jsonString = DatabaseBackupSerializer.serializeToJson(
      accounts = accounts,
      transactions = transactions,
      stockHoldings = stockHoldings,
      timestampMillis = 1750000000000L
    )

    assertTrue(jsonString.isNotBlank())
    assertTrue(jsonString.contains("\"app\": \"AYNAAS Finance\""))
    assertTrue(jsonString.contains("\"version\": 1"))
    assertTrue(jsonString.contains("\"BCA Tabungan\""))
    assertTrue(jsonString.contains("\"BBCA\""))
    assertTrue(jsonString.contains("\"totalAccounts\": 2"))
    assertTrue(jsonString.contains("\"totalTransactions\": 2"))
    assertTrue(jsonString.contains("\"totalStockHoldings\": 1"))

    // 2. Deserialize back from JSON
    val payload = DatabaseBackupSerializer.deserializeFromJson(jsonString)

    assertEquals(1, payload.version)
    assertEquals("AYNAAS Finance", payload.app)
    assertEquals(2, payload.totalAccounts)
    assertEquals(2, payload.totalTransactions)
    assertEquals(1, payload.totalStockHoldings)

    // Verify accounts
    assertEquals(2, payload.accounts.size)
    assertEquals("BCA Tabungan", payload.accounts[0].name)
    assertEquals(12500000.0, payload.accounts[0].balance, 0.001)
    assertEquals("GoPay", payload.accounts[1].name)

    // Verify transactions
    assertEquals(2, payload.transactions.size)
    assertEquals("INCOME", payload.transactions[0].type)
    assertEquals(7500000.0, payload.transactions[0].amount, 0.001)
    assertEquals("EXPENSE", payload.transactions[1].type)

    // Verify stock holdings
    assertEquals(1, payload.stockHoldings.size)
    assertEquals("BBCA", payload.stockHoldings[0].ticker)
    assertEquals(10, payload.stockHoldings[0].lots)
    assertEquals(9500.0, payload.stockHoldings[0].avgPrice, 0.001)
    assertEquals(10200.0, payload.stockHoldings[0].currentPrice, 0.001)
  }

  @Test
  fun testBackupFileNameFormat() {
    val fileName = DatabaseBackupSerializer.generateBackupFileName(1750000000000L)
    assertTrue(fileName.startsWith("aynaas_finance_backup_"))
    assertTrue(fileName.endsWith(".json"))
  }

  @Test
  fun testTransactionCsvExportFormatting() {
    val items = listOf(
      TransactionItem(
        entity = TransactionEntity(
          id = 101,
          type = "INCOME",
          amount = 7500000.0,
          category = "Gaji",
          note = "Gaji Pokok, Bonus & THR",
          dateMillis = 1750000000000L,
          sourceAccountId = null,
          destinationAccountId = 1
        ),
        sourceAccountName = null,
        destinationAccountName = "BCA Tabungan"
      ),
      TransactionItem(
        entity = TransactionEntity(
          id = 102,
          type = "EXPENSE",
          amount = 45000.0,
          category = "Makanan & Minuman",
          note = "Makan siang \"Spesial\"",
          dateMillis = 1750001000000L,
          sourceAccountId = 2,
          destinationAccountId = null
        ),
        sourceAccountName = "GoPay",
        destinationAccountName = null
      )
    )

    val csvString = TransactionCsvExporter.exportToCsv(items)

    // Check Header
    assertTrue(csvString.contains("ID,Tanggal,Waktu,Tipe Transaksi,Kategori,Jumlah (IDR),Rekening Sumber,Rekening Tujuan,Catatan"))

    // Check Content
    assertTrue(csvString.contains("101,"))
    assertTrue(csvString.contains("Pemasukan"))
    assertTrue(csvString.contains("7500000.00"))
    assertTrue(csvString.contains("\"BCA Tabungan\"") || csvString.contains("BCA Tabungan"))
    assertTrue(csvString.contains("\"Gaji Pokok, Bonus & THR\"")) // Commas escaped with quotes

    assertTrue(csvString.contains("102,"))
    assertTrue(csvString.contains("Pengeluaran"))
    assertTrue(csvString.contains("45000.00"))
    assertTrue(csvString.contains("GoPay"))
    assertTrue(csvString.contains("\"Makan siang \"\"Spesial\"\"\"")) // Double quotes escaped with double quotes
  }

  @Test
  fun testCsvFileNameGeneration() {
    val fileName = TransactionCsvExporter.generateCsvFileName(1750000000000L)
    assertTrue(fileName.startsWith("aynaas_transaksi_"))
    assertTrue(fileName.endsWith(".csv"))
  }

  @Test
  fun testEscapeCsvRules() {
    assertEquals("Normal Text", TransactionCsvExporter.escapeCsv("Normal Text"))
    assertEquals("\"Text, with comma\"", TransactionCsvExporter.escapeCsv("Text, with comma"))
    assertEquals("\"Text with \"\"quotes\"\"\"", TransactionCsvExporter.escapeCsv("Text with \"quotes\""))
    assertEquals("\"Text; with semicolon\"", TransactionCsvExporter.escapeCsv("Text; with semicolon"))
  }
}
