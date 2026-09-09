package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.entity.TransactionEntity
import java.util.Calendar
import java.util.Locale

data class MonthlyTrendPoint(
    val monthKey: String,
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

data class CategoryDistributionItem(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

object FinancialChartAnalytics {
    fun calculateMonthlyTrends(transactions: List<TransactionEntity>, monthsCount: Int = 6): List<MonthlyTrendPoint> {
        val cal = Calendar.getInstance()
        val result = LinkedHashMap<String, MonthlyTrendPoint>()
        
        for (i in (monthsCount - 1) downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val key = String.format(Locale.US, "%d-%02d", year, month + 1)
            val label = when (month) {
                0 -> "Jan"
                1 -> "Feb"
                2 -> "Mar"
                3 -> "Apr"
                4 -> "Mei"
                5 -> "Jun"
                6 -> "Jul"
                7 -> "Agu"
                8 -> "Sep"
                9 -> "Okt"
                10 -> "Nov"
                else -> "Des"
            }
            result[key] = MonthlyTrendPoint(key, label, 0.0, 0.0)
        }

        for (txn in transactions) {
            cal.timeInMillis = txn.dateMillis
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val key = String.format(Locale.US, "%d-%02d", year, month + 1)
            
            val existing = result[key]
            if (existing != null) {
                if (txn.type.uppercase() == "INCOME") {
                    result[key] = existing.copy(income = existing.income + txn.amount)
                } else if (txn.type.uppercase() == "EXPENSE") {
                    result[key] = existing.copy(expense = existing.expense + txn.amount)
                }
            }
        }
        return result.values.toList()
    }

    fun calculateCategoryDistribution(transactions: List<TransactionEntity>, type: String = "INCOME"): List<CategoryDistributionItem> {
        val filtered = transactions.filter { it.type.uppercase() == type.uppercase() }
        val map = mutableMapOf<String, Double>()
        var total = 0.0
        for (txn in filtered) {
            val cat = txn.category.ifBlank { "Lainnya" }
            val current = map[cat] ?: 0.0
            map[cat] = current + txn.amount
            total += txn.amount
        }

        val colors = listOf(
            Color(0xFF38BDF8),
            Color(0xFF34D399),
            Color(0xFFF43F5E),
            Color(0xFFFBBF24),
            Color(0xFF818CF8),
            Color(0xFFEC4899),
            Color(0xFF14B8A6),
            Color(0xFF6366F1)
        )

        var colorIdx = 0
        return map.entries.sortedByDescending { it.value }.map { (cat, amt) ->
            val pct = if (total > 0.0) (amt / total).toFloat() else 0f
            val c = colors[colorIdx % colors.size]
            colorIdx++
            CategoryDistributionItem(cat, amt, pct, c)
        }
    }
}
