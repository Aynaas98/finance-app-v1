package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.util.CurrencyFormatter

@Composable
fun SensorNominalText(
    amount: Double,
    isBalanceVisible: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null,
    prefix: String = "",
    showSign: Boolean = false,
    maskText: String = "••••••••"
) {
    AnimatedContent(
        targetState = isBalanceVisible,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "SensorNominalTextAnim"
    ) { visible ->
        val textValue = if (visible) {
            val signStr = if (showSign && amount > 0) "+" else ""
            val formatted = CurrencyFormatter.formatIdr(amount, isVisible = true)
            "$prefix$signStr$formatted"
        } else {
            "$prefix$maskText"
        }

        Text(
            text = textValue,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight
        )
    }
}
