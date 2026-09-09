package com.example.domain.model

/**
 * Data point representing the portfolio valuation and performance on a specific day
 * within the 30-day historical timeline.
 */
data class PortfolioHistoryPoint(
    val dayIndex: Int, // 0 to 29 (29 is today)
    val dateMillis: Long,
    val dateLabel: String, // Short date, e.g., "10 Agu"
    val fullDateLabel: String, // Full date, e.g., "10 Agustus 2026"
    val portfolioValue: Double,
    val modalValue: Double,
    val unrealizedPL: Double,
    val plPercentage: Double
) {
    val isProfitable: Boolean get() = unrealizedPL >= 0.0
}
