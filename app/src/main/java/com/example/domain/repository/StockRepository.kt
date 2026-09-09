package com.example.domain.repository

import com.example.data.api.YahooChartResponse
import com.example.data.api.YahooFinanceApi
import com.example.data.dao.StockHoldingDao
import com.example.data.entity.StockHoldingEntity
import com.example.domain.model.StockPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Result state for stock refresh operation.
 */
data class StockRefreshSummary(
    val updatedCount: Int,
    val totalCount: Int,
    val isOffline: Boolean,
    val message: String
)

/**
 * Result of Average Down/Up calculation including broker fee.
 */
data class AverageDownCalculation(
    val existingShares: Long,
    val existingModal: Double,
    val newShares: Long,
    val newGrossModal: Double,
    val brokerFeeAmount: Double,
    val newTotalCost: Double,
    val finalShares: Long,
    val finalLots: Int,
    val finalTotalModal: Double,
    val newAveragePrice: Double,
    val priceDiff: Double,
    val priceDiffPercent: Double
)

/**
 * Result of Dividend Yield calculation.
 */
data class DividendCalculation(
    val dividendPerShare: Double,
    val currentPrice: Double,
    val totalShares: Long,
    val totalDividend: Double,
    val dividendYieldPercentage: Double
)

/**
 * Repository handling stock holdings, Yahoo Finance real-time price updates,
 * offline fallback caching via Room Database, and stock math helpers.
 */
