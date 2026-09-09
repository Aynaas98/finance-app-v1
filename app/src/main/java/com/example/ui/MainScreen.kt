package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.export.ExportFormat
import com.example.ui.dialogs.ExportBackupDialog
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AddTransactionSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RecurringTransactionsScreen
import com.example.ui.screens.SavingsGoalsScreen
import com.example.ui.screens.StockPortfolioScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.NavyCardBackground
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySlate800
import com.example.ui.theme.NavySlate900
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Beranda", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    data object Transactions : Screen("transactions", "Transaksi", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    data object Stocks : Screen("stocks", "Saham", Icons.Filled.ShowChart, Icons.Outlined.ShowChart)
    data object Accounts : Screen("accounts", "Rekening", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
    data object Goals : Screen("goals", "Target", Icons.Filled.Flag, Icons.Outlined.Flag)
    data object Recurring : Screen("recurring", "Rutin", Icons.Filled.EventRepeat, Icons.Outlined.EventRepeat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FinanceViewModel) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isAddTransactionSheetOpen by remember { mutableStateOf(false) }
    var isExportBackupDialogOpen by remember { mutableStateOf(false) }
    var exportDialogInitialFormat by remember { mutableStateOf(ExportFormat.JSON) }

    val isBalanceVisible by viewModel.isBalanceVisible.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val stockPositions by viewModel.stockPositions.collectAsStateWithLifecycle()
    val netWorthSummary by viewModel.netWorthSummary.collectAsStateWithLifecycle()
    val cashflowSummary by viewModel.cashflowSummary.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val recurringTransactions by viewModel.recurringTransactions.collectAsStateWithLifecycle()
    val monthlyTrends by viewModel.monthlyTrends.collectAsStateWithLifecycle()
    val incomeCategoryDistribution by viewModel.incomeCategoryDistribution.collectAsStateWithLifecycle()
    val expenseCategoryDistribution by viewModel.expenseCategoryDistribution.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshingStocks by viewModel.isRefreshingStocks.collectAsStateWithLifecycle()
    val stockSyncStatusMessage by viewModel.stockSyncStatusMessage.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val navigationItems = listOf(
        Screen.Dashboard,
        Screen.Transactions,
        Screen.Stocks,
        Screen.Accounts,
        Screen.Goals,
        Screen.Recurring
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    fontWeight = FontWeight.Black,
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AYNAAS Finance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NavySlate800
                        ) {
                            Text(
                                text = "v1.2",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                },
                actions = {
                    // Manual Database Backup & Export JSON Action
                    IconButton(
                        onClick = {
                            exportDialogInitialFormat = ExportFormat.JSON
                            viewModel.loadBackupSnapshot()
                            isExportBackupDialogOpen = true
                        },
                        modifier = Modifier.testTag("topbar_backup_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = "Cadangan Database JSON",
                            tint = NeonCyan
                        )
                    }

                    // Global Sensor Nominal Privacy Toggle in Top Bar
                    IconButton(
                        onClick = { viewModel.toggleBalanceVisibility() },
                        modifier = Modifier.testTag("topbar_sensor_nominal_toggle")
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isBalanceVisible) "Sensor Nominal Aktifkan" else "Sensor Nominal Nonaktifkan",
                            tint = if (isBalanceVisible) NeonCyan else TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NavyDeep
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NavySlate900,
                contentColor = TextSecondary,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav_bar")
            ) {
                navigationItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyDeep,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        },
        containerColor = NavyDeep
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        netWorthSummary = netWorthSummary,
                        cashflowSummary = cashflowSummary,
                        accounts = accounts,
                        recentTransactions = allTransactions,
                        stockPositions = stockPositions,
                        monthlyTrends = monthlyTrends,
                        incomeCategoryDistribution = incomeCategoryDistribution,
                        expenseCategoryDistribution = expenseCategoryDistribution,
                        isBalanceVisible = isBalanceVisible,
                        onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                        onOpenAddTransaction = { isAddTransactionSheetOpen = true },
                        onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                        onNavigateToStocks = { navController.navigate(Screen.Stocks.route) },
                        onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                }
                composable(Screen.Transactions.route) {
                    TransactionsScreen(
                        transactions = filteredTransactions,
                        isBalanceVisible = isBalanceVisible,
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        onFilterChange = { viewModel.setFilter(it) },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onOpenAddTransaction = { isAddTransactionSheetOpen = true },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onExportCsv = {
                            exportDialogInitialFormat = ExportFormat.CSV
                            viewModel.prepareCsvSnapshot(filteredTransactions)
                            isExportBackupDialogOpen = true
                        }
                    )
                }
                composable(Screen.Stocks.route) {
                    StockPortfolioScreen(
                        stockPositions = stockPositions,
                        isBalanceVisible = isBalanceVisible,
                        isRefreshing = isRefreshingStocks,
                        statusMessage = stockSyncStatusMessage,
                        onRefreshAll = { viewModel.refreshStockPrices() },
                        onRefreshSingle = { id -> viewModel.refreshSingleStockPrice(id) },
                        onClearStatusMessage = { viewModel.clearStockSyncMessage() },
                        onAddStock = { ticker, name, lots, avg, curr ->
                            viewModel.addStockHolding(ticker, name, lots, avg, curr)
                        },
                        onUpdatePrice = { id, price ->
                            viewModel.updateStockPrice(id, price)
                        },
                        onDeleteStock = { holding ->
                            viewModel.deleteStockHolding(holding)
                        },
                        calculateAverageDown = { lots, avg, nLots, nPrice, fee ->
                            viewModel.calculateAverageDown(lots, avg, nLots, nPrice, fee)
                        },
                        calculateDividend = { shares, price, dps ->
                            viewModel.calculateDividend(shares, price, dps)
                        }
                    )
                }
                composable(Screen.Accounts.route) {
                    AccountsScreen(
                        accounts = accounts,
                        isBalanceVisible = isBalanceVisible,
                        onAddAccount = { name, type, balance, num ->
                            viewModel.addAccount(name, type, balance, num)
                        },
                        onUpdateAccount = { acc ->
                            viewModel.updateAccount(acc)
                        },
                        onDeleteAccount = { acc ->
                            viewModel.deleteAccount(acc)
                        },
                        onOpenBackupDialog = {
                            exportDialogInitialFormat = ExportFormat.JSON
                            viewModel.loadBackupSnapshot()
                            isExportBackupDialogOpen = true
                        }
                    )
                }
                composable(Screen.Goals.route) {
                    SavingsGoalsScreen(
                        goals = savingsGoals,
                        isBalanceVisible = isBalanceVisible,
                        onAddGoal = { title, target, current, deadline, icon, color, note ->
                            viewModel.createSavingsGoal(title, target, current, deadline, icon, color, note)
                        },
                        onUpdateGoal = { goal ->
                            viewModel.updateSavingsGoal(goal)
                        },
                        onDeleteGoal = { goal ->
                            viewModel.deleteSavingsGoal(goal)
                        },
                        onAddFunds = { goalId, amount ->
                            viewModel.addFundsToGoal(goalId, amount)
                        }
                    )
                }
            }
        }
    }

    if (isExportBackupDialogOpen) {
        val context = LocalContext.current
        val backupPayload by viewModel.backupPayload.collectAsStateWithLifecycle()
        val serializedBackupJson by viewModel.serializedBackupJson.collectAsStateWithLifecycle()
        val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()
        val csvString by viewModel.serializedCsv.collectAsStateWithLifecycle()
        val csvExportStatus by viewModel.csvExportStatus.collectAsStateWithLifecycle()

        ExportBackupDialog(
            initialFormat = exportDialogInitialFormat,
            payload = backupPayload,
            jsonString = serializedBackupJson,
            exportStatus = exportStatus,
            transactions = filteredTransactions,
            csvString = csvString,
            csvExportStatus = csvExportStatus,
            onDismiss = {
                isExportBackupDialogOpen = false
                viewModel.resetExportStatus()
                viewModel.resetCsvExportStatus()
            },
            onSaveToUri = { uri, fileName ->
                viewModel.saveBackupToUri(context, uri, fileName)
            },
            onSaveToLocalStorage = {
                viewModel.saveBackupToAppLocalStorage(context)
            },
            onShare = {
                viewModel.shareBackup(context)
            },
            onCopyToClipboard = {
                viewModel.copyBackupToClipboard(context)
            },
            onReload = {
                viewModel.loadBackupSnapshot()
            },
            onPrepareCsv = {
                viewModel.prepareCsvSnapshot(filteredTransactions)
            },
            onSaveCsvToUri = { uri, fileName ->
                viewModel.saveCsvToUri(context, uri, fileName, filteredTransactions)
            },
            onSaveCsvToLocalStorage = {
                viewModel.saveCsvToAppLocalStorage(context, filteredTransactions)
            },
            onShareCsv = {
                viewModel.shareCsv(context, filteredTransactions)
            },
            onCopyCsvToClipboard = {
                viewModel.copyCsvToClipboard(context, filteredTransactions)
            }
        )
    }

    if (isAddTransactionSheetOpen) {
        AddTransactionSheet(
            sheetState = sheetState,
            accounts = accounts,
            onDismiss = { isAddTransactionSheetOpen = false },
            onSubmit = { type, amount, sourceId, destId, cat, note ->
                viewModel.addTransaction(
                    type = type,
                    amount = amount,
                    sourceAccountId = sourceId,
                    destinationAccountId = destId,
                    category = cat,
                    note = note
                )
                scope.launch {
                    sheetState.hide()
                    isAddTransactionSheetOpen = false
                }
            }
        )
    }
}
