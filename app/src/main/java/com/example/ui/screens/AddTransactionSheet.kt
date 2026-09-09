package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.AccountEntity
import com.example.domain.model.TransactionType
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.NavyCardBackground
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavySlate800
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TransferBlue
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    sheetState: SheetState,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSubmit: (
        type: TransactionType,
        amount: Double,
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        category: String,
        note: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by remember { mutableStateOf("") }
    var selectedSourceAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull()) }
    var selectedDestinationAccount by remember(accounts) {
        mutableStateOf(accounts.getOrNull(1) ?: accounts.firstOrNull())
    }
    var selectedCategory by remember { mutableStateOf("Makanan & Minuman") }
    var noteInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val incomeCategories = listOf("Gaji", "Bonus", "Dividen Saham", "Hasil Usaha", "Hadiah", "Investasi", "Lain-lain")
    val expenseCategories = listOf("Makanan & Minuman", "Belanja", "Transportasi", "Tagihan & Utilitas", "Hiburan", "Kesehatan", "Pendidikan", "Donasi", "Lain-lain")
    val transferCategories = listOf("Top Up RDN Saham", "Top Up E-Wallet", "Pindah Tabungan", "Tarik Tunai", "Transfer Antar Rekening")

    val categories = when (selectedType) {
        TransactionType.INCOME -> incomeCategories
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.TRANSFER -> transferCategories
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NavyCardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tambah Transaksi Baru",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_add_transaction_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Type Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavySlate800)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TypePill(
                    label = "Pengeluaran",
                    isSelected = selectedType == TransactionType.EXPENSE,
                    activeColor = ExpenseRed,
                    icon = Icons.Default.ArrowDownward,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        selectedCategory = expenseCategories.first()
                        errorMessage = null
                    }
                )
                TypePill(
                    label = "Pemasukan",
                    isSelected = selectedType == TransactionType.INCOME,
                    activeColor = ProfitGreen,
                    icon = Icons.Default.ArrowUpward,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        selectedCategory = incomeCategories.first()
                        errorMessage = null
                    }
                )
                TypePill(
                    label = "Transfer",
                    isSelected = selectedType == TransactionType.TRANSFER,
                    activeColor = TransferBlue,
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    onClick = {
                        selectedType = TransactionType.TRANSFER
                        selectedCategory = transferCategories.first()
                        errorMessage = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nominal Input
            Text(
                text = "Nominal (Rp)",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amountInput,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    amountInput = clean
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_transaction_amount"),
                placeholder = { Text("0", color = TextMuted) },
                prefix = {
                    Text(
                        text = "Rp ",
                        fontWeight = FontWeight.Bold,
                        color = when (selectedType) {
                            TransactionType.INCOME -> ProfitGreen
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.TRANSFER -> TransferBlue
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = NavyCardElevated,
                    unfocusedContainerColor = NavyCardElevated
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account Selectors based on Type
            when (selectedType) {
                TransactionType.INCOME -> {
                    AccountDropdown(
                        label = "Masuk ke Rekening Tujuan",
                        accounts = accounts,
                        selectedAccount = selectedDestinationAccount,
                        onSelect = { selectedDestinationAccount = it }
                    )
                }
                TransactionType.EXPENSE -> {
                    AccountDropdown(
                        label = "Sumber Rekening Pembayaran",
                        accounts = accounts,
                        selectedAccount = selectedSourceAccount,
                        onSelect = { selectedSourceAccount = it }
                    )
                }
                TransactionType.TRANSFER -> {
                    Column {
                        AccountDropdown(
                            label = "Dari Rekening (Asal)",
                            accounts = accounts,
                            selectedAccount = selectedSourceAccount,
                            onSelect = {
                                selectedSourceAccount = it
                                errorMessage = null
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AccountDropdown(
                            label = "Ke Rekening (Tujuan)",
                            accounts = accounts,
                            selectedAccount = selectedDestinationAccount,
                            onSelect = {
                                selectedDestinationAccount = it
                                errorMessage = null
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        // Info Card regarding double counting prevention
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NavyCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TransferBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Transfer antar-rekening tidak akan dihitung sebagai pengeluaran/pemasukan bulanan (bebas double counting).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips
            Text(
                text = "Kategori Transaksi",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = cat == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCatSelected) NeonCyan.copy(alpha = 0.2f) else NavyCardElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCatSelected) NeonCyan else BorderSubtle
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedCategory = cat }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCatSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCatSelected) NeonCyan else TextSecondary,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Catatan / Note
            Text(
                text = "Catatan (Opsional)",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_transaction_note"),
                placeholder = { Text("Contoh: Makan siang, Top up RDN saham", color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = NavyCardElevated,
                    unfocusedContainerColor = NavyCardElevated
                )
            )

            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        errorMessage = "Masukkan nominal transaksi yang valid (> 0)"
                        return@Button
                    }
                    if (selectedType == TransactionType.TRANSFER && selectedSourceAccount?.id == selectedDestinationAccount?.id) {
                        errorMessage = "Rekening asal dan tujuan tidak boleh sama!"
                        return@Button
                    }
                    onSubmit(
                        selectedType,
                        amt,
                        if (selectedType == TransactionType.INCOME) null else selectedSourceAccount?.id,
                        if (selectedType == TransactionType.EXPENSE) null else selectedDestinationAccount?.id,
                        selectedCategory,
                        noteInput.ifBlank { selectedCategory }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_transaction_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedType) {
                        TransactionType.INCOME -> ProfitGreen
                        TransactionType.EXPENSE -> NeonCyan
                        TransactionType.TRANSFER -> TransferBlue
                    },
                    contentColor = NavyCardBackground
                )
            ) {
                Text(
                    text = "Simpan Transaksi",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TypePill(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) activeColor else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NavyCardBackground else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) NavyCardBackground else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<AccountEntity>,
    selectedAccount: AccountEntity?,
    onSelect: (AccountEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedAccount?.let { "${it.name} (${CurrencyFormatter.formatIdr(it.balance)})" } ?: "Pilih Rekening",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = NavyCardElevated,
                    unfocusedContainerColor = NavyCardElevated
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(NavyCardElevated)
            ) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(acc.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text(CurrencyFormatter.formatIdr(acc.balance), color = NeonCyan)
                            }
                        },
                        onClick = {
                            onSelect(acc)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
