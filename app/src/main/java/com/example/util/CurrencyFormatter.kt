package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        currencySymbol = "Rp "
        groupingSeparator = '.'
        monetaryDecimalSeparator = ','
    }

    private val idrFormat = DecimalFormat("Rp #,##0", symbols)
    private val numberOnlyFormat = DecimalFormat("#,##0", symbols)

    fun formatIdr(amount: Double, isVisible: Boolean = true, maskText: String = "Rp ••••••••"): String {
        if (!isVisible) return maskText
        val isNegative = amount < 0
        val absVal = kotlin.math.abs(amount)
        val formatted = idrFormat.format(absVal)
        return if (isNegative) "-$formatted" else formatted
    }

    fun formatNumber(amount: Double, isVisible: Boolean = true, maskText: String = "••••••••"): String {
        if (!isVisible) return maskText
        return numberOnlyFormat.format(amount)
    }

    fun formatPercentage(percentage: Double): String {
        val sign = if (percentage >= 0) "+" else ""
        return String.format(Locale.US, "%s%.2f%%", sign, percentage)
    }

    fun formatCompactAxis(amount: Double): String {
        val abs = kotlin.math.abs(amount)
        val isNeg = amount < 0
        val prefix = if (isNeg) "-" else ""
        return when {
            abs >= 1_000_000_000 -> String.format(Locale.US, "%sRp %.1fM", prefix, abs / 1_000_000_000.0)
            abs >= 1_000_000 -> String.format(Locale.US, "%sRp %.1fJt", prefix, abs / 1_000_000.0)
            abs >= 1_000 -> String.format(Locale.US, "%sRp %.0fRb", prefix, abs / 1_000.0)
            else -> String.format(Locale.US, "%sRp %.0f", prefix, abs)
        }
    }

    fun parseAmount(input: String): Double {
        val clean = input.replace("[^0-9]".toRegex(), "")
        return clean.toDoubleOrNull() ?: 0.0
    }
}
