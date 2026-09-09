package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.NavyCardBackground
import com.example.ui.theme.NavySlate700
import com.example.ui.theme.NavySlate800
import com.example.ui.theme.NavySlate900
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.MonthlyTrendPoint

@Composable
fun MonthlyTrendChartCard(
    trends: List<MonthlyTrendPoint>,
    isBalanceVisible: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val maxAmount = remember(trends) {
        val peak = trends.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0
        if (peak <= 0.0) 1.0 else peak * 1.15
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_monthly_trend"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = NeonCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tren Arus Kas Bulanan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Perbandingan Pemasukan vs Pengeluaran (D3/Recharts)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = ProfitGreen, modifier = Modifier.size(8.dp)) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Masuk", fontSize = 10.sp, color = TextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = ExpenseRed, modifier = Modifier.size(8.dp)) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Keluar", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tooltip or Active Summary Display
            val activePoint = selectedIndex?.let { trends.getOrNull(it) } ?: trends.lastOrNull()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NavySlate900
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = activePoint?.let { "Bulan: ${it.monthLabel}" } ?: "Ringkasan Bulanan",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row {
                                Text("Masuk: ", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                SensorNominalText(
                                    amount = activePoint?.income ?: 0.0,
                                    isBalanceVisible = isBalanceVisible,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                            Row {
                                Text("Keluar: ", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                SensorNominalText(
                                    amount = activePoint?.expense ?: 0.0,
                                    isBalanceVisible = isBalanceVisible,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(trends) {
                        detectTapGestures { offset ->
                            if (trends.isNotEmpty()) {
                                val colWidth = size.width / trends.size
                                val idx = (offset.x / colWidth).toInt().coerceIn(0, trends.size - 1)
                                selectedIndex = idx
                            }
                        }
                    }
                    .pointerInput(trends) {
                        detectDragGestures { change, _ ->
                            if (trends.isNotEmpty()) {
                                val colWidth = size.width / trends.size
                                val idx = (change.position.x / colWidth).toInt().coerceIn(0, trends.size - 1)
                                selectedIndex = idx
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val stepX = if (trends.size > 1) w / (trends.size - 1) else w

                    // 1. Draw horizontal reference grid lines (D3 style)
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val y = h * i / gridSteps
                        drawLine(
                            color = NavySlate700.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    if (trends.isEmpty()) return@Canvas

                    // Calculate point coordinates
                    val incomePoints = mutableListOf<Offset>()
                    val expensePoints = mutableListOf<Offset>()

                    trends.forEachIndexed { index, pt ->
                        val x = if (trends.size > 1) index * stepX else w / 2f
                        val yIncome = h - ((pt.income / maxAmount) * (h - 20f)).toFloat() - 10f
                        val yExpense = h - ((pt.expense / maxAmount) * (h - 20f)).toFloat() - 10f
                        incomePoints.add(Offset(x, yIncome))
                        expensePoints.add(Offset(x, yExpense))
                    }

                    // Draw Income Area & Smooth Curve
                    val incomePath = Path()
                    val incomeAreaPath = Path()
                    if (incomePoints.isNotEmpty()) {
                        incomePath.moveTo(incomePoints[0].x, incomePoints[0].y)
                        incomeAreaPath.moveTo(incomePoints[0].x, h)
                        incomeAreaPath.lineTo(incomePoints[0].x, incomePoints[0].y)

                        for (i in 0 until incomePoints.size - 1) {
                            val p0 = incomePoints[i]
                            val p1 = incomePoints[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            incomePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            incomeAreaPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                        incomeAreaPath.lineTo(incomePoints.last().x, h)
                        incomeAreaPath.close()

                        drawPath(
                            path = incomeAreaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(ProfitGreen.copy(alpha = 0.3f), Color.Transparent),
                                startY = 0f,
                                endY = h
                            )
                        )
                        drawPath(
                            path = incomePath,
                            color = ProfitGreen,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Draw Expense Area & Smooth Curve
                    val expensePath = Path()
                    val expenseAreaPath = Path()
                    if (expensePoints.isNotEmpty()) {
                        expensePath.moveTo(expensePoints[0].x, expensePoints[0].y)
                        expenseAreaPath.moveTo(expensePoints[0].x, h)
                        expenseAreaPath.lineTo(expensePoints[0].x, expensePoints[0].y)

                        for (i in 0 until expensePoints.size - 1) {
                            val p0 = expensePoints[i]
                            val p1 = expensePoints[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            expensePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            expenseAreaPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                        expenseAreaPath.lineTo(expensePoints.last().x, h)
                        expenseAreaPath.close()

                        drawPath(
                            path = expenseAreaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(ExpenseRed.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = h
                            )
                        )
                        drawPath(
                            path = expensePath,
                            color = ExpenseRed,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Draw Data Dots & Active scrubber line
                    selectedIndex?.let { idx ->
                        if (idx in trends.indices) {
                            val x = if (trends.size > 1) idx * stepX else w / 2f
                            // Vertical crosshair guide
                            drawLine(
                                color = NeonCyan.copy(alpha = 0.7f),
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                            // Draw glowing dots
                            val incPt = incomePoints[idx]
                            val expPt = expensePoints[idx]
                            drawCircle(color = ProfitGreen, radius = 6f, center = incPt)
                            drawCircle(color = Color.White, radius = 3f, center = incPt)
                            drawCircle(color = ExpenseRed, radius = 6f, center = expPt)
                            drawCircle(color = Color.White, radius = 3f, center = expPt)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                trends.forEachIndexed { index, pt ->
                    val isSelected = selectedIndex == index
                    Text(
                        text = pt.monthLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) NeonCyan else TextMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
