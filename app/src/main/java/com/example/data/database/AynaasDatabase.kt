package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        StockHoldingEntity::class,
        SavingsGoalEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AynaasDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockHoldingDao(): StockHoldingDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AynaasDatabase? = null

        fun getInstance(context: Context): AynaasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AynaasDatabase::class.java,
                    "aynaas_finance.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            prepopulateDatabase(getInstance(context))
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateDatabase(db: AynaasDatabase) {
            val accountDao = db.accountDao()
            val transactionDao = db.transactionDao()
            val stockDao = db.stockHoldingDao()

            if (accountDao.countAccounts() == 0) {
                val accounts = listOf(
                    AccountEntity(
                        id = 1,
                        name = "Bank BCA - Tabungan",
                        type = "BANK",
                        balance = 18750000.0,
                        accountNumber = "8280-1234-56",
                        colorHex = 0xFF005696 // BCA Blue
                    ),
                    AccountEntity(
                        id = 2,
                        name = "Bank Mandiri - Darurat",
                        type = "BANK",
                        balance = 25000000.0,
                        accountNumber = "137-00-9876-54",
                        colorHex = 0xFF003865 // Mandiri Deep Blue
                    ),
                    AccountEntity(
                        id = 3,
                        name = "RDN Saham Mandiri Sekuritas",
                        type = "INVESTMENT_RDN",
                        balance = 12500000.0,
                        accountNumber = "RDN-8829-01",
                        colorHex = 0xFF0F766E // Teal
                    ),
                    AccountEntity(
                        id = 4,
                        name = "GoPay Wallet",
                        type = "E_WALLET",
                        balance = 850000.0,
                        accountNumber = "0812-3456-7890",
                        colorHex = 0xFF00880C // Gojek Green
                    ),
                    AccountEntity(
                        id = 5,
                        name = "Dompet Tunai (Cash)",
                        type = "CASH",
                        balance = 450000.0,
                        accountNumber = "Kas Fisik",
                        colorHex = 0xFFD97706 // Amber
                    )
                )
                accountDao.insertAll(accounts)
            }

            if (stockDao.countStockHoldings() == 0) {
                val holdings = listOf(
                    StockHoldingEntity(
                        id = 1,
                        ticker = "BBCA",
                        companyName = "PT Bank Central Asia Tbk",
                        lots = 15, // 1,500 shares
                        avgPrice = 9800.0,
                        currentPrice = 10450.0,
                        lastUpdatedMillis = System.currentTimeMillis()
                    ),
                    StockHoldingEntity(
                        id = 2,
                        ticker = "BBRI",
                        companyName = "PT Bank Rakyat Indonesia Tbk",
                        lots = 25, // 2,500 shares
                        avgPrice = 5150.0,
                        currentPrice = 5425.0,
                        lastUpdatedMillis = System.currentTimeMillis()
                    ),
                    StockHoldingEntity(
                        id = 3,
                        ticker = "TLKM",
                        companyName = "PT Telkom Indonesia Tbk",
                        lots = 20, // 2,000 shares
                        avgPrice = 3300.0,
                        currentPrice = 3120.0,
                        lastUpdatedMillis = System.currentTimeMillis()
                    ),
                    StockHoldingEntity(
                        id = 4,
                        ticker = "ASII",
                        companyName = "PT Astra International Tbk",
                        lots = 10, // 1,000 shares
                        avgPrice = 4900.0,
                        currentPrice = 5100.0,
                        lastUpdatedMillis = System.currentTimeMillis()
                    )
                )
                stockDao.insertAll(holdings)
            }

            if (transactionDao.countTransactions() == 0) {
                val now = System.currentTimeMillis()
                val dayMillis = 86400000L
                val sampleTransactions = listOf(
                    TransactionEntity(
                        type = "INCOME",
                        amount = 17500000.0,
                        destinationAccountId = 1,
                        category = "Gaji Bulanan",
                        note = "Payroll Gaji Pokok",
                        dateMillis = now - (dayMillis * 3)
                    ),
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 450000.0,
                        sourceAccountId = 1,
                        category = "Belanja Bulanan",
                        note = "Supermarket & Kebutuhan Rumah",
                        dateMillis = now - (dayMillis * 2)
                    ),
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 125000.0,
                        sourceAccountId = 4,
                        category = "Makanan & Minuman",
                        note = "Makan Siang & Kopi",
                        dateMillis = now - (dayMillis * 1)
                    ),
                    TransactionEntity(
                        type = "TRANSFER",
                        amount = 2500000.0,
                        sourceAccountId = 1,
                        destinationAccountId = 3,
                        category = "Top Up RDN",
                        note = "Top Up Dana Investasi Saham",
                        dateMillis = now - 3600000L * 5
                    ),
                    TransactionEntity(
                        type = "TRANSFER",
                        amount = 500000.0,
                        sourceAccountId = 1,
                        destinationAccountId = 4,
                        category = "Top Up E-Wallet",
                        note = "Top Up GoPay mingguan",
                        dateMillis = now - 3600000L * 2
                    )
                )
                transactionDao.insertAll(sampleTransactions)
            }

            val savingsDao = db.savingsGoalDao()
            if (savingsDao.countGoals() == 0) {
                val now = System.currentTimeMillis()
                val dayMillis = 86400000L
                val sampleGoals = listOf(
                    SavingsGoalEntity(
                        title = "Dana Darurat Keluarga",
                        targetAmount = 50000000.0,
                        currentAmount = 25000000.0,
                        deadlineMillis = now + (dayMillis * 365),
                        categoryIcon = "shield",
                        colorHex = 0xFF005696,
                        note = "Tabungan pengaman 6 bulan pengeluaran rutin"
                    ),
                    SavingsGoalEntity(
                        title = "Liburan ke Jepang",
                        targetAmount = 25000000.0,
                        currentAmount = 12500000.0,
                        deadlineMillis = now + (dayMillis * 180),
                        categoryIcon = "flight",
                        colorHex = 0xFF38BDF8,
                        note = "Tiket pesawat & akomodasi Tokyo-Kyoto"
                    ),
                    SavingsGoalEntity(
                        title = "MacBook Pro M3",
                        targetAmount = 28000000.0,
                        currentAmount = 18500000.0,
                        deadlineMillis = now + (dayMillis * 90),
                        categoryIcon = "laptop",
                        colorHex = 0xFF0F766E,
                        note = "Peralatan produktivitas kerja & coding"
                    )
                )
                savingsDao.insertAll(sampleGoals)
            }

            val recurringDao = db.recurringTransactionDao()
            if (recurringDao.countRecurring() == 0) {
                val now = System.currentTimeMillis()
                val dayMillis = 86400000L
                val sampleRecurring = listOf(
                    RecurringTransactionEntity(
                        title = "Gaji Bulanan",
                        type = "INCOME",
                        amount = 12500000.0,
                        sourceAccountId = null,
                        destinationAccountId = 1,
                        category = "Gaji / Pendapatan",
                        frequency = "MONTHLY",
                        nextExecutionMillis = now + (dayMillis * 3),
                        isActive = true,
                        note = "Penerimaan gaji bulanan otomatis"
                    ),
                    RecurringTransactionEntity(
                        title = "Netflix Subscription",
                        type = "EXPENSE",
                        amount = 186000.0,
                        sourceAccountId = 4,
                        destinationAccountId = null,
                        category = "Hiburan & Streaming",
                        frequency = "MONTHLY",
                        nextExecutionMillis = now + (dayMillis * 5),
                        isActive = true,
                        note = "Langganan streaming 4K"
                    ),
                    RecurringTransactionEntity(
                        title = "Spotify Premium",
                        type = "EXPENSE",
                        amount = 54000.0,
                        sourceAccountId = 4,
                        destinationAccountId = null,
                        category = "Hiburan & Streaming",
                        frequency = "MONTHLY",
                        nextExecutionMillis = now + (dayMillis * 12),
                        isActive = true,
                        note = "Langganan musik bulanan"
                    )
                )
                recurringDao.insertAll(sampleRecurring)
            }
        }
    }
}
