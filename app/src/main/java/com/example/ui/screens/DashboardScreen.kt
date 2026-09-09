package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AccountEntity
import com.example.domain.model.CashflowSummary
import com.example.domain.model.NetWorthSummary
import com.example.domain.model.StockPosition
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.ui.components.AynaasCard
import com.example.ui.components.CategoryDistributionChartCard
import com.example.ui.components.MonthlyTrendChartCard
import com.example.ui.components.SensorNominalText
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBg
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.NavyCardBackground
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySlate800
import com.example.ui.theme.NavySlate900
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.theme.ProfitGreenLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.TransferBlueBg
import com.example.util.CategoryDistributionItem
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.util.MonthlyTrendPoint

@Composable
fun DashboardScreen(
    netWorthSummary: NetWorthSummary,
    cashflowSummary: CashflowSummary,
    accounts: List<AccountEntity>,
    recentTransactions: List<TransactionItem>,
    stockPositions: List<StockPosition>,
    monthlyTrends: List<MonthlyTrendPoint>,
    incomeCategoryDistribution: List<CategoryDistributionItem>,
    expenseCategoryDistribution: List<CategoryDistributionItem>,
    isBalanceVisible: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToStocks: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen_scroll"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Hero Net Worth Card
        item {
            NetWorthHeroCard(
                netWorthSummary = netWorthSummary,
                isBalanceVisible = isBalanceVisible,
                onToggleBalanceVisibility = onToggleBalanceVisibility,
                onOpenAddTransaction = onOpenAddTransaction,
                onNavigateToStocks = onNavigateToStocks
            )
        }

        // 2. Monthly Cashflow Section (With Double Counting Protection notice)
        item {
            CashflowSummarySection(
                cashflow = cashflowSummary,
                isBalanceVisible = isBalanceVisible
            )
        }

        // 2.5 Recharts / D3 Inspired Monthly Spending & Income Trends Chart
        item {
            MonthlyTrendChartCard(
                trends = monthlyTrends,
                isBalanceVisible = isBalanceVisible,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 2.6 Recharts / D3 Inspired Category Distribution Chart
        item {
            CategoryDistributionChartCard(
                incomeCategories = incomeCategoryDistribution,
                expenseCategories = expenseCategoryDistribution,
                isBalanceVisible = isBalanceVisible,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 3. Accounts / Wallets Carousel
        item {
            AccountsCarouselSection(
                accounts = accounts,
                isBalanceVisible = isBalanceVisible,
                onNavigateToAccounts = onNavigateToAccounts
            )
        }

        // 4. Quick Stock Portfolio Snapshot
        item {
            StockPortfolioSnapshotSection(
                stockPositions = stockPositions,
                isBalanceVisible = isBalanceVisible,
                onNavigateToStocks = onNavigateToStocks
            )
        }

        // 5. Recent Transactions
        item {
            RecentTransactionsSection(
                transactions = recentTransactions.take(5),
                isBalanceVisible = isBalanceVisible,
                onNavigateToTransactions = onNavigateToTransactions,
                onDeleteTransaction = onDeleteTransaction
            )
        }
    }
}

@Composable
private fun NetWorthHeroCard(
    netWorthSummary: NetWorthSummary,
    isBalanceVisible: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    onNavigateToStocks: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("net_worth_hero_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCardElevated),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.6f), AccentIndigo.copy(alpha = 0.3f)))
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NavyCardElevated,
                            NavyCardBackground
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TOTAL KEKAYAAN BERSIH",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "AYNAAS Finance v1.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }

                    // Sensor nominal privacy toggle button
                    IconButton(
                        onClick = onToggleBalanceVisibility,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("toggle_sensor_nominal_btn")
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isBalanceVisible) "Sembunyikan Saldo (Sensor Aktif)" else "Tampilkan Saldo",
                            tint = if (isBalanceVisible) NeonCyan else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Net Worth Amount
                SensorNominalText(
                    amount = netWorthSummary.totalNetWorth,
                    isBalanceVisible = isBalanceVisible,
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maskText = "Rp ••••••••••"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown chips (Kas/Bank & Portofolio Saham)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Kas & Bank chip
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = NavySlate800,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Kas & Rekening",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            SensorNominalText(
                                amount = netWorthSummary.totalCashAndBank,
                                isBalanceVisible = isBalanceVisible,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Saham chip with Unrealized P/L
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToStocks),
                        shape = RoundedCornerShape(12.dp),
                        color = NavySlate800,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Portofolio Saham",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                val isProfitable = netWorthSummary.totalStockPL >= 0
                                Text(
                                    text = CurrencyFormatter.formatPercentage(netWorthSummary.totalStockPLPercentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isProfitable) ProfitGreen else ExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            SensorNominalText(
                                amount = netWorthSummary.totalStockMarketValue,
                                isBalanceVisible = isBalanceVisible,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ElevatedButton(
                        onClick = onOpenAddTransaction,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("hero_add_transaction_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = NeonCyan,
                            contentColor = NavyDeep
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Catat Transaksi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToStocks,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                            .testTag("hero_invest_stock_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentIndigo.copy(alpha = 0.25f),
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Saham", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun CashflowSummarySection(
    cashflow: CashflowSummary,
    isBalanceVisible: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Arus Kas Bulanan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        AynaasCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NavyCardBackground
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ProfitGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = ProfitGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pemasukan",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SensorNominalText(
                        amount = cashflow.totalIncome,
                        isBalanceVisible = isBalanceVisible,
                        style = MaterialTheme.typography.titleMedium,
                        color = ProfitGreenLight,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Expense
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ExpenseRedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pengeluaran",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SensorNominalText(
                        amount = cashflow.totalExpense,
                        isBalanceVisible = isBalanceVisible,
                        style = MaterialTheme.typography.titleMedium,
                        color = ExpenseRedLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Net Cashflow Row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NavySlate800,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Cashflow (Bersih):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    val isNetPositive = cashflow.netCashflow >= 0
                    SensorNominalText(
                        amount = cashflow.netCashflow,
                        isBalanceVisible = isBalanceVisible,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isNetPositive) ProfitGreen else ExpenseRed,
                        fontWeight = FontWeight.Bold,
                        showSign = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Double counting safety disclaimer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = null,
                    tint = TransferBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Transfer antar rekening tidak dihitung double counting",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun AccountsCarouselSection(
    accounts: List<AccountEntity>,
    isBalanceVisible: Boolean,
    onNavigateToAccounts: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rekening & Dompet (${accounts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            TextButton(onClick = onNavigateToAccounts) {
                Text("Kelola", color = NeonCyan, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts, key = { it.id }) { acc ->
                AccountCardItem(
                    account = acc,
                    isBalanceVisible = isBalanceVisible
                )
            }
        }
    }
}

@Composable
private fun AccountCardItem(
    account: AccountEntity,
    isBalanceVisible: Boolean
) {
    val typeIcon = when (account.type) {
        "INVESTMENT_RDN" -> Icons.Default.ShowChart
        "E_WALLET" -> Icons.Default.AccountBalanceWallet
        "CASH" -> Icons.Default.MonetizationOn
        else -> Icons.Default.AccountBalance
    }

    Surface(
        modifier = Modifier
            .width(185.dp)
            .height(115.dp),
        shape = RoundedCornerShape(16.dp),
        color = NavyCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = if (account.accountNumber.isNotBlank()) account.accountNumber else account.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                SensorNominalText(
                    amount = account.balance,
                    isBalanceVisible = isBalanceVisible,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun StockPortfolioSnapshotSection(
    stockPositions: List<StockPosition>,
    isBalanceVisible: Boolean,
    onNavigateToStocks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Investasi Saham (IDX)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            TextButton(onClick = onNavigateToStocks) {
                Text("Lihat Portofolio", color = NeonCyan, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
        }

        AynaasCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NavyCardBackground
        ) {
            val totalMarket = stockPositions.sumOf { it.marketValue }
            val totalModal = stockPositions.sumOf { it.modal }
            val totalPL = totalMarket - totalModal
            val totalPLPercent = if (totalModal > 0) (totalPL / totalModal) * 100.0 else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nilai Pasar Portofolio",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    SensorNominalText(
                        amount = totalMarket,
                        isBalanceVisible = isBalanceVisible,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (totalPL >= 0) ProfitGreenBg else ExpenseRedBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (totalPL >= 0) Icons.Default.TrendingUp else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (totalPL >= 0) ProfitGreen else ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = CurrencyFormatter.formatPercentage(totalPLPercent),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalPL >= 0) ProfitGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock list preview (top 3 holdings)
            stockPositions.take(3).forEach { stock ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NavySlate800,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = stock.ticker,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        Text(
                            text = "${stock.lots} Lot (${stock.totalShares} lbr)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SensorNominalText(
                            amount = stock.marketValue,
                            isBalanceVisible = isBalanceVisible,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = CurrencyFormatter.formatPercentage(stock.unrealizedPLPercentage),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (stock.isProfitable) ProfitGreen else ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    transactions: List<TransactionItem>,
    isBalanceVisible: Boolean,
    onNavigateToTransactions: () -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaksi Terakhir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            TextButton(onClick = onNavigateToTransactions) {
                Text("Semua", color = NeonCyan, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
        }

        if (transactions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NavyCardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Belum ada transaksi tercatat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                transactions.forEach { txn ->
                    TransactionRowCard(
                        transaction = txn,
                        isBalanceVisible = isBalanceVisible,
                        onDelete = { onDeleteTransaction(txn.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRowCard(
    transaction: TransactionItem,
    isBalanceVisible: Boolean,
    onDelete: (() -> Unit)? = null
) {
    val (icon, badgeBg, badgeTint, sign, amountColor) = when (transaction.type) {
        TransactionType.INCOME -> Tuple5(Icons.Default.ArrowUpward, ProfitGreenBg, ProfitGreen, "+", ProfitGreen)
        TransactionType.EXPENSE -> Tuple5(Icons.Default.ArrowDownward, ExpenseRedBg, ExpenseRed, "-", ExpenseRed)
        TransactionType.TRANSFER -> Tuple5(Icons.AutoMirrored.Filled.CompareArrows, TransferBlueBg, TransferBlue, "⇄", TransferBlue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NavyCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val accountDesc = when (transaction.type) {
                        TransactionType.INCOME -> "Masuk: ${transaction.destinationAccountName ?: "Rekening"}"
                        TransactionType.EXPENSE -> "Dari: ${transaction.sourceAccountName ?: "Rekening"}"
                        TransactionType.TRANSFER -> "${transaction.sourceAccountName ?: "Asal"} → ${transaction.destinationAccountName ?: "Tujuan"}"
                    }
                    Text(
                        text = "$accountDesc • ${DateFormatter.formatDate(transaction.dateMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    if (transaction.note.isNotBlank() && transaction.note != transaction.category) {
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SensorNominalText(
                    amount = transaction.amount,
                    isBalanceVisible = isBalanceVisible,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    prefix = "$sign "
                )

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Transaksi",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
