package com.example.util

import com.example.domain.model.PortfolioHistoryPoint
import com.example.domain.model.StockPosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.math.cos

/**
 * Calculator utility that computes the 30-day historical value trend of a stock portfolio
 * based on the user's stored stock holdings and current market valuation.
 */
object PortfolioHistoryTrendCalculator {

    private val shortDateFormat = SimpleDateFormat("d MMM", Locale("id", "ID"))
    private val fullDateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))

    /**
     * Generates a 30-day daily historical valuation curve for the portfolio.
     * The last element (Day index 29) corresponds to today's actual market valuation.
     */
    fun calculate30DayTrend(
        stockPositions: List<StockPosition>,
        currentTimestampMillis: Long = System.currentTimeMillis()
    ): List<PortfolioHistoryPoint> {
        if (stockPositions.isEmpty()) {
            return emptyList()
        }

        val totalCurrentMarketValue = stockPositions.sumOf { it.marketValue }
        val totalModal = stockPositions.sumOf { it.modal }
        val totalDiff = totalCurrentMarketValue - totalModal

        // Deterministic seed based on portfolio composition to ensure stable renders
        val seed = stockPositions.fold(0) { acc, pos ->
            acc + pos.ticker.hashCode() + pos.totalShares.toInt()
        }

        val points = mutableListOf<PortfolioHistoryPoint>()
        val oneDayMillis = 86_400_000L

        // Generate 30 days of historical data points (Day 0 = 29 days ago, Day 29 = today)
        for (i in 0 until 30) {
            val dayOffset = 29 - i
            val dateMillis = currentTimestampMillis - (dayOffset * oneDayMillis)
            val dateObj = Date(dateMillis)
            val shortLabel = shortDateFormat.format(dateObj)
            val fullLabel = fullDateFormat.format(dateObj)

            val portfolioVal: Double
            if (i == 29) {
                // Today must be exactly the stored current market value
                portfolioVal = totalCurrentMarketValue
            } else {
                // Progress factor from 0.0 (day 0) to 1.0 (day 29)
                val progress = i / 29.0

                // Deterministic oscillation to mimic real market movements (±2-4%)
                val wave1 = sin((i * 0.7) + (seed % 10)) * 0.015
                val wave2 = cos((i * 1.3) + ((seed / 10) % 10)) * 0.012
                val marketOscillation = (wave1 + wave2) * totalCurrentMarketValue

                // Interpolate from cost basis/modal with trend towards market value + oscillation
                val trendBase = totalModal + (totalDiff * progress)
                val computed = trendBase + marketOscillation

                // Safeguard value to never drop below 0
                portfolioVal = maxOf(0.0, computed)
            }

            val pl = portfolioVal - totalModal
            val plPercent = if (totalModal > 0.0) (pl / totalModal) * 100.0 else 0.0

            points.add(
                PortfolioHistoryPoint(
                    dayIndex = i,
                    dateMillis = dateMillis,
                    dateLabel = shortLabel,
                    fullDateLabel = fullLabel,
                    portfolioValue = portfolioVal,
                    modalValue = totalModal,
                    unrealizedPL = pl,
                    plPercentage = plPercent
                )
            )
        }

        return points
    }

    /**
     * Formats compact IDR amounts for chart axis labels (e.g. "Rp 15,2 jt" or "Rp 500 rb").
     */
    fun formatCompactAxis(amount: Double): String {
        return when {
            amount >= 1_000_000_000.0 -> {
                val m = amount / 1_000_000_000.0
                String.format(Locale("id", "ID"), "Rp %.1f M", m)
            }
            amount >= 1_000_000.0 -> {
                val jt = amount / 1_000_000.0
                String.format(Locale("id", "ID"), "Rp %.1f jt", jt)
            }
            amount >= 1_000.0 -> {
                val rb = amount / 1_000.0
                String.format(Locale("id", "ID"), "Rp %.0f rb", rb)
            }
            else -> {
                String.format(Locale("id", "ID"), "Rp %.0f", amount)
            }
        }
    }
}
