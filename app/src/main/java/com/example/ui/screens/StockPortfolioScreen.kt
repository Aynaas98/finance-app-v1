package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.StockHoldingEntity
import com.example.domain.model.StockPosition
import com.example.domain.repository.AverageDownCalculation
import com.example.domain.repository.DividendCalculation
import com.example.ui.components.AynaasCard
import com.example.ui.components.PortfolioHistoryLineChart
import com.example.ui.components.SensorNominalText
import com.example.util.PortfolioHistoryTrendCalculator
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.NavyCardBackground
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySlate800
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter

@Composable
fun StockPortfolioScreen(
    stockPositions: List<StockPosition>,
    isBalanceVisible: Boolean,
    isRefreshing: Boolean = false,
    statusMessage: String? = null,
    onRefreshAll: () -> Unit = {},
    onRefreshSingle: ((Long) -> Unit)? = null,
    onClearStatusMessage: () -> Unit = {},
    onAddStock: (ticker: String, companyName: String, lots: Int, avgPrice: Double, currentPrice: Double) -> Unit,
    onUpdatePrice: (id: Long, newPrice: Double) -> Unit,
    onDeleteStock: (StockHoldingEntity) -> Unit,
    calculateAverageDown: ((currentLots: Int, currentAvg: Double, newLots: Int, newPrice: Double, brokerFeePercent: Double) -> AverageDownCalculation)? = null,
    calculateDividend: ((totalShares: Long, currentPrice: Double, dividendPerShare: Double) -> DividendCalculation)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showAverageDownDialog by remember { mutableStateOf(false) }
    var showDividendDialog by remember { mutableStateOf(false) }
    var stockToUpdatePrice by remember { mutableStateOf<StockPosition?>(null) }

    // Loading rotation animation for the Refresh Price button
    val infiniteTransition = rememberInfiniteTransition(label = "stock_refresh_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation_angle"
    )

    val totalMarketValue = stockPositions.sumOf { it.marketValue }
    val totalModal = stockPositions.sumOf { it.modal }
    val totalUnrealizedPL = totalMarketValue - totalModal
    val totalPLPercent = if (totalModal > 0) (totalUnrealizedPL / totalModal) * 100.0 else 0.0

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonCyan,
                contentColor = NavyCardBackground,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_stock")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Beli / Tambah Saham")
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Portfolio Summary Card with Live Yahoo Finance Refresh Action
            item {
                StockPortfolioHeroCard(
                    totalMarketValue = totalMarketValue,
                    totalModal = totalModal,
                    totalUnrealizedPL = totalUnrealizedPL,
                    totalPLPercent = totalPLPercent,
                    stockCount = stockPositions.size,
                    isBalanceVisible = isBalanceVisible,
                    isRefreshing = isRefreshing,
                    onRefreshClick = onRefreshAll
                )
            }

            // Sync Status Notification Banner (e.g. Offline fallback or Yahoo Finance update success)
            if (statusMessage != null) {
                item {
                    val isOffline = statusMessage.contains("offline", ignoreCase = true)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOffline) NavySlate800 else NeonCyan.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOffline) BorderSubtle else NeonCyan.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (isOffline) TextSecondary else NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onClearStatusMessage,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Tutup status",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Calculator Tools Row (Average Down & Dividend Yield)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAverageDownDialog = true },
                        color = NavyCardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonCyan.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Avg Down", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Kalkulator", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDividendDialog = true },
                        color = NavyCardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ProfitGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Paid, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dividen", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Yield Tracker", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // 30-Day Historical Valuation Trend Chart (Recharts / D3 inspired Monotone Spline with interactive scrubber)
            item {
                val trendPoints = remember(stockPositions) {
                    PortfolioHistoryTrendCalculator.calculate30DayTrend(stockPositions)
                }
                PortfolioHistoryLineChart(
                    points = trendPoints,
                    isBalanceVisible = isBalanceVisible
                )
            }

            // Formula Reference Card (Directly showing compliance with user prompt principles)
            item {
                StockFormulaCard()
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Kepemilikan (${stockPositions.size} Saham)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Refresh Button in header with rotation animation
                    TextButton(
                        onClick = onRefreshAll,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("btn_refresh_all_stocks")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = if (isRefreshing) "Memperbarui harga saham..." else "Perbarui Harga",
                            tint = NeonCyan,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(if (isRefreshing) rotationAngle else 0f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRefreshing) "Memperbarui..." else "Perbarui Harga",
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // List of Stocks
            if (stockPositions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = NavyCardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum Ada Portofolio Saham",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Klik tombol '+' di bawah untuk menambahkan saham perdana kamu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(stockPositions, key = { it.id }) { position ->
                    StockPositionCard(
                        position = position,
                        isBalanceVisible = isBalanceVisible,
                        isRefreshing = isRefreshing,
                        onUpdatePriceClick = { stockToUpdatePrice = position },
                        onRefreshOnlineClick = { onRefreshSingle?.invoke(position.id) },
                        onDeleteClick = { onDeleteStock(position.entity) }
                    )
                }
            }
        }
    }

    // Dialog Tambah Saham
    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { ticker, name, lots, avg, curr ->
                onAddStock(ticker, name, lots, avg, curr)
                showAddDialog = false
            }
        )
    }

    // Dialog Update Harga Terkini Manual
    stockToUpdatePrice?.let { position ->
        UpdateStockPriceDialog(
            position = position,
            onDismiss = { stockToUpdatePrice = null },
            onConfirm = { newPrice ->
                onUpdatePrice(position.id, newPrice)
                stockToUpdatePrice = null
            }
        )
    }

    // Dialog Kalkulator Average Down & Biaya Broker
    if (showAverageDownDialog) {
        AverageDownDialog(
            stocks = stockPositions,
            onDismiss = { showAverageDownDialog = false },
            onCalculate = { lots, avg, nLots, nPrice, fee ->
                calculateAverageDown?.invoke(lots, avg, nLots, nPrice, fee)
            }
        )
    }

    // Dialog Kalkulator Dividen & Dividend Yield Tracker
    if (showDividendDialog) {
        DividendTrackerDialog(
            stocks = stockPositions,
            onDismiss = { showDividendDialog = false },
            onCalculate = { shares, price, dps ->
                calculateDividend?.invoke(shares, price, dps)
            }
        )
    }
}

