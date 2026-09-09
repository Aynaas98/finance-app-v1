package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.domain.model.PortfolioHistoryPoint
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.NavyCardBackground
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySlate700
import com.example.ui.theme.NavySlate800
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.PortfolioHistoryTrendCalculator

/**
 * Interactive Line Chart component inspired by Recharts / D3.js standards:
 * - Cubic Bézier smooth curve (Monotone spline interpolation)
 * - Area gradient glow fill below the trendline
 * - Reference guide grid lines & dynamic Y/X axis scale
 * - Interactive touch scrubbing with crosshair and floating tooltip
 * - Full privacy sensor nominal (isBalanceVisible) compliance
 */
@Composable
fun PortfolioHistoryLineChart(
    points: List<PortfolioHistoryPoint>,
    isBalanceVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("chart_empty_state"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Grafik Tren 30 Hari Belum Tersedia",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tambahkan emiten saham ke portofolio untuk melihat tren historis valuasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        return
    }

    // Min and Max values for chart scaling
    val values = points.map { it.portfolioValue }
    val rawMin = values.minOrNull() ?: 0.0
    val rawMax = values.maxOrNull() ?: 1.0

    // Add padding margin to chart Y bounds (5% top and bottom) to prevent clipping
    val margin = if (rawMax > rawMin) (rawMax - rawMin) * 0.08 else rawMax * 0.08
    val minY = maxOf(0.0, rawMin - margin)
    val maxY = rawMax + margin
    val rangeY = if (maxY > minY) maxY - minY else 1.0

    // 30-day change comparison (Day 0 vs Day 29)
    val firstPoint = points.first()
    val lastPoint = points.last()
    val change30D = lastPoint.portfolioValue - firstPoint.portfolioValue
    val change30DPercent = if (firstPoint.portfolioValue > 0.0) {
        (change30D / firstPoint.portfolioValue) * 100.0
    } else 0.0
    val isOverallPositive = change30D >= 0.0

    // Interactive scrubber selection (default is the latest point - today)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val activePoint = selectedIndex?.let { points.getOrNull(it) } ?: lastPoint

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("portfolio_history_line_chart"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Chart Title & Recharts/D3 Badge Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tren Valuasi 30 Hari",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "D3/Recharts Monotone Spline",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // 30-Day Growth Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOverallPositive) ProfitGreenBg else ExpenseRedBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isOverallPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isOverallPositive) ProfitGreen else ExpenseRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isOverallPositive) "+" else ""}${CurrencyFormatter.formatPercentage(change30DPercent)} (30H)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverallPositive) ProfitGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Scrubber Tooltip Card (Recharts <Tooltip /> styled)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NavyCardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_active_tooltip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (activePoint.dayIndex == 29) "Hari Ini (${activePoint.dateLabel})" else activePoint.fullDateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        SensorNominalText(
                            amount = activePoint.portfolioValue,
                            isBalanceVisible = isBalanceVisible,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Unrealized P/L",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SensorNominalText(
                                amount = activePoint.unrealizedPL,
                                isBalanceVisible = isBalanceVisible,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activePoint.isProfitable) ProfitGreen else ExpenseRed,
                                showSign = true
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${if (activePoint.isProfitable) "+" else ""}${CurrencyFormatter.formatPercentage(activePoint.plPercentage)})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activePoint.isProfitable) ProfitGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Canvas Line Chart (Smooth Bézier, Gradient Fill, Grid Guides & Interactive Scrubber)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chart_canvas_surface")
                        .pointerInput(points) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val pointSpacing = size.width / (points.size - 1).coerceAtLeast(1)
                                    val index = (offset.x / pointSpacing)
                                        .toInt()
                                        .coerceIn(0, points.size - 1)
                                    selectedIndex = index
                                }
                            )
                        }
                        .pointerInput(points) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val pointSpacing = size.width / (points.size - 1).coerceAtLeast(1)
                                    val index = (offset.x / pointSpacing)
                                        .toInt()
                                        .coerceIn(0, points.size - 1)
                                    selectedIndex = index
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pointSpacing = size.width / (points.size - 1).coerceAtLeast(1)
                                    val index = (change.position.x / pointSpacing)
                                        .toInt()
                                        .coerceIn(0, points.size - 1)
                                    selectedIndex = index
                                },
                                onDragEnd = {
                                    // Keep selected until tapped elsewhere or reset
                                }
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val n = points.size

                    val topPadding = 12f
                    val bottomPadding = 20f
                    val availableHeight = height - topPadding - bottomPadding

                    // Horizontal Grid Lines (4 reference levels like D3/Recharts)
                    val gridLineCount = 3
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                    for (i in 0..gridLineCount) {
                        val yRatio = i / gridLineCount.toFloat()
                        val gridY = topPadding + (yRatio * availableHeight)

                        drawLine(
                            color = NavySlate800,
                            start = Offset(0f, gridY),
                            end = Offset(width, gridY),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }

                    // Compute Coordinates
                    val stepX = width / (n - 1).coerceAtLeast(1)
                    val coordinates = points.mapIndexed { index, pt ->
                        val x = index * stepX
                        val normalizedY = ((pt.portfolioValue - minY) / rangeY).toFloat().coerceIn(0f, 1f)
                        // Canvas Y is inverted (0 is top)
                        val y = topPadding + availableHeight * (1f - normalizedY)
                        Offset(x, y)
                    }

                    // Construct Smooth Cubic Bézier Spline Path (Monotone Spline)
                    val linePath = Path().apply {
                        if (coordinates.isNotEmpty()) {
                            moveTo(coordinates.first().x, coordinates.first().y)
                            for (i in 0 until coordinates.size - 1) {
                                val p0 = coordinates[i]
                                val p1 = coordinates[i + 1]
                                // Cubic control points for smooth monotone curve
                                val controlX = (p0.x + p1.x) / 2f
                                cubicTo(
                                    controlX, p0.y,
                                    controlX, p1.y,
                                    p1.x, p1.y
                                )
                            }
                        }
                    }

                    // Construct Area Fill Path
                    val fillPath = Path().apply {
                        addPath(linePath)
                        if (coordinates.isNotEmpty()) {
                            lineTo(coordinates.last().x, height)
                            lineTo(coordinates.first().x, height)
                            close()
                        }
                    }

                    // 1. Draw Gradient Area (Recharts <Area fill="url(#...)" />)
                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.28f),
                            NeonCyan.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        startY = topPadding,
                        endY = height
                    )
                    drawPath(path = fillPath, brush = gradientBrush)

                    // 2. Draw Smooth Spline Stroke (Recharts <Line stroke="#38bdf8" />)
                    drawPath(
                        path = linePath,
                        color = NeonCyan,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // 3. Draw Active Scrubber Vertical Cursor & Highlight Dot
                    selectedIndex?.let { index ->
                        if (index in coordinates.indices) {
                            val activeCoord = coordinates[index]

                            // Vertical Scrubber Line
                            drawLine(
                                color = NeonCyan.copy(alpha = 0.6f),
                                start = Offset(activeCoord.x, 0f),
                                end = Offset(activeCoord.x, height),
                                strokeWidth = 1.5f.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )

                            // Outer Glow Halo Ring
                            drawCircle(
                                color = NeonCyan.copy(alpha = 0.35f),
                                radius = 10.dp.toPx(),
                                center = activeCoord
                            )

                            // Inner Neon Ring
                            drawCircle(
                                color = NeonCyan,
                                radius = 5.5f.dp.toPx(),
                                center = activeCoord
                            )

                            // Center White Core
                            drawCircle(
                                color = Color.White,
                                radius = 2.5f.dp.toPx(),
                                center = activeCoord
                            )
                        }
                    }
                }
            }

            // X-Axis Timeline Dates (5 equidistant markers across 30 days)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val step = (points.size - 1) / 4
                listOf(0, step, step * 2, step * 3, points.size - 1).distinct().forEach { idx ->
                    val pt = points.getOrNull(idx)
                    if (pt != null) {
                        Text(
                            text = if (idx == points.size - 1) "Hari Ini" else pt.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (idx == selectedIndex) NeonCyan else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Legend: Min, Max, and Touch Drag Hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Min: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (isBalanceVisible) PortfolioHistoryTrendCalculator.formatCompactAxis(rawMin) else "••••",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Maks: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (isBalanceVisible) PortfolioHistoryTrendCalculator.formatCompactAxis(rawMax) else "••••",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Geser untuk telusuri",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
