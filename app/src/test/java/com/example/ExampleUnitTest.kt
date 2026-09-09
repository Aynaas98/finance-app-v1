package com.example

import com.example.data.entity.StockHoldingEntity
import com.example.domain.model.StockPosition
import com.example.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCurrencyFormatting() {
        val formattedVisible = CurrencyFormatter.formatIdr(15000000.0, isVisible = true)
        assertEquals("Rp 15.000.000", formattedVisible)

        val formattedHidden = CurrencyFormatter.formatIdr(15000000.0, isVisible = false)
        assertEquals("Rp ••••••••", formattedHidden)
    }

    @Test
    fun testStockFormulas() {
        // Lot: 15, Avg Price: 9800, Current Price: 10450
        val entity = StockHoldingEntity(
            id = 1,
            ticker = "BBCA",
            companyName = "Bank Central Asia",
            lots = 15,
            avgPrice = 9800.0,
            currentPrice = 10450.0
        )
        val position = StockPosition(entity)

        // Rumus 1: Total Shares = Lot * 100
        assertEquals(1500L, position.totalShares)

        // Rumus 2: Modal = Total Shares * Avg Price
        assertEquals(14700000.0, position.modal, 0.001)

        // Rumus 3: Market Value = Total Shares * Current Price
        assertEquals(15675000.0, position.marketValue, 0.001)

        // Rumus 4: Unrealized P/L = Market Value - Modal
        val expectedPL = 15675000.0 - 14700000.0
        assertEquals(expectedPL, position.unrealizedPL, 0.001)
        assertEquals(975000.0, position.unrealizedPL, 0.001)

        // Percentage P/L
        val expectedPercent = (975000.0 / 14700000.0) * 100.0
        assertEquals(expectedPercent, position.unrealizedPLPercentage, 0.01)
        assertTrue(position.isProfitable)
    }

    @Test
    fun testAverageDownCalculator() {
        // Initial: 10 lot (1000 shares) @ 10,000 = Modal Rp 10,000,000
        // New Buy: 5 lot (500 shares) @ 8,000 = Gross Rp 4,000,000, fee 0.15% = Rp 6,000 -> Total Rp 4,006,000
        // Total Modal = Rp 14,006,000, Total shares = 1500 (15 lot)
        // New Avg Price = 14,006,000 / 1500 = 9337.33
        val existingLots = 10
        val existingAvg = 10000.0
        val newLots = 5
        val newPrice = 8000.0
        val brokerFeePercent = 0.15

        val existingShares = existingLots.toLong() * 100L
        val existingModal = existingShares * existingAvg
        val newShares = newLots.toLong() * 100L
        val newGrossModal = newShares * newPrice
        val fee = newGrossModal * (brokerFeePercent / 100.0)
        val newTotalCost = newGrossModal + fee

        val finalShares = existingShares + newShares
        val finalTotalModal = existingModal + newTotalCost
        val newAvg = finalTotalModal / finalShares

        assertEquals(1500L, finalShares)
        assertEquals(15, (finalShares / 100).toInt())
        assertEquals(14006000.0, finalTotalModal, 0.01)
        assertEquals(9337.33, newAvg, 0.01)
        assertTrue(newAvg < existingAvg)
    }

    @Test
    fun testDividendYieldCalculator() {
        // 10 lot (1000 shares), price Rp 10,000, DPS Rp 500
        // Total dividend = 1000 * 500 = Rp 500,000
        // Yield = (500 / 10,000) * 100 = 5.0%
        val shares = 1000L
        val price = 10000.0
        val dps = 500.0

        val totalDividend = shares * dps
        val yieldPercent = (dps / price) * 100.0

        assertEquals(500000.0, totalDividend, 0.001)
        assertEquals(5.0, yieldPercent, 0.001)
    }

    @Test
    fun testPortfolioHistoryTrendCalculatorWithData() {
        val entity1 = StockHoldingEntity(
            id = 1,
            ticker = "BBCA",
            companyName = "Bank Central Asia",
            lots = 10,
            avgPrice = 9500.0,
            currentPrice = 10000.0
        )
        val entity2 = StockHoldingEntity(
            id = 2,
            ticker = "BBRI",
            companyName = "Bank Rakyat Indonesia",
            lots = 20,
            avgPrice = 4500.0,
            currentPrice = 4800.0
        )
        val positions = listOf(StockPosition(entity1), StockPosition(entity2))
        val expectedTotalMarketValue = positions.sumOf { it.marketValue }

        val points = com.example.util.PortfolioHistoryTrendCalculator.calculate30DayTrend(positions)

        // Must produce 30 points
        assertEquals(30, points.size)

        // Day 0 must have index 0, Day 29 must have index 29
        assertEquals(0, points.first().dayIndex)
        assertEquals(29, points.last().dayIndex)

        // The 30th day (index 29) must match the exact current market value
        assertEquals(expectedTotalMarketValue, points.last().portfolioValue, 0.001)

        // Verify compact axis formatting
        assertEquals("Rp 1,5 M", com.example.util.PortfolioHistoryTrendCalculator.formatCompactAxis(1_500_000_000.0))
        assertEquals("Rp 15,0 jt", com.example.util.PortfolioHistoryTrendCalculator.formatCompactAxis(15_000_000.0))
        assertEquals("Rp 500 rb", com.example.util.PortfolioHistoryTrendCalculator.formatCompactAxis(500_000.0))
    }

    @Test
    fun testPortfolioHistoryTrendCalculatorEmpty() {
        val points = com.example.util.PortfolioHistoryTrendCalculator.calculate30DayTrend(emptyList())
        assertTrue(points.isEmpty())
    }
}