@Composable
private fun StockPortfolioHeroCard(
    totalMarketValue: Double,
    totalModal: Double,
    totalUnrealizedPL: Double,
    totalPLPercent: Double,
    stockCount: Int,
    isBalanceVisible: Boolean,
    isRefreshing: Boolean = false,
    onRefreshClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(NavyCardElevated, NavyCardBackground)
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TOTAL PORTOFOLIO SAHAM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val heroTransition = rememberInfiniteTransition(label = "hero_refresh_rotation")
                    val heroRotation by heroTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 900, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "hero_rotation_angle"
                    )

                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !isRefreshing,
                        modifier = Modifier.size(28.dp).testTag("hero_btn_refresh_stocks")
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = if (isRefreshing) "Sedang sinkronisasi harga..." else "Sinkronkan harga saham",
                            tint = NeonCyan,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(if (isRefreshing) heroRotation else 0f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (totalUnrealizedPL >= 0) ProfitGreenBg else ExpenseRedBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (totalUnrealizedPL >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (totalUnrealizedPL >= 0) ProfitGreen else ExpenseRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = CurrencyFormatter.formatPercentage(totalPLPercent),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (totalUnrealizedPL >= 0) ProfitGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Market Value
            SensorNominalText(
                amount = totalMarketValue,
                isBalanceVisible = isBalanceVisible,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub Stats (Modal & Unrealized P/L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = NavySlate800
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Modal Beli", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        SensorNominalText(
                            amount = totalModal,
                            isBalanceVisible = isBalanceVisible,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = NavySlate800
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Unrealized P/L", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        SensorNominalText(
                            amount = totalUnrealizedPL,
                            isBalanceVisible = isBalanceVisible,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalUnrealizedPL >= 0) ProfitGreen else ExpenseRed,
                            showSign = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockFormulaCard() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NavyCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Standar Rumus Perhitungan Saham IDX:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "• 1 Lot = 100 Lembar | Modal = Shares × Avg Price\n• Market Value = Shares × Current Price | P/L = Market - Modal",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StockPositionCard(
    position: StockPosition,
    isBalanceVisible: Boolean,
    isRefreshing: Boolean = false,
    onUpdatePriceClick: () -> Unit,
    onRefreshOnlineClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AynaasCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NavyCardBackground
    ) {
        // Ticker & Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = position.ticker,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = position.companyName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "${position.lots} Lot • ${position.totalShares} Lembar",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // P/L Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (position.isProfitable) ProfitGreenBg else ExpenseRedBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (position.isProfitable) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (position.isProfitable) ProfitGreen else ExpenseRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = CurrencyFormatter.formatPercentage(position.unrealizedPLPercentage),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (position.isProfitable) ProfitGreen else ExpenseRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Price comparison row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Avg Price", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = CurrencyFormatter.formatIdr(position.avgPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
            Column {
                Text("Current Price", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = CurrencyFormatter.formatIdr(position.currentPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Column {
                Text("Modal", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                SensorNominalText(
                    amount = position.modal,
                    isBalanceVisible = isBalanceVisible,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Market Value", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                SensorNominalText(
                    amount = position.marketValue,
                    isBalanceVisible = isBalanceVisible,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Unrealized P/L & Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NavySlate800)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Unrealized P/L: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                SensorNominalText(
                    amount = position.unrealizedPL,
                    isBalanceVisible = isBalanceVisible,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (position.isProfitable) ProfitGreen else ExpenseRed,
                    showSign = true
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val itemTransition = rememberInfiniteTransition(label = "item_refresh_rotation_${position.ticker}")
                val itemRotation by itemTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "item_rotation_${position.ticker}"
                )

                // Online sync via Yahoo Finance
                IconButton(
                    onClick = onRefreshOnlineClick,
                    enabled = !isRefreshing,
                    modifier = Modifier.size(32.dp).testTag("btn_refresh_single_${position.ticker}")
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "Sync Yahoo Finance",
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (isRefreshing) itemRotation else 0f)
                    )
                }

                // Update Price Button
                TextButton(
                    onClick = onUpdatePriceClick,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.PriceChange, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ubah", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus Saham", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AddStockDialog(
    onDismiss: () -> Unit,
    onConfirm: (ticker: String, name: String, lots: Int, avgPrice: Double, currentPrice: Double) -> Unit
) {
    var ticker by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var lotsInput by remember { mutableStateOf("") }
    var avgPriceInput by remember { mutableStateOf("") }
    var currentPriceInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCardBackground,
        title = {
            Text("Tambah Posisi Saham", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it.uppercase() },
                    label = { Text("Kode Saham (Ticker)") },
                    placeholder = { Text("Contoh: BBCA") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_ticker"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Nama Emiten / Perusahaan") },
                    placeholder = { Text("Contoh: PT Bank Central Asia Tbk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_company_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lotsInput,
                        onValueChange = { lotsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Jumlah Lot") },
                        placeholder = { Text("10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dialog_input_lots"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    val shares = (lotsInput.toIntOrNull() ?: 0) * 100
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavySlate800)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "= $shares lbr",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }

                OutlinedTextField(
                    value = avgPriceInput,
                    onValueChange = { avgPriceInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Beli Rata-Rata (Avg/lbr)") },
                    placeholder = { Text("Contoh: 9800") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_avg_price"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = currentPriceInput,
                    onValueChange = { currentPriceInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Pasar Terkini (Current/lbr)") },
                    placeholder = { Text("Contoh: 10450") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_current_price"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let {
                        Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t = ticker.trim()
                    val lots = lotsInput.toIntOrNull() ?: 0
                    val avg = avgPriceInput.toDoubleOrNull() ?: 0.0
                    val curr = currentPriceInput.toDoubleOrNull() ?: avg

                    if (t.isBlank()) {
                        errorMessage = "Kode saham (ticker) tidak boleh kosong"
                        return@Button
                    }
                    if (lots <= 0) {
                        errorMessage = "Jumlah lot harus lebih dari 0"
                        return@Button
                    }
                    if (avg <= 0.0) {
                        errorMessage = "Harga beli rata-rata harus valid"
                        return@Button
                    }

                    onConfirm(t, companyName.ifBlank { t }, lots, avg, curr)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = NavyDeep),
                modifier = Modifier.testTag("dialog_btn_submit_stock")
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun UpdateStockPriceDialog(
    position: StockPosition,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var priceInput by remember { mutableStateOf(position.currentPrice.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCardBackground,
        title = {
            Text("Ubah Harga ${position.ticker}", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Harga Pasar Terkini per Lembar Saham (Rp):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_update_stock_price"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                val newPrice = priceInput.toDoubleOrNull() ?: position.currentPrice
                val newMarket = position.totalShares * newPrice
                val newPL = newMarket - position.modal
                val newPLPercent = if (position.modal > 0) (newPL / position.modal) * 100.0 else 0.0

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NavySlate800,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Simulasi P/L:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = "${CurrencyFormatter.formatIdr(newPL)} (${CurrencyFormatter.formatPercentage(newPLPercent)})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (newPL >= 0) ProfitGreen else ExpenseRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceInput.toDoubleOrNull() ?: position.currentPrice
                    onConfirm(p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = NavyDeep),
                modifier = Modifier.testTag("dialog_btn_confirm_update_price")
            ) {
                Text("Perbarui", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AverageDownDialog(
    stocks: List<StockPosition>,
    onDismiss: () -> Unit,
    onCalculate: ((Int, Double, Int, Double, Double) -> AverageDownCalculation?)? = null
) {
    var selectedStock by remember { mutableStateOf(stocks.firstOrNull()) }
    var currentLotsInput by remember { mutableStateOf(selectedStock?.lots?.toString() ?: "10") }
    var currentAvgInput by remember { mutableStateOf(selectedStock?.avgPrice?.toInt()?.toString() ?: "10000") }
    var newLotsInput by remember { mutableStateOf("5") }
    var newPriceInput by remember { mutableStateOf((selectedStock?.currentPrice?.toInt() ?: 9500).toString()) }
    var brokerFeeInput by remember { mutableStateOf("0.15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kalkulator Average Down", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (stocks.isNotEmpty()) {
                    Text("Pilih Saham Portofolio:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stocks.take(4).forEach { pos ->
                            val isSelected = selectedStock?.id == pos.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else NavySlate800,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonCyan else BorderSubtle
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedStock = pos
                                        currentLotsInput = pos.lots.toString()
                                        currentAvgInput = pos.avgPrice.toInt().toString()
                                        newPriceInput = pos.currentPrice.toInt().toString()
                                    }
                            ) {
                                Text(
                                    text = pos.ticker,
                                    color = if (isSelected) NeonCyan else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentLotsInput,
                        onValueChange = { currentLotsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Lot Saat Ini") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = currentAvgInput,
                        onValueChange = { currentAvgInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Avg Price (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newLotsInput,
                        onValueChange = { newLotsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Beli Lot Baru") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = newPriceInput,
                        onValueChange = { newPriceInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Baru (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = brokerFeeInput,
                    onValueChange = { brokerFeeInput = it },
                    label = { Text("Fee Broker Beli (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Calculation Result Preview
                val curLots = currentLotsInput.toIntOrNull() ?: 0
                val curAvg = currentAvgInput.toDoubleOrNull() ?: 0.0
                val nLots = newLotsInput.toIntOrNull() ?: 0
                val nPrice = newPriceInput.toDoubleOrNull() ?: 0.0
                val feePct = brokerFeeInput.toDoubleOrNull() ?: 0.15

                val calc = onCalculate?.invoke(curLots, curAvg, nLots, nPrice, feePct)

                if (calc != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NavySlate800,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rata-rata Baru:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatIdr(calc.newAveragePrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Modal Tambahan:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatIdr(calc.newTotalCost),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Lot Baru:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${calc.finalLots} Lot (${calc.finalShares} lembar)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = NavyDeep)
            ) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DividendTrackerDialog(
    stocks: List<StockPosition>,
    onDismiss: () -> Unit,
    onCalculate: ((Long, Double, Double) -> DividendCalculation?)? = null
) {
    var selectedStock by remember { mutableStateOf(stocks.firstOrNull()) }
    var sharesInput by remember { mutableStateOf(selectedStock?.totalShares?.toString() ?: "1000") }
    var priceInput by remember { mutableStateOf(selectedStock?.currentPrice?.toInt()?.toString() ?: "5000") }
    var dpsInput by remember { mutableStateOf("250") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Paid, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulasi Dividen & Yield", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (stocks.isNotEmpty()) {
                    Text("Pilih Saham Portofolio:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stocks.take(4).forEach { pos ->
                            val isSelected = selectedStock?.id == pos.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ProfitGreen.copy(alpha = 0.2f) else NavySlate800,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ProfitGreen else BorderSubtle
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedStock = pos
                                        sharesInput = pos.totalShares.toString()
                                        priceInput = pos.currentPrice.toInt().toString()
                                    }
                            ) {
                                Text(
                                    text = pos.ticker,
                                    color = if (isSelected) ProfitGreen else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = sharesInput,
                    onValueChange = { sharesInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Lembar Saham") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfitGreen,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Pasar (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfitGreen,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = dpsInput,
                        onValueChange = { dpsInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Dividen/Lembar (DPS Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfitGreen,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // Calculation Result Preview
                val shares = sharesInput.toLongOrNull() ?: 0L
                val price = priceInput.toDoubleOrNull() ?: 0.0
                val dps = dpsInput.toDoubleOrNull() ?: 0.0

                val calc = onCalculate?.invoke(shares, price, dps)

                if (calc != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NavySlate800,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimasi Dividen:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatIdr(calc.totalDividend),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dividend Yield:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatPercentage(calc.dividendYieldPercentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen, contentColor = NavyDeep)
            ) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        }
    )
}