class StockRepository(
    private val stockHoldingDao: StockHoldingDao,
    private val yahooFinanceApi: YahooFinanceApi = YahooFinanceApi.create()
) {

    val allStockHoldings: Flow<List<StockHoldingEntity>> = stockHoldingDao.getAllStockHoldings()

    val stockPositions: Flow<List<StockPosition>> = allStockHoldings.map { list ->
        list.map { StockPosition(it) }
    }

    suspend fun getStockById(id: Long): StockHoldingEntity? = withContext(Dispatchers.IO) {
        stockHoldingDao.getStockHoldingById(id)
    }

    suspend fun addStockHolding(
        ticker: String,
        companyName: String,
        lots: Int,
        avgPrice: Double,
        currentPrice: Double
    ): Long = withContext(Dispatchers.IO) {
        val formattedTicker = ticker.uppercase().trim()
        val holding = StockHoldingEntity(
            ticker = formattedTicker,
            companyName = companyName.trim(),
            lots = lots,
            avgPrice = avgPrice,
            currentPrice = currentPrice,
            lastUpdatedMillis = System.currentTimeMillis()
        )
        stockHoldingDao.insertStockHolding(holding)
    }

    suspend fun updateStockHolding(holding: StockHoldingEntity) = withContext(Dispatchers.IO) {
        stockHoldingDao.updateStockHolding(holding.copy(lastUpdatedMillis = System.currentTimeMillis()))
    }

    suspend fun updateStockCurrentPrice(id: Long, newPrice: Double) = withContext(Dispatchers.IO) {
        val existing = stockHoldingDao.getStockHoldingById(id) ?: return@withContext
        stockHoldingDao.updateStockHolding(
            existing.copy(
                currentPrice = newPrice,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteStockHolding(holding: StockHoldingEntity) = withContext(Dispatchers.IO) {
        stockHoldingDao.deleteStockHolding(holding)
    }

    /**
     * Fetch latest price from Yahoo Finance for a given ticker (e.g., "BBCA" or "BBCA.JK").
     * Safely returns Result with price, or failure if network/API error.
     */
    suspend fun fetchLatestPriceOnline(ticker: String): Result<Double> = withContext(Dispatchers.IO) {
        val formattedTicker = YahooFinanceApi.formatTickerForIdx(ticker)
        try {
            // First attempt: Chart endpoint (highly reliable with metadata)
            val chartResponse: YahooChartResponse = yahooFinanceApi.getChart(formattedTicker)
            val price = chartResponse.chart?.result?.firstOrNull()?.meta?.effectivePrice
            if (price != null && price > 0.0) {
                return@withContext Result.success(price)
            }

            // Fallback attempt: Quote endpoint
            val quoteResponse = yahooFinanceApi.getQuote(formattedTicker)
            val quotePrice = quoteResponse.quoteResponse?.result?.firstOrNull()?.effectivePrice
            if (quotePrice != null && quotePrice > 0.0) {
                return@withContext Result.success(quotePrice)
            }

            Result.failure(Exception("Harga untuk ticker $formattedTicker tidak ditemukan."))
        } catch (e: UnknownHostException) {
            Result.failure(IOException("Koneksi offline: Tidak dapat menjangkau Yahoo Finance.", e))
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("Koneksi timeout: Waktu permintaan habis.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh a single stock holding price from Yahoo Finance.
     * Updates Room database on success.
     * On network failure, preserves Room DB data without crashing.
     */
    suspend fun refreshStockPrice(id: Long): StockRefreshSummary = withContext(Dispatchers.IO) {
        val stock = stockHoldingDao.getStockHoldingById(id)
            ?: return@withContext StockRefreshSummary(0, 0, false, "Saham tidak ditemukan.")

        val result = fetchLatestPriceOnline(stock.ticker)
        if (result.isSuccess) {
            val price = result.getOrThrow()
            stockHoldingDao.updateStockHolding(
                stock.copy(
                    currentPrice = price,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )
            StockRefreshSummary(
                updatedCount = 1,
                totalCount = 1,
                isOffline = false,
                message = "Berhasil memperbarui harga ${stock.ticker}: Rp${price.toLong()}"
            )
        } else {
            val isOffline = result.exceptionOrNull() is IOException
            StockRefreshSummary(
                updatedCount = 0,
                totalCount = 1,
                isOffline = isOffline,
                message = if (isOffline) {
                    "Mode offline: Menggunakan harga tersimpan untuk ${stock.ticker}."
                } else {
                    "Gagal memperbarui harga: ${result.exceptionOrNull()?.localizedMessage}"
                }
            )
        }
    }

    /**
     * Refresh all stock holding prices in Room Database from Yahoo Finance.
     * Offline-first: if internet is unavailable, retains cached prices in Room without crash.
     */
    suspend fun refreshAllStockPrices(holdings: List<StockHoldingEntity>): StockRefreshSummary = withContext(Dispatchers.IO) {
        if (holdings.isEmpty()) {
            return@withContext StockRefreshSummary(0, 0, false, "Portofolio saham masih kosong.")
        }

        var updatedCount = 0
        var offlineOccurred = false
        var lastError: String? = null

        // Try batch quote endpoint first for efficiency
        val symbols = holdings.joinToString(",") { YahooFinanceApi.formatTickerForIdx(it.ticker) }
        var batchSucceeded = false

        try {
            val batchResponse = yahooFinanceApi.getQuote(symbols)
            val quoteList = batchResponse.quoteResponse?.result
            if (!quoteList.isNullOrEmpty()) {
                val priceMap = mutableMapOf<String, Double>()
                for (item in quoteList) {
                    val sym = item.symbol?.uppercase() ?: continue
                    val p = item.effectivePrice ?: continue
                    priceMap[sym] = p
                    // Also map raw symbol without .JK
                    if (sym.endsWith(".JK")) {
                        priceMap[sym.removeSuffix(".JK")] = p
                    }
                }

                for (holding in holdings) {
                    val cleanTicker = holding.ticker.uppercase()
                    val idxTicker = YahooFinanceApi.formatTickerForIdx(holding.ticker)
                    val newPrice = priceMap[idxTicker] ?: priceMap[cleanTicker]
                    if (newPrice != null && newPrice > 0.0) {
                        stockHoldingDao.updateStockHolding(
                            holding.copy(
                                currentPrice = newPrice,
                                lastUpdatedMillis = System.currentTimeMillis()
                            )
                        )
                        updatedCount++
                    }
                }
                batchSucceeded = updatedCount > 0
            }
        } catch (e: UnknownHostException) {
            offlineOccurred = true
            lastError = "Koneksi offline: Tidak ada jaringan internet."
        } catch (e: SocketTimeoutException) {
            offlineOccurred = true
            lastError = "Koneksi timeout."
        } catch (e: Exception) {
            lastError = e.localizedMessage
        }

        // If batch quote was incomplete or failed due to non-network reasons, fallback per-item
        if (!batchSucceeded && !offlineOccurred) {
            for (holding in holdings) {
                try {
                    val singleResult = fetchLatestPriceOnline(holding.ticker)
                    if (singleResult.isSuccess) {
                        val price = singleResult.getOrThrow()
                        stockHoldingDao.updateStockHolding(
                            holding.copy(
                                currentPrice = price,
                                lastUpdatedMillis = System.currentTimeMillis()
                            )
                        )
                        updatedCount++
                    } else {
                        if (singleResult.exceptionOrNull() is IOException) {
                            offlineOccurred = true
                        }
                    }
                } catch (e: Exception) {
                    if (e is IOException) offlineOccurred = true
                }
            }
        }

        val message = when {
            updatedCount == holdings.size -> "Semua harga saham ($updatedCount/${holdings.size}) berhasil diperbarui dari Yahoo Finance."
            updatedCount > 0 -> "$updatedCount dari ${holdings.size} harga saham berhasil diperbarui."
            offlineOccurred -> "Mode offline: Menggunakan data harga terakhir yang tersimpan di Room."
            else -> lastError ?: "Gagal memperbarui harga saham. Menggunakan data lokal."
        }

        StockRefreshSummary(
            updatedCount = updatedCount,
            totalCount = holdings.size,
            isOffline = offlineOccurred,
            message = message
        )
    }

    /**
     * Kalkulator Average Down / Up & Biaya Transaksi Broker (V1.2).
     * @param currentLots Jumlah lot saat ini
     * @param currentAvg Harga rata-rata saat ini
     * @param newLots Jumlah lot baru yang akan dibeli
     * @param newPrice Harga pembelian baru per lembar
     * @param brokerFeePercent Persentase fee beli broker (default 0.15% untuk IDX)
     */
    fun calculateAverageDown(
        currentLots: Int,
        currentAvg: Double,
        newLots: Int,
        newPrice: Double,
        brokerFeePercent: Double = 0.15
    ): AverageDownCalculation {
        val existingShares = currentLots * 100L
        val existingModal = existingShares * currentAvg

        val newShares = newLots * 100L
        val newGrossModal = newShares * newPrice
        val feeAmount = newGrossModal * (brokerFeePercent / 100.0)
        val newTotalCost = newGrossModal + feeAmount

        val finalShares = existingShares + newShares
        val finalLots = currentLots + newLots
        val finalTotalModal = existingModal + newTotalCost
        val newAvgPrice = if (finalShares > 0) finalTotalModal / finalShares else 0.0

        val priceDiff = newAvgPrice - currentAvg
        val priceDiffPercent = if (currentAvg > 0) (priceDiff / currentAvg) * 100.0 else 0.0

        return AverageDownCalculation(
            existingShares = existingShares,
            existingModal = existingModal,
            newShares = newShares,
            newGrossModal = newGrossModal,
            brokerFeeAmount = feeAmount,
            newTotalCost = newTotalCost,
            finalShares = finalShares,
            finalLots = finalLots,
            finalTotalModal = finalTotalModal,
            newAveragePrice = newAvgPrice,
            priceDiff = priceDiff,
            priceDiffPercent = priceDiffPercent
        )
    }

    /**
     * Kalkulator Dividen & Dividend Yield (V1.2).
     */
    fun calculateDividend(
        totalShares: Long,
        currentPrice: Double,
        dividendPerShare: Double
    ): DividendCalculation {
        val totalDividend = totalShares * dividendPerShare
        val yieldPercent = if (currentPrice > 0) (dividendPerShare / currentPrice) * 100.0 else 0.0
        return DividendCalculation(
            dividendPerShare = dividendPerShare,
            currentPrice = currentPrice,
            totalShares = totalShares,
            totalDividend = totalDividend,
            dividendYieldPercentage = yieldPercent
        )
    }
}
