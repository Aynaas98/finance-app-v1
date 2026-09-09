package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AynaasDatabase
import com.example.domain.repository.AynaasRepository
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AynaasDatabase.getInstance(applicationContext)
        val repository = AynaasRepository(
            accountDao = database.accountDao(),
            transactionDao = database.transactionDao(),
            stockHoldingDao = database.stockHoldingDao(),
            savingsGoalDao = database.savingsGoalDao(),
            recurringTransactionDao = database.recurringTransactionDao()
        )

        setContent {
            MyApplicationTheme {
                val viewModel: FinanceViewModel = viewModel(
                    factory = FinanceViewModelFactory(repository)
                )
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
